package android.net;

import java.util.ArrayList;
import java.util.List;

public class LinkProperties {
    public List<RouteInfo> getRoutes() {
        List<RouteInfo> result = new ArrayList<>();
        result.add(new RouteInfo());
        return result;
    }
}
