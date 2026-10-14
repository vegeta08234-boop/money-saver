import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var store: Store
    @Environment(\.colorScheme) private var scheme
    @Binding var path: [Destination]

    private func bind<T>(_ kp: WritableKeyPath<AppSettings, T>) -> Binding<T> {
        Binding(
            get: { store.settings[keyPath: kp] },
            set: { newValue in
                var s = store.settings
                s[keyPath: kp] = newValue
                store.updateSettings(s)
            }
        )
    }

    private var reminderTime: Binding<Date> {
        Binding(
            get: {
                var c = DateComponents()
                c.hour = store.settings.reminderHour
                c.minute = store.settings.reminderMinute
                return Calendar.current.date(from: c) ?? Date()
            },
            set: { d in
                let c = Calendar.current.dateComponents([.hour, .minute], from: d)
                var s = store.settings
                s.reminderHour = c.hour ?? 20
                s.reminderMinute = c.minute ?? 0
                store.updateSettings(s)
            }
        )
    }

    var body: some View {
        Form {
            Section("Color") {
                HStack(spacing: 14) {
                    ForEach(Array(Theme.names.enumerated()), id: \.offset) { i, name in
                        Button {
                            var s = store.settings
                            s.themeIndex = i
                            store.updateSettings(s)
                        } label: {
                            VStack(spacing: 4) {
                                Circle()
                                    .fill(Color(uiColor: Theme.baseUI(index: i, dark: scheme == .dark)))
                                    .frame(width: 36, height: 36)
                                    .overlay {
                                        if store.settings.themeIndex == i {
                                            Image(systemName: "checkmark").foregroundStyle(.white).bold()
                                        }
                                    }
                                Text(name).font(.caption2).foregroundStyle(.secondary)
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
                .frame(maxWidth: .infinity)
                Toggle("Shift color as budget runs out", isOn: bind(\.tint))
            }
            Section {
                Toggle("Daily reminder", isOn: bind(\.reminderOn))
                if store.settings.reminderOn {
                    DatePicker("Time", selection: reminderTime, displayedComponents: .hourAndMinute)
                }
            } footer: {
                Text("Skipped on days you have already logged something.")
            }
            Section {
                Toggle("Lock with Face ID or passcode", isOn: bind(\.lockOn))
            }
            Section {
                Button { path.append(.autoAdd) } label: {
                    Label("Auto-add payments", systemImage: "wave.3.right")
                }
            }
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: store.settings.reminderOn) { _, _ in
            Task { await Reminder.refresh(store: store) }
        }
        .onChange(of: store.settings.reminderHour) { _, _ in
            Task { await Reminder.refresh(store: store) }
        }
        .onChange(of: store.settings.reminderMinute) { _, _ in
            Task { await Reminder.refresh(store: store) }
        }
    }
}
