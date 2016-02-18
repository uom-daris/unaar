package unaar;

public class PlatformUtil {

    public static boolean isWindows() {
        return getOsName().toLowerCase().contains("windows");
    }

    public static boolean isLinux() {
        return getOsName().toLowerCase().contains("linux");
    }

    public static boolean isMac() {
        return getOsName().toLowerCase().contains("mac os x");
    }

    public static String getOsName() {
        return System.getProperty("os.name");
    }

    public static void main(String[] args) {
        System.out.println(getOsName());
        System.out.println(isMac());
    }

}
