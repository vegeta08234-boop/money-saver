import Foundation
import SwiftUI
import WidgetKit

/// All app data lives here, saved as JSON in a shared App Group so the widget and the app see the same thing.
final class Store: ObservableObject {
    static let groupID = "group.com.spendlog.app"
    static let shared = Store()

    @Published private(set) var expenses: [Expense] = []
    @Published private(set) var categories: [Category] = []
    @Published private(set) var goals: [Goal] = []
    @Published private(set) var recurring: [RecurringItem] = []
    @Published private(set) var monthlyBudget: Int = 0
    @Published private(set) var categoryBudgets: [String: Int] = [:]
    @Published private(set) var settings = AppSettings()

    private let defaults: UserDefaults

    static let defaultCategories: [Category] = [
        Category(name: "Food", colorHex: "F97316"),
        Category(name: "Groceries", colorHex: "22C55E"),
        Category(name: "Transport", colorHex: "06B6D4"),
        Category(name: "Bills", colorHex: "6366F1"),
        Category(name: "Shopping", colorHex: "EC4899"),
        Category(name: "Health", colorHex: "F43F5E"),
        Category(name: "Fun", colorHex: "A855F7"),
        Category(name: "Other", colorHex: "64748B")
    ]

    init() {
        defaults = UserDefaults(suiteName: Store.groupID) ?? UserDefaults.standard
        reload()
    }

    // MARK: - Loading and saving

    func reload() {
        expenses = read("expenses", as: [Expense].self) ?? []
        categories = read("categories", as: [Category].self) ?? Store.defaultCategories
        goals = read("goals", as: [Goal].self) ?? []
        recurring = read("recurring", as: [RecurringItem].self) ?? []
        monthlyBudget = defaults.integer(forKey: "monthlyBudget")
        categoryBudgets = read("categoryBudgets", as: [String: Int].self) ?? [:]
        settings = read("settings", as: AppSettings.self) ?? AppSettings()
    }

    private func read<T: Decodable>(_ key: String, as type: T.Type) -> T? {
        guard let data = defaults.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(T.self, from: data)
    }

    private func save<T: Encodable>(_ value: T, key: String) {
        if let data = try? JSONEncoder().encode(value) {
            defaults.set(data, forKey: key)
        }
    }

    private func changed() {
        WidgetCenter.shared.reloadAllTimelines()
    }

    // MARK: - Expenses

    func add(_ e: Expense) {
        expenses.append(e)
        save(expenses, key: "expenses")
        changed()
    }

    func update(_ e: Expense) {
        guard let i = expenses.firstIndex(where: { $0.id == e.id }) else { return }
        expenses[i] = e
        save(expenses, key: "expenses")
        changed()
    }

    func delete(_ id: UUID) {
        expenses.removeAll { $0.id == id }
        save(expenses, key: "expenses")
        changed()
    }

    // MARK: - Categories

    func categoryColor(_ name: String) -> Color {
        if let c = categories.first(where: { $0.name == name }) {
            return Color(hexString: c.colorHex)
        }
        return Color(hexString: "94A3B8")
    }

    func addCategory(name: String, colorHex: String) {
        let n = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !n.isEmpty, !categories.contains(where: { $0.name.caseInsensitiveCompare(n) == .orderedSame }) else { return }
        categories.append(Category(name: n, colorHex: colorHex))
        save(categories, key: "categories")
        changed()
    }

    func updateCategory(old: String, name: String, colorHex: String) {
        let n = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !n.isEmpty, let i = categories.firstIndex(where: { $0.name == old }) else { return }
        if n != old, categories.contains(where: { $0.name.caseInsensitiveCompare(n) == .orderedSame }) { return }
        categories[i] = Category(name: n, colorHex: colorHex)
        save(categories, key: "categories")
        if n != old { renameEverywhere(from: old, to: n, moveBudget: true) }
        changed()
    }

    /// Removes a category; its entries move to `moveTo`.
    func deleteCategory(_ name: String, moveTo: String) {
        guard categories.count > 1 else { return }
        categories.removeAll { $0.name == name }
        save(categories, key: "categories")
        renameEverywhere(from: name, to: moveTo, moveBudget: false)
        categoryBudgets[name] = nil
        save(categoryBudgets, key: "categoryBudgets")
        changed()
    }

    private func renameEverywhere(from old: String, to new: String, moveBudget: Bool) {
        for i in expenses.indices where expenses[i].category == old {
            expenses[i].category = new
        }
        save(expenses, key: "expenses")
        for i in recurring.indices where recurring[i].category == old {
            recurring[i].category = new
        }
        save(recurring, key: "recurring")
        if moveBudget, let b = categoryBudgets[old] {
            categoryBudgets[old] = nil
            categoryBudgets[new] = b
            save(categoryBudgets, key: "categoryBudgets")
        }
        if settings.autoCategory == old {
            settings.autoCategory = new
            save(settings, key: "settings")
        }
    }

