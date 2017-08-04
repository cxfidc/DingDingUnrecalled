package me.veryyoung.dingding.unrecalled;

/**
 * Created by veryyoung on 2017/1/9.
 */
public class VersionParam {

    public static String MessageDs = "eli";

    public static void init(String version) {
        switch (version) {
            case "3.3.0":
            case "3.3.1":
                MessageDs = "cuw";
                break;
            case "3.3.3":
            case "3.3.5":
                MessageDs = "cwz";
                break;
            case "3.4.0":
                MessageDs = "cuy";
                break;
            case "3.4.6":
                MessageDs = "cxw";
                break;
            case "3.4.8":
            case "3.4.10":
                MessageDs = "dey";
                break;
            case "3.5.0":
                MessageDs = "dtk";
                break;
            case "3.5.1":
                MessageDs = "ekb";
                break;
            case "3.5.2":
                MessageDs = "eli";
                break;
            default:
                MessageDs = "eli";

        }
    }

}