package com.wirelessclock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private final Handler handler = new Handler();
    private BroadcastReceiver chargingReceiver;
    private boolean isLandscape = false;
    private SharedPreferences prefs;

    private static final Set<String> HOLIDAYS = new HashSet<>();
    static {
        String[][] data = {
            {"2025","01-01"},{"2025","01-06"},{"2025","03-24"},{"2025","04-17"},
            {"2025","04-18"},{"2025","05-01"},{"2025","06-02"},{"2025","06-23"},
            {"2025","06-30"},{"2025","07-20"},{"2025","08-07"},{"2025","08-18"},
            {"2025","10-13"},{"2025","11-03"},{"2025","11-17"},{"2025","12-08"},{"2025","12-25"},
            {"2026","01-01"},{"2026","01-12"},{"2026","03-23"},{"2026","04-02"},
            {"2026","04-03"},{"2026","05-01"},{"2026","05-18"},{"2026","06-08"},
            {"2026","06-15"},{"2026","06-29"},{"2026","07-20"},{"2026","08-07"},
            {"2026","08-17"},{"2026","10-12"},{"2026","11-02"},{"2026","11-16"},
            {"2026","12-08"},{"2026","12-25"},
            {"2027","01-01"},{"2027","01-11"},{"2027","03-22"},{"2027","03-25"},
            {"2027","03-26"},{"2027","05-01"},{"2027","05-10"},{"2027","05-31"},
            {"2027","06-07"},{"2027","07-05"},{"2027","07-20"},{"2027","08-07"},
            {"2027","08-16"},{"2027","10-18"},{"2027","11-01"},{"2027","11-15"},
            {"2027","12-08"},{"2027","12-25"}
        };
        for (String[] d : data) HOLIDAYS.add(d[0] + "-" + d[1]);
    }

    private static final String[] DAYS = {"Domingo","Lunes","Martes","Miércoles","Jueves","Viernes","Sábado"};
    private static final String[] MONTHS = {"Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
    private static final String[] MONTHS_UP = {"ENERO","FEBRERO","MARZO","ABRIL","MAYO","JUNIO","JULIO","AGOSTO","SEPTIEMBRE","OCTUBRE","NOVIEMBRE","DICIEMBRE"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        prefs = getSharedPreferences("WCPrefs", MODE_PRIVATE);
        isLandscape = prefs.getBoolean("land", false);
        setRequestedOrientation(isLandscape ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        ImageButton btn = findViewById(R.id.btn_rotate);
        if (btn != null) btn.setOnClickListener(v -> {
            isLandscape = !isLandscape;
            prefs.edit().putBoolean("land", isLandscape).apply();
            setRequestedOrientation(isLandscape ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        });
        handler.post(tick);
        registerCharging();
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            updateClock();
            updateCalendar();
            handler.postDelayed(this, 1000);
        }
    };

    private void updateClock() {
        Calendar c = Calendar.getInstance();
        int h = c.get(Calendar.HOUR); if (h == 0) h = 12;
        int m = c.get(Calendar.MINUTE);
        boolean am = c.get(Calendar.AM_PM) == Calendar.AM;
        setText(R.id.tv_time, String.format(Locale.getDefault(), "%02d:%02d", h, m));
        setText(R.id.tv_ampm, am ? "AM" : "PM");
        setText(R.id.tv_date, DAYS[c.get(Calendar.DAY_OF_WEEK)-1].toUpperCase() + ", " + c.get(Calendar.DAY_OF_MONTH) + " " + MONTHS[c.get(Calendar.MONTH)].toUpperCase());
    }

    private void updateCalendar() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR), month = c.get(Calendar.MONTH), today = c.get(Calendar.DAY_OF_MONTH);
        setText(R.id.tv_cal_month, MONTHS_UP[month]);
        setText(R.id.tv_cal_year, String.valueOf(year));
        LinearLayout grid = findViewById(R.id.cal_grid);
        if (grid == null) return;
        grid.removeAllViews();
        int firstDow = new java.util.GregorianCalendar(year, month, 1).get(Calendar.DAY_OF_WEEK) - 1;
        int days = new java.util.GregorianCalendar(year, month + 1, 0).get(Calendar.DAY_OF_MONTH);
        String y = String.format(Locale.US, "%04d", year);
        String mo = String.format(Locale.US, "%02d", month + 1);
        int cellSz = dpToPx(28);
        LinearLayout row = null;
        for (int i = 0; i < firstDow + days; i++) {
            if (i % 7 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                grid.addView(row);
            }
            TextView cell = new TextView(this);
            cell.setLayoutParams(new LinearLayout.LayoutParams(0, cellSz, 1f));
            cell.setGravity(android.view.Gravity.CENTER);
            cell.setTextSize(9.5f);
            if (i >= firstDow) {
                int d = i - firstDow + 1;
                cell.setText(String.valueOf(d));
                String key = y + "-" + mo + "-" + String.format(Locale.US, "%02d", d);
                int dow = new java.util.GregorianCalendar(year, month, d).get(Calendar.DAY_OF_WEEK);
                boolean holiday = HOLIDAYS.contains(key);
                boolean sunday = dow == Calendar.SUNDAY;
                boolean isToday = d == today;
                if (isToday && holiday) { cell.setBackgroundResource(R.drawable.circle_red); cell.setTextColor(Color.WHITE); }
                else if (isToday) { cell.setBackgroundResource(R.drawable.circle_white); cell.setTextColor(Color.parseColor("#050505")); }
                else if (holiday) { cell.setTextColor(Color.parseColor("#e74c3c")); }
                else if (sunday) { cell.setTextColor(Color.parseColor("#7a2a2a")); }
                else { cell.setTextColor(Color.parseColor("#555555")); }
            }
            if (row != null) row.addView(cell);
        }
    }

    private void setText(int id, String t) { TextView v = findViewById(id); if (v != null) v.setText(t); }
    private int dpToPx(int dp) { return (int)(dp * getResources().getDisplayMetrics().density); }

    private void registerCharging() {
        IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        chargingReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent i) {
                int plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean wireless = plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
                boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
                View badge = findViewById(R.id.charging_badge);
                if (badge != null) badge.setVisibility(wireless && charging ? View.VISIBLE : View.GONE);
            }
        };
        registerReceiver(chargingReceiver, f);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(tick);
        try { if (chargingReceiver != null) unregisterReceiver(chargingReceiver); } catch (Exception ignored) {}
    }
}
