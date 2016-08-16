package io.pivotal.spring.geode.async.lib;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.pdx.JSONFormatter;
import com.gemstone.gemfire.pdx.PdxInstance;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by lei_xu on 7/21/16.
 */
public class RegionProcessor {

    private Region regionCount;
    private Region<Integer, PdxInstance> regionTop;
    private Region regionTopTen;

    public RegionProcessor(Region regionCount, Region<Integer, PdxInstance> regionTop, Region regionTopTen) {
        this.regionCount = regionCount;
        this.regionTop = regionTop;
        this.regionTopTen = regionTopTen;
    }

    public void processRegionCount(String route, String pickupAddress, String dropoffAddress, Integer originalCount, Long originalTimestamp, Integer newCount, Long newTimestamp) throws Exception{
        // process RegionCount

        if (newCount == 0) {
            regionCount.destroy(route);
        } else {
            JSONObject jsonObj = new JSONObject().put("route", route).put("pickup_address", pickupAddress).put("dropoff_address", dropoffAddress).put("route_count", newCount).put("timestamp", newTimestamp);
            PdxInstance entryValue = JSONFormatter.fromJSON(jsonObj.toString());

            if(originalCount == 0){
                regionCount.putIfAbsent(route, entryValue);
            }
            else
            {
                regionCount.put(route, entryValue);
            }
        }
    }

    public void processRegionTop(String route, String pickupAddress, String dropoffAddress, Integer originalCount, Long originalTimestamp, Integer newCount, Long newTimestamp) throws Exception{

        if (newCount > originalCount) {

            // add to new entry
            addRouteToTop(regionTop, route, pickupAddress, dropoffAddress, newCount, newTimestamp);


            if (originalCount != 0) {

                // remove from old entry
                removeRouteFromOldTop(regionTop, route, pickupAddress, dropoffAddress, originalCount, originalTimestamp);
            }

        }
        else if (newCount < originalCount) {
            // TODO waiting for expiration destroy opertaion to be published in async event
            // https://issues.apache.org/jira/browse/GEODE-1209
            if (newCount != 0) {
                // add to new entry
                addRouteToTop(regionTop, route, pickupAddress, dropoffAddress, newCount, newTimestamp);

            }

            // add to new entry
            removeRouteFromOldTop(regionTop, route, pickupAddress, dropoffAddress, originalCount, originalTimestamp);

        }

    }

    private PdxInstance generateRoutesJson(Integer targetCount, LinkedList<String> targetRoutes, LinkedList<String> targetPickupAddresses, LinkedList<String> targetDropoffAddresses, LinkedList<Long> targetTimestamps) throws Exception{


        JSONObject jsonObj = new JSONObject()
                                .put("route_count", targetCount)
                                .put("routes", targetRoutes)
                                .put("pickup_addresses", targetPickupAddresses)
                                .put("dropoff_addresses", targetDropoffAddresses)
                                .put("timestamps", targetTimestamps);

        PdxInstance regionTopValue = JSONFormatter.fromJSON(jsonObj.toString());

        return regionTopValue;

    }

    private void addRouteToTop(Region<Integer, PdxInstance> regionTop, String route, String pickupAddress, String dropoffAddress, Integer newCount, Long newTimestamp) throws Exception{
//        System.out.println("RawChangeListener: addRouteToTop " + route + " " + newCount);

        LinkedList<String> routes = null;
        LinkedList<Long> timestamps = null;
        LinkedList<String> pickupAddresses = null;
        LinkedList<String> dropoffAddresses = null;
        PdxInstance pdxObj = regionTop.get(newCount);

        if (pdxObj != null)
        {
            routes = (LinkedList<String>)pdxObj.getField("routes");
            timestamps = (LinkedList<Long>)pdxObj.getField("timestamps");
            pickupAddresses = (LinkedList<String>)pdxObj.getField("pickup_addresses");
            dropoffAddresses = (LinkedList<String>)pdxObj.getField("dropoff_addresses");
        }
        else
        {
            routes = new LinkedList<String>();
            timestamps = new LinkedList<Long>();
            pickupAddresses = new LinkedList<String>();
            dropoffAddresses = new LinkedList<String>();
        }

       // routes.addFirst(route);
       // timestamps.addFirst(newTimestamp);
       // pickupAddresses.addFirst(pickupAddress);
       // dropoffAddresses.addFirst(dropoffAddress);

        routes.addLast(route);
        timestamps.addLast(newTimestamp);
        pickupAddresses.addLast(pickupAddress);
        dropoffAddresses.addLast(dropoffAddress);

        PdxInstance newCountValue = generateRoutesJson(newCount, routes, pickupAddresses, dropoffAddresses, timestamps);

        regionTop.put(newCount, newCountValue);
    }

