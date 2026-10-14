import SwiftUI

struct RecurringView: View {
    @EnvironmentObject var store: Store
    @State private var editing: RecurringItem? = nil
    @State private var adding = false

    var body: some View {
        List {
            if store.recurring.isEmpty {
                Text("Rent, subscriptions and other bills that repeat each month. They are added on their day automatically.")
                    .foregroundStyle(.secondary)
            }
            ForEach(store.recurring) { r in
                HStack {
                    Circle().fill(store.categoryColor(r.category)).frame(width: 10, height: 10)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(r.note.isEmpty ? r.category : r.note)
                        Text("\(r.category) · day \(r.day) of each month")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Text(Money.format(r.cents)).monospacedDigit()
                }
                .contentShape(Rectangle())
                .onTapGesture { editing = r }
                .swipeActions {
                    Button(role: .destructive) {
                        store.deleteRecurring(r.id)
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
            }
        }
        .navigationTitle("Recurring")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button { adding = true } label: { Image(systemName: "plus") }
            }
        }
        .sheet(isPresented: $adding) {
            RecurringEditView(existing: nil).environmentObject(store)
        }
        .sheet(item: $editing) { r in
            RecurringEditView(existing: r).environmentObject(store)
        }
    }
}

struct RecurringEditView: View {
    @EnvironmentObject var store: Store
    @Environment(\.dismiss) private var dismiss
    let existing: RecurringItem?
    @State private var amount: String
    @State private var category: String
    @State private var note: String
    @State private var day: Int

    init(existing: RecurringItem?) {
        self.existing = existing
        _amount = State(initialValue: existing.map { Money.plain($0.cents) } ?? "")
        _category = State(initialValue: existing?.category ?? Store.shared.categories.first?.name ?? "Other")
        _note = State(initialValue: existing?.note ?? "")
        _day = State(initialValue: existing?.day ?? 1)
    }

    private var cents: Int? {
        if let c = Money.parse(amount), c > 0 { return c }
        return nil
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Amount", text: $amount).keyboardType(.decimalPad)
                    TextField("Name (e.g. Rent)", text: $note)
                    Picker("Category", selection: $category) {
                        ForEach(store.categories) { c in
                            Text(c.name).tag(c.name)
                        }
                    }
                    Stepper("Day of month: \(day)", value: $day, in: 1...31)
                }
            }
            .navigationTitle(existing == nil ? "New recurring" : "Edit recurring")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }.disabled(cents == nil)
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func save() {
        guard let c = cents else { return }
        let trimmed = note.trimmingCharacters(in: .whitespacesAndNewlines)
        if var r = existing {
            let dayChanged = r.day != day
            r.cents = c
            r.category = category
            r.note = trimmed
            r.day = day
            if dayChanged { r.last = store.initialLast(day: day) }
            store.upsertRecurring(r)
        } else {
            store.upsertRecurring(RecurringItem(cents: c, category: category, note: trimmed, day: day, last: store.initialLast(day: day)))
        }
        dismiss()
    }
}
