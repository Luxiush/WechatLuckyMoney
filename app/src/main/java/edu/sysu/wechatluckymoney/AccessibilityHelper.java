package edu.sysu.wechatluckymoney;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityNodeInfo;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

/**
 * Created by LuXiushun on 2017/5/14.
 */
public final class AccessibilityHelper {

    private AccessibilityHelper() {}

    /** 通过id查找*/
    public static AccessibilityNodeInfo findNodeInfosById(AccessibilityNodeInfo nodeInfo, String resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(resId);
            if(list != null && !list.isEmpty()) {
                return list.get(0);
            }
        }
        return null;
    }

    /** 通过关键字查找，返回最后一个节点*/
    public static AccessibilityNodeInfo findNodeInfosByTexts(AccessibilityNodeInfo nodeInfo, String texts) {
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(texts);
        if(list != null&&!list.isEmpty()) return list.get(list.size()-1);
        return null;
    }

    public static AccessibilityNodeInfo getClickableParent(AccessibilityNodeInfo nodeInfo, int limit){
        while(nodeInfo!=null&&!nodeInfo.isClickable()&&(limit>0)){
            nodeInfo = nodeInfo.getParent();
            --limit;
        }
        System.out.println("getClickableParent, limit: "+limit);
        if(nodeInfo!=null&&nodeInfo.isClickable()) return nodeInfo;
        return null;
    }

    /** 通过组件名字查找*/
    public static AccessibilityNodeInfo findNodeInfosByClassName(AccessibilityNodeInfo nodeInfo, String className) {
        if(TextUtils.isEmpty(className)||nodeInfo==null) {
            return null;
        }
        Stack<AccessibilityNodeInfo> stack = new Stack<>();
        stack.push(nodeInfo);
        AccessibilityNodeInfo temp;
        while(!stack.isEmpty()){
            temp = stack.pop();
            for(int i=temp.getChildCount()-1; i>=0; i--){
                AccessibilityNodeInfo node = temp.getChild(i);
                if (node==null||node.getClassName()==null) continue;
                if(className.equals(node.getClassName())) {
                    return node;
                }
                else{
                    stack.push(node);
                }
            }
        }
        return null;
    }

    /** 返回主界面事件*/
    public static void performHome(AccessibilityService service) {
        if(service == null) {
            return;
        }
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
    }

    /** 返回事件*/
    public static void performBack(AccessibilityService service) {
        if(service == null) {
            return;
        }
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    /** 点击事件*/
    public static void performClick(AccessibilityNodeInfo nodeInfo){
        while(nodeInfo!=null&&!nodeInfo.isClickable()){
            nodeInfo = nodeInfo.getParent();
        }
        if(nodeInfo==null) return;
        if(nodeInfo.isClickable())
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    /** DFS显示节点结构 */
    public static String showStructure(AccessibilityNodeInfo rootNode){
        StringBuffer str = new StringBuffer("");
        if(rootNode==null) str.toString();

        Stack<Pair<AccessibilityNodeInfo, String> > stack = new Stack<>();
        stack.push(Pair.create(rootNode,""));
        AccessibilityNodeInfo temp;
        String deep;
        while(!stack.isEmpty()) {
            temp = stack.peek().first;
            deep = stack.peek().second;
            stack.pop();
            str.append(deep+temp.getClassName()+", "+temp.getText()+", "+temp.getContentDescription()+"\n");

            for(int i=temp.getChildCount()-1; i>=0; --i){
                if(temp.getChild(i)!=null){
                    stack.push(Pair.create(temp.getChild(i),deep+"\t"));
                }
            }
        }
        return str.toString();
    }
}
