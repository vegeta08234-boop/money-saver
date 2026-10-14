import SwiftUI

struct BudgetsView: View {
    @EnvironmentObject var store: Store
    @Environment(\.dismiss) private var dismiss
    @State private var monthly = ""
    @State private var perCategory: [String: String] = [:]
    @State private var loaded = false

    var body: some View {
        Form {
            Section {
                HStack {
                    Text("Per month")
                    TextField("None", text: $monthly)
                        .keyboardType(.decimalPad)
                        .multilineTextAlignment(.trailing)
                }
            } header: {
                Text("Monthly budget")
            } footer: {
                Text("The bar, the app color and the widget shift from green to amber to red as you use it up.")
            }
            Section {
                ForEach(store.categories) { c in
                    HStack {
                        Circle().fill(Color(hexString: c.colorHex)).frame(width: 10, height: 10)
                        Text(c.name)
                        TextField("None", text: binding(for: c.name))
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                    }
                }
            } header: {
                Text("Category budgets")
            } footer: {
                Text("Leave a field empty for no limit.")
            }
        }
        .navigationTitle("Budgets")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { save() }
            }
        }
        .onAppear {
            guard !loaded else { return }
            loaded = true
            monthly = store.monthlyBudget > 0 ? Money.plain(store.monthlyBudget) : ""
            var d: [String: String] = [:]
            for (k, v) in store.categoryBudgets where v > 0 {
                d[k] = Money.plain(v)
            }
            perCategory = d
        }
    }

    private func binding(for name: String) -> Binding<String> {
        Binding(
            get: { perCategory[name] ?? "" },
            set: { perCategory[name] = $0 }
        )
    }

    private func save() {
        store.setMonthlyBudget(Money.parse(monthly) ?? 0)
        for c in store.categories {
            store.setCategoryBudget(c.name, cents: Money.parse(perCategory[c.name] ?? "") ?? 0)
        }
        dismiss()
    }
}
