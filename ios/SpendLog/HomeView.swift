import SwiftUI
import Charts

struct HomeView: View {
    @EnvironmentObject var store: Store
    @Environment(\.colorScheme) private var scheme
    @Binding var path: [Destination]
    @Binding var addRequest: AddRequest?

    @State private var month = Date()
    @State private var query = ""
    @State private var filter: String? = nil
    @State private var editing: Expense? = nil

    private var stats: MonthStats { Stats.compute(store.expenses, month: month) }
    private var isCurrent: Bool { DateKey.ym(month) == DateKey.ym(Date()) }
    private var dark: Bool { scheme == .dark }
    private var accent: Color { store.accent(dark: dark) }

    private var filtered: [Expense] {
        let ym = DateKey.ym(month)
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        let list = store.expenses.filter { e in
            guard e.date.hasPrefix(ym) else { return false }
            if let f = filter, e.category != f { return false }
            if q.isEmpty { return true }
            return e.note.lowercased().contains(q)
                || e.category.lowercased().contains(q)
                || Money.plain(e.cents).contains(q)
        }
        return list.sorted { a, b in
            a.date == b.date ? a.createdAt > b.createdAt : a.date > b.date
        }
    }

    private var grouped: [(date: String, items: [Expense])] {
        let dict = Dictionary(grouping: filtered, by: { $0.date })
        return dict.keys.sorted(by: >).map { (date: $0, items: dict[$0] ?? []) }
    }

