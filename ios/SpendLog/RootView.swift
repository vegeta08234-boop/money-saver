import SwiftUI

enum Destination: String, Hashable {
    case budgets, categories, recurring, goals, settings, autoAdd
}

struct AddRequest: Identifiable {
    let id = UUID()
    var category: String?
}

struct RootView: View {
    @EnvironmentObject var store: Store
    @Environment(\.colorScheme) private var scheme
    @State private var path: [Destination]
    @State private var addRequest: AddRequest?

    init() {
        var initial: [Destination] = []
        var add: AddRequest? = nil
        #if targetEnvironment(simulator)
        if let s = UserDefaults.standard.string(forKey: "startDestination") {
            if s == "add" {
                add = AddRequest(category: nil)
            } else if let d = Destination(rawValue: s) {
                initial = [d]
            }
        }
        #endif
        _path = State(initialValue: initial)
        _addRequest = State(initialValue: add)
    }

    var body: some View {
        NavigationStack(path: $path) {
            HomeView(path: $path, addRequest: $addRequest)
                .navigationDestination(for: Destination.self) { d in
                    switch d {
                    case .budgets: BudgetsView()
                    case .categories: CategoriesView()
                    case .recurring: RecurringView()
                    case .goals: GoalsView()
                    case .settings: SettingsView(path: $path)
                    case .autoAdd: AutoAddView()
                    }
                }
        }
        .sheet(item: $addRequest) { req in
            ExpenseFormView(existing: nil, presetCategory: req.category)
                .environmentObject(store)
        }
        .onOpenURL { url in
            guard url.scheme == "spendlog", url.host == "add" else { return }
            let items = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems ?? []
            let cat = items.first(where: { $0.name == "category" })?.value
            path = []
            addRequest = AddRequest(category: cat)
        }
        .tint(store.accent(dark: scheme == .dark))
    }
}
