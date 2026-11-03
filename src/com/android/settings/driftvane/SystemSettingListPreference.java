/*
 * SPDX-FileCopyrightText: DriftVaneOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.driftvane;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

/** A ListPreference stored as an int in Settings.System (key = preference key). */
public class SystemSettingListPreference extends ListPreference {

    public SystemSettingListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
        setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
    }

    @Override
    protected boolean persistString(String value) {
        if (value == null) return false;
        Settings.System.putIntForUser(getContext().getContentResolver(), getKey(),
                Integer.parseInt(value), UserHandle.USER_CURRENT);
        return true;
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        int def = defaultReturnValue == null ? 0 : Integer.parseInt(defaultReturnValue);
        return String.valueOf(Settings.System.getIntForUser(getContext().getContentResolver(),
                getKey(), def, UserHandle.USER_CURRENT));
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        setValue(getPersistedString(defaultValue == null ? null : defaultValue.toString()));
    }
}
