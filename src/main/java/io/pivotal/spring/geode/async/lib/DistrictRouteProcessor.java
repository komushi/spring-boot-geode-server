package io.pivotal.spring.geode.async.lib;

import org.apache.geode.cache.Region;
import org.apache.geode.pdx.JSONFormatter;
import org.apache.geode.pdx.PdxInstance;
import org.json.JSONObject;

/**
 * Created by lei_xu on 8/25/16.
 */
public class DistrictRouteProcessor {
    private Region regDistrictRouteCount;
    private Region<Integer, PdxInstance> regDistrictRouteTop;

    public DistrictRouteProcessor(Region regDistrictRouteCount, Region<Integer, PdxInstance> regDistrictRouteTop) {
        this.regDistrictRouteCount = regDistrictRouteCount;
        this.regDistrictRouteTop = regDistrictRouteTop;
    }

    public void processRouteCount(String route, String pickupDistrict, String dropoffDistrict, Integer originalCount, Integer newCount, Long newTimestamp) throws Exception{
        // process RegRouteCount

        if (newCount == 0) {
            regDistrictRouteCount.destroy(route);
        } else {
            JSONObject jsonObj = new JSONObject().put("route", route)
                    .put("pickupDistrict", pickupDistrict).put("dropoffDistrict", dropoffDistrict)
                    .put("route_count", newCount).put("timestamp", newTimestamp);
            PdxInstance entryValue = JSONFormatter.fromJSON(jsonObj.toString());

            if(originalCount == 0){
                regDistrictRouteCount.putIfAbsent(route, entryValue);
            }
            else
            {
                regDistrictRouteCount.put(route, entryValue);
            }
        }
    }
}
