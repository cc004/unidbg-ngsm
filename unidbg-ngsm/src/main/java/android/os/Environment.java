package android.os;

import jvav.io.File;

public class Environment {
    public static java.io.File getExternalStorageDirectory() {
        return new File("/storage/emulated/0");
    }
}
