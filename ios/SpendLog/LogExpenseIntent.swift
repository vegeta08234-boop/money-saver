import AppIntents
import Foundation

/// Used by a Shortcuts automation (Wallet "Transaction" trigger) to log a payment the moment you tap to pay.
struct LogExpenseIntent: AppIntent {
    static let title: LocalizedStringResource = "Log Expense"
    static let description = IntentDescription("Adds an expense to Spend Log.")
    static let openAppWhenRun: Bool = false

    @Parameter(title: "Amount")
    var amount: String

    @Parameter(title: "Merchant")
    var merchant: String?

    static var parameterSummary: some ParameterSummary {
        Summary("Log \(\.$amount) at \(\.$merchant)")
    }

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        guard let cents = Money.parse(amount), cents > 0 else {
            return .result(dialog: "Couldn't read that amount, so nothing was logged.")
        }
        let store = Store()
        let shop = (merchant ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        let now = Date().timeIntervalSince1970

        // The same payment can trigger twice; ignore a repeat within a minute.
        let duplicate = store.expenses.contains {
            $0.source == "wallet" && $0.cents == cents && $0.note == shop && now - $0.createdAt < 60
        }
        if duplicate {
            return .result(dialog: "Already logged.")
        }

        let names = store.categories.map { $0.name }
        let category = CategoryGuess.pick(names: names, merchant: shop, fallback: store.settings.autoCategory)
        store.add(Expense(cents: cents, category: category, date: DateKey.today(), note: shop, source: "wallet"))
        return .result(dialog: "Logged \(Money.format(cents)).")
    }
}

struct SpendLogShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: LogExpenseIntent(),
            phrases: ["Log an expense in \(.applicationName)"],
            shortTitle: "Log Expense",
            systemImageName: "plus.circle"
        )
    }
}
