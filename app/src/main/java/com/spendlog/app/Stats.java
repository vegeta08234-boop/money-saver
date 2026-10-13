package com.spendlog.app;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Stats {
    long monthTotal;
    long prevMonthTotal;
    long todayTotal;
    long dailyAvg;
    long[] byDay;                       // index 0 = day 1
    Map<String, Long> byCategory = new LinkedHashMap<>();
    Map<String, Long> byDate = new HashMap<>();
    Expense biggest;
    int count;

    static int daysLeftInMonth() {
        Calendar n = Calendar.getInstance();
        return Dates.daysInMonth(n) - n.get(Calendar.DAY_OF_MONTH) + 1;
    }

    static Map<String, Long> sumByDate(List<Expense> all) {
        Map<String, Long> m = new HashMap<>();
        for (Expense e : all) {
            Long cur = m.get(e.date);
            m.put(e.date, (cur == null ? 0L : cur) + e.cents);
        }
        return m;
    }

    /** viewMonth must be the first day of the month being viewed. */
    static Stats compute(List<Expense> all, Calendar viewMonth) {
        Stats s = new Stats();
        String ym = Dates.ym(viewMonth);
        Calendar prev = (Calendar) viewMonth.clone();
        prev.add(Calendar.MONTH, -1);
        String prevYm = Dates.ym(prev);
        int days = Dates.daysInMonth(viewMonth);
        s.byDay = new long[days];
        String today = Dates.today();

        for (Expense e : all) {
            if (e.date.equals(today)) {
                s.todayTotal += e.cents;
            }
            if (e.date.startsWith(prevYm)) {
                s.prevMonthTotal += e.cents;
            }
            if (e.date.startsWith(ym)) {
                s.monthTotal += e.cents;
                s.count++;
                if (s.biggest == null || e.cents > s.biggest.cents) {
                    s.biggest = e;
                }
                int day = Integer.parseInt(e.date.substring(8, 10));
                if (day >= 1 && day <= days) {
                    s.byDay[day - 1] += e.cents;
                }
                Long c = s.byCategory.get(e.cat);
                s.byCategory.put(e.cat, (c == null ? 0L : c) + e.cents);
                Long d = s.byDate.get(e.date);
                s.byDate.put(e.date, (d == null ? 0L : d) + e.cents);
            }
        }

        Calendar now = Calendar.getInstance();
        int elapsed;
        String nowYm = Dates.ym(now);
        int cmp = ym.compareTo(nowYm);
        if (cmp == 0) {
            elapsed = now.get(Calendar.DAY_OF_MONTH);
        } else if (cmp < 0) {
            elapsed = days;
        } else {
            elapsed = 1;
        }
        s.dailyAvg = s.monthTotal / Math.max(1, elapsed);
        return s;
    }
}
