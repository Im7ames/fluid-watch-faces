package com.fluid.glance;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.*;
import androidx.wear.tiles.TileService;

public final class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout panel;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = Glance.prefs(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xff141219);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(24), dp(27), dp(24), dp(30));
        scroll.addView(panel);
        setContentView(scroll);
        TextView heading = label("Fluid Glance", 24);
        heading.setTextColor(0xffd4bbff);
        label("Tile settings", 15);
        choices("Layout", "layout", "Clock first", "Weather first", "Compact");
        choices("Accent", "accent", "Lilac", "Sky", "Mint", "Peach");
        choices("Text size", "size", "Standard", "Small", "Large");
        toggle("Time", "time");
        toggle("Date", "date");
        toggle("Battery", "battery");
        toggle("Weather", "weather");
        choices("Time format", "clock", "Watch setting", "12-hour", "24-hour");
        choices("Date format", "dateFormat", "Thu, 26 Nov", "26/11/2026", "Nov 26, 2026", "2026-11-26");
        choices("Battery format", "batteryFormat", "Percentage", "Charging status");
        choices("Weather format", "weatherFormat", "Temp + conditions", "Temperature only");
        choices("Temperature", "units", "Celsius", "Fahrenheit");
        label("Weather city", 15);
        EditText city = new EditText(this);
        city.setSingleLine();
        city.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        city.setHint("Enter a city");
        city.setText(prefs.getString("city", ""));
        panel.addView(city);
        Button save = new Button(this);
        save.setText("Save city");
        panel.addView(save);
        save.setOnClickListener(view -> {
            prefs.edit().putString("city", city.getText().toString().trim()).apply();
            updateTile();
            Toast.makeText(this, "City saved", Toast.LENGTH_SHORT).show();
        });
        label("Weather data by Open-Meteo. * means cached weather is over two hours old.", 12);
        Button done = new Button(this);
        done.setText("Done");
        done.setOnClickListener(view -> finish());
        panel.addView(done);
    }

    private void choices(String title, String key, String... values) {
        label(title, 14);
        Spinner spinner = new Spinner(this, Spinner.MODE_DIALOG);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(prefs.getInt(key, 0));
        panel.addView(spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View view, int position, long id) {
                if (prefs.getInt(key, 0) == position) return;
                prefs.edit().putInt(key, position).apply();
                updateTile();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void toggle(String title, String key) {
        Switch toggle = new Switch(this);
        toggle.setText(title);
        toggle.setMinHeight(dp(48));
        toggle.setChecked(prefs.getBoolean(key, true));
        panel.addView(toggle);
        toggle.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(key, checked).apply();
            updateTile();
        });
    }

    private TextView label(String title, int size) {
        TextView label = new TextView(this);
        label.setText(title);
        label.setTextSize(size);
        label.setTextColor(Color.WHITE);
        label.setPadding(0, dp(7), 0, 0);
        panel.addView(label);
        return label;
    }

    private void updateTile() { TileService.getUpdater(this).requestUpdate(GlanceTile.class); }
    private int dp(int size) { return Math.round(size * getResources().getDisplayMetrics().density); }
}