    var body: some View {
        List {
            Section { summaryCard }
            if stats.count > 0 {
                Section("Each day") { chartCard }
                Section("Top spending") { topCategories }
            }
            if let f = filter {
                Section {
                    Button {
                        filter = nil
                    } label: {
                        Label("Showing \(f) only. Tap to clear", systemImage: "xmark.circle.fill")
                    }
                }
            }
            if grouped.isEmpty {
                Section {
                    Text(stats.count == 0 ? "Nothing logged this month yet." : "No matches.")
                        .foregroundStyle(.secondary)
                }
            }
            ForEach(grouped, id: \.date) { group in
                Section {
                    ForEach(group.items) { e in
                        row(e)
                    }
                } header: {
                    HStack {
                        Text(DateKey.dayLabel(group.date))
                        Spacer()
                        Text(Money.format(group.items.reduce(0) { $0 + $1.cents }))
                            .monospacedDigit()
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Spend Log")
        .navigationBarTitleDisplayMode(.inline)
        .searchable(text: $query, prompt: "Search notes and categories")
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Menu {
                    Button { path.append(.budgets) } label: { Label("Budgets", systemImage: "target") }
                    Button { path.append(.goals) } label: { Label("Savings goals", systemImage: "flag") }
                    Button { path.append(.recurring) } label: { Label("Recurring", systemImage: "repeat") }
                    Button { path.append(.categories) } label: { Label("Categories", systemImage: "tag") }
                    Button { path.append(.autoAdd) } label: { Label("Auto-add payments", systemImage: "wave.3.right") }
                    Button { path.append(.settings) } label: { Label("Settings", systemImage: "gearshape") }
                } label: {
                    Image(systemName: "line.3.horizontal")
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    addRequest = AddRequest(category: nil)
                } label: {
                    Image(systemName: "plus.circle.fill")
                }
            }
        }
        .sheet(item: $editing) { e in
            ExpenseFormView(existing: e, presetCategory: nil)
                .environmentObject(store)
        }
    }

    // MARK: - Pieces

    private var summaryCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Button { shift(-1) } label: { Image(systemName: "chevron.left") }
                    .buttonStyle(.borderless)
                Spacer()
                Text(DateKey.monthLabel(month)).font(.headline)
                Spacer()
                Button { shift(1) } label: { Image(systemName: "chevron.right") }
                    .buttonStyle(.borderless)
            }
            Text(Money.format(stats.total))
                .font(.system(size: 38, weight: .bold, design: .rounded))
                .foregroundStyle(accent)
                .monospacedDigit()
            Text(compareText).font(.subheadline).foregroundStyle(.secondary)
            if isCurrent {
                HStack {
                    stat("Today", Money.format(stats.todayTotal))
                    Spacer()
                    stat("Daily average", Money.format(stats.dailyAvg))
                }
            }
            budgetBlock
        }
        .padding(.vertical, 4)
    }

    private var compareText: String {
        if stats.prevTotal == 0 { return "\(stats.count) entries" }
        let diff = stats.total - stats.prevTotal
        let word = diff >= 0 ? "more" : "less"
        return "\(Money.format(abs(diff))) \(word) than last month"
    }

    private func stat(_ title: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title).font(.caption).foregroundStyle(.secondary)
            Text(value).font(.subheadline.weight(.semibold)).monospacedDigit()
        }
    }

    @ViewBuilder private var budgetBlock: some View {
        let budget = store.monthlyBudget
        if budget > 0 {
            let ratio = Double(stats.total) / Double(budget)
            let color = BudgetColor.gradient(ratio, dark: dark)
            let left = budget - stats.total
            VStack(alignment: .leading, spacing: 6) {
                GeometryReader { g in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Color.secondary.opacity(0.2))
                        Capsule().fill(color)
                            .frame(width: g.size.width * CGFloat(min(1.0, max(0.0, ratio))))
                    }
                }
                .frame(height: 12)
                HStack {
                    Text("\(Int((ratio * 100).rounded()))% of \(Money.format(budget))")
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text(left >= 0 ? "\(Money.format(left)) left" : "Over by \(Money.format(-left))")
                        .foregroundStyle(color)
                }
                .font(.caption)
                if isCurrent && left > 0 {
                    Text("About \(Money.format(left / DateKey.daysLeftInMonth())) a day for the rest of the month")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        } else {
            Button("Set a monthly budget") { path.append(.budgets) }
                .font(.subheadline)
        }
    }

    private var chartCard: some View {
        Chart(Array(stats.byDay.enumerated()), id: \.offset) { item in
            BarMark(
                x: .value("Day", item.offset + 1),
                y: .value("Spent", Double(item.element) / 100.0)
            )
            .foregroundStyle(accent)
        }
        .frame(height: 140)
    }

    private var topCategories: some View {
        let sorted = stats.byCategory.sorted { $0.value > $1.value }
        return ForEach(Array(sorted.prefix(5)), id: \.key) { pair in
            Button {
                filter = pair.key
            } label: {
                topRow(name: pair.key, amount: pair.value)
            }
            .buttonStyle(.plain)
        }
    }

    private func topRow(name: String, amount: Int) -> some View {
        let pct = stats.total > 0 ? Int((Double(amount) / Double(stats.total) * 100).rounded()) : 0
        let limit = store.categoryBudgets[name] ?? 0
        return VStack(alignment: .leading, spacing: 4) {
            HStack {
                Circle().fill(store.categoryColor(name)).frame(width: 10, height: 10)
                Text(name)
                Spacer()
                Text(Money.format(amount)).monospacedDigit()
                Text("\(pct)%").font(.caption).foregroundStyle(.secondary)
            }
            if limit > 0 {
                let r = Double(amount) / Double(limit)
                ProgressView(value: min(1.0, r))
                    .tint(BudgetColor.gradient(r, dark: dark))
                Text("\(Money.format(amount)) of \(Money.format(limit))")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private func row(_ e: Expense) -> some View {
        HStack {
            Circle().fill(store.categoryColor(e.category)).frame(width: 10, height: 10)
            VStack(alignment: .leading, spacing: 2) {
                Text(e.note.isEmpty ? e.category : e.note)
                Text(subtitle(e)).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            Text(Money.format(e.cents)).monospacedDigit()
        }
        .contentShape(Rectangle())
        .onTapGesture { editing = e }
        .swipeActions {
            Button(role: .destructive) {
                store.delete(e.id)
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
    }

    private func subtitle(_ e: Expense) -> String {
        switch e.source {
        case "wallet": return "\(e.category) · Auto-added"
        case "recurring": return "\(e.category) · Recurring"
        default: return e.category
        }
    }

    private func shift(_ n: Int) {
        if let d = Calendar.current.date(byAdding: .month, value: n, to: month) {
            month = d
            filter = nil
        }
    }
}
