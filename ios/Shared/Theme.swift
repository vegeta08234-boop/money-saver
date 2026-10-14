import SwiftUI
import UIKit

extension UIColor {
    convenience init(hex: UInt32) {
        self.init(red: CGFloat((hex >> 16) & 0xFF) / 255.0,
                  green: CGFloat((hex >> 8) & 0xFF) / 255.0,
                  blue: CGFloat(hex & 0xFF) / 255.0,
                  alpha: 1)
    }
}

extension Color {
    init(hexString: String) {
        let cleaned = hexString.replacingOccurrences(of: "#", with: "")
        let v = UInt32(cleaned, radix: 16) ?? 0x94A3B8
        self.init(uiColor: UIColor(hex: v))
    }
}

/// Green when the budget is fresh, amber as it fills, red when it is used up.
enum BudgetColor {
    /// Share of this month's budget already spent (1.0 = all of it), or nil when no budget is set.
    static func ratio(_ store: Store, now: Date = Date()) -> Double? {
        guard store.monthlyBudget > 0 else { return nil }
        let ym = DateKey.ym(now)
        var total = 0
        for e in store.expenses where e.date.hasPrefix(ym) {
            total += e.cents
        }
        return Double(total) / Double(store.monthlyBudget)
    }

    static func gradientUI(_ ratio: Double, dark: Bool) -> UIColor {
        let r = max(0.0, min(1.0, ratio))
        let hue = (140.0 / 360.0) * (1.0 - r)
        return UIColor(hue: CGFloat(hue), saturation: 0.82, brightness: dark ? 0.95 : 0.80, alpha: 1)
    }

    static func gradient(_ ratio: Double, dark: Bool) -> Color {
        Color(uiColor: gradientUI(ratio, dark: dark))
    }

    static func blend(_ a: UIColor, _ b: UIColor, _ t: Double) -> UIColor {
        var ar: CGFloat = 0, ag: CGFloat = 0, ab: CGFloat = 0, aa: CGFloat = 0
        var br: CGFloat = 0, bg: CGFloat = 0, bb: CGFloat = 0, ba: CGFloat = 0
        a.getRed(&ar, green: &ag, blue: &ab, alpha: &aa)
        b.getRed(&br, green: &bg, blue: &bb, alpha: &ba)
        let v = CGFloat(t)
        let u = 1 - v
        return UIColor(red: ar * u + br * v, green: ag * u + bg * v, blue: ab * u + bb * v, alpha: 1)
    }
}

enum Theme {
    static let names = ["Indigo", "Green", "Orange", "Rose", "Graphite"]
    private static let lightHex: [UInt32] = [0x4F46E5, 0x15803D, 0xC2410C, 0xBE123C, 0x374151]
    private static let darkHex: [UInt32] = [0x6366F1, 0x16A34A, 0xEA580C, 0xF43F5E, 0x6B7280]

    /// Colors people can give a category.
    static let categoryColors: [String] = [
        "6366F1", "22C55E", "F97316", "F43F5E", "06B6D4",
        "EAB308", "A855F7", "14B8A6", "64748B", "EC4899"
    ]

    static func baseUI(index: Int, dark: Bool) -> UIColor {
        let i = (index >= 0 && index < names.count) ? index : 0
        return UIColor(hex: dark ? darkHex[i] : lightHex[i])
    }

    /// The chosen theme color, shifted toward amber and red once half the monthly budget is used.
    static func accentUI(settings: AppSettings, ratio: Double?, dark: Bool) -> UIColor {
        let base = baseUI(index: settings.themeIndex, dark: dark)
        guard settings.tint, let r = ratio, r >= 0.5 else { return base }
        let t = min(1.0, (r - 0.5) / 0.5)
        return BudgetColor.blend(base, BudgetColor.gradientUI(r, dark: dark), t)
    }

    static func accent(store: Store, dark: Bool) -> Color {
        Color(uiColor: accentUI(settings: store.settings, ratio: BudgetColor.ratio(store), dark: dark))
    }
}
