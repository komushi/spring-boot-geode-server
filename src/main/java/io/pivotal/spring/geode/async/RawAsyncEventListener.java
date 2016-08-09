package io.pivotal.spring.geode.async;

import com.gemstone.gemfire.cache.*;
import com.gemstone.gemfire.cache.asyncqueue.AsyncEvent;
import com.gemstone.gemfire.cache.asyncqueue.AsyncEventListener;
import com.gemstone.gemfire.pdx.PdxInstance;
import io.pivotal.spring.geode.async.lib.RegionProcessor;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Created by lei_xu on 7/17/16.
 */
public class RawAsyncEventListener implements AsyncEventListener, Declarable {

    private GemFireCache gemFireCache;
    private Region regionCount;
//    private Region regionRaw = gemFireCache.getRegion("RegionRaw");
    private Region<Integer, PdxInstance> regionTop;
    private Region regionTopTen;

    @Override
    public boolean processEvents(List<AsyncEvent> events) {

//        GemFireCache gemFireCache = CacheFactory.getAnyInstance();
//        Region regionCount = gemFireCache.getRegion("RegionCount");
//        Region<Integer, PdxInstance> regionTop = gemFireCache.getRegion("RegionTop");
//        Region regionTopTen = gemFireCache.getRegion("RegionTopTen");

        gemFireCache = CacheFactory.getAnyInstance();
        regionCount = gemFireCache.getRegion("RegionCount");
        regionTop = gemFireCache.getRegion("RegionTop");
        regionTopTen = gemFireCache.getRegion("RegionTopTen");

        RegionProcessor processor = new RegionProcessor(regionCount, regionTop, regionTopTen);

        Integer smallestToptenCount = 0;
        Boolean isProcessTopTen = false;

        PdxInstance topTenValue = (PdxInstance)regionTopTen.get(1);
        if (topTenValue != null) {
            LinkedList toptenList = (LinkedList)topTenValue.getField("toptenlist");
            if (toptenList.size() != 0) {
                smallestToptenCount = ((Byte)((PdxInstance)toptenList.getLast()).getField("count")).intValue();
            }
        }

        Long keyTimestamp = 0L;
        String keyUuid = "";
        String keyRoute = "";
        String keyPickupAddress = "";
        String keyDropoffAddress = "";
        Integer keyCount = 0;
        Boolean incremental = true;

        System.out.println("new events, events.size:" + events.size());
        try {
            for (AsyncEvent event : events) {


                Operation operation = event.getOperation();
                Integer countDiff = 0;

                if (operation.equals(Operation.PUTALL_CREATE)) {
                    countDiff = 1;
                }
                else if (operation.equals(Operation.EXPIRE_DESTROY)) {
                    countDiff = -1;
                }
                else {
                    System.out.println("unknown ooperation " + event.getOperation());
                    continue;
                }

                PdxInstance raw = (PdxInstance) event.getDeserializedValue();

                // get route from the key in JSON format
                String route = (String)raw.getField("route");
                String pickupAddress = (String)raw.getField("pickupAddress");
                String dropoffAddress = (String)raw.getField("dropoffAddress");
                Long newTimestamp = (Long)raw.getField("timestamp");

                Integer originalCount = 0 ;
                Integer newCount = 0;
                Long originalTimestamp = 0L;

                PdxInstance originCountValue = (PdxInstance)regionCount.get(route);

                if(originCountValue==null){
                    newCount = 1;
                }
                else
                {
                    originalCount = ((Byte)originCountValue.getField("route_count")).intValue();
                    originalTimestamp = (Long)originCountValue.getField("timestamp");
                    newCount = originalCount + countDiff;
                }

//                processRegionCount(regionCount, route, originalCount, originalTimestamp, newCount, newTimestamp);
                processor.processRegionCount(route, pickupAddress, dropoffAddress, originalCount, originalTimestamp, newCount, newTimestamp);

//                processRegionTop(regionTop, route, originalCount, originalTimestamp, newCount, newTimestamp);
                processor.processRegionTop(route, pickupAddress, dropoffAddress, originalCount, originalTimestamp, newCount, newTimestamp);

                // Check whether need to refresh topten
                if (newCount > originalCount) {

                    if (newCount >= smallestToptenCount) {
                        isProcessTopTen = Boolean.logicalOr(isProcessTopTen, Boolean.TRUE);

                        if (keyRoute.isEmpty()) {
                            keyTimestamp = (Long)raw.getField("timestamp");
                            keyUuid = (String)raw.getField("uuid");
                            keyRoute = route;
                            keyPickupAddress = pickupAddress;
                            keyDropoffAddress = dropoffAddress;
                            keyCount = newCount;
                            incremental = true;
                        }
                    }
                }
//                else if (newCount < originalCount){
//                    // TODO waiting for expiration destroy opertaion to be published in async event
//                    // https://issues.apache.org/jira/browse/GEODE-1209
//                    if (originalCount >= smallestToptenCount) {
//                        isProcessTopTen = Boolean.logicalOr(isProcessTopTen, Boolean.TRUE);
//                    }
//                }
            }

            if (isProcessTopTen) {
                processor.processRegionTopTen(keyRoute, keyPickupAddress, keyDropoffAddress, keyUuid, keyCount, keyTimestamp, incremental);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }



    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public void init(Properties arg0) {
        // TODO Auto-generated method stub

    }
}