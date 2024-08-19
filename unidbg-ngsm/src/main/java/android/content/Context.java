package android.content;

import android.Constants;
import android.net.ConnectivityManager;
import android.content.pm.PackageManager;

public class Context {
    public static String CONNECTIVITY_SERVICE = "CONNECTIVITY_SERVICE";

    public Object getSystemService(String serviceName) {
        if (serviceName == CONNECTIVITY_SERVICE) return new ConnectivityManager();
        throw new UnsupportedOperationException();
    }

    public String getPackageName() {
        return Constants.PACKAGE_NAME;
    }
    public ContentResolver getContentResolver() {
        return new ContentResolver();
    }
    public PackageManager getPackageManager() {
        return new PackageManager();
    }

    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return new Intent();
    }
}
