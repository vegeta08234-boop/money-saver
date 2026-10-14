import Foundation

struct MonthStats {
    var total = 0
    var prevTotal = 0
    var todayTotal = 0
    var dailyAvg = 0
    var count = 0
    var byDay: [Int] = []
    var byCategory: [String: Int] = [:]
    var byDate: [String: Int] = [:]
    var biggest: Expense? = nil
}

enum Stats {
    static func compute(_ all: [Expense], month: Date, now: Date = Date()) -> MonthStats {
        var s = MonthStats()
        let ym = DateKey.ym(month)
        let prevYM = DateKey.shiftYM(ym, by: -1)
        let days = DateKey.daysInMonth(month)
        s.byDay = Array(repeating: 0, count: days)
        let today = DateKey.key(now)

        for e in all {
            if e.date == today { s.todayTotal += e.cents }
            if e.date.hasPrefix(prevYM) { s.prevTotal += e.cents }
            if e.date.hasPrefix(ym) {
                s.total += e.cents
                s.count += 1
                if let big = s.biggest {
                    if e.cents > big.cents { s.biggest = e }
                } else {
                    s.biggest = e
                }
                if let d = Int(e.date.suffix(2)), d >= 1, d <= days {
                    s.byDay[d - 1] += e.cents
                }
                s.byCategory[e.category, default: 0] += e.cents
                s.byDate[e.date, default: 0] += e.cents
            }
        }

        let nowYM = DateKey.ym(now)
        let elapsed: Int
        if ym == nowYM {
            elapsed = DateKey.dayOfMonth(now)
        } else if ym < nowYM {
            elapsed = days
        } else {
            elapsed = 1
        }
        s.dailyAvg = s.total / max(1, elapsed)
        return s
    }

    static func sumByDate(_ all: [Expense]) -> [String: Int] {
        var m: [String: Int] = [:]
        for e in all {
            m[e.date, default: 0] += e.cents
        }
        return m
    }
}

/// Picks one of the user's own category names from a shop name.
enum CategoryGuess {
    private static let hints: [(pattern: String, names: [String])] = [
        (#"\b(?:starbucks|coffee|cafe|café|tim hortons|restaurant|pizza|burger|sushi|grill|kitchen|bakery|mcdonald'?s|subway|wendy'?s|bistro|diner|pub|doordash|skip ?the ?dishes|uber ?eats)\b"#,
         ["Food", "Eating out", "Dining"]),
        (#"\b(?:safeway|sobeys|superstore|save-on|costco|walmart|grocer\w*|market|co-?op|no frills|loblaws?|iga|freshco)\b"#,
         ["Groceries"]),
        (#"\b(?:uber|lyft|taxi|transit|shell|petro\w*|esso|chevron|husky|gas|fuel|parking|presto)\b"#,
         ["Transport"]),
        (#"\b(?:pharmacy|shoppers|rexall|clinic|dental|dentist|medical|health|london drugs)\b"#,
         ["Health"]),
        (#"\b(?:netflix|spotify|cinema|theat(?:re|er)|movie|steam|playstation|xbox|nintendo|concert|ticket\w*)\b"#,
         ["Fun"]),
        (#"\b(?:amazon|ikea|best buy|h&m|zara|mall|winners|canadian tire)\b"#,
         ["Shopping"]),
        (#"\b(?:hydro|enmax|telus|rogers|bell|shaw|fortis|insurance|rent|utility)\b"#,
         ["Bills"])
    ]

    static func pick(names: [String], merchant: String, fallback: String) -> String {
        if merchant.isEmpty { return fallback }
        for h in hints {
            if merchant.range(of: h.pattern, options: [.regularExpression, .caseInsensitive]) != nil {
                for want in h.names {
                    if let n = names.first(where: { $0.caseInsensitiveCompare(want) == .orderedSame }) {
                        return n
                    }
                }
            }
        }
        return fallback
    }
}
