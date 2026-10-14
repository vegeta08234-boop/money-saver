# Spend Log for iOS

SwiftUI app + WidgetKit widget (iOS 17+). Same features as the Android app, except auto-add:
iOS can't read other apps' notifications, so a Shortcuts "Transaction" (Wallet) automation calls the
**Log Expense** action instead (see Settings > Auto-add payments in the app).

Built on GitHub macOS runners (`.github/workflows/build-ios.yml`) with XcodeGen. The workflow publishes an
**unsigned** `SpendLog.ipa` to the `ios-latest` release. Unsigned IPAs must be signed with your own Apple ID
(AltStore, Sideloadly) or a paid Apple Developer account before they install on a device.
