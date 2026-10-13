package com.spendlog.app;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;

/** App lock using the phone's own screen lock (PIN, pattern or password). */
final class Lock {
    static final int REQUEST = 4242;
    private static final long GRACE_MS = 30_000L;

    private static boolean unlocked;
    private static long leftAt;
    private static boolean prompting;

    private Lock() {}

    static boolean deviceSecure(Context c) {
        KeyguardManager km = (KeyguardManager) c.getSystemService(Context.KEYGUARD_SERVICE);
        return km != null && km.isDeviceSecure();
    }

    private static boolean needsUnlock(Context c) {
        return AppSettings.lockOn(c)
                && (!unlocked || System.currentTimeMillis() - leftAt > GRACE_MS);
    }

    /** Call from onResume. Hides the screen and asks for the phone's lock when the app is locked. */
    static void guard(Activity a) {
        View content = a.findViewById(android.R.id.content);
        if (!needsUnlock(a) || !deviceSecure(a)) {
            if (content != null) {
                content.setVisibility(View.VISIBLE);
            }
            return;
        }
        if (content != null) {
            content.setVisibility(View.INVISIBLE);
        }
        if (prompting) {
            return;
        }
        KeyguardManager km = (KeyguardManager) a.getSystemService(Context.KEYGUARD_SERVICE);
        Intent i = km == null ? null
                : km.createConfirmDeviceCredentialIntent("Spend Log", "Unlock to see your spending");
        if (i == null) {
            if (content != null) {
                content.setVisibility(View.VISIBLE);
            }
            return;
        }
        prompting = true;
        a.startActivityForResult(i, REQUEST);
    }

    /** Call from onActivityResult. Returns true when the result belonged to the lock. */
    static boolean handleResult(Activity a, int requestCode, int resultCode) {
        if (requestCode != REQUEST) {
            return false;
        }
        prompting = false;
        if (resultCode == Activity.RESULT_OK) {
            unlocked = true;
            leftAt = System.currentTimeMillis();
            View content = a.findViewById(android.R.id.content);
            if (content != null) {
                content.setVisibility(View.VISIBLE);
            }
        } else {
            a.finish();
        }
        return true;
    }

    /** Call from onPause. */
    static void paused() {
        leftAt = System.currentTimeMillis();
    }
}
