/*
 * SPDX-FileCopyrightText: DriftVaneOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.driftvane;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.app.UiModeManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * DriftVaneOS Settings home styles, picked in DriftHome's first-boot setup or in
 * DriftVane enhancements (Settings.System driftvane_settings_style):
 *   0 = stock, 1 = Live tiles, 2 = Device card, 3 = Category chips.
 */
public final class DriftHomepage {
    private static final String TAG = "DriftHomepage";
    public static final String KEY_STYLE = "driftvane_settings_style";
    private static final String HEADER_KEY = "driftvane_homepage_header";
    private static final int BLUE = 0xFF2F6BFF;

    private DriftHomepage() {}

    public static int style(Context c) {
        return Settings.System.getIntForUser(c.getContentResolver(), KEY_STYLE, 0,
                UserHandle.USER_CURRENT);
    }

    /** Adds the header for the current style to the top of the Settings homepage. */
    public static void apply(PreferenceFragmentCompat fragment, int style) {
        try {
            final PreferenceScreen screen = fragment.getPreferenceScreen();
            if (screen == null || style == 0) return;
            final Context c = fragment.getPreferenceManager().getContext();
            if (screen.findPreference(HEADER_KEY) != null) return;
            final HeaderPreference header = new HeaderPreference(c, fragment, style);
            header.setKey(HEADER_KEY);
            header.setOrder(-10000);
            screen.addPreference(header);
            if (style == 1) {
                // these live in the tiles now
                for (String k : TILE_KEYS) {
                    Preference p = screen.findPreference(k);
                    if (p != null) p.setVisible(false);
                }
            } else if (style == 2) {
                colorIcons(screen);
            }
        } catch (Throwable t) {
            // never break Settings over a look
            Log.e(TAG, "couldn't apply style " + style, t);
        }
    }

    /** Re-reads live values (Wi-Fi name, battery, toggles) when you come back to Settings. */
    public static void refresh(PreferenceFragmentCompat fragment) {
        try {
            Preference p = fragment.findPreference(HEADER_KEY);
            if (p instanceof HeaderPreference) ((HeaderPreference) p).refresh();
        } catch (Throwable ignored) { }
    }

    // ------------------------------------------------------------------ helpers
    private static boolean night(Context c) {
        return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }
    private static int card(Context c) { return night(c) ? 0xFF1A2138 : 0xFFE6ECFA; }
    private static int text(Context c) { return night(c) ? 0xFFE8EEFF : 0xFF101828; }
    private static int sub(Context c) { return night(c) ? 0xFF8EA3D6 : 0xFF4A5A80; }

