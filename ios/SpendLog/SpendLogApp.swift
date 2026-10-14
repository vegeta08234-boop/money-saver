import SwiftUI
import LocalAuthentication

@main
struct SpendLogApp: App {
    @StateObject private var store = Store.shared
    @Environment(\.scenePhase) private var phase
    @State private var locked = Store.shared.settings.lockOn

    init() {
        Store.shared.seedDemoIfRequested()
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                RootView()
                if locked {
                    LockView(locked: $locked)
                        .transition(.opacity)
                }
            }
            .environmentObject(store)
            .onAppear { refreshEverything() }
            .onChange(of: phase) { _, newPhase in
                switch newPhase {
                case .active:
                    refreshEverything()
                case .background:
                    if store.settings.lockOn { locked = true }
                default:
                    break
                }
            }
        }
    }

    private func refreshEverything() {
        store.reload()
        store.processRecurring()
        Task { await Reminder.refresh(store: store) }
    }
}

struct LockView: View {
    @Binding var locked: Bool
    @Environment(\.scenePhase) private var phase
    @State private var busy = false

    var body: some View {
        ZStack {
            Rectangle().fill(.background).ignoresSafeArea()
            VStack(spacing: 16) {
                Image(systemName: "lock.fill").font(.system(size: 44))
                Text("Spend Log is locked").font(.headline)
                Button("Unlock") { authenticate() }
                    .buttonStyle(.borderedProminent)
            }
        }
        .onAppear { if phase == .active { authenticate() } }
        .onChange(of: phase) { _, newPhase in
            if newPhase == .active { authenticate() }
        }
    }

    private func authenticate() {
        if busy { return }
        let ctx = LAContext()
        var err: NSError?
        guard ctx.canEvaluatePolicy(.deviceOwnerAuthentication, error: &err) else {
            // No passcode on this device, so there is nothing to check against.
            locked = false
            return
        }
        busy = true
        ctx.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: "Unlock Spend Log") { ok, _ in
            DispatchQueue.main.async {
                busy = false
                if ok { locked = false }
            }
        }
    }
}
