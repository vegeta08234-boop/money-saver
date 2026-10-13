package com.spendlog.app;

final class Expense {
    final long id;
    final long cents;
    final String cat;
    final String date; // yyyy-MM-dd
    final String note;

    Expense(long id, long cents, String cat, String date, String note) {
        this.id = id;
        this.cents = cents;
        this.cat = cat;
        this.date = date;
        this.note = note == null ? "" : note;
    }
}
