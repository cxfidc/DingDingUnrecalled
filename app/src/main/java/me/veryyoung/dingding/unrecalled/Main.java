package me.veryyoung.dingding.unrecalled;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.newInstance;
import static me.veryyoung.dingding.unrecalled.VersionParam.MESSAGE_DS_CLASS_NAME;
import static me.veryyoung.dingding.unrecalled.VersionParam.init;


public class Main implements IXposedHookLoadPackage {

    private static final String DINGDING_PACKAGE_NAME = "com.alibaba.android.rimet";

    private static final String MESSAGE_CLASS_NAME = "com.alibaba.wukong.im.message.MessageImpl";
    private static final String CONVERSATION_CLASS_NAME = "com.alibaba.wukong.im.conversation.ConversationImpl";
    private static final String RECALLED_MSG_TEXT = "Msg has been recalled.";
    private static final int NUM_MESSAGE_TEXT_TYPE = 1;

    private static String dingdingVersion = "";


    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {


        if (lpparam.packageName.equals(DINGDING_PACKAGE_NAME)) {

            initVersion(lpparam);


            findAndHookMethod(MESSAGE_CLASS_NAME, lpparam.classLoader, "recallStatus", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // if msg is already recalled and RECALLED msg is stored in local DB,
                    // then let it shown as usual
                    if (RECALLED_MSG_TEXT.equalsIgnoreCase(getMsgText(param.thisObject))) {
                        return;
                    }

                    param.setResult(0);
                }
            });

            // stop replacing message content with default recalled string in database
            findAndHookMethod(MESSAGE_DS_CLASS_NAME, lpparam.classLoader, "b", String.class, Collection.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final Class<?> messageDS = findClass(MESSAGE_DS_CLASS_NAME, lpparam.classLoader);
                    final String dbName = (String) callStaticMethod(messageDS, "getReadableDatabase");

                    Object conversation = newInstance(findClass(CONVERSATION_CLASS_NAME, lpparam.classLoader));

                    String cid = (String) param.args[0];

                    Collection msgs = new ArrayList();
                    for (Object o : (Collection) param.args[1]) {
                        if (!RECALLED_MSG_TEXT.equalsIgnoreCase(getMsgText(o))) {
                            // normal message to be updated, add it to collection
                            msgs.add(o);
                        } else {
                            long mid = XposedHelpers.getLongField(o, "mMid");
                            // load messageImpl from DB
                            Object msgImpl = callStaticMethod(messageDS, "a", cid, mid, conversation);
                            String originalMsgText = getMsgText(msgImpl);
                            // append info suffix
                            setMsgText(msgImpl, originalMsgText + " [已撤回]");
                            // write messageImpl back into DB
                            callStaticMethod(messageDS, "a", dbName, cid, Collections.singletonList(msgImpl));
                        }
                    }

                    param.args[1] = msgs;
                }
            });
        }
    }


    private String getMsgText(Object msg) {
        Object innerContent = getObjectField(msg, "mMessageContent");
        int type = (int) callMethod(innerContent, "type");

        if (type == NUM_MESSAGE_TEXT_TYPE) {
            return (String) callMethod(innerContent, "text");
        }
        return null;
    }

    private void setMsgText(Object msg, String newText) {
        Object innerContent = getObjectField(msg, "mMessageContent");
        int type = (int) callMethod(innerContent, "type");

        if (type == NUM_MESSAGE_TEXT_TYPE) {
            callMethod(innerContent, "setText", newText);
        }
    }

    private void initVersion(LoadPackageParam lpparam) throws PackageManager.NameNotFoundException {
        if (TextUtils.isEmpty(dingdingVersion)) {
            Context context = (Context) callMethod(callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread", new Object[0]), "getSystemContext", new Object[0]);
            String versionName = context.getPackageManager().getPackageInfo(lpparam.packageName, 0).versionName;
            dingdingVersion = versionName;
            init(versionName);
        }
    }

}