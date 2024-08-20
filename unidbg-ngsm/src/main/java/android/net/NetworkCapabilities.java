package android.net;

public class NetworkCapabilities {
    public static final int TRANSPORT_VPN = 0x00000004;
    public static final int TRANSPORT_WIFI = 1;
    public boolean hasTransport (int transportType) {
        if (transportType == TRANSPORT_VPN) return false;
        if (transportType == TRANSPORT_WIFI) return true;
        throw new UnsupportedOperationException();
    }
}
