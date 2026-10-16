package fr.monrythmesleeve.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Interface native Android, 100 % hors ligne et sans bibliothèques supplémentaires. */
public class MainActivity extends Activity {
    private static final int BG = Color.rgb(255, 249, 248);
    private static final int ROSE = Color.rgb(194, 103, 143);
    private static final int ROSE_LIGHT = Color.rgb(251, 226, 236);
    private static final int LAVENDER = Color.rgb(241, 231, 250);
    private static final int INK = Color.rgb(70, 53, 67);
    private static final int MUTED = Color.rgb(120, 104, 118);
    private static final int GREEN = Color.rgb(224, 245, 233);
    private static final int GREEN_INK = Color.rgb(52, 115, 80);
    private static final int PEACH = Color.rgb(255, 235, 221);
    private static final int PEACH_INK = Color.rgb(157, 91, 69);
    private static final int WHITE = Color.WHITE;
    private static final String[] TABS = {"Accueil", "Planning", "Alertes", "Paramètres"};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (currentTab == 0 && countdown != null) refreshCountdown();
            handler.postDelayed(this, 1000);
        }
    };
    private int currentTab = 0;
    private LinearLayout root;
    private LinearLayout page;
    private TextView heroTitle;
    private TextView heroSubtitle;
    private TextView countdown;
    private TextView nextInfo;
    private LinearLayout heroCard;
    private int anchorIndex = 4;
    private int selectedMinute = 19 * 60 + 30;
    private int chosenGap = 150;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        AlarmReceiver.ensureChannel(this);
        AlarmScheduler.rescheduleAll(this);
        requestNotificationPermissionIfNeeded();
        showTab(0);
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 200);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 200 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            AlarmScheduler.rescheduleAll(this);
        }
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout horizontal() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }
    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(sp);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setIncludeFontPadding(true);
        return t;
    }
    private LinearLayout card(int color) {
        LinearLayout c = vertical();
        c.setPadding(dp(18), dp(17), dp(18), dp(17));
        c.setBackground(round(color, 24));
        return c;
    }
    private void addGap(LinearLayout l, int size) { View v = new View(this); l.addView(v, new LinearLayout.LayoutParams(1, dp(size))); }
    private void addCard(LinearLayout parent, View child, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(bottom);
        parent.addView(child, lp);
    }
    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? WHITE : ROSE);
        b.setBackground(round(primary ? ROSE : ROSE_LIGHT, 17));
        b.setPadding(dp(12), dp(11), dp(12), dp(11));
        b.setMinHeight(dp(49));
        b.setElevation(0);
        return b;
    }
    private void sectionTitle(String title, String subtitle) {
        page.addView(text(title, 23, INK, true));
        if (!subtitle.isEmpty()) {
            addGap(page, 5);
            page.addView(text(subtitle, 13, MUTED, false));
        }
        addGap(page, 18);
    }

    private void showTab(int tab) {
        currentTab = tab;
        heroTitle = null;
        heroSubtitle = null;
        countdown = null;
        nextInfo = null;
        heroCard = null;
        root = vertical();
        root.setBackgroundColor(BG);
        setContentView(root);
        LinearLayout header = vertical();
        header.setPadding(dp(22), dp(17), dp(22), dp(13));
        TextView brand = text("✿  Mon Rythme Sleeve", 20, ROSE, true);
        header.addView(brand);
        addGap(header, 3);
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE));
        header.addView(text(date.substring(0, 1).toUpperCase(Locale.FRANCE) + date.substring(1), 12, MUTED, false));
        root.addView(header);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        page = vertical();
        page.setPadding(dp(20), dp(8), dp(20), dp(24));
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (tab == 0) buildHome();
        if (tab == 1) buildPlanning();
        if (tab == 2) buildAlerts();
        if (tab == 3) buildSettings();
        buildNavigation();
    }

    private void buildNavigation() {
        LinearLayout nav = horizontal();
        nav.setPadding(dp(5), dp(9), dp(5), dp(9));
        nav.setBackgroundColor(WHITE);
        String[] icons = {"⌂", "▦", "♧", "⚙"};
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            LinearLayout cell = vertical();
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(0, dp(5), 0, dp(4));
            if (i == currentTab) cell.setBackground(round(ROSE_LIGHT, 15));
            TextView icon = text(icons[i], 22, i == currentTab ? ROSE : MUTED, true);
            icon.setGravity(Gravity.CENTER);
            cell.addView(icon);
            TextView title = text(TABS[i], 11, i == currentTab ? ROSE : MUTED, i == currentTab);
            title.setGravity(Gravity.CENTER);
            cell.addView(title);
            cell.setContentDescription("Onglet " + TABS[i]);
            cell.setOnClickListener(v -> showTab(index));
            nav.addView(cell, new LinearLayout.LayoutParams(0, dp(55), 1));
        }
        root.addView(nav);
    }

    private void buildHome() {
        page.addView(text("Bonjour 🌷", 25, INK, true));
        addGap(page, 3);
        page.addView(text("Prends soin de toi, à ton rythme.", 14, MUTED, false));
        addGap(page, 22);

        heroCard = card(GREEN);
        TextView label = text("TON STATUT ACTUEL", 11, MUTED, true);
        heroCard.addView(label);
        addGap(heroCard, 9);
        heroTitle = text("Tu peux boire", 26, INK, true);
        heroCard.addView(heroTitle);
        addGap(heroCard, 4);
        heroSubtitle = text("Jusqu'à la prochaine pause", 14, MUTED, false);
        heroCard.addView(heroSubtitle);
        addGap(heroCard, 13);
        countdown = text("--:--:--", 32, INK, true);
        countdown.setContentDescription("Compte à rebours avant la prochaine étape");
        heroCard.addView(countdown);
        addCard(page, heroCard, 14);

        LinearLayout upcoming = card(LAVENDER);
        upcoming.addView(text("PROCHAINE ÉTAPE", 11, MUTED, true));
        addGap(upcoming, 7);
        nextInfo = text("Chargement...", 17, INK, true);
        upcoming.addView(nextInfo);
        addCard(page, upcoming, 18);
        refreshCountdown();

        TextView heading = text("Mes 5 repas aujourd'hui", 18, INK, true);
        page.addView(heading);
        addGap(page, 11);
        int[] all = ScheduleStore.times(this);
        for (int i = 0; i < ScheduleStore.MEALS; i++) {
            LinearLayout row = horizontal();
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            row.setBackground(round(i % 2 == 0 ? WHITE : Color.rgb(255, 243, 247), 16));
            TextView time = text(ScheduleStore.hhmm(all[i]), 15, ROSE, true);
            row.addView(time, new LinearLayout.LayoutParams(dp(66), -2));
            LinearLayout details = vertical();
            details.addView(text(ScheduleStore.NAMES[i], 14, INK, true));
            String note = ScheduleStore.isCompleted(this, i) ? "Terminé ✓" : "Jusqu'à " + ScheduleStore.hhmmDisplay(all[i] + 30);
            details.addView(text(note, 12, MUTED, false));
            row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
            addCard(page, row, 7);
        }
        addGap(page, 10);
        Button plan = button("Modifier les horaires", true);
        plan.setOnClickListener(v -> showTab(1));
        addCard(page, plan, 10);

        int active = mealEligibleNow();
        if (active >= 0) {
            LinearLayout actions = horizontal();
            if (!ScheduleStore.isStarted(this, active)) {
                Button start = button("Je commence", false);
                start.setOnClickListener(v -> startMeal(active));
                actions.addView(start, new LinearLayout.LayoutParams(0, dp(54), 1));
                View space = new View(this);
                actions.addView(space, new LinearLayout.LayoutParams(dp(8), 1));
            }
            Button finish = button("J'ai terminé", false);
            finish.setOnClickListener(v -> finishMeal(active));
            actions.addView(finish, new LinearLayout.LayoutParams(0, dp(54), 1));
            addCard(page, actions, 4);
            page.addView(text("Ces boutons ajustent les horaires selon le début ou la fin réelle de ton repas.", 12, MUTED, false));
        }
    }

    private int mealEligibleNow() {
        int now = ZonedDateTime.now().getHour() * 60 + ZonedDateTime.now().getMinute();
        int[] times = ScheduleStore.times(this);
        for (int i = ScheduleStore.MEALS - 1; i >= 0; i--) {
            if (now >= times[i] && !ScheduleStore.isCompleted(this, i)) return i;
        }
        return -1;
    }

    private int minutesNow() {
        ZonedDateTime now = ZonedDateTime.now();
        return now.getHour() * 60 + now.getMinute();
    }
    private void startMeal(int index) {
        int now = minutesNow();
        if (ScheduleStore.startNow(this, index, now)) {
            AlarmScheduler.rescheduleAll(this);
            toast("Repas commencé. Planning recalculé.");
            showTab(0);
        } else {
            new AlertDialog.Builder(this).setTitle("Horaire impossible")
                .setMessage("Ce changement ne respecte pas l'intervalle prévu ou repousse le dernier repas après minuit. Modifie ton planning pour continuer.")
                .setPositiveButton("Compris", null).show();
        }
    }
    private void finishMeal(int index) {
        int now = minutesNow();
        if (ScheduleStore.finishNow(this, index, now)) {
            AlarmScheduler.rescheduleAll(this);
            toast("Repas terminé. Reprise des boissons dans 30 min.");
            showTab(0);
        } else {
            new AlertDialog.Builder(this).setTitle("Planning à ajuster")
                .setMessage("Il n'est pas possible de conserver les intervalles choisis et les 5 repas avant minuit. Essaie de modifier ton planning.")
                .setPositiveButton("Compris", null).show();
        }
    }

    private static class Event {
        final long time;
        final int kind;
        final int meal;
        final String label;
        Event(long time, int kind, int meal, String label) {
            this.time = time; this.kind = kind; this.meal = meal; this.label = label;
        }
    }

    /** Événements ordonnés, y compris après minuit et la veille. */
    private List<Event> events() {
        List<Event> result = new ArrayList<>();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        int[] times = ScheduleStore.times(this);
        for (int delta = -1; delta <= 1; delta++) {
            LocalDate day = today.plusDays(delta);
            ZonedDateTime midnight = day.atStartOfDay(zone);
            for (int i = 0; i < ScheduleStore.MEALS; i++) {
                int start = times[i];
                int actual = ScheduleStore.actualEnd(this, i, day);
                int end = actual >= 0 ? actual : start + ScheduleStore.MEAL_DURATION;
                result.add(new Event(midnight.plusMinutes(start - 30).toInstant().toEpochMilli(), 0, i, "Arrêt des boissons"));
                result.add(new Event(midnight.plusMinutes(start).toInstant().toEpochMilli(), 1, i, "Début : " + ScheduleStore.NAMES[i]));
                result.add(new Event(midnight.plusMinutes(end).toInstant().toEpochMilli(), 2, i, "Fin du repas"));
                result.add(new Event(midnight.plusMinutes(end + 30).toInstant().toEpochMilli(), 3, i, "Reprise des boissons"));
            }
        }
        Collections.sort(result, Comparator.comparingLong(e -> e.time));
        return result;
    }

    private void refreshCountdown() {
        if (heroTitle == null || countdown == null) return;
        long now = System.currentTimeMillis();
        Event last = null;
        Event next = null;
        for (Event e : events()) {
            if (e.time <= now) last = e;
            else { next = e; break; }
        }
        int kind = last == null ? 3 : last.kind;
        if (kind == 1) {
            heroCard.setBackground(round(ROSE_LIGHT, 24));
            heroTitle.setText("Repas en cours 🍽");
            heroSubtitle.setText("Prends ton temps, 30 minutes sont prévues.");
        } else if (kind == 0 || kind == 2) {
            heroCard.setBackground(round(PEACH, 24));
            heroTitle.setText("Pause boisson ♡");
            heroSubtitle.setText("Attends la prochaine reprise des boissons.");
        } else {
            heroCard.setBackground(round(GREEN, 24));
            heroTitle.setText("Tu peux boire 💧");
            heroSubtitle.setText("Jusqu'à la prochaine pause boisson.");
        }
        if (next != null) {
            long s = Math.max(0, (next.time - now + 999) / 1000);
            long h = s / 3600;
            long m = (s % 3600) / 60;
            long sec = s % 60;
            countdown.setText(String.format(Locale.FRANCE, "%02d:%02d:%02d", h, m, sec));
            String when = new java.text.SimpleDateFormat("HH:mm", Locale.FRANCE).format(new java.util.Date(next.time));
            nextInfo.setText(when + "  ·  " + next.label);
        }
    }

    private void buildPlanning() {
        sectionTitle("Mon planning 🌸", "Choisis l'heure d'un repas, tous les autres se calculent automatiquement.");
        LinearLayout editor = card(ROSE_LIGHT);
        editor.addView(text("Repas de référence", 14, INK, true));
        addGap(editor, 5);
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ScheduleStore.NAMES);
        spinner.setAdapter(adapter);
        spinner.setSelection(anchorIndex);
        editor.addView(spinner, new LinearLayout.LayoutParams(-1, dp(45)));
        addGap(editor, 11);
        editor.addView(text("Heure souhaitée", 14, INK, true));
        addGap(editor, 7);
        Button time = button(ScheduleStore.hhmm(ScheduleStore.times(this)[anchorIndex]) + "  ·  Changer l'heure", false);
        selectedMinute = ScheduleStore.times(this)[anchorIndex];
        time.setOnClickListener(v -> {
            int hour = selectedMinute / 60;
            int minute = selectedMinute % 60;
            new TimePickerDialog(this, (picker, h, m) -> {
                selectedMinute = h * 60 + m;
                time.setText(ScheduleStore.hhmm(selectedMinute) + "  ·  Changer l'heure");
            }, hour, minute, true).show();
        });
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                anchorIndex = position;
                selectedMinute = ScheduleStore.times(MainActivity.this)[anchorIndex];
                time.setText(ScheduleStore.hhmm(selectedMinute) + "  ·  Changer l'heure");
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        addCard(editor, time, 14);
        editor.addView(text("Intervalle après la fin du repas", 14, INK, true));
        addGap(editor, 5);
        RadioGroup gaps = new RadioGroup(this);
        gaps.setOrientation(RadioGroup.HORIZONTAL);
        String[] captions = {"2h30", "2h45", "3h"};
        int[] values = {150, 165, 180};
        chosenGap = ScheduleStore.gap(this);
        for (int i = 0; i < values.length; i++) {
            final int gap = values[i];
            RadioButton r = new RadioButton(this);
            r.setId(View.generateViewId());
            r.setText(captions[i]);
            r.setTextColor(INK);
            r.setTextSize(12);
            r.setButtonTintList(android.content.res.ColorStateList.valueOf(ROSE));
            r.setChecked(chosenGap == gap);
            r.setOnClickListener(v -> chosenGap = gap);
            gaps.addView(r, new RadioGroup.LayoutParams(0, dp(45), 1));
        }
        editor.addView(gaps);
        addGap(editor, 9);
        Button calculate = button("Recalculer mes 5 repas", true);
        calculate.setOnClickListener(v -> {
            int idx = spinner.getSelectedItemPosition();
            if (ScheduleStore.saveAnchor(this, idx, selectedMinute, chosenGap)) {
                AlarmScheduler.rescheduleAll(this);
                toast("Tes horaires sont enregistrés.");
                showTab(1);
            } else {
                new AlertDialog.Builder(this).setTitle("Horaires incompatibles")
                    .setMessage("Avec cet intervalle, le premier repas commencerait trop tôt ou le dernier se terminerait après minuit. Choisis une autre heure ou un autre intervalle.")
                    .setPositiveButton("OK", null).show();
            }
        });
        addCard(editor, calculate, 0);
        addCard(page, editor, 20);

        page.addView(text("Mes horaires du jour", 18, INK, true));
        addGap(page, 10);
        int[] meals = ScheduleStore.times(this);
        for (int i = 0; i < ScheduleStore.MEALS; i++) {
            LinearLayout c = card(WHITE);
            LinearLayout top = horizontal();
            top.addView(text((i + 1) + ".  " + ScheduleStore.NAMES[i], 16, INK, true), new LinearLayout.LayoutParams(0, -2, 1));
            TextView badge = text(ScheduleStore.isCompleted(this, i) ? "Terminé ✓" : ScheduleStore.hhmm(meals[i]), 14, ROSE, true);
            top.addView(badge);
            c.addView(top);
            addGap(c, 10);
            detailLine(c, "Arrêt des boissons", ScheduleStore.hhmm(meals[i] - 30), PEACH_INK);
            detailLine(c, "Début du repas", ScheduleStore.hhmm(meals[i]), INK);
            detailLine(c, "Fin prévue", ScheduleStore.hhmmDisplay(meals[i] + 30), INK);
            int actual = ScheduleStore.actualEnd(this, i, LocalDate.now());
            detailLine(c, "Reprise des boissons", ScheduleStore.hhmmDisplay((actual < 0 ? meals[i] + 30 : actual) + 30), GREEN_INK);
            addCard(page, c, 10);
        }
        page.addView(text("Le calcul prévoit 30 minutes par repas et 30 minutes sans boisson avant et après. Le 5e repas peut se terminer à minuit.", 12, MUTED, false));
    }

    private void detailLine(LinearLayout box, String label, String time, int color) {
        LinearLayout row = horizontal();
        TextView left = text(label, 13, MUTED, false);
        row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(text(time, 13, color, true));
        box.addView(row);
        addGap(box, 6);
    }

    private Switch toggle(String title, String subtitle, boolean enabled, java.util.function.Consumer<Boolean> action) {
        LinearLayout wrapper = vertical();
        wrapper.setPadding(dp(2), dp(8), dp(2), dp(9));
        LinearLayout line = horizontal();
        LinearLayout labels = vertical();
        labels.addView(text(title, 15, INK, true));
        if (!subtitle.isEmpty()) { addGap(labels, 2); labels.addView(text(subtitle, 12, MUTED, false)); }
        line.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        Switch sw = new Switch(this);
        sw.setChecked(enabled);
        sw.setThumbTintList(new android.content.res.ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{ROSE, Color.rgb(220, 210, 216)}));
        sw.setOnCheckedChangeListener((button, isOn) -> action.accept(isOn));
        line.addView(sw);
        wrapper.addView(line);
        page.addView(wrapper);
        return sw;
    }

    private void buildAlerts() {
        sectionTitle("Mes notifications ♡", "Une seule notification avec vibration pour chaque événement activé.");
        LinearLayout info = card(LAVENDER);
        info.addView(text("Notification + vibration", 16, INK, true));
        addGap(info, 6);
        info.addView(text("Les alertes peuvent aussi apparaître sur ta montre si elle reçoit les notifications du téléphone.", 13, MUTED, false));
        addCard(page, info, 19);
        page.addView(text("Gérer les alertes", 18, INK, true));
        addGap(page, 9);
        toggle("Activer toutes les alertes", "Pour les repas et les boissons", ScheduleStore.alertsEnabled(this), checked -> {
            ScheduleStore.prefs(this).edit().putBoolean("alerts", checked).apply();
            AlarmScheduler.rescheduleAll(this);
        });
        toggle("Arrêt des boissons", "30 min avant chaque repas", ScheduleStore.eventEnabled(this, 0), checked -> {
            ScheduleStore.prefs(this).edit().putBoolean("event_0", checked).apply();
            AlarmScheduler.rescheduleAll(this);
        });
        toggle("Début du repas", "À l'heure prévue", ScheduleStore.eventEnabled(this, 1), checked -> {
            ScheduleStore.prefs(this).edit().putBoolean("event_1", checked).apply();
            AlarmScheduler.rescheduleAll(this);
        });
        toggle("Reprise des boissons", "30 min après la fin du repas", ScheduleStore.eventEnabled(this, 2), checked -> {
            ScheduleStore.prefs(this).edit().putBoolean("event_2", checked).apply();
            AlarmScheduler.rescheduleAll(this);
        });
        addGap(page, 18);
        LinearLayout warning = card(PEACH);
        warning.addView(text("Pour ne rien manquer", 15, INK, true));
        addGap(warning, 7);
        warning.addView(text("Autorise les notifications, la vibration et, si nécessaire, les alarmes exactes dans les réglages Android. Certaines économies de batterie peuvent retarder les alertes.", 13, MUTED, false));
        addGap(warning, 12);
        Button access = button("Vérifier les autorisations", false);
        access.setOnClickListener(v -> openPermissions());
        warning.addView(access);
        addCard(page, warning, 10);
    }

    private void openPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (mgr != null && !mgr.canScheduleExactAlarms()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    return;
                } catch (Exception ignored) { }
            }
        }
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void buildSettings() {
        sectionTitle("Mes paramètres ⚙", "Une routine souple, adaptée à tes journées.");
        LinearLayout routines = card(WHITE);
        routines.addView(text("Règles de ma journée", 17, INK, true));
        addGap(routines, 11);
        detailLine(routines, "Prises alimentaires", "5 par jour", ROSE);
        detailLine(routines, "Durée d'un repas", "30 min", ROSE);
        detailLine(routines, "Intervalle après la fin", gapText(ScheduleStore.gap(this)), ROSE);
        detailLine(routines, "Arrêt boissons avant", "30 min", ROSE);
        detailLine(routines, "Reprise boissons après", "30 min", ROSE);
        detailLine(routines, "Dernier repas terminé", "Avant minuit", ROSE);
        addCard(page, routines, 15);
        LinearLayout note = card(LAVENDER);
        note.addView(text("Heure de lever variable", 16, INK, true));
        addGap(note, 7);
        note.addView(text("Tu n'as pas besoin de te lever à heure fixe. Dans Planning, choisis l'heure de n'importe quel repas, par exemple un dîner à 19h30, puis laisse l'application calculer la journée.", 13, MUTED, false));
        addGap(note, 13);
        Button edit = button("Ajuster mon planning", true);
        edit.setOnClickListener(v -> showTab(1));
        note.addView(edit);
        addCard(page, note, 15);
        LinearLayout help = card(ROSE_LIGHT);
        help.addView(text("À savoir", 16, INK, true));
        addGap(help, 7);
        help.addView(text("L'application conserve tes horaires sur ton téléphone, sans compte et sans connexion Internet. Elle sert à organiser les consignes qui t'ont été données ; en cas de différence, suis toujours celles de ton équipe soignante.", 13, MUTED, false));
        addGap(help, 9);
        help.addView(text("Mon Rythme Sleeve  ·  Version 1.0", 12, MUTED, false));
        addCard(page, help, 8);
    }
    private String gapText(int minutes) {
        if (minutes == 165) return "2h45";
        if (minutes == 180) return "3h";
        return "2h30";
    }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
