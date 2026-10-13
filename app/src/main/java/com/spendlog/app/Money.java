package com.spendlog.app;

import java.text.NumberFormat;

final class Money {
    private Money() {}

    static String fmt(long cents) {
        return NumberFormat.getCurrencyInstance().format(cents / 100.0);
    }
}
