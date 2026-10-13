package com.spendlog.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls a purchase amount and merchant out of a payment notification.
 * Uses no Android classes so it can be tested on a plain JVM.
 */
final class PaymentParser {

    static final class Result {
        long cents;
        String merchant = "";
        /** Null when the notification is a purchase to add; otherwise why it was skipped. */
        String skip;
    }

    private static final class Amt {
        long cents;
        int start;
        int end;
    }

    private static final int CI = Pattern.CASE_INSENSITIVE;
    private static final String NUM = "(\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?|\\d+(?:\\.\\d{1,2})?)";
    private static final String CODES = "(?:USD|CAD|EUR|GBP|INR|AUD|NZD)";

    private static final Pattern[] AMOUNTS = {
            Pattern.compile("[$\u20AC\u00A3\u00A5\u20B9]\\s?" + NUM),
            Pattern.compile("\\b" + CODES + "\\s?" + NUM, CI),
            Pattern.compile("\\b" + NUM + "\\s?" + CODES + "\\b", CI),
    };

    private static final Pattern NOT_DEBIT = Pattern.compile(
            "\\b(?:refund(?:ed)?|reversal|reversed|deposit(?:ed)?|received|credited|cash ?back|declined|failed"
                    + "|unsuccessful|insufficient|overdraft|statement|due|otp|one-time|verification|e-?transfer"
                    + "|requested|request)\\b", CI);

    private static final Pattern DEBIT_CUE = Pattern.compile(
            "\\b(?:purchase[ds]?|paid|pay(?:ment)?|spent|charged?|debit(?:ed)?|withdr(?:awal|ew)|transaction"
                    + "|tap(?:ped)?|sent)\\b"
                    + "|\\bwith (?:your )?(?:visa|mastercard|amex|american express|discover|debit|credit|card)\\b"
                    + "|\\bcard ending\\b|\\bending in \\d{4}\\b|\u2022+\\s?\\d{3,4}|\\*{2,}\\s?\\d{3,4}", CI);

    private static final Pattern BALANCE_BEFORE = Pattern.compile(
            "(?:balance|available|limit|points|remaining)(?:\\W|\\bis\\b|\\bnow\\b|\\bof\\b){0,8}$", CI);

    private static final Pattern PREFER_BEFORE = Pattern.compile(
            "(?:purchase|paid|spent|charged?|payment|debit(?:ed)?|withdr\\w+|transaction|sent)\\b[^0-9]{0,25}$", CI);

    private static final Pattern AT = Pattern.compile(
            "(?:\\bat\\b|@)\\s+(.+?)(?=\\s+(?:on|with|using|via|for|from|card)\\b|[.,;!\\n]|$)", CI);

    private static final Pattern GENERIC_TITLE = Pattern.compile(
            "^(?:google\\s*(?:wallet|pay)|samsung\\s*(?:wallet|pay)|wallet|paid|payment(?:\\s+\\w+)?|purchase"
                    + "|transaction|you paid|tap to pay|contactless payment)$", CI);

    private static final String[][] HINTS = {
            {"\\b(?:starbucks|coffee|cafe|caf\u00E9|tim hortons|restaurant|pizza|burger|sushi|grill|kitchen|bakery"
                    + "|mcdonald'?s|subway|wendy'?s|bistro|diner|pub|doordash|skip ?the ?dishes|uber ?eats)\\b",
                    "Food", "Eating out", "Dining"},
            {"\\b(?:safeway|sobeys|superstore|save-on|costco|walmart|grocer\\w*|market|co-?op|no frills|loblaws?"
                    + "|iga|freshco)\\b", "Groceries"},
            {"\\b(?:uber|lyft|taxi|transit|shell|petro\\w*|esso|chevron|husky|gas|fuel|parking|presto)\\b",
                    "Transport"},
            {"\\b(?:pharmacy|shoppers|rexall|clinic|dental|dentist|medical|health|london drugs)\\b", "Health"},
            {"\\b(?:netflix|spotify|cinema|theat(?:re|er)|movie|steam|playstation|xbox|nintendo|concert|ticket\\w*)\\b",
                    "Fun"},
            {"\\b(?:amazon|ikea|best buy|h&m|zara|mall|winners|canadian tire)\\b", "Shopping"},
            {"\\b(?:hydro|enmax|telus|rogers|bell|shaw|fortis|insurance|rent|utility)\\b", "Bills"},
    };

