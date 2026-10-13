# Spend Log

A small Android app for logging what you spend and seeing totals per day and per month, with a home-screen widget.

- Add an expense: amount, category, date, optional note
- Month total, today, daily average, and change vs last month
- Bar chart of spending per day and a by-category breakdown
- Press and hold an entry to remove it
- Five color themes (Indigo, Green, Orange, Rose, Graphite); light and dark mode follow the phone
- Home-screen widget: this month's total, today's total, last 7 days, and a "+ Add" shortcut

Entries are stored on the phone only.

## Get the APK

**GitHub Actions:** open the Actions tab, run "Build APK" (or push to `main`), then download `spend-log-apk` from the finished run and unzip it to get `app-debug.apk`.

**Android Studio:** open this folder and choose Build > Build APK(s).

## Install and add the widget

1. Copy `app-debug.apk` to the phone and open it. Allow "install unknown apps" for your file manager or browser if asked.
2. Touch and hold an empty spot on the home screen, choose Widgets, find Spend Log, and drag it out.
