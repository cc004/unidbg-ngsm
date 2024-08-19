package android.net;

public class ConnectivityManager {
    public static final int TYPE_VPN = 0x00000011;

    public NetworkInfo getNetworkInfo(int networkType) {
        if (networkType == TYPE_VPN) return null;
        return new NetworkInfo();
    }

    public LinkProperties getLinkProperties(Network network) {
        return new LinkProperties();
    }

    public Network getActiveNetwork() {
        return new Network();
    }
    public NetworkCapabilities getNetworkCapabilities (Network network) {
        return new NetworkCapabilities();
    }
}
