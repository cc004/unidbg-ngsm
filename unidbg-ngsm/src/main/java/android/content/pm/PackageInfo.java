package android.content.pm;

public class PackageInfo {
    public String packageName;
    public String versionName;
    public int versionCode;
    public ApplicationInfo applicationInfo;
    public PackageInfo(String packageName) {
        this.packageName = packageName;
        applicationInfo = new ApplicationInfo(packageName);
        if (packageName.equals("com.nexon.bluearchive")) {
            versionName = "1.66.291639";
            versionCode = 291639;
        }
        else
            throw new UnsupportedOperationException();
    }
}
