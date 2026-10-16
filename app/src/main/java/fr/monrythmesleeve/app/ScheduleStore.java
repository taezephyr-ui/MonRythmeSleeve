package fr.monrythmesleeve.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;
import java.util.Locale;

/** Calcul des horaires et stockage local, sans connexion ni compte. */
public final class ScheduleStore {
    private ScheduleStore() {}
    public static final int MEALS = 5;
    public static final int MEAL_DURATION = 30;
    public static final int DRINKS_BEFORE = 30;
    public static final int DRINKS_AFTER = 30;
    public static final String[] NAMES = {"Petit-déjeuner", "Collation 1", "Déjeuner", "Collation 2", "Dîner"};
    private static final String PREFS = "mon_rythme_sleeve_v2";

    public static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Intervalle entre la FIN d'un repas et le DEBUT du suivant. */
    public static int gap(Context ctx) {
        int minutes = prefs(ctx).getInt("gap", 150);
        return minutes == 165 || minutes == 180 ? minutes : 150;
    }

    public static int[] times(Context ctx) {
        SharedPreferences p = prefs(ctx);
        int[] result = new int[MEALS];
        // Valeurs initiales : 07:30, 10:30, 13:30, 16:30, 19:30.
        for (int i = 0; i < MEALS; i++) result[i] = p.getInt("meal_" + i, 450 + i * 180);
        return result;
    }

    /** Fixe un des repas (p. ex. le dîner à 19h30) et calcule tous les autres. */
    public static boolean saveAnchor(Context ctx, int index, int minute, int rest) {
        if (index < 0 || index >= MEALS || minute < 0 || minute >= 1440) return false;
        if (rest != 150 && rest != 165 && rest != 180) return false;
        int step = rest + MEAL_DURATION;
        int first = minute - index * step;
        int lastEnd = first + (MEALS - 1) * step + MEAL_DURATION;
        if (first < DRINKS_BEFORE || lastEnd > 1440) return false;
        SharedPreferences.Editor edit = prefs(ctx).edit().putInt("gap", rest);
        for (int i = 0; i < MEALS; i++) edit.putInt("meal_" + i, first + i * step);
        edit.apply();
        return true;
    }

    /** Ajuste le départ du repas et recalcule ceux qui suivent. */
    public static boolean startNow(Context ctx, int meal, int actualStart) {
        if (meal < 0 || meal >= MEALS || actualStart < 0 || actualStart >= 1440) return false;
        int[] current = times(ctx);
        if (meal > 0 && actualStart < current[meal - 1] + MEAL_DURATION + gap(ctx)) return false;
        int finalEnd = actualStart + (MEALS - 1 - meal) * (MEAL_DURATION + gap(ctx)) + MEAL_DURATION;
        if (finalEnd > 1440) return false;
        SharedPreferences.Editor editor = prefs(ctx).edit();
        for (int i = meal; i < MEALS; i++) editor.putInt("meal_" + i, actualStart + (i - meal) * (MEAL_DURATION + gap(ctx)));
        editor.putString("start_day_" + meal, LocalDate.now().toString());
        editor.apply();
        return true;
    }

    /** Fin réelle du repas : les suivants commencent 2h30/2h45/3h plus tard. */
    public static boolean finishNow(Context ctx, int meal, int actualEnd) {
        if (meal < 0 || meal >= MEALS || actualEnd < 0 || actualEnd > 1440) return false;
        int[] current = times(ctx);
        if (actualEnd < current[meal]) return false;
        int step = gap(ctx) + MEAL_DURATION;
        int lastEnd = actualEnd + gap(ctx) + (MEALS - meal - 2) * step + MEAL_DURATION;
        if (meal < MEALS - 1 && lastEnd > 1440) return false;
        SharedPreferences.Editor editor = prefs(ctx).edit();
        for (int i = meal + 1; i < MEALS; i++) editor.putInt("meal_" + i, actualEnd + gap(ctx) + (i - meal - 1) * step);
        editor.putString("done_day_" + meal, LocalDate.now().toString());
        editor.putInt("actual_end_" + meal, actualEnd);
        editor.apply();
        return true;
    }

    public static boolean isStarted(Context ctx, int meal) {
        return LocalDate.now().toString().equals(prefs(ctx).getString("start_day_" + meal, ""));
    }
    public static boolean isCompleted(Context ctx, int meal) {
        return LocalDate.now().toString().equals(prefs(ctx).getString("done_day_" + meal, ""));
    }
    public static int actualEnd(Context ctx, int meal, LocalDate day) {
        return day.toString().equals(prefs(ctx).getString("done_day_" + meal, ""))
                ? prefs(ctx).getInt("actual_end_" + meal, -1) : -1;
    }
    public static boolean alertsEnabled(Context ctx) { return prefs(ctx).getBoolean("alerts", true); }
    public static boolean eventEnabled(Context ctx, int type) { return prefs(ctx).getBoolean("event_" + type, true); }
    public static String hhmm(int minute) {
        int m = Math.floorMod(minute, 1440);
        return String.format(Locale.FRANCE, "%02d:%02d", m / 60, m % 60);
    }
    public static String hhmmDisplay(int minute) { return minute == 1440 ? "00:00" : hhmm(minute); }
}
