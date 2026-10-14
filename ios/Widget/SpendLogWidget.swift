import WidgetKit
import SwiftUI

struct SpendEntry: TimelineEntry {
    let date: Date
    let today: Int
    let month: Int
    let budget: Int
    let settings: AppSettings
    let chips: [Category]
}

struct SpendProvider: TimelineProvider {
    func placeholder(in context: Context) -> SpendEntry {
        SpendEntry(date: Date(), today: 1250, month: 24500, budget: 60000,
                   settings: AppSettings(), chips: Array(Store.defaultCategories.prefix(4)))
    }

    func getSnapshot(in context: Context, completion: @escaping (SpendEntry) -> Void) {
        completion(makeEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SpendEntry>) -> Void) {
        let entry = makeEntry()
        let nextMidnight = Calendar.current.nextDate(
            after: Date(),
            matching: DateComponents(hour: 0, minute: 1),
            matchingPolicy: .nextTime
        ) ?? Date().addingTimeInterval(3600)
        completion(Timeline(entries: [entry], policy: .after(nextMidnight)))
    }

    private func makeEntry() -> SpendEntry {
        let store = Store()
        let now = Date()
        let s = Stats.compute(store.expenses, month: now, now: now)
        return SpendEntry(date: now, today: s.todayTotal, month: s.total, budget: store.monthlyBudget,
                          settings: store.settings, chips: Array(store.categories.prefix(4)))
    }
}

struct SpendWidgetView: View {
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var scheme
    let entry: SpendEntry

    private var ratio: Double? {
        entry.budget > 0 ? Double(entry.month) / Double(entry.budget) : nil
    }

    private var accent: Color {
        Color(uiColor: Theme.accentUI(settings: entry.settings, ratio: ratio, dark: scheme == .dark))
    }

    var body: some View {
        Group {
            if family == .systemSmall {
                content.widgetURL(URL(string: "spendlog://add"))
            } else {
                content
            }
        }
        .containerBackground(Color(uiColor: .secondarySystemBackground), for: .widget)
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 0) {
                    Text("This month").font(.caption).foregroundStyle(.secondary)
                    Text(Money.format(entry.month))
                        .font(.system(size: family == .systemSmall ? 24 : 30, weight: .bold, design: .rounded))
                        .foregroundStyle(accent)
                        .minimumScaleFactor(0.6)
                        .lineLimit(1)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 0) {
                    Text("Today").font(.caption).foregroundStyle(.secondary)
                    Text(Money.format(entry.today))
                        .font(.subheadline.weight(.semibold))
                        .minimumScaleFactor(0.6)
                        .lineLimit(1)
                }
            }
            budgetBar
            if family != .systemSmall {
                Spacer(minLength: 2)
                HStack(spacing: 6) {
                    ForEach(entry.chips) { c in
                        Link(destination: chipURL(c)) {
                            Text(c.name)
                                .font(.caption.weight(.semibold))
                                .lineLimit(1)
                                .minimumScaleFactor(0.7)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 7)
                                .background(Capsule().fill(Color(hexString: c.colorHex).opacity(0.25)))
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder private var budgetBar: some View {
        if let r = ratio {
            let color = BudgetColor.gradient(r, dark: scheme == .dark)
            let left = entry.budget - entry.month
            VStack(alignment: .leading, spacing: 3) {
                GeometryReader { g in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Color.secondary.opacity(0.25))
                        Capsule().fill(color)
                            .frame(width: g.size.width * CGFloat(min(1.0, max(0.0, r))))
                    }
                }
                .frame(height: 8)
                Text(left >= 0 ? "\(Money.format(left)) left" : "Over by \(Money.format(-left))")
                    .font(.caption2)
                    .foregroundStyle(color)
            }
        } else {
            Text("Set a budget in the app")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    private func chipURL(_ c: Category) -> URL {
        var comps = URLComponents()
        comps.scheme = "spendlog"
        comps.host = "add"
        comps.queryItems = [URLQueryItem(name: "category", value: c.name)]
        return comps.url ?? URL(string: "spendlog://add")!
    }
}

struct SpendLogWidget: Widget {
    let kind = "SpendLogWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SpendProvider()) { entry in
            SpendWidgetView(entry: entry)
        }
        .configurationDisplayName("Spend Log")
        .description("This month's spending, your budget, and quick add.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

@main
struct SpendLogWidgetBundle: WidgetBundle {
    var body: some Widget {
        SpendLogWidget()
    }
}
