package edu.sysu.wechatluckymoney;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private MainFragment mMainFragment;
    private Dialog mTipsDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMainFragment = new MainFragment();
        getFragmentManager().beginTransaction().add(R.id.container, mMainFragment).commitAllowingStateLoss();
        setTitle(getString(R.string.app_name));

        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.ACTION_ACCESSIBILITY_CONNECT);
        filter.addAction(Config.ACTION_ACCESSIBILITY_DISCONNECT);
        registerReceiver(accessibilityReceiver, filter);
    }

    private BroadcastReceiver accessibilityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(isFinishing()) return;
            String action = intent.getAction();
            if(action.equals(Config.ACTION_ACCESSIBILITY_CONNECT)){
                if(mTipsDialog!=null) mTipsDialog.dismiss();
            }
            else if(action.equals(Config.ACTION_ACCESSIBILITY_DISCONNECT)){
                showOpenAccessibilityDialog();
            }
        }
    };

    private void showOpenAccessibilityDialog(){
        if(mTipsDialog != null && mTipsDialog.isShowing()) {
            return;
        }
        View view = getLayoutInflater().inflate(R.layout.tips_dialog, null);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAccessibilityServiceSettings();
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示");
        builder.setView(view);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                openAccessibilityServiceSettings();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        mTipsDialog = builder.show();
    }

    private void openAccessibilityServiceSettings(){
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(MyAccessibilityService.isRunning()){
            if(mTipsDialog!=null)
                mTipsDialog.dismiss();
        }
        else{
            showOpenAccessibilityDialog();
        }
    }
    @Override
    protected void onPause(){
        super.onPause();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        try {
            unregisterReceiver(accessibilityReceiver);
        } catch (Exception e) {}
        mTipsDialog = null;
    }

    /*右上角菜单栏*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuItem item = menu.add(0,0,1,"开启辅助功能");
        item.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        MenuItem about = menu.add(0,4,4,"关于");
        about.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);

        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                openAccessibilityServiceSettings();
                return true;
            case 4:
                startActivity(new Intent(this, AboutMeActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /*主界面内容*/
    public static class MainFragment extends BaseSettingsFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.main);
             /*抢红包开关*/
            Preference wechatPref = findPreference(Config.PREFERENCE_KEY_ENABLE_ACCESSIBILITY);
            wechatPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if((Boolean) newValue && !MyAccessibilityService.isRunning()) {
                        ((MainActivity)getActivity()).showOpenAccessibilityDialog();
                    }
                    return true;
                }
            });
            /*设置延迟*/
            final EditTextPreference delayPref = (EditTextPreference)findPreference(Config.PREFERENCE_KEY_DELAY_TIME);
            delayPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object delay) {
                    if("".equals(String.valueOf(delay))||"0".equals(String.valueOf(delay))){
                        preference.setSummary("");
                    }
                    else {
                        preference.setSummary("已延时" +delay+ "毫秒");
                    }
                    return true;
                }
            });
            String delay = delayPref.getText();
            if("".equals(String.valueOf(delay))||"0".equals(String.valueOf(delay))){
                delayPref.setSummary("");
            }
            else {
                delayPref.setSummary("已延时" +delay+ "毫秒");
            }
        }
    }
}