    private static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    private static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    private static TextView label(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    /** Opens a homepage entry exactly as if it had been tapped in the list. */
    private static void open(PreferenceFragmentCompat f, String key) {
        Preference p = f.findPreference(key);
        if (p != null) f.onPreferenceTreeClick(p);
    }

    // ------------------------------------------------------------------ S2 colors
    private static final String[] COLOR_KEYS = {
        "top_level_network", "top_level_connected_devices", "top_level_apps",
        "top_level_notifications", "top_level_sound", "top_level_priority_modes",
        "top_level_display", "top_level_wallpaper", "top_level_driftvane", "top_level_storage",
        "top_level_battery", "top_level_system", "top_level_about_device",
        "top_level_safety_center", "top_level_security", "top_level_privacy",
        "top_level_location", "top_level_accounts", "top_level_emergency",
        "top_level_accessibility",
    };
    private static final int[] COLORS = {
        0xFF2F6BFF, 0xFF2FBF8F, 0xFF5B8CFF, 0xFFFF8A3D, 0xFFFF8A3D, 0xFF9D6BFF,
        0xFF9D6BFF, 0xFFE05AC8, 0xFFFF5A7A, 0xFF2FBF8F, 0xFFFFB547, 0xFF6B7A99,
        0xFF6B7A99, 0xFF2FBF8F, 0xFF2FBF8F, 0xFF5B8CFF, 0xFF2FBF8F, 0xFFFF8A3D,
        0xFFFF5A5A, 0xFF5B8CFF,
    };

    private static void colorIcons(PreferenceGroup group) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference p = group.getPreference(i);
            if (p instanceof PreferenceGroup) {
                colorIcons((PreferenceGroup) p);
                continue;
            }
            int idx = -1;
            for (int k = 0; k < COLOR_KEYS.length; k++) {
                if (COLOR_KEYS[k].equals(p.getKey())) { idx = k; break; }
            }
            Drawable icon = p.getIcon();
            if (idx < 0 || !(icon instanceof LayerDrawable)) continue;
            LayerDrawable layers = (LayerDrawable) icon.mutate();
            if (layers.getNumberOfLayers() < 2) continue;
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(COLORS[idx]);
            layers.setDrawable(0, circle);
            layers.getDrawable(1).mutate().setTint(Color.WHITE);
            p.setIcon(layers);
        }
    }

    // ------------------------------------------------------------------ S1 tiles
    private static final String[] TILE_KEYS = {
        "top_level_network", "top_level_battery", "top_level_display",
        "top_level_sound", "top_level_apps", "top_level_privacy",
    };
    private static final String[] TILE_TITLES = {
        "Network", "Battery", "Look", "Sound", "Apps", "Privacy",
    };

    private static String tileValue(Context c, int i) {
        try {
            switch (i) {
                case 0: {
                    WifiManager wm = c.getSystemService(WifiManager.class);
                    if (wm == null || !wm.isWifiEnabled()) return "Wi-Fi off";
                    WifiInfo info = wm.getConnectionInfo();
                    String ssid = info == null ? null : info.getSSID();
                    if (ssid == null || ssid.contains("unknown")) return "Wi-Fi on";
                    return ssid.replace("\"", "");
                }
                case 1: {
                    BatteryManager bm = c.getSystemService(BatteryManager.class);
                    return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) + "%";
                }
                case 2:
                    return night(c) ? "Dark theme" : "Light theme";
                case 3: {
                    AudioManager am = c.getSystemService(AudioManager.class);
                    int max = Math.max(1, am.getStreamMaxVolume(AudioManager.STREAM_RING));
                    return "Ring " + (am.getStreamVolume(AudioManager.STREAM_RING) * 100 / max) + "%";
                }
                case 4: {
                    Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                    int n = c.getPackageManager().queryIntentActivities(launcher, 0).size();
                    return n + " apps";
                }
                case 5: {
                    LocationManager lm = c.getSystemService(LocationManager.class);
                    return lm != null && lm.isLocationEnabled() ? "Location on" : "Location off";
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "tile value " + i, t);
        }
        return "";
    }

    private static View tiles(Context c, PreferenceFragmentCompat f) {
        GridLayout grid = new GridLayout(c);
        grid.setColumnCount(2);
        int gap = dp(c, 5);
        for (int i = 0; i < TILE_KEYS.length; i++) {
            final String key = TILE_KEYS[i];
            boolean hero = i < 2;
            LinearLayout tile = new LinearLayout(c);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
            int bg = i == 0 ? BLUE : i == 1 ? 0xFFFFB547 : card(c);
            int fg = i == 0 ? Color.WHITE : i == 1 ? 0xFF412402 : text(c);
            int fg2 = i == 0 ? 0xCCFFFFFF : i == 1 ? 0xCC412402 : sub(c);
            tile.setBackground(rounded(bg, dp(c, 22)));
            tile.addView(label(c, TILE_TITLES[i], 16, fg, true));
            tile.addView(label(c, tileValue(c, i), 13, fg2, false));
            tile.setMinimumHeight(dp(c, hero ? 92 : 76));
            tile.setGravity(Gravity.BOTTOM);
            tile.setClickable(true);
            tile.setFocusable(true);
            tile.setOnClickListener(v -> open(f, key));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.width = 0;
            lp.setMargins(gap, gap, gap, gap);
            grid.addView(tile, lp);
        }
        return grid;
    }

    // ------------------------------------------------------------------ S2 device card
    private static View deviceCard(Context c, PreferenceFragmentCompat f) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(c, 18), dp(c, 16), dp(c, 18), dp(c, 18));
        box.setBackground(rounded(night(c) ? 0xFF1D2A52 : 0xFFDCE4FF, dp(c, 28)));
        String version = SystemProperties.get("ro.lineage.display.version", "");
        if (version.isEmpty()) version = Build.DISPLAY;
        String name = Settings.Global.getString(c.getContentResolver(), Settings.Global.DEVICE_NAME);
        if (name == null || name.isEmpty()) name = Build.MODEL;
        box.addView(label(c, "DriftVaneOS · " + version, 12, night(c) ? 0xFF85B7EB : 0xFF2F4FB0, false));
        TextView title = label(c, name, 22, text(c), true);
        title.setPadding(0, dp(c, 2), 0, dp(c, 12));
        box.addView(title);

        long total = 0, free = 0;
        try {
            StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
            total = fs.getTotalBytes();
            free = fs.getAvailableBytes();
        } catch (Throwable ignored) { }
        long used = total - free;
        long gb = 1000L * 1000L * 1000L;
        box.addView(label(c, "Storage " + (used / gb) + " of " + (total / gb) + " GB used", 12, sub(c), false));
        FrameLayout bar = new FrameLayout(c);
        bar.setBackground(rounded(night(c) ? 0xFF0E1430 : 0xFFC4D1F5, dp(c, 4)));
        View fill = new View(c);
        fill.setBackground(rounded(0xFF5B8CFF, dp(c, 4)));
        bar.addView(fill);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 8));
        barLp.topMargin = dp(c, 6);
        box.addView(bar, barLp);
        final float frac = total > 0 ? Math.min(1f, (float) used / total) : 0f;
        bar.post(() -> {
            fill.setLayoutParams(new FrameLayout.LayoutParams(Math.round(bar.getWidth() * frac),
                    ViewGroup.LayoutParams.MATCH_PARENT));
        });
        box.setClickable(true);
        box.setOnClickListener(v -> open(f, "top_level_about_device"));
        return box;
    }

    // ------------------------------------------------------------------ S3 chips
    private static final String[] CHIPS = {"Connect", "Look", "Sound", "Power", "Privacy"};

    private interface Getter { boolean get(); }
    private interface Setter { void set(boolean on); }

    private static View toggleCard(Context c, String title, String subtitle, Getter get, Setter set) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(c, 16), dp(c, 12), dp(c, 12), dp(c, 12));
        row.setBackground(rounded(card(c), dp(c, 22)));
        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(label(c, title, 16, text(c), true));
        if (subtitle != null) texts.addView(label(c, subtitle, 13, sub(c), false));
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Switch sw = new Switch(c);
        boolean on = false;
        try { on = get.get(); } catch (Throwable ignored) { }
        sw.setChecked(on);
        sw.setOnCheckedChangeListener((b, checked) -> {
            try { set.set(checked); } catch (Throwable t) {
                Log.w(TAG, "toggle " + title, t);
                b.setChecked(!checked);
            }
        });
        row.addView(sw);
        row.setOnClickListener(v -> sw.toggle());
        return row;
    }

    private static View linkCard(Context c, PreferenceFragmentCompat f, String title, String key) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16));
        row.setBackground(rounded(card(c), dp(c, 22)));
        row.addView(label(c, title, 16, text(c), true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(label(c, "›", 20, sub(c), false));
        row.setClickable(true);
        row.setOnClickListener(v -> open(f, key));
        return row;
    }

    private static void fillCategory(Context c, PreferenceFragmentCompat f, LinearLayout box, int cat) {
        box.removeAllViews();
        final WifiManager wm = c.getSystemService(WifiManager.class);
        final TelephonyManager tm = c.getSystemService(TelephonyManager.class);
        final BluetoothManager btm = c.getSystemService(BluetoothManager.class);
        final BluetoothAdapter bt = btm == null ? null : btm.getAdapter();
        final UiModeManager ui = c.getSystemService(UiModeManager.class);
        final PowerManager pm = c.getSystemService(PowerManager.class);
        final LocationManager lm = c.getSystemService(LocationManager.class);
        switch (cat) {
            case 0:
                if (wm != null) box.addView(toggleCard(c, "Wi-Fi", "Wireless networks",
                        wm::isWifiEnabled, wm::setWifiEnabled));
                if (tm != null && c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_DATA)) {
                    box.addView(toggleCard(c, "Mobile data", "Your carrier's network",
                            tm::isDataEnabled,
                            on -> tm.setDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER, on)));
                }
                if (bt != null) box.addView(toggleCard(c, "Bluetooth", "Headphones, watches, cars",
                        bt::isEnabled, on -> { if (on) bt.enable(); else bt.disable(); }));
                box.addView(linkCard(c, f, "More network settings", "top_level_network"));
                break;
            case 1:
                if (ui != null) box.addView(toggleCard(c, "Dark theme", "Easier on the eyes at night",
                        () -> night(c), ui::setNightModeActivated));
                box.addView(linkCard(c, f, "Wallpaper & style", "top_level_wallpaper"));
                box.addView(linkCard(c, f, "Display", "top_level_display"));
                box.addView(linkCard(c, f, "DriftVane enhancements", "top_level_driftvane"));
                break;
            case 2:
                box.addView(linkCard(c, f, "Sound & vibration", "top_level_sound"));
                box.addView(linkCard(c, f, "Modes", "top_level_priority_modes"));
                box.addView(linkCard(c, f, "Notifications", "top_level_notifications"));
                break;
            case 3:
                if (pm != null) box.addView(toggleCard(c, "Battery Saver", "Stretch your battery",
                        pm::isPowerSaveMode, pm::setPowerSaveModeEnabled));
                box.addView(linkCard(c, f, "Battery", "top_level_battery"));
                box.addView(linkCard(c, f, "Storage", "top_level_storage"));
                break;
            case 4:
                if (lm != null) box.addView(toggleCard(c, "Location", "Let apps use your location",
                        lm::isLocationEnabled,
                        on -> lm.setLocationEnabledForUser(on, UserHandle.of(UserHandle.myUserId()))));
                box.addView(linkCard(c, f, "Privacy", "top_level_privacy"));
                box.addView(linkCard(c, f, "Security", "top_level_security"));
                break;
        }
        for (int i = 0; i < box.getChildCount(); i++) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) box.getChildAt(i).getLayoutParams();
            if (lp == null) {
                lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            lp.topMargin = dp(c, 8);
            box.getChildAt(i).setLayoutParams(lp);
        }
    }

    private static int sChip = 0;

    private static View chips(Context c, PreferenceFragmentCompat f) {
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        HorizontalScrollView scroll = new HorizontalScrollView(c);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(c);
        scroll.addView(row);
        root.addView(scroll);
        LinearLayout content = new LinearLayout(c);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content);
        final TextView[] chipViews = new TextView[CHIPS.length];
        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            for (int i = 0; i < chipViews.length; i++) {
                boolean sel = i == sChip;
                chipViews[i].setBackground(rounded(sel ? BLUE : card(c), dp(c, 18)));
                chipViews[i].setTextColor(sel ? Color.WHITE : sub(c));
            }
            fillCategory(c, f, content, sChip);
        };
        for (int i = 0; i < CHIPS.length; i++) {
            final int idx = i;
            TextView chip = label(c, CHIPS[i], 14, sub(c), true);
            chip.setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 8));
            chip.setClickable(true);
            chip.setOnClickListener(v -> { sChip = idx; refresh[0].run(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(c, 8);
            row.addView(chip, lp);
            chipViews[i] = chip;
        }
        refresh[0].run();
        return root;
    }

    // ------------------------------------------------------------------ the header preference
    /** One preference at the top of the homepage that hosts the style's custom view. */
    public static class HeaderPreference extends Preference {
        private final PreferenceFragmentCompat mFragment;
        private final int mStyle;

        HeaderPreference(Context context, PreferenceFragmentCompat fragment, int style) {
            super(context);
            mFragment = fragment;
            mStyle = style;
            setLayoutResource(R.layout.driftvane_homepage_header);
            setSelectable(false);
            setPersistent(false);
        }

        void refresh() {
            notifyChanged();
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            holder.setDividerAllowedAbove(false);
            holder.setDividerAllowedBelow(false);
            FrameLayout root = (FrameLayout) holder.findViewById(R.id.driftvane_header_root);
            if (root == null) return;
            root.removeAllViews();
            Context c = getContext();
            View v;
            try {
                if (mStyle == 1) v = tiles(c, mFragment);
                else if (mStyle == 2) v = deviceCard(c, mFragment);
                else v = chips(c, mFragment);
            } catch (Throwable t) {
                Log.e(TAG, "header", t);
                return;
            }
            root.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }
}
