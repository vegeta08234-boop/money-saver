import SwiftUI

struct CategoriesView: View {
    @EnvironmentObject var store: Store
    @State private var editing: Category? = nil
    @State private var adding = false

    var body: some View {
        List {
            Section {
                ForEach(store.categories) { c in
                    HStack {
                        Circle().fill(Color(hexString: c.colorHex)).frame(width: 14, height: 14)
                        Text(c.name)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture { editing = c }
                    .swipeActions {
                        if store.categories.count > 1 {
                            Button(role: .destructive) {
                                store.deleteCategory(c.name, moveTo: fallback(for: c.name))
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                }
            } footer: {
                Text("Deleting a category moves its entries to \"\(fallback(for: ""))\".")
            }
        }
        .navigationTitle("Categories")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button { adding = true } label: { Image(systemName: "plus") }
            }
        }
        .sheet(isPresented: $adding) {
            CategoryEditView(existing: nil).environmentObject(store)
        }
        .sheet(item: $editing) { c in
            CategoryEditView(existing: c).environmentObject(store)
        }
    }

    /// Where entries go when a category is deleted.
    private func fallback(for name: String) -> String {
        let auto = store.settings.autoCategory
        if auto != name, store.categories.contains(where: { $0.name == auto }) { return auto }
        return store.categories.first(where: { $0.name != name })?.name ?? "Other"
    }
}

struct CategoryEditView: View {
    @EnvironmentObject var store: Store
    @Environment(\.dismiss) private var dismiss
    let existing: Category?
    @State private var name: String
    @State private var colorHex: String

    init(existing: Category?) {
        self.existing = existing
        _name = State(initialValue: existing?.name ?? "")
        _colorHex = State(initialValue: existing?.colorHex ?? Theme.categoryColors[0])
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Name", text: $name)
                }
                Section("Color") {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 5), spacing: 14) {
                        ForEach(Theme.categoryColors, id: \.self) { hex in
                            Button {
                                colorHex = hex
                            } label: {
                                Circle()
                                    .fill(Color(hexString: hex))
                                    .frame(width: 36, height: 36)
                                    .overlay {
                                        if hex == colorHex {
                                            Image(systemName: "checkmark").foregroundStyle(.white).bold()
                                        }
                                    }
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.vertical, 6)
                }
            }
            .navigationTitle(existing == nil ? "New category" : "Edit category")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        if let e = existing {
                            store.updateCategory(old: e.name, name: name, colorHex: colorHex)
                        } else {
                            store.addCategory(name: name, colorHex: colorHex)
                        }
                        dismiss()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
        .presentationDetents([.medium])
    }
}
