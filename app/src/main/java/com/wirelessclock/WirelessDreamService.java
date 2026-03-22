package com.wirelessclock;

import android.content.Intent;
import android.os.BatteryManager;
import android.service.dreams.DreamService;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class WirelessDreamService extends DreamService {

    private Handler handler = new Handler();
    private boolean isLandscape = false;
    private SharedPreferences prefs;

    private static final Set<String> COLOMBIA_HOLIDAYS = new HashSet<>();

    static {
        // 2025
        addHolidays("2025", new String[]{
            "01-01","01-06","03-24","04-17","04-18","05-01","06-02","06-23","06-30",
            "07-20","08-07","08-18","10-13","11-03","11-17","12-08","12-25"
        });
        // 2026
        addHolidays("2026", new String[]{
            "01-01","01-12","03-23","04-02","04-03","05-01","05-18","06-08","06-15",
            "06-29","07-20","08-07","08-17","10-12","11-02","11-16","12-08","12-25"
        });
        // 2027
        addHolidays("2027", new String[]{
            "01-01","01-11","03-22","03-25","03-26","05-01","05-10","05-31","06-07",
            "07-05","07-20","08-07","08-16","10-18","11-01","11-15","12-08","12-25"
        });
        // 2028
        addHolidays("2028", new String[]{
            "01-01","01-10","04-13","04-14","05-01","05-22","06-12","06-19","07-03",
            "07-20","08-07","08-21","10-16","11-06","11-13","12-08","12-25"
        });
    }

    private static void addHolidays(String year, String[] dates) {
        for (String d : dates) COLOMBIA_HOLIDAYS.add(year + "-" + d);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(true);
        setFullscreen(true);
        setScreenBright(true);

        prefs = getSharedPreferences("WirelessClockPrefs", MODE_PRIVATE);
        isLandscape = prefs.getBoolean("orientation", false);

        View contentView = LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        setContentView(contentView);

        ImageButton btn = contentView.findViewById(R.id.btn_rotate);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                isLandscape = !isLandscape;
                prefs.edit().putBoolean("orientation", isLandscape).apply();
                // restart dream to apply orientation
                finish();
            });
        }

        handler.post(clockRunnable);
    }

    private Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            updateDateTime();
            updateCalendar();
            handler.postDelayed(this, 1000);
        }
    };

    private void updateDateTime() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR);
        if (hour == 0) hour = 12;
        int minute = cal.get(Calendar.MINUTE);
        boolean isAM = cal.get(Calendar.AM_PM) == Calendar.AM;

        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        String[] dayNames = {"Domingo","Lunes","Martes","Miércoles","Jueves","Viernes","Sábado"};
        String[] monthNames = {"Enero","Febrero","Marzo","Abril","Mayo","Junio",
                "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};

        String dayName = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1];
        String monthName = monthNames[cal.get(Calendar.MONTH)];
        int dayNum = cal.get(Calendar.DAY_OF_MONTH);
        String dateStr = (dayName + ", " + dayNum + " " + monthName).toUpperCase();

        setText(R.id.tv_time, timeStr);
        setText(R.id.tv_ampm, isAM ? "AM" : "PM");
        setText(R.id.tv_date, dateStr);

        View badge = getWindow().getDecorView().findViewWithTag("charging_badge");
        View chBadge = ((View) getWindow().getDecorView()).findViewWithTag("charging_badge");

        View rootView = getWindow().getDecorView().findViewWithTag(null);
        View chargingBadge = ((View)getWindow().getDecorView()).findViewWithTag("charging");
        if (chargingBadge != null) chargingBadge.setVisibility(View.VISIBLE);

        View cbadge = getWindow().getDecorView().findViewById(R.id.charging_badge);
        if (cbadge != null) cbadge.setVisibility(View.VISIBLE);
        View ctv = getWindow().getDecorView().findViewById(R.id.tv_charging);
        if (ctv != null) ctv.setVisibility(View.VISIBLE);
    }

    private void updateCalendar() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int today = cal.get(Calendar.DAY_OF_MONTH);

        String[] monthNames = {"ENERO","FEBRERO","MARZO","ABRIL","MAYO","JUNIO",
                "JULIO","AGOSTO","SEPTIEMBRE","OCTUBRE","NOVIEMBRE","DICIEMBRE"};

        setText(R.id.tv_cal_month, monthNames[month]);
        setText(R.id.tv_cal_year, String.valueOf(year));

        View root = getWindow().getDecorView();
        LinearLayout calGrid = root.findViewById(R.id.cal_grid);
        if (calGrid == null) return;
        calGrid.removeAllViews();

        Calendar firstDay = Calendar.getInstance();
        firstDay.set(year, month, 1);
        int startDow = firstDay.get(Calendar.DAY_OF_WEEK) - 1;

        Calendar lastDay = Calendar.getInstance();
        lastDay.set(year, month + 1, 0);
        int daysInMonth = lastDay.get(Calendar.DAY_OF_MONTH);

        String yearStr = String.format(Locale.getDefault(), "%04d", year);
        String monthStr = String.format(Locale.getDefault(), "%02d", month + 1);

        LinearLayout row = null;
        int cellIndex = 0;

        for (int i = 0; i < startDow + daysInMonth; i++) {
            if (cellIndex % 7 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                row.setLayoutParams(rowParams);
                calGrid.addView(row);
            }

            TextView cell = new TextView(this);
            int cellSizePx = dpToPx(26);
            LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, cellSizePx, 1f);
            cell.setLayoutParams(cellParams);
            cell.setTextSize(9.5f);
            cell.setGravity(android.view.Gravity.CENTER);

            if (i >= startDow) {
                int dayNum = i - startDow + 1;
                cell.setText(String.valueOf(dayNum));
                String dayStr = String.format(Locale.getDefault(), "%02d", dayNum);
                String dateKey = yearStr + "-" + monthStr + "-" + dayStr;
                Calendar dayCal = Calendar.getInstance();
                dayCal.set(year, month, dayNum);
                int dow = dayCal.get(Calendar.DAY_OF_WEEK);
                boolean isHoliday = COLOMBIA_HOLIDAYS.contains(dateKey);
                boolean isSunday = (dow == Calendar.SUNDAY);
                boolean isToday = (dayNum == today);

                if (isToday && isHoliday) {
                    cell.setBackgroundResource(R.drawable.circle_red);
                    cell.setTextColor(Color.WHITE);
                } else if (isToday) {
                    cell.setBackgroundResource(R.drawable.circle_white);
                    cell.setTextColor(Color.parseColor("#050505"));
                } else if (isHoliday) {
                    cell.setTextColor(Color.parseColor("#e74c3c"));
                } else if (isSunday) {
                    cell.setTextColor(Color.parseColor("#7a2a2a"));
                } else {
                    cell.setTextColor(Color.parseColor("#555555"));
                }
            }

            if (row != null) row.addView(cell);
            cellIndex++;
        }

        if (cellIndex % 7 != 0 && row != null) {
            while (cellIndex % 7 != 0) {
                TextView cell = new TextView(this);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dpToPx(26), 1f);
                cell.setLayoutParams(p);
                row.addView(cell);
                cellIndex++;
            }
        }
    }

    private void setText(int id, String text) {
        View root = getWindow().getDecorView();
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(clockRunnable);
    }
}
