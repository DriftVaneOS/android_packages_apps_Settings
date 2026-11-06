/*
 * SPDX-FileCopyrightText: DriftVaneOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.driftvane;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.settings.R;

/** DriftVaneOS: restarts SystemUI (status bar, lock screen, Quick Settings). */
public class DriftRestartSystemUiPreference extends Preference {
    private static final String ACTION_RESTART = "org.driftvane.action.RESTART_SYSTEMUI";

    public DriftRestartSystemUiPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);
    }

    @Override
    protected void onClick() {
        Toast.makeText(getContext(), R.string.driftvane_restart_systemui_toast, Toast.LENGTH_SHORT).show();
        getContext().sendBroadcastAsUser(
                new Intent(ACTION_RESTART).setPackage("com.android.systemui"),
                UserHandle.SYSTEM);
    }
}
