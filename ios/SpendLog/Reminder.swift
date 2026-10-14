import Foundation
import UserNotifications

/// A daily nudge that is skipped on days you have already logged something.
/// iOS cannot decide that at delivery time, so the next 14 days are scheduled and refreshed whenever the app opens.
enum Reminder {
    private static let ids: [String] = (0..<14).map { "reminder-\($0)" }

    static func refresh(store: Store) async {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: ids)

        let s = store.settings
        guard s.reminderOn else { return }

        let granted = (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
        guard granted else { return }

        let cal = Calendar.current
        let loggedToday = store.expenses.contains { $0.date == DateKey.today() }

        for i in 0..<14 {
            if i == 0 && loggedToday { continue }
            guard let day = cal.date(byAdding: .day, value: i, to: Date()) else { continue }
            var c = cal.dateComponents([.year, .month, .day], from: day)
            c.hour = s.reminderHour
            c.minute = s.reminderMinute
            guard let fire = cal.date(from: c), fire > Date() else { continue }

            let content = UNMutableNotificationContent()
            content.title = "Spend Log"
            content.body = "Anything to log today?"
            content.sound = .default
            let trigger = UNCalendarNotificationTrigger(dateMatching: c, repeats: false)
            let request = UNNotificationRequest(identifier: ids[i], content: content, trigger: trigger)
            try? await center.add(request)
        }
    }
}
