package com.fluid.glance;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class Glance {
    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("glance", Context.MODE_PRIVATE);
    }

    static String clockPattern(Context context) {
        int mode = prefs(context).getInt("clock", 0);
        return mode == 2 || (mode == 0 && android.text.format.DateFormat.is24HourFormat(context))
                ? "HH:mm" : "h:mm a";
    }

    static String date(Context context, ZonedDateTime now) {
        String pattern = new String[]{"EEE, d MMM", "dd/MM/yyyy", "MMM d, yyyy", "yyyy-MM-dd"}[prefs(context).getInt("dateFormat", 0)];
        return now.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()));
    }

    static String battery(Context context) {
        BatteryManager manager = context.getSystemService(BatteryManager.class);
        int percent = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        if (percent < 0 || percent > 100) return "Unavailable";
        return prefs(context).getInt("batteryFormat", 0) == 1 && manager.isCharging()
                ? percent + "% · Charging" : percent + "%";
    }

    static final int[] ACCENTS = {0xffd4bbff, 0xffa8d8ff, 0xffa8dfc5, 0xffffbaa7};
}