    private PaymentParser() {}

    static Result parse(String title, String body, boolean wallet) {
        Result r = new Result();
        String t = title == null ? "" : title.trim();
        String b = body == null ? "" : body.trim();
        String full = t.isEmpty() ? b : (b.isEmpty() ? t : t + "\n" + b);
        if (full.isEmpty()) {
            r.skip = "empty notification";
            return r;
        }
        if (NOT_DEBIT.matcher(full).find()) {
            r.skip = "looks like a refund, deposit, alert or request, not a purchase";
            return r;
        }
        List<Amt> all = amounts(full);
        if (all.isEmpty()) {
            r.skip = "no amount found";
            return r;
        }
        List<Amt> usable = new ArrayList<>();
        for (Amt a : all) {
            if (!BALANCE_BEFORE.matcher(before(full, a)).find()) {
                usable.add(a);
            }
        }
        if (usable.isEmpty()) {
            r.skip = "only a balance amount found";
            return r;
        }
        Amt pick = null;
        if (usable.size() == 1) {
            pick = usable.get(0);
        } else {
            for (Amt a : usable) {
                if (PREFER_BEFORE.matcher(before(full, a)).find()) {
                    pick = a;
                    break;
                }
            }
        }
        if (pick == null) {
            r.skip = "several amounts, not sure which is the purchase";
            return r;
        }
        boolean cue = DEBIT_CUE.matcher(full).find();
        if (!cue && !(wallet && usable.size() == 1)) {
            r.skip = "no purchase wording";
            return r;
        }
        if (pick.cents <= 0) {
            r.skip = "zero amount";
            return r;
        }
        r.cents = pick.cents;
        r.merchant = merchant(t, b, wallet);
        return r;
    }

    private static String before(String s, Amt a) {
        return s.substring(Math.max(0, a.start - 35), a.start);
    }

    private static List<Amt> amounts(String s) {
        List<Amt> out = new ArrayList<>();
        for (Pattern p : AMOUNTS) {
            Matcher m = p.matcher(s);
            while (m.find()) {
                boolean overlaps = false;
                for (Amt o : out) {
                    if (m.start() < o.end && m.end() > o.start) {
                        overlaps = true;
                        break;
                    }
                }
                if (overlaps) {
                    continue;
                }
                Amt a = new Amt();
                a.start = m.start();
                a.end = m.end();
                try {
                    a.cents = new BigDecimal(m.group(1).replace(",", ""))
                            .movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
                } catch (NumberFormatException e) {
                    continue;
                }
                out.add(a);
            }
        }
        out.sort((x, y) -> Integer.compare(x.start, y.start));
        return out;
    }

    private static String merchant(String title, String body, boolean wallet) {
        Matcher m = AT.matcher(body);
        if (m.find()) {
            return clean(m.group(1));
        }
        m = AT.matcher(title);
        if (m.find()) {
            return clean(m.group(1));
        }
        if (wallet && !title.isEmpty() && !GENERIC_TITLE.matcher(title).find() && amounts(title).isEmpty()) {
            return clean(title);
        }
        return "";
    }

    static String clean(String s) {
        String x = s.replaceAll("\\s+", " ").trim();
        x = x.replaceAll("^[\\-\u2013\u2014:\\s]+|[\\-\u2013\u2014:*\\s]+$", "");
        if (x.length() > 40) {
            x = x.substring(0, 40).trim();
        }
        if (x.length() > 3 && x.equals(x.toUpperCase(Locale.ROOT)) && x.matches(".*[A-Z].*")) {
            x = titleCase(x);
        }
        return x;
    }

    private static String titleCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (String w : s.split(" ")) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (w.matches(".*\\d.*") || w.length() <= 1) {
                sb.append(w);
            } else {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    /** Picks one of the user's own category names from the merchant, or the fallback. */
    static String guessCategory(List<String> names, String merchant, String fallback) {
        if (merchant == null || merchant.isEmpty()) {
            return fallback;
        }
        for (String[] hint : HINTS) {
            if (Pattern.compile(hint[0], CI).matcher(merchant).find()) {
                for (int i = 1; i < hint.length; i++) {
                    for (String n : names) {
                        if (n.equalsIgnoreCase(hint[i])) {
                            return n;
                        }
                    }
                }
            }
        }
        return fallback;
    }
}
