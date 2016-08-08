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

    public void processRegionCount(String route, Integer originalCount, Long originalTimestamp, Integer newCount, Long newTimestamp) throws Exception{
        // process RegionCount

        if (newCount == 0) {
            regionCount.destroy(route);
        } else {
            JSONObject jsonObj = new JSONObject().put("route", route).put("route_count", newCount).put("timestamp", newTimestamp);
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

    public void processRegionTop(String route, Integer originalCount, Long originalTimestamp, Integer newCount, Long newTimestamp) throws Exception{

        if (newCount > originalCount) {

            // add to new entry
            addRouteToTop(regionTop, route, newCount, newTimestamp);


            if (originalCount != 0) {

                // remove from old entry
                removeRouteFromOldTop(regionTop, route, originalCount, originalTimestamp);
            }

        }
        else if (newCount < originalCount) {
            // TODO waiting for expiration destroy opertaion to be published in async event
            // https://issues.apache.org/jira/browse/GEODE-1209
            if (newCount != 0) {
                // add to new entry
                addRouteToTop(regionTop, route, newCount, newTimestamp);

            }

            // add to new entry
            removeRouteFromOldTop(regionTop, route, originalCount, originalTimestamp);

        }

    }

    private PdxInstance generateRoutesJson(Integer targetCount, LinkedList<String> targetRoutes, LinkedList<Long> targetTimestamps) throws Exception{


        JSONObject jsonObj = new JSONObject().put("route_count", targetCount).put("routes", targetRoutes).put("timestamps", targetTimestamps);

        PdxInstance regionTopValue = JSONFormatter.fromJSON(jsonObj.toString());

        return regionTopValue;

    }

    private void addRouteToTop(Region<Integer, PdxInstance> regionTop, String route, Integer newCount, Long newTimestamp) throws Exception{
//        System.out.println("RawChangeListener: addRouteToTop " + route + " " + newCount);

        LinkedList<String> routes = null;
        LinkedList<Long> timestamps = null;
        Object pdxObj = regionTop.get(newCount);

        if (pdxObj != null)
        {
            routes = (LinkedList)((PdxInstance)pdxObj).getField("routes");
            timestamps = (LinkedList)((PdxInstance)pdxObj).getField("timestamps");
        }
        else
        {
            routes = new LinkedList<String>();
            timestamps = new LinkedList<Long>();
        }


        routes.addFirst(route);
        timestamps.addFirst(newTimestamp);

        PdxInstance newCountValue = generateRoutesJson(newCount, routes, timestamps);

        regionTop.put(newCount, newCountValue);
    }

    private void removeRouteFromOldTop(Region<Integer, PdxInstance> regionTop, String route, Integer originalCount, Long originalTimestamp) throws Exception{

        PdxInstance pdxObj = regionTop.get(originalCount);
        LinkedList<String> routes = (LinkedList<String>)pdxObj.getField("routes");
        LinkedList<Long> timestamps = (LinkedList<Long>)pdxObj.getField("timestamps");

        routes.remove(route);
        timestamps.remove(originalTimestamp);


        if (routes.size() == 0)
        {
            regionTop.destroy(originalCount);

        }
        else {
            PdxInstance newPdxObj = generateRoutesJson(originalCount, routes, timestamps);
            regionTop.replace(originalCount, newPdxObj);
        }
    }


    public void processRegionTopTen(String keyRoute, String keyUuid, Integer keyCount, Long keyTimestamp, Boolean incremental) throws Exception{
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
            ListIterator<String> listIterator = routes.listIterator();

            while (listIterator.hasNext()) {

                rank++;

                String route = listIterator.next();
                String[] routeArray = route.split("_");
                String fromCellValue = routeArray[0];
                String toCellValue = routeArray[1];

                // top ten list element
                JSONObject topTenElement = new JSONObject();
                topTenElement.put("rank", rank);
                topTenElement.put("count", key);
                topTenElement.put("from", fromCellValue);
                topTenElement.put("to", toCellValue);

                topTenList.addLast(topTenElement);

                // top ten matrix element
                JSONObject fromNodeElement = new JSONObject();
                JSONObject toNodeElement = new JSONObject();
                JSONObject linkElement = new JSONObject();

                fromNodeElement.put("name", fromCellValue);
                toNodeElement.put("name", toCellValue);

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
        String crtFromCellValue = crtRouteArray[0];
        String crtToCellValue = crtRouteArray[1];

        toptenJson.put("from", crtFromCellValue);
        toptenJson.put("to", crtToCellValue);
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

//        PdxInstance newRegionTopTenValue = JSONFormatter.fromJSON(toptenJson.toString());
//        regionTopTen.put(1, newRegionTopTenValue);
    }

    private Integer addNodeToNodes(LinkedList<JSONObject> nodes, JSONObject nodeElement) throws Exception{

        for(int num=0; num < nodes.size(); num++) {
            JSONObject crtNode = nodes.get(num);

            if (crtNode.getString("name").equals(nodeElement.getString("name"))) {
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

                if (!crtTopTenElement.getString("from").equals(newTopTenElement.getString("from"))) {
                    return true;
                }

                if (!crtTopTenElement.getString("to").equals(newTopTenElement.getString("to"))) {
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