    // MARK: - Budgets

    func setMonthlyBudget(_ cents: Int) {
        monthlyBudget = max(0, cents)
        defaults.set(monthlyBudget, forKey: "monthlyBudget")
        changed()
    }

    func setCategoryBudget(_ name: String, cents: Int) {
        if cents <= 0 {
            categoryBudgets[name] = nil
        } else {
            categoryBudgets[name] = cents
        }
        save(categoryBudgets, key: "categoryBudgets")
        changed()
    }

    // MARK: - Goals

    func upsertGoal(_ g: Goal) {
        if let i = goals.firstIndex(where: { $0.id == g.id }) {
            goals[i] = g
        } else {
            goals.append(g)
        }
        save(goals, key: "goals")
    }

    func deleteGoal(_ id: UUID) {
        goals.removeAll { $0.id == id }
        save(goals, key: "goals")
    }

    // MARK: - Recurring

    /// A new item starts "caught up": it is first added next time its day comes around.
    func initialLast(day: Int, now: Date = Date()) -> String {
        let ym = DateKey.ym(now)
        return DateKey.dayOfMonth(now) >= day ? ym : DateKey.shiftYM(ym, by: -1)
    }

    func upsertRecurring(_ r: RecurringItem) {
        if let i = recurring.firstIndex(where: { $0.id == r.id }) {
            recurring[i] = r
        } else {
            recurring.append(r)
        }
        save(recurring, key: "recurring")
    }

    func deleteRecurring(_ id: UUID) {
        recurring.removeAll { $0.id == id }
        save(recurring, key: "recurring")
    }

    /// Adds this month's entry for every recurring item whose day has arrived.
    func processRecurring(now: Date = Date()) {
        let ym = DateKey.ym(now)
        let today = DateKey.dayOfMonth(now)
        let days = DateKey.daysInMonth(now)
        var added = false
        for i in recurring.indices {
            let r = recurring[i]
            let due = min(r.day, days)
            if r.last < ym && today >= due {
                let date = String(format: "%@-%02d", ym, due)
                expenses.append(Expense(cents: r.cents, category: r.category, date: date, note: r.note, source: "recurring"))
                recurring[i].last = ym
                added = true
            }
        }
        if added {
            save(expenses, key: "expenses")
            save(recurring, key: "recurring")
            changed()
        }
    }

    // MARK: - Settings

    func updateSettings(_ s: AppSettings) {
        settings = s
        save(settings, key: "settings")
        changed()
    }

    func accent(dark: Bool) -> Color {
        Theme.accent(store: self, dark: dark)
    }

    // MARK: - Demo data (simulator only, used for screenshots)

    func seedDemoIfRequested() {
        #if targetEnvironment(simulator)
        guard UserDefaults.standard.bool(forKey: "seedDemo") else { return }
        let now = Date()
        let cal = Calendar.current
        var list: [Expense] = []
        let samples: [(Int, String, String)] = [
            (450, "Food", "Coffee"), (6820, "Groceries", "Weekly shop"), (1250, "Transport", "Bus pass"),
            (3999, "Shopping", "Headphones"), (1599, "Fun", "Movie night"), (2400, "Food", "Lunch"),
            (9500, "Bills", "Phone"), (780, "Health", "Pharmacy"), (1830, "Groceries", "Market")
        ]
        let todayDay = DateKey.dayOfMonth(now)
        for (i, s) in samples.enumerated() {
            let back = min(i * 2, max(0, todayDay - 1))
            if let d = cal.date(byAdding: .day, value: -back, to: now) {
                list.append(Expense(cents: s.0, category: s.1, date: DateKey.key(d), note: s.2))
            }
        }
        expenses = list
        categories = Store.defaultCategories
        monthlyBudget = 60000
        categoryBudgets = ["Food": 15000, "Groceries": 25000]
        goals = [Goal(name: "Trip fund", target: 120000, saved: 45000)]
        recurring = [RecurringItem(cents: 1599, category: "Bills", note: "Streaming", day: 15, last: DateKey.ym(now))]
        save(expenses, key: "expenses")
        save(categories, key: "categories")
        defaults.set(monthlyBudget, forKey: "monthlyBudget")
        save(categoryBudgets, key: "categoryBudgets")
        save(goals, key: "goals")
        save(recurring, key: "recurring")
        changed()
        #endif
    }
}
