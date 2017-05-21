package edu.sysu.wechatluckymoney;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.Iterator;
import java.util.List;

/**
 * Created by LuXiushun on 2017/5/14.
 */

public class MyAccessibilityService extends AccessibilityService {
    private static int CHATTING_UI_NOT          = 0;
    private static int CHATTING_UI_SINGLE       = 1;// 单人聊天
    private static int CHATTING_UI_GROUP        = 2;// 群聊天

    private static int WINDOW_STATE_OTHER           = 0;
    private static int WINDOW_STATE_CHATTING        = 3;
    private static int WINDOW_STATE_LUCKYMONEY      = 4;
    private static int WINDOW_STATE_DETAIL          = 5;

    private static int PACKAGE_STAGE_NEW         = 0;//发现新红包
    private static int PACKAGE_STAGE_OPEN        = 2;//打开红包
    private static int PACKAGE_STAGE_WAIT        = 4;//等待新的红包

    private int windowState = WINDOW_STATE_OTHER;
    private int packageStage = PACKAGE_STAGE_WAIT;

    private static MyAccessibilityService service;

    private Handler mHandler = null;

    @Override
    public void onCreate() {
        super.onCreate();
        windowState = WINDOW_STATE_OTHER;
        packageStage = PACKAGE_STAGE_WAIT;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        service = null;

        Intent intent = new Intent(Config.ACTION_ACCESSIBILITY_DISCONNECT);
        sendBroadcast(intent);
    }
    @Override
    public void onInterrupt() {
        Toast.makeText(this, "Accessibility服务中断", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onServiceConnected(){
        super.onServiceConnected();
        service = this;
        Intent intent = new Intent(Config.ACTION_ACCESSIBILITY_CONNECT);
        sendBroadcast(intent);
        Toast.makeText(this, "成功连接Accessibility服务", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String pkn = String.valueOf(event.getPackageName());
        System.out.println("onAccessibilityEvent "+ "package name: "+pkn);
        System.out.println("onAccessibilityEvent "+ "eventType: "+event.getEventType());
        System.out.println("onAccessibilityEvent "+ "className: "+event.getClassName());

        if(!pkn.equals(Config.TARGET_PACKAGE_NAME)||!getConfig().isEnable()){
            return;
        }

        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:    //(64)通知栏状态发生改变
                Parcelable data = event.getParcelableData();
                if(data == null || !(data instanceof Notification)) {
                    return;
                }
                List<CharSequence> texts = event.getText();
                if(!texts.isEmpty()) {
                    String text = String.valueOf(texts.get(0));
                    onNotificationStateChange(text, (Notification) data);
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:          //(32)窗口状态改变
                onWindowStateChange(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:        //(2048)窗口内容改变
                onWindowContentChange();
                break;
        }
    }
    /*处理通知栏事件*/
    private void onNotificationStateChange(String ticker, Notification nf){
        StringBuffer str = new StringBuffer("");
        str.append("onNotificationStateChange: ");
        String text = ticker;
        int index = text.indexOf(":");
        if(index!=-1){
            text = text.substring(index+1);
        }
        text = text.trim();
        if(!text.contains(Config.TARGET_TEXT)){
            return;
        }
        str.append("Found "+Config.TARGET_TEXT+" in notification event; ");
        //发现 [微信红包] 关键字
        packageStage = PACKAGE_STAGE_NEW;
        //打开通知栏消息
        PendingIntent pendingIntent = nf.contentIntent;
        if(!NotifyHelper.isLockScreen(getApplicationContext())){
            try{
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e){
                e.printStackTrace();
            }
        }
    }

    private void onWindowStateChange(AccessibilityEvent event){
        String className = event.getClassName().toString();
        AccessibilityNodeInfo rootWindow = this.getRootInActiveWindow();
        //当微信主界面启动时触发（从通知栏进入聊天界面、从桌面进入微信）（主界面进入聊天界面时不会再触发）
        StringBuffer str = new StringBuffer("WindowChangeHandler: ");
        if(className.equals("com.tencent.mm.ui.LauncherUI")){
            if(isChattingUi(rootWindow)!=CHATTING_UI_NOT){
                str.append("is Chatting Ui, ");
                windowState = WINDOW_STATE_CHATTING;
            }
            else{
                str.append("no in Chatting Ui, ");
                windowState = WINDOW_STATE_OTHER;
            }
        }
        else if(className.equals("com.tencent.mm.plugin.luckymoney.ui.En_fba4b94f")){//“開”字界面
            str.append("find key word '開', ");
            windowState = WINDOW_STATE_LUCKYMONEY;
        }
        else if(className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")){//点完“開”字之后的界面
            str.append("LuckyMoneyDetailUI, ");
            windowState = WINDOW_STATE_DETAIL;
        }
        else{//其他情况
            str.append("else, ");
            if(isChattingUi(rootWindow)!=CHATTING_UI_NOT){
                str.append("is Chatting Ui, ");
                windowState = WINDOW_STATE_CHATTING;
            }
            else{
                str.append("not in Chatting Ui, ");
                windowState = WINDOW_STATE_OTHER;
            }
        }
        System.out.println(str.toString());
    }

    private void onWindowContentChange(){
        StringBuffer str = new StringBuffer("onWindowContentChange, windowState: ");
        AccessibilityNodeInfo rootWindow = this.getRootInActiveWindow();
//        if(windowState==WINDOW_STATE_CHATTING) test();
        if(windowState==WINDOW_STATE_CHATTING&&packageStage==PACKAGE_STAGE_NEW){
            str.append("WINDOW_STATE_CHATTING, ");
            AccessibilityNodeInfo pkg = findPackage(rootWindow);
            if(pkg!=null) {
                packageStage = PACKAGE_STAGE_OPEN;
                AccessibilityHelper.performClick(pkg);
            }
            else{
                packageStage = PACKAGE_STAGE_WAIT;
                AccessibilityHelper.performHome(this);
            }
            windowState = WINDOW_STATE_OTHER;//避免重复触发，因为一次WINDOW_STATE_CHANGE事件之后会有多个WINDOW_CONTENT_CHANGE事件
        }
        else if(windowState==WINDOW_STATE_LUCKYMONEY&&packageStage==PACKAGE_STAGE_OPEN){
            str.append("WINDOW_STATE_LUCKYMONEY, ");
            str.append("\nLuckyMoney Structure:\n"+AccessibilityHelper.showStructure(rootWindow)+"\n");
            final AccessibilityNodeInfo open = AccessibilityHelper.findNodeInfosByClassName(rootWindow,"android.widget.Button");//開 按钮
            if(open==null){//红包已经被抢完
                packageStage = PACKAGE_STAGE_WAIT;
                AccessibilityHelper.performHome(this);
            }
            else {
                long delay = getConfig().getDelayTime();
                if(mHandler==null) mHandler = new Handler();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AccessibilityHelper.performClick(open);
                    }
                },delay);

            }
            windowState = WINDOW_STATE_OTHER;
        }
        else if(windowState==WINDOW_STATE_DETAIL&&packageStage==PACKAGE_STAGE_OPEN){
            str.append("WINDOW_STATE_DETAIL, ");
            packageStage = PACKAGE_STAGE_WAIT;
            AccessibilityHelper.performHome(this);
        }
        else{
            str.append("WINDOW_STATE_OTHER, ");
        }

        System.out.println(str);
    }

    public void test(){
        System.out.println("test: ");
        AccessibilityNodeInfo rootWindow = this.getRootInActiveWindow();
        List<AccessibilityNodeInfo> list = rootWindow.findAccessibilityNodeInfosByText("微信红包");
        System.out.println("number of '微信红包': "+list.size());
    }

    /** 通过标题是否以“）”结尾判断是否是群聊天 */
    private int isChattingUi(AccessibilityNodeInfo nodeInfo){
        if(nodeInfo == null)
            return CHATTING_UI_NOT;
        // 根据是否有输入框判断是否是聊天界面
        if(AccessibilityHelper.findNodeInfosByClassName(nodeInfo, "android.widget.EditText")==null)
            return CHATTING_UI_NOT;

        // 通过id查找title
        String id = "com.tencent.mm:id/gh";
        String title = null;
        AccessibilityNodeInfo target = AccessibilityHelper.findNodeInfosById(nodeInfo, id);
        if(target != null) {
            title = String.valueOf(target.getText());
        }
        if(title != null){
            if(title.endsWith(")")) return CHATTING_UI_GROUP;
            else return CHATTING_UI_SINGLE;
        }

        // 通过“返回”按钮查找title
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("返回");
        if(list != null && !list.isEmpty()) {
            AccessibilityNodeInfo parent = null;
            for(AccessibilityNodeInfo node : list) {
                if(!"android.widget.ImageView".equals(node.getClassName())) {
                    continue;
                }
                String desc = String.valueOf(node.getContentDescription());
                if(!"返回".equals(desc)) {
                    continue;
                }
                parent = node.getParent();
                break;
            }
            if(parent != null) {
                parent = parent.getParent();
            }
            if(parent != null) {
                if( parent.getChildCount() >= 2) {
                    AccessibilityNodeInfo node = parent.getChild(1);
                    if("android.widget.TextView".equals(node.getClassName())) {
                        title = String.valueOf(node.getText());
                    }
                }
            }
        }
        if(title != null){
            if(title.endsWith(")")) return CHATTING_UI_GROUP;
            else return CHATTING_UI_SINGLE;
        }
        return CHATTING_UI_NOT;
    }

    /** 识别聊天界面中的红包 */
    private AccessibilityNodeInfo findPackage(AccessibilityNodeInfo rootNode){
        System.out.println("findPackage:");
        AccessibilityNodeInfo nodeInfo11, nodeInfo12, nodeInfo1;
        List<AccessibilityNodeInfo> list = rootNode.findAccessibilityNodeInfosByText("微信红包");
        for(int i=list.size()-1; i>=0; --i) {
            nodeInfo12 = list.get(i);
            nodeInfo1 = nodeInfo12.getParent();
            if(nodeInfo1.getChildCount()<2) continue;
            nodeInfo11 = nodeInfo1.getChild(1); //通过"微信红包"节点找到"领取红包"节点
            if(nodeInfo11.getText()==null) continue;
            String s = nodeInfo11.getText().toString();
            if(s==null) continue;
            if (s.equals("领取红包") || s.equals("查看红包")) {
//                System.out.println("Package Structure:\n" + AccessibilityHelper.showStructure(nodeInfo1));
                return nodeInfo1;
            }
        }
        return null;
    }

    /** 判断服务是否正在运行 */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static boolean isRunning() {
        if (service == null) {
            return false;
        }
        AccessibilityManager accessibilityManager = (AccessibilityManager)service.getSystemService(Context.ACCESSIBILITY_SERVICE);
        AccessibilityServiceInfo serviceInfo = service.getServiceInfo();
        if(serviceInfo==null) return false;

        List<AccessibilityServiceInfo> list = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        Iterator<AccessibilityServiceInfo> iterator = list.iterator();

        while(iterator.hasNext()){
            AccessibilityServiceInfo i = iterator.next();
            if(i.getId().equals(serviceInfo.getId())){
                return true;
            }
        }
        return false;
    }

    private Config getConfig(){
        return Config.getConfig(this);
    }
}
