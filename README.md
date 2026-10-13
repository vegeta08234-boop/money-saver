# Spend Log

A small Android app for logging what you spend and seeing totals per day and per month, with a home-screen widget.

**Logging**
- Add an expense: amount, category, date, optional note
- Tap an entry to edit it; press and hold to remove it
- Recurring expenses (rent, subscriptions) that add themselves each month
- Custom categories with their own colors

**Budgets**
- Monthly budget with a progress bar and what is left
- Daily allowance: what is left divided by the days remaining
- Per-category limits with amber and red warnings
- Savings goals with progress

**Seeing your spending**
- Month total, today, daily average, and change vs last month
- Bar chart per day, by-category breakdown, and a "where it went" summary (biggest days, top categories, biggest purchase)
- Search by note, category or amount, plus category and amount-range filters

**Widget**
- This month's total, today, what is left in the budget, and the last 7 days
- Four category buttons: tap one, type the amount, done, without opening the app
- "+ Add" opens the app ready to type

**Extras**
- Daily reminder notification (skipped on days you already logged something)
- App lock using your phone's screen lock
- Five color themes (Indigo, Green, Orange, Rose, Graphite); light and dark mode follow the phone

Everything is stored on the phone only.

## Get the APK

**GitHub Actions:** every push to `main` builds the app and publishes `SpendLog.apk` to the `latest` release:
https://github.com/vegeta08234-boop/money-saver/releases/download/latest/SpendLog.apk

You can also run "Build APK" from the Actions tab and download `spend-log-apk` from the run.

**Android Studio:** open this folder and choose Build > Build APK(s).

## Install and add the widget

1. Open the APK on the phone. Allow "install unknown apps" for your browser or file manager if asked.
2. Touch and hold an empty spot on the home screen, choose Widgets, find Spend Log, and drag it out.
