package me.veryyoung.dingding.unrecallled;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;


public class Main implements IXposedHookLoadPackage {

    private static final String DINGDING_PACKAGE_NAME = "com.alibaba.android.rimet";

    private static final String MAP_CLASS_NAME = "com.alibaba.wukong.im.message.MessageImpl";
    private static final String MAP_FUNCTION_NAME = "recallStatus";
    private static final String CONVERSATION_CLASS_NAME = "com.alibaba.wukong.im.conversation.ConversationImpl";
    private static final String RECALLED_MSG_TEXT = "Msg has been recalled.";
    private static final int NUM_MESSAGE_TEXT_TYPE = 1;


    @SuppressWarnings("unchecked")
    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {


        if (lpparam.packageName.equals(DINGDING_PACKAGE_NAME)) {

            //map
            findAndHookMethod(MAP_CLASS_NAME, lpparam.classLoader, MAP_FUNCTION_NAME, new XC_MethodHook() {
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
            XposedHelpers.findAndHookMethod(MAP_CLASS_NAME, lpparam.classLoader, "b", String.class, Collection.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final Class<?> CLASS_MESSAGE_DB = XposedHelpers.findClass(MAP_CLASS_NAME, lpparam.classLoader);
                    final String dbName = (String) XposedHelpers.callStaticMethod(CLASS_MESSAGE_DB, "getReadableDatabase");

                    Object conversation = XposedHelpers.newInstance(XposedHelpers.findClass(CONVERSATION_CLASS_NAME, lpparam.classLoader));

                    String cid = (String) param.args[0];

                    Collection msgs = new ArrayList();
                    for (Object o : (Collection) param.args[1]) {
                        if (!RECALLED_MSG_TEXT.equalsIgnoreCase(getMsgText(o))) {
                            // normal message to be updated, add it to collection
                            msgs.add(o);
                        } else {
                            long mid = XposedHelpers.getLongField(o, "mMid");
                            // load messageImpl from DB
                            Object msgImpl = XposedHelpers.callStaticMethod(CLASS_MESSAGE_DB, "a", cid, mid, conversation);
                            String originalMsgText = getMsgText(msgImpl);
                            // append info suffix
                            setMsgText(msgImpl, originalMsgText + " [已撤回]");
                            // write messageImpl back into DB
                            XposedHelpers.callStaticMethod(CLASS_MESSAGE_DB, "a", dbName, cid, Collections.singletonList(msgImpl));
                        }
                    }

                    param.args[1] = msgs;
                }
            });
        }
    }

    /**
     * Load text from messageImpl instance.
     *
     * @param msg message
     * @return message string, null if not text type
     */
    private String getMsgText(Object msg) {
        try {
            // msg.class = com.alibaba.wukong.im.MessageImpl
            Object innerContent = XposedHelpers.getObjectField(msg, "mMessageContent");
            // indicate which type of content
            int type = (int) XposedHelpers.callMethod(innerContent, "type");

            if (type == NUM_MESSAGE_TEXT_TYPE) {
                // innerContent.class = com.alibaba.wukong.im.message.MessageContentImpl$TextContentImpl
                return (String) XposedHelpers.callMethod(innerContent, "text");
            }
        } catch (Throwable t) {
        }

        return null;
    }

    /**
     * Set text into messageImpl instance.
     *
     * @param msg     message
     * @param newText text to be set
     */
    private void setMsgText(Object msg, String newText) {
        try {
            // msg.class = com.alibaba.wukong.im.MessageImpl
            Object innerContent = XposedHelpers.getObjectField(msg, "mMessageContent");
            // indicate which type of content
            int type = (int) XposedHelpers.callMethod(innerContent, "type");

            if (type == NUM_MESSAGE_TEXT_TYPE) {
                // innerContent.class = com.alibaba.wukong.im.message.MessageContentImpl$TextContentImpl
                XposedHelpers.callMethod(innerContent, "setText", newText);
            }
        } catch (Throwable t) {
        }
    }
}
