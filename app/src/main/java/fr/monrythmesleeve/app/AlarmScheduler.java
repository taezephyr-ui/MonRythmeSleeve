package fr.monrythmesleeve.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Une alerte pour chaque événement, quotidiennement ; aucun rappel en rafale. */
public final class AlarmScheduler {
    private AlarmScheduler() {}
    public static final int STOP_DRINKING = 0;
    public static final int MEAL_START = 1;
    public static final int DRINK_AGAIN = 2;
    public static final String CHANNEL = "sleeve_alertes_v2";

    private static PendingIntent pending(Context ctx, int meal, int kind) {
        Intent intent = new Intent(ctx, AlarmReceiver.class)
                .putExtra("meal", meal).putExtra("kind", kind);
        return PendingIntent.getBroadcast(ctx, 1000 + meal * 3 + kind, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static int minuteFor(Context ctx, int meal, int kind, LocalDate day) {
        int minute = ScheduleStore.times(ctx)[meal];
        if (kind == STOP_DRINKING) minute -= ScheduleStore.DRINKS_BEFORE;
        if (kind == DRINK_AGAIN) {
            int realEnd = ScheduleStore.actualEnd(ctx, meal, day);
            minute = (realEnd >= 0 ? realEnd : minute + ScheduleStore.MEAL_DURATION) + ScheduleStore.DRINKS_AFTER;
        }
        return minute;
    }

    public static void rescheduleAll(Context ctx) {
        AlarmManager manager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        for (int i = 0; i < ScheduleStore.MEALS; i++) {
            for (int kind = 0; kind <= 2; kind++) {
                manager.cancel(pending(ctx, i, kind));
                if (ScheduleStore.alertsEnabled(ctx) && ScheduleStore.eventEnabled(ctx, kind)) scheduleOne(ctx, i, kind);
            }
        }
    }

    public static void scheduleOne(Context ctx, int meal, int kind) {
        if (!ScheduleStore.alertsEnabled(ctx) || !ScheduleStore.eventEnabled(ctx, kind)) return;
        AlarmManager manager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);
        LocalDate date = LocalDate.now(zone);
        ZonedDateTime next = date.atStartOfDay(zone).plusMinutes(minuteFor(ctx, meal, kind, date));
        if (!next.isAfter(now)) {
            date = date.plusDays(1);
            next = date.atStartOfDay(zone).plusMinutes(minuteFor(ctx, meal, kind, date));
        }
        PendingIntent action = pending(ctx, meal, kind);
        long when = next.toInstant().toEpochMilli();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, action);
            } else {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, action);
            }
        } catch (SecurityException ignored) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, action);
        }
    }
}
