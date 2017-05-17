package edu.sysu.wechatluckymoney;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by LuXiushun on 2017/5/14.
 */

public class Config {
    public static String ACTION_ACCESSIBILITY_CONNECT = "ACTION_ACCESSIBILITY_CONNECT";
    public static String ACTION_ACCESSIBILITY_DISCONNECT = "ACTION_ACCESSIBILITY_DISCONNECT";

    public static String TARGET_PACKAGE_NAME = "com.tencent.mm";
    public static String TARGET_TEXT = "[微信红包]";
    /** 不能再使用文字匹配的最小版本号 */
    public static final int USE_ID_MIN_VERSION = 700;// 6.3.8 对应code为680,6.3.9对应code为700
    public static final String BUTTON_CLASS_NAME = "android.widget.Button";

    public static final String PREFERENCE_NAME = "config";
    public static final String PREFERENCE_KEY_ENABLE_ACCESSIBILITY ="PREFERENCE_KEY_ENABLE_ACCESSIBILITY";
    public static final String PREFERENCE_KEY_DELAY_TIME = "PREFERENCE_KEY_DELAY_TIME";

    private static Config current;

    public static synchronized Config getConfig(Context context) {
        if(current == null) {
            current = new Config(context.getApplicationContext());
        }
        return current;
    }

    private SharedPreferences preferences;
    private Context mContext;

    private Config(Context context) {
        mContext = context;
        preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnable() {
        return preferences.getBoolean(PREFERENCE_KEY_ENABLE_ACCESSIBILITY, true);
    }

    public long getDelayTime(){
        int delay = 0;
        String res = preferences.getString(PREFERENCE_KEY_DELAY_TIME,String.valueOf(delay));
        try{
            delay = Integer.parseInt(res);
        } catch (Exception e){}
        return delay;
    }
}
