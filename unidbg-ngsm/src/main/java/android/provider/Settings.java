package android.provider;

import android.content.ContentResolver;

public class Settings {
    public static class Secure {
        public static String getString(ContentResolver resolver, String key) {
            if (key.equals("android_id")) return "353240110222911";
            throw new UnsupportedOperationException();
        }
        public static int getInt(ContentResolver resolver, String key, int def) {
            if (key.equals("development_settings_enabled")) return 0;
            return def;
        }
    }
}