    private void removeRouteFromOldTop(Region<Integer, PdxInstance> regionTop, String route, String pickupAddress, String dropoffAddress, Integer originalCount, Long originalTimestamp) throws Exception{

        PdxInstance pdxObj = regionTop.get(originalCount);
        LinkedList<String> routes = (LinkedList<String>)pdxObj.getField("routes");
        LinkedList<Long> timestamps = (LinkedList<Long>)pdxObj.getField("timestamps");
        LinkedList<String> pickupAddresses = (LinkedList<String>)pdxObj.getField("pickup_addresses");
        LinkedList<String> dropoffAddresses = (LinkedList<String>)pdxObj.getField("dropoff_addresses");

        routes.remove(route);
        timestamps.remove(originalTimestamp);
        pickupAddresses.remove(pickupAddress);
        dropoffAddresses.remove(dropoffAddress);

        if (routes.size() == 0)
        {
            regionTop.destroy(originalCount);

        }
        else {
            PdxInstance newPdxObj = generateRoutesJson(originalCount, routes, pickupAddresses, dropoffAddresses, timestamps);
            regionTop.replace(originalCount, newPdxObj);
        }
    }


    public void processRegionTopTen(String keyRoute, String keyPickupAddress, String keyDropoffAddress, String keyUuid, Integer keyCount, Long keyTimestamp, Boolean incremental) throws Exception{
        JSONObject toptenJson = new JSONObject();

        Set<Integer> keySet = regionTop.keySet();

        List<Integer> keyList = new ArrayList<Integer>(keySet);
        Collections.sort(keyList, Collections.reverseOrder());

        // top ten list for gui table
        LinkedList<JSONObject> topTenList = new LinkedList();


        // top ten matrix for d3 matrix
        JSONObject matrix = new JSONObject();
        LinkedList<JSONObject> nodes = new LinkedList();
        LinkedList<JSONObject> links = new LinkedList();

        Integer rank = 0;

        for (Iterator<Integer> iter = keyList.iterator(); iter.hasNext();) {

            Integer key = iter.next();
            PdxInstance regionTopValue = regionTop.get(key);
            LinkedList<String> routes = (LinkedList<String>)regionTopValue.getField("routes");
            LinkedList<String> pickupAddresses = (LinkedList<String>)regionTopValue.getField("pickup_addresses");
            LinkedList<String> dropoffAddresses = (LinkedList<String>)regionTopValue.getField("dropoff_addresses");
            ListIterator<String> routesIterator = routes.listIterator();
            ListIterator<String> pickupAddressesIterator = pickupAddresses.listIterator();
            ListIterator<String> dropoffAddressesIterator = dropoffAddresses.listIterator();

            while (routesIterator.hasNext()) {

                rank++;

                String route = routesIterator.next();
                String[] routeArray = route.split("_");
                String fromCode = routeArray[0];
                String toCode = routeArray[1];

                String pickupAddress = pickupAddressesIterator.next();
                String dropoffAddress = dropoffAddressesIterator.next();

                // top ten list element
                JSONObject topTenElement = new JSONObject();
                topTenElement.put("rank", rank);
                topTenElement.put("count", key);
                topTenElement.put("from_code", fromCode);
                topTenElement.put("to_code", toCode);
                topTenElement.put("from", pickupAddress);
                topTenElement.put("to", dropoffAddress);

                topTenList.addLast(topTenElement);

                // top ten matrix element
                JSONObject fromNodeElement = new JSONObject();
                JSONObject toNodeElement = new JSONObject();
                JSONObject linkElement = new JSONObject();

                fromNodeElement.put("code", fromCode);
                fromNodeElement.put("name", pickupAddress);
                toNodeElement.put("code", toCode);
                toNodeElement.put("name", dropoffAddress);

                Integer fromPosition = addNodeToNodes(nodes, fromNodeElement);
                Integer toPosition = addNodeToNodes(nodes, toNodeElement);

                linkElement.put("source", fromPosition);
                linkElement.put("target", toPosition);
                linkElement.put("value", key);
                linkElement.put("rank", rank);

                links.addLast(linkElement);

                if (topTenList.size() >= 10)
                {
                    break;
                }
            }

            if (topTenList.size() >= 10)
            {
                break;
            }
        }


        Long delay = (Calendar.getInstance().getTimeInMillis() - keyTimestamp);
        String[] crtRouteArray = keyRoute.split("_");
        String crtFromCode = crtRouteArray[0];
        String crtToCode = crtRouteArray[1];

        toptenJson.put("from_code", crtFromCode);
        toptenJson.put("to_code", crtToCode);
        toptenJson.put("from", keyPickupAddress);
        toptenJson.put("to", keyDropoffAddress);
        toptenJson.put("count", keyCount);
        toptenJson.put("uuid", keyUuid);
        toptenJson.put("delay", delay);
        toptenJson.put("timestamp", keyTimestamp);
        toptenJson.put("incremental", incremental);


        // set toptenlist
        toptenJson.put("toptenlist", topTenList);

        // set matrix
        matrix.put("nodes", nodes);
        matrix.put("links", links);
        toptenJson.put("matrix", matrix);


        PdxInstance newRegionTopTenValue = JSONFormatter.fromJSON(toptenJson.toString());
        PdxInstance crtRegionTopTenValue = (PdxInstance)regionTopTen.get(1);

        if (differTopTen(crtRegionTopTenValue, newRegionTopTenValue))
        {
            regionTopTen.put(1, newRegionTopTenValue);
        }

    }

    private Integer addNodeToNodes(LinkedList<JSONObject> nodes, JSONObject nodeElement) throws Exception{

        for(int num=0; num < nodes.size(); num++) {
            JSONObject crtNode = nodes.get(num);

            if (crtNode.getString("code").equals(nodeElement.getString("code"))) {
                return num;
            }
        }

        nodes.addLast(nodeElement);

        return nodes.size() - 1;
    }

    private boolean differTopTen(PdxInstance crtRegionTopTenValue, PdxInstance newRegionTopTenValue){

        if (crtRegionTopTenValue == null && newRegionTopTenValue!= null)
        {
            return true;
        }

        if (crtRegionTopTenValue != null && newRegionTopTenValue== null)
        {
            return true;
        }


        LinkedList<PdxInstance> crtTopTenList = (LinkedList)crtRegionTopTenValue.getField("toptenlist");
        LinkedList<PdxInstance> newTopTenList = (LinkedList)newRegionTopTenValue.getField("toptenlist");

        if (crtTopTenList.size() != newTopTenList.size()) {
            return true;
        }

        for(int num=0; num < crtTopTenList.size(); num++)
        {
            try {
                JSONObject crtTopTenElement = new JSONObject(JSONFormatter.toJSON(crtTopTenList.get(num)));
                JSONObject newTopTenElement = new JSONObject(JSONFormatter.toJSON(newTopTenList.get(num)));

                if (crtTopTenElement.getInt("rank") != newTopTenElement.getInt("rank")) {
                    return true;
                }

                if (crtTopTenElement.getInt("count") != newTopTenElement.getInt("count")) {
                    return true;
                }

                if (!crtTopTenElement.getString("from_code").equals(newTopTenElement.getString("from_code"))) {
                    return true;
                }

                if (!crtTopTenElement.getString("to_code").equals(newTopTenElement.getString("to_code"))) {
                    return true;
                }
            }
            catch (JSONException e) {
                return true;
            }

        }

        return false;
    }
}
