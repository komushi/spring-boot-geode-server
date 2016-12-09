package io.pivotal.spring.geode.async.lib;

import org.apache.geode.cache.Region;
//import org.apache.geode.cache.query.Query;
//import com.gemstone.gemfire.management.internal.beans.stats.IntegerStatsDeltaAggregator;
import org.apache.geode.pdx.JSONFormatter;
import org.apache.geode.pdx.PdxInstance;
//import org.apache.geode.cache.query.QueryService;
import org.apache.geode.cache.query.SelectResults;
//import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by lei_xu on 8/16/16.
 */
public class DistrictProcessor {

    private Region regDistrictCount;
    private Region<Integer, PdxInstance> regDropoffDistrictTop;
    private Region<Integer, PdxInstance> regPickupDistrictTop;
//    private QueryService queryService;

//    public DistrictProcessor(QueryService queryService,Region regDistrictCount, Region<Integer, PdxInstance> regDropoffDistrictTop) {
    public DistrictProcessor(Region regDistrictCount, Region<Integer, PdxInstance> regDropoffDistrictTop) {
        this.regDistrictCount = regDistrictCount;
        this.regDropoffDistrictTop = regDropoffDistrictTop;
//        this.queryService = queryService;
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

    public void processDropoffDistrictTop(Long keyTimestamp) throws Exception{
        JSONObject topJson = new JSONObject();
//        LinkedList<JSONObject> topList = new LinkedList();
        JSONObject topList = new JSONObject();
//        Collection countPdxCollection = regDistrictCount.values();
//        System.out.println("not query:");
//        for (Iterator<PdxInstance> iter = countPdxCollection.iterator(); iter.hasNext();) {
//            PdxInstance entryValue = iter.next();
//
//            System.out.print("districtCode:" + entryValue.getField("districtCode"));
//            System.out.print(" district:" + entryValue.getField("district"));
//            System.out.println(" dropoffCount:" + entryValue.getField("dropoffCount").toString());
//        }

        System.out.println("query:");
        Integer rank = 1;
        SelectResults<PdxInstance> results = regDistrictCount.query("dropoffCount > 0 order by dropoffCount desc, timstamp desc");
        for (Iterator<PdxInstance> iter = results.iterator(); iter.hasNext();) {
            PdxInstance entryValue = iter.next();

            String district = entryValue.getField("district").toString();
            String districtCode = entryValue.getField("districtCode").toString();
            Integer dropoffCount = Integer.parseInt(entryValue.getField("dropoffCount").toString());

            System.out.print("districtCode:" + districtCode);
            System.out.print(" district:" + district);
            System.out.println(" dropoffCount:" + dropoffCount);

            JSONObject dropoffDistrictElement = new JSONObject();
            dropoffDistrictElement.put("district", district);
            dropoffDistrictElement.put("dropoffCount", dropoffCount);
            dropoffDistrictElement.put("rank", rank++);

            // top ten list element

            topList.put(entryValue.getField("districtCode").toString(), dropoffDistrictElement);

//            topList.addLast(topTenElement);
        }

        // set toptenlist
        topJson.put("toplist", topList);

        PdxInstance newRegDropoffDistrictTopValue = JSONFormatter.fromJSON(topJson.toString());
        PdxInstance crtRegDropoffDistrictTopValue = (PdxInstance)regDropoffDistrictTop.get(1);

        if (differTop(crtRegDropoffDistrictTopValue, newRegDropoffDistrictTopValue))
        {
            regDropoffDistrictTop.put(1, newRegDropoffDistrictTopValue);
        }

    }

    private boolean differTop(PdxInstance crtRegDropoffDistrictTopValue, PdxInstance newRegDropoffDistrictTopValue){

        if (crtRegDropoffDistrictTopValue == null && newRegDropoffDistrictTopValue!= null)
        {
            return true;
        }

        if (crtRegDropoffDistrictTopValue != null && newRegDropoffDistrictTopValue== null)
        {
            return true;
        }


        PdxInstance crtTopList = (PdxInstance)crtRegDropoffDistrictTopValue.getField("toplist");
        PdxInstance newTopList = (PdxInstance)newRegDropoffDistrictTopValue.getField("toplist");

        List<String> crtFields = crtTopList.getFieldNames();
        List<String> newFields = newTopList.getFieldNames();
        if (newTopList.getFieldNames().size() != crtTopList.getFieldNames().size()) {
            return true;
        }

        for (Iterator<String> iter = crtFields.iterator(); iter.hasNext();) {

            String key = iter.next();
            if (crtTopList.getField(key) != newTopList.getField(key)) {
                return true;
            }

        }

        return false;
    }

}
