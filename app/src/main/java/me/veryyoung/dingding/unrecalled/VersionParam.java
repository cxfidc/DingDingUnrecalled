package me.veryyoung.dingding.unrecalled;

/**
 * Created by veryyoung on 2017/1/9.
 */
public class VersionParam {

    public static String MESSAGE_DS_CLASS_NAME = "cuy";

    public static void init(String version) {
        switch (version) {
            case "3.3.0":
            case "3.3.1":
                MESSAGE_DS_CLASS_NAME = "cuw";
                break;
            case "3.3.3":
            case "3.3.5":
                MESSAGE_DS_CLASS_NAME = "cwz";
                break;
            case "3.4.0":
                MESSAGE_DS_CLASS_NAME = "cuy";
                break;
            default:
                MESSAGE_DS_CLASS_NAME = "cuy";

        }
    }

}