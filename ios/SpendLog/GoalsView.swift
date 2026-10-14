import SwiftUI

struct GoalsView: View {
    @EnvironmentObject var store: Store
    @State private var editing: Goal? = nil
    @State private var adding = false

    var body: some View {
        List {
            if store.goals.isEmpty {
                Text("Set something to save for and track how close you are.")
                    .foregroundStyle(.secondary)
            }
            ForEach(store.goals) { g in
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text(g.name).font(.headline)
                        Spacer()
                        Text("\(Money.format(g.saved)) of \(Money.format(g.target))")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    ProgressView(value: g.target > 0 ? min(1.0, Double(g.saved) / Double(g.target)) : 0)
                }
                .padding(.vertical, 4)
                .contentShape(Rectangle())
                .onTapGesture { editing = g }
                .swipeActions {
                    Button(role: .destructive) {
                        store.deleteGoal(g.id)
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
            }
        }
        .navigationTitle("Savings goals")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button { adding = true } label: { Image(systemName: "plus") }
            }
        }
        .sheet(isPresented: $adding) {
            GoalEditView(existing: nil).environmentObject(store)
        }
        .sheet(item: $editing) { g in
            GoalEditView(existing: g).environmentObject(store)
        }
    }
}

struct GoalEditView: View {
    @EnvironmentObject var store: Store
    @Environment(\.dismiss) private var dismiss
    let existing: Goal?
    @State private var name: String
    @State private var target: String
    @State private var saved: String

    init(existing: Goal?) {
        self.existing = existing
        _name = State(initialValue: existing?.name ?? "")
        _target = State(initialValue: existing.map { Money.plain($0.target) } ?? "")
        _saved = State(initialValue: existing.map { Money.plain($0.saved) } ?? "")
    }

    private var targetCents: Int? {
        if let c = Money.parse(target), c > 0 { return c }
        return nil
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Goal (e.g. Trip fund)", text: $name)
                    TextField("Target amount", text: $target).keyboardType(.decimalPad)
                    TextField("Saved so far", text: $saved).keyboardType(.decimalPad)
                }
            }
            .navigationTitle(existing == nil ? "New goal" : "Edit goal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }
                        .disabled(targetCents == nil || name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func save() {
        guard let t = targetCents else { return }
        let s = Money.parse(saved) ?? 0
        let n = name.trimmingCharacters(in: .whitespacesAndNewlines)
        if var g = existing {
            g.name = n
            g.target = t
            g.saved = s
            store.upsertGoal(g)
        } else {
            store.upsertGoal(Goal(name: n, target: t, saved: s))
        }
        dismiss()
    }
}
