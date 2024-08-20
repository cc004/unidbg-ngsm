package android.content.pm;

import java.io.File;

public class ApplicationInfo {
    public String packageName;
    public String sourceDir;
    public String dataDir;
    public String nativeLibraryDir;
    public int flags = 0x00800000 | 0x10000000 | 0x00040000 | 0x02000000;

    public ApplicationInfo(String packageName) {
        this.packageName = packageName;
        this.nativeLibraryDir = "/data/app/" + packageName + "/lib/arm64";
        this.sourceDir = "/data/app/" + packageName + "/base.apk";
        this.dataDir = "/data/data/" + packageName + "/files";
    }

}
