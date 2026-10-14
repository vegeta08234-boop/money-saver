import Foundation

struct Expense: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var cents: Int
    var category: String
    var date: String            // yyyy-MM-dd
    var note: String = ""
    var source: String = ""     // "" (typed in), "wallet" (auto-added) or "recurring"
    var createdAt: Double = Date().timeIntervalSince1970
}

struct Category: Identifiable, Codable, Equatable, Hashable {
    var name: String
    var colorHex: String
    var id: String { name }
}

struct Goal: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var name: String
    var target: Int
    var saved: Int
}

struct RecurringItem: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var cents: Int
    var category: String
    var note: String
    var day: Int
    var last: String            // yyyy-MM of the last month already added
}

struct AppSettings: Codable, Equatable {
    var themeIndex: Int = 0
    var tint: Bool = true
    var lockOn: Bool = false
    var reminderOn: Bool = false
    var reminderHour: Int = 20
    var reminderMinute: Int = 0
    var autoCategory: String = "Other"
}
