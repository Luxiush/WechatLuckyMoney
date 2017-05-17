package edu.sysu.wechatluckymoney;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by LuXiushun on 2017/5/15.
 */

public class BaseSettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(Config.PREFERENCE_NAME);
    }
}