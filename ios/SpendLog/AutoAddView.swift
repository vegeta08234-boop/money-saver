import SwiftUI

struct AutoAddView: View {
    @EnvironmentObject var store: Store
    @Environment(\.openURL) private var openURL

    private var categoryBinding: Binding<String> {
        Binding(
            get: { store.settings.autoCategory },
            set: { v in
                var s = store.settings
                s.autoCategory = v
                store.updateSettings(s)
            }
        )
    }

    var body: some View {
        Form {
            Section {
                Text("iPhone doesn't let one app read another app's notifications, so Spend Log can't watch for payments the way it does on Android. Instead, a Shortcuts automation can tell Spend Log about each Apple Pay payment the moment it happens.")
                    .font(.subheadline)
            }
            Section("Set it up once") {
                step(1, "Open the Shortcuts app and go to the Automation tab.")
                step(2, "Tap +, choose Transaction (Wallet).")
                step(3, "Pick the cards to watch, then tap Next.")
                step(4, "Add the action Log Expense (from Spend Log).")
                step(5, "Tap Amount and choose the Amount variable from the Transaction. Tap Merchant and choose Merchant.")
                step(6, "Set it to Run Immediately and turn off Notify When Run if you like.")
            }
            Section {
                Picker("Category when unsure", selection: categoryBinding) {
                    ForEach(store.categories) { c in
                        Text(c.name).tag(c.name)
                    }
                }
            } footer: {
                Text("Spend Log picks a category from the shop name when it recognises one (coffee shops, grocers, fuel and so on) and uses this one otherwise. Payments added this way are marked Auto-added.")
            }
            Section {
                Button("Open Shortcuts") {
                    if let url = URL(string: "shortcuts://") { openURL(url) }
                }
            }
        }
        .navigationTitle("Auto-add payments")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func step(_ n: Int, _ text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Text("\(n)")
                .font(.caption.bold())
                .frame(width: 22, height: 22)
                .background(Circle().fill(Color.secondary.opacity(0.2)))
            Text(text).font(.subheadline)
        }
    }
}
