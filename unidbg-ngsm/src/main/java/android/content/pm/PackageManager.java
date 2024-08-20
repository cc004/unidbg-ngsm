package android.content.pm;

import android.Constants;

import java.util.ArrayList;
import java.util.List;

public class PackageManager {
    public PackageInfo getPackageInfo(String packageName, int flags) {
        System.out.println("get package info :" + packageName + "flags: " + flags);
        return new PackageInfo(packageName);
    }

    public List<PackageInfo> getInstalledPackages(int flags) {
        System.out.println("list package, flags: " + flags);
        return new ArrayList<>();
        /*
        List<PackageInfo> result = new ArrayList<>();
        result.add(new PackageInfo(Constants.PACKAGE_NAME));
        return result;*/
    }
}
