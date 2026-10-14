import Foundation

enum Money {
    private static let currency: NumberFormatter = {
        let f = NumberFormatter()
        f.numberStyle = .currency
        return f
    }()

    static func format(_ cents: Int) -> String {
        currency.string(from: NSNumber(value: Double(cents) / 100.0)) ?? "$0.00"
    }

    /// A plain number like "12.50", for text fields.
    static func plain(_ cents: Int) -> String {
        String(format: "%.2f", Double(cents) / 100.0)
    }

    /// Reads "5.25", "$5.25", "CA$ 1,234.50" or "12,50". Returns cents, or nil when there is no number.
    static func parse(_ text: String) -> Int? {
        var t = String(text.filter { $0.isNumber || $0 == "." || $0 == "," })
        if t.isEmpty { return nil }
        if t.contains(",") && !t.contains(".") {
            let parts = t.split(separator: ",", omittingEmptySubsequences: false)
            if parts.count == 2, let last = parts.last, last.count <= 2 {
                t = String(parts[0]) + "." + String(parts[1])
            } else {
                t = t.replacingOccurrences(of: ",", with: "")
            }
        } else {
            t = t.replacingOccurrences(of: ",", with: "")
        }
        guard let v = Double(t), v >= 0, v <= 10_000_000 else { return nil }
        return Int((v * 100).rounded())
    }
}
