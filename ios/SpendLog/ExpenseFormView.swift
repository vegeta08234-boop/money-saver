import SwiftUI

struct ExpenseFormView: View {
    @EnvironmentObject var store: Store
    @Environment(\.dismiss) private var dismiss

    let existing: Expense?
    @State private var amount: String
    @State private var category: String
    @State private var date: Date
    @State private var note: String
    @FocusState private var amountFocused: Bool

    init(existing: Expense?, presetCategory: String?) {
        self.existing = existing
        let names = Store.shared.categories.map { $0.name }
        _amount = State(initialValue: existing.map { Money.plain($0.cents) } ?? "")
        var cat = existing?.category ?? presetCategory ?? names.first ?? "Other"
        if let p = presetCategory, existing == nil, !names.contains(p) { cat = names.first ?? "Other" }
        _category = State(initialValue: cat)
        _date = State(initialValue: existing.map { DateKey.date($0.date) } ?? Date())
        _note = State(initialValue: existing?.note ?? "")
    }

    private var cents: Int? {
        if let c = Money.parse(amount), c > 0 { return c }
        return nil
    }

    private var categoryNames: [String] {
        var names = store.categories.map { $0.name }
        if !names.contains(category) { names.append(category) }
        return names
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("0.00", text: $amount)
                        .keyboardType(.decimalPad)
                        .font(.system(size: 32, weight: .bold, design: .rounded))
                        .focused($amountFocused)
                }
                Section {
                    Picker("Category", selection: $category) {
                        ForEach(categoryNames, id: \.self) { n in
                            Text(n).tag(n)
                        }
                    }
                    DatePicker("Date", selection: $date, in: ...Date(), displayedComponents: .date)
                    TextField("Note (optional)", text: $note)
                }
                if let e = existing {
                    Section {
                        Button("Delete entry", role: .destructive) {
                            store.delete(e.id)
                            dismiss()
                        }
                    }
                }
            }
            .navigationTitle(existing == nil ? "Add expense" : "Edit expense")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }.disabled(cents == nil)
                }
            }
            .onAppear {
                if existing == nil { amountFocused = true }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func save() {
        guard let c = cents else { return }
        let key = DateKey.key(date)
        let trimmed = note.trimmingCharacters(in: .whitespacesAndNewlines)
        if var e = existing {
            e.cents = c
            e.category = category
            e.date = key
            e.note = trimmed
            store.update(e)
        } else {
            store.add(Expense(cents: c, category: category, date: key, note: trimmed))
        }
        dismiss()
    }
}
