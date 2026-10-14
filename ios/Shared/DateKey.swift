import Foundation

/// Dates are stored as "yyyy-MM-dd" strings so grouping by day or month is a prefix match.
enum DateKey {
    private static var cal: Calendar { Calendar.autoupdatingCurrent }

    static func key(_ d: Date) -> String {
        let c = cal.dateComponents([.year, .month, .day], from: d)
        return String(format: "%04d-%02d-%02d", c.year ?? 1970, c.month ?? 1, c.day ?? 1)
    }

    static func today() -> String { key(Date()) }

    static func ym(_ d: Date) -> String { String(key(d).prefix(7)) }

    static func dayOfMonth(_ d: Date) -> Int { cal.component(.day, from: d) }

    static func date(_ key: String) -> Date {
        let p = key.split(separator: "-").compactMap { Int($0) }
        guard p.count >= 3 else { return Date() }
        var c = DateComponents()
        c.year = p[0]
        c.month = p[1]
        c.day = p[2]
        c.hour = 12
        return cal.date(from: c) ?? Date()
    }

    static func daysInMonth(_ d: Date) -> Int {
        cal.range(of: .day, in: .month, for: d)?.count ?? 30
    }

    /// Moves a "yyyy-MM" string by a number of months.
    static func shiftYM(_ ym: String, by delta: Int) -> String {
        let p = ym.split(separator: "-").compactMap { Int($0) }
        guard p.count >= 2 else { return ym }
        let total = p[0] * 12 + (p[1] - 1) + delta
        return String(format: "%04d-%02d", total / 12, total % 12 + 1)
    }

    static func daysLeftInMonth(_ now: Date = Date()) -> Int {
        daysInMonth(now) - dayOfMonth(now) + 1
    }

    private static let dayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.setLocalizedDateFormatFromTemplate("EEE MMM d")
        return f
    }()

    private static let monthFormatter: DateFormatter = {
        let f = DateFormatter()
        f.setLocalizedDateFormatFromTemplate("LLLL yyyy")
        return f
    }()

    static func dayLabel(_ key: String) -> String {
        dayFormatter.string(from: date(key))
    }

    static func monthLabel(_ d: Date) -> String {
        monthFormatter.string(from: d)
    }
}
