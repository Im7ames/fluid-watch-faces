package com.fluid.glance;

import android.content.SharedPreferences;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.wear.protolayout.ActionBuilders.*;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.LayoutElementBuilders.*;
import androidx.wear.protolayout.ModifiersBuilders.*;
import androidx.wear.protolayout.ResourceBuilders.Resources;
import androidx.wear.protolayout.TimelineBuilders.Timeline;
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry;
import androidx.wear.protolayout.TypeBuilders.StringProp;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant;
import androidx.wear.protolayout.expression.util.DynamicDateFormat;
import androidx.wear.tiles.RequestBuilders.*;
import androidx.wear.tiles.TileBuilders.Tile;
import androidx.wear.tiles.TileService;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import static androidx.wear.protolayout.ColorBuilders.argb;
import static androidx.wear.protolayout.DimensionBuilders.*;
import static androidx.wear.protolayout.LayoutElementBuilders.*;

public final class GlanceTile extends TileService {
    private static final String VERSION = "glance-1";
    private static final int INK = 0xfff1eaf6;

    @Override protected ListenableFuture<Tile> onTileRequest(TileRequest request) {
        Weather.refresh(this, () -> getUpdater(this).requestUpdate(GlanceTile.class));
        Layout layout = new Layout.Builder().setRoot(render(request.getDeviceConfiguration())).build();
        Timeline timeline = new Timeline.Builder().addTimelineEntry(
                new TimelineEntry.Builder().setLayout(layout).build()).build();
        return completed(new Tile.Builder().setResourcesVersion(VERSION).setTileTimeline(timeline)
                .setFreshnessIntervalMillis(60_000).build());
    }

    @Override protected ListenableFuture<Resources> onTileResourcesRequest(ResourcesRequest request) {
        return completed(new Resources.Builder().setVersion(VERSION).build());
    }

    private LayoutElement render(DeviceParameters device) {
        SharedPreferences p = Glance.prefs(this);
        ZonedDateTime now = ZonedDateTime.now();
        int accent = Glance.ACCENTS[p.getInt("accent", 0)];
        float scale = new float[]{1f, .87f, 1.11f}[p.getInt("size", 0)];
        float width = device == null || device.getScreenWidthDp() < 100
                ? 162 : Math.min(device.getScreenWidthDp() - 30, 176);
        int layout = p.getInt("layout", 0);
        Column.Builder stack = new Column.Builder().setWidth(dp(width)).setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER);
        boolean showTime = p.getBoolean("time", true);
        boolean showDate = p.getBoolean("date", true);
        boolean showBattery = p.getBoolean("battery", true);
        boolean showWeather = p.getBoolean("weather", true);

        if (layout == 1 && showWeather) {
            stack.addContent(pill(Weather.value(p), width, 34, 16 * scale, accent, 0xff332742));
            gap(stack, 5);
        }
        if (showTime) {
            String pattern = Glance.clockPattern(this);
            StringProp.Builder current = new StringProp.Builder(now.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault())));
            if (device != null && device.getRendererSchemaVersion() != null) {
                var version = device.getRendererSchemaVersion();
                if (version.getMajor() > 1 || version.getMinor() >= 300) current.setDynamicValue(
                        new DynamicDateFormat(pattern, now.getZone()).format(DynamicInstant.platformTimeWithSecondsPrecision()));
            }
            stack.addContent(text(current.build(), (layout == 0 ? 30 : 24) * scale, accent, true));
        }
        if (showDate) stack.addContent(text(new StringProp.Builder(Glance.date(this, now)).build(), 12 * scale, INK, false));
        if (showBattery || (showWeather && layout != 1)) gap(stack, 5);
        if (showBattery && showWeather && layout == 2) {
            String weather = Weather.value(p);
            int separator = weather.indexOf(" · ");
            if (separator >= 0) weather = weather.substring(0, separator);
            String battery = Glance.battery(this).replace(" · Charging", " ⚡");
            stack.addContent(new Row.Builder()
                    .addContent(pill(battery, (width - 4) / 2, 31, 12 * scale, INK, 0xff28252f))
                    .addContent(new Spacer.Builder().setWidth(dp(4)).build())
                    .addContent(pill(weather, (width - 4) / 2, 31, 12 * scale, accent, 0xff332742)).build());
        } else {
            if (showBattery) stack.addContent(pill("Battery  " + Glance.battery(this), width, 23, 11 * scale, INK, 0xff28252f));
            if (showWeather && layout != 1) {
                if (showBattery) gap(stack, 3);
                stack.addContent(pill(Weather.value(p), width, 23, 11 * scale, accent, 0xff332742));
            }
        }
        gap(stack, 5);
        Clickable edit = new Clickable.Builder().setId("settings").setOnClick(new LaunchAction.Builder()
                .setAndroidActivity(new AndroidActivity.Builder().setPackageName(getPackageName())
                        .setClassName(SettingsActivity.class.getName()).build()).build()).build();
        stack.addContent(new Box.Builder().setWidth(dp(84)).setHeight(dp(24))
                .setModifiers(new Modifiers.Builder().setClickable(edit).setBackground(background(0xff292531, 12)).build())
                .addContent(text(new StringProp.Builder("Customize").build(), 10, accent, true)).build());
        return new Box.Builder().setWidth(expand()).setHeight(expand())
                .setModifiers(new Modifiers.Builder().setBackground(background(0xff141219, 0)).build())
                .addContent(stack.build()).build();
    }

    private static void gap(Column.Builder stack, int height) {
        stack.addContent(new Spacer.Builder().setHeight(dp(height)).build());
    }

    private static Box pill(String value, float width, int height, float size, int color, int fill) {
        return new Box.Builder().setWidth(dp(width)).setHeight(dp(height))
                .setModifiers(new Modifiers.Builder().setBackground(background(fill, height / 2f)).build())
                .addContent(text(new StringProp.Builder(value).build(), size, color, true)).build();
    }

    private static Text text(StringProp value, float size, int color, boolean bold) {
        return new Text.Builder().setText(value).setMaxLines(1)
                .setOverflow(TEXT_OVERFLOW_ELLIPSIZE_END)
                .setFontStyle(new FontStyle.Builder().setSize(sp(size)).setColor(argb(color))
                        .setWeight(bold ? FONT_WEIGHT_BOLD : FONT_WEIGHT_NORMAL).build()).build();
    }

    private static Background background(int color, float radius) {
        return new Background.Builder().setColor(argb(color))
                .setCorner(new Corner.Builder().setRadius(dp(radius)).build()).build();
    }

    private static <T> ListenableFuture<T> completed(T value) {
        ResolvableFuture<T> future = ResolvableFuture.create();
        future.set(value);
        return future;
    }
}

