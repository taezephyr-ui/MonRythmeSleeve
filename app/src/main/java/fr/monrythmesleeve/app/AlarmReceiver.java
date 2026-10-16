package fr.monrythmesleeve.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

/** Notification + vibration lisible sur Android et les montres qui répliquent les alertes. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent intent) {
        int meal = intent.getIntExtra("meal", -1);
        int kind = intent.getIntExtra("kind", -1);
        if (meal < 0 || meal >= ScheduleStore.MEALS || kind < 0 || kind > 2) return;
        if (ScheduleStore.alertsEnabled(ctx) && ScheduleStore.eventEnabled(ctx, kind)) {
            sendNotification(ctx, meal, kind);
            AlarmScheduler.scheduleOne(ctx, meal, kind);
        }
    }

    public static void ensureChannel(Context ctx) {
        NotificationManager mgr = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mgr == null) return;
        NotificationChannel chan = new NotificationChannel(AlarmScheduler.CHANNEL,
                "Repas et boissons", NotificationManager.IMPORTANCE_HIGH);
        chan.setDescription("Une notification vibrante lors de chaque étape activée");
        chan.enableVibration(true);
        chan.setVibrationPattern(new long[]{0, 280, 150, 280});
        mgr.createNotificationChannel(chan);
    }

    private static void sendNotification(Context ctx, int meal, int kind) {
        NotificationManager mgr = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mgr == null) return;
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        ensureChannel(ctx);
        String title;
        String body;
        if (kind == AlarmScheduler.STOP_DRINKING) {
            title = "Pause boisson";
            body = "Arrête de boire : " + ScheduleStore.NAMES[meal] + " dans 30 minutes.";
        } else if (kind == AlarmScheduler.MEAL_START) {
            title = "C'est l'heure du repas";
            body = ScheduleStore.NAMES[meal] + " : tu as 30 minutes pour manger.";
        } else {
            title = "Tu peux boire à nouveau";
            body = "La pause après " + ScheduleStore.NAMES[meal] + " est terminée.";
        }
        Intent open = new Intent(ctx, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openAction = PendingIntent.getActivity(ctx, 10, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(ctx, AlarmScheduler.CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(openAction)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build();
        mgr.notify(2000 + meal * 3 + kind, notification);
    }
}
