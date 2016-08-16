package io.pivotal.spring.geode.async.lib;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.pdx.JSONFormatter;
import com.gemstone.gemfire.pdx.PdxInstance;
import org.json.JSONObject;

/**
 * Created by lei_xu on 8/16/16.
 */
public class DistrictProcessor {

    private Region regDistrictCount;
    private Region<Integer, PdxInstance> regDropoffDistrictTop;
    private Region<Integer, PdxInstance> regPickupDistrictTop;

//    public DistrictProcessor(Region regDistrictCount, Region<Integer, PdxInstance> regDropoffDistrictTop, Region<Integer, PdxInstance> regPickupDistrictTop) {
    public DistrictProcessor(Region regDistrictCount, Region<Integer, PdxInstance> regDropoffDistrictTop) {
        this.regDistrictCount = regDistrictCount;
        this.regDropoffDistrictTop = regDropoffDistrictTop;
//        this.regPickupDistrictTop = regPickupDistrictTop;
    }

    public void processDropoffDistrictCount(String districtCode, String district, Integer originalDropoffDistrictCount, Integer originalDropoffDistrictAsPickupCount, Integer newDropoffDistrictCount, Long newTimestamp) throws Exception{

        // process RegDistrictCount
        if (newDropoffDistrictCount == 0 && originalDropoffDistrictAsPickupCount == 0) {
            regDistrictCount.destroy(districtCode);
        } else {
            JSONObject jsonObj = new JSONObject().put("districtCode", districtCode).put("district", district)
                    .put("pickupCount", originalDropoffDistrictAsPickupCount).put("dropoffCount", newDropoffDistrictCount).put("timestamp", newTimestamp);
            PdxInstance entryValue = JSONFormatter.fromJSON(jsonObj.toString());

            if(originalDropoffDistrictCount == 0 && originalDropoffDistrictAsPickupCount == 0) {
                regDistrictCount.putIfAbsent(districtCode, entryValue);
            }
            else
            {
                regDistrictCount.put(districtCode, entryValue);
            }
        }
    }

    public void processPickupDistrictCount(String districtCode, String district, Integer originalPickupDistrictCount, Integer originalPickupDistrictAsDropoffCount, Integer newPickupDistrictCount, Long newTimestamp) throws Exception{

        // process RegDistrictCount
        if (newPickupDistrictCount == 0 && originalPickupDistrictAsDropoffCount == 0) {
            regDistrictCount.destroy(districtCode);
        } else {
            JSONObject jsonObj = new JSONObject().put("districtCode", districtCode).put("district", district)
                    .put("pickupCount", newPickupDistrictCount).put("dropoffCount", originalPickupDistrictAsDropoffCount).put("timestamp", newTimestamp);
            PdxInstance entryValue = JSONFormatter.fromJSON(jsonObj.toString());

            if(originalPickupDistrictCount == 0 && originalPickupDistrictAsDropoffCount == 0) {
                regDistrictCount.putIfAbsent(districtCode, entryValue);
            }
            else
            {
                regDistrictCount.put(districtCode, entryValue);
            }
        }
    }

//    public void processDropoffDistrictTop(String districtCode, String district, Integer originalCount, Long originalTimestamp, Integer newCount, Long newTimestamp) throws Exception{
//
//        if (newCount > originalCount) {
//            // add to new entry
//            addDistrictToTop(regDropoffDistrictTop, districtCode, district, newCount, newTimestamp);
//
//            if (originalCount != 0) {
//                // remove from old entry
//                removeDistrictFromOldTop(regDropoffDistrictTop, districtCode, district, originalCount, originalTimestamp);
//            }
//
//        }
//        else if (newCount < originalCount) {
//            // TODO waiting for expiration destroy opertaion to be published in async event
//            // https://issues.apache.org/jira/browse/GEODE-1209
//            if (newCount != 0) {
//                // add to new entry
//                addDistrictToTop(regDropoffDistrictTop, districtCode, district, newCount, newTimestamp);
//            }
//
//            // add to new entry
//            removeDistrictFromOldTop(regDropoffDistrictTop, districtCode, district, originalCount, originalTimestamp);
//
//        }
//
//    }
}
