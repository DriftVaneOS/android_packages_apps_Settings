/*
 * SPDX-FileCopyrightText: DriftVaneOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.driftvane;

import static android.os.UserHandle.USER_SYSTEM;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.ListPreference;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * DriftVaneOS: picks one overlay from a category (the preference key), e.g. the status bar
 * signal or Wi-Fi icon style. Each style is two overlays: one for "android" and one for
 * SystemUI (same package name + ".systemui"); both are switched together.
 */
public class DriftOverlayListPreference extends ListPreference {
    private static final String TAG = "DriftOverlayList";
    private static final String DEFAULT = "default";
    private static final String TARGET_ANDROID = "android";
    private static final String SYSUI_SUFFIX = ".systemui";

    private final IOverlayManager mOverlayManager;
    private final Handler mMain = new Handler(Looper.getMainLooper());

    public DriftOverlayListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));
        setPersistent(false);
        setOnPreferenceChangeListener((pref, value) -> {
            apply((String) value);
            return true;
        });
        refresh();
    }

    @Override
    public void onAttached() {
        super.onAttached();
        refresh();
    }

    private List<OverlayInfo> overlays(String target) {
        final List<OverlayInfo> out = new ArrayList<>();
        if (mOverlayManager == null) return out;
        try {
            for (OverlayInfo info : mOverlayManager.getOverlayInfosForTarget(target, USER_SYSTEM)) {
                if (getKey().equals(info.category)) out.add(info);
            }
        } catch (Exception e) {
            Log.w(TAG, "couldn't list overlays", e);
        }
        return out;
    }

    private void refresh() {
        final PackageManager pm = getContext().getPackageManager();
        final List<String[]> items = new ArrayList<>(); // {label, package}
        String selected = DEFAULT;
        for (OverlayInfo info : overlays(TARGET_ANDROID)) {
            String label;
            try {
                label = pm.getApplicationInfo(info.packageName, 0).loadLabel(pm).toString();
            } catch (PackageManager.NameNotFoundException e) {
                label = info.packageName;
            }
            items.add(new String[] {label, info.packageName});
            if (info.isEnabled()) selected = info.packageName;
        }
        items.sort((a, b) -> a[0].compareToIgnoreCase(b[0]));
        final CharSequence[] entries = new CharSequence[items.size() + 1];
        final CharSequence[] values = new CharSequence[items.size() + 1];
        entries[0] = getContext().getString(R.string.driftvane_iconpack_default);
        values[0] = DEFAULT;
        for (int i = 0; i < items.size(); i++) {
            entries[i + 1] = items.get(i)[0];
            values[i + 1] = items.get(i)[1];
        }
        setEntries(entries);
        setEntryValues(values);
        setValue(selected);
        setSummary(entries[Math.max(0, findIndexOfValue(selected))]);
    }

    private void apply(String pkg) {
        new Thread(() -> {
            boolean ok = true;
            try {
                if (DEFAULT.equals(pkg)) {
                    for (String target : new String[] {TARGET_ANDROID, "com.android.systemui"}) {
                        for (OverlayInfo info : overlays(target)) {
                            if (info.isEnabled()) {
                                mOverlayManager.setEnabled(info.packageName, false, USER_SYSTEM);
                            }
                        }
                    }
                } else {
                    ok = mOverlayManager.setEnabledExclusiveInCategory(pkg, USER_SYSTEM);
                    try {
                        mOverlayManager.setEnabledExclusiveInCategory(pkg + SYSUI_SUFFIX, USER_SYSTEM);
                    } catch (Exception e) {
                        Log.w(TAG, "no SystemUI part for " + pkg, e);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "couldn't switch to " + pkg, e);
                ok = false;
            }
            final boolean success = ok;
            mMain.post(() -> {
                refresh();
                if (!success) {
                    Toast.makeText(getContext(), R.string.overlay_toast_failed_to_apply,
                            Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
}
