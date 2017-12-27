package io.pivotal.spring.geode.async;

import com.gemstone.gemfire.cache.*;
import com.gemstone.gemfire.cache.asyncqueue.AsyncEvent;
import com.gemstone.gemfire.cache.asyncqueue.AsyncEventListener;
//import com.gemstone.gemfire.cache.query.QueryService;
import com.gemstone.gemfire.pdx.PdxInstance;
import io.pivotal.spring.geode.async.lib.DistrictProcessor;
import io.pivotal.spring.geode.async.lib.RouteProcessor;
import io.pivotal.spring.geode.async.lib.DistrictRouteProcessor;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Created by lei_xu on 7/17/16.
 */
public class RawAsyncEventListener implements AsyncEventListener, Declarable {

    private GemFireCache gemFireCache;

    // block route process
    private Region regRouteCount;
    private Region<Integer, PdxInstance> regRouteTop;
    private Region regRouteTopTen;

    // district process
    private Region regDistrictCount;
    private Region regPickupDistrictTop;
    private Region regDropoffDistrictTop;

    // district route process
    private Region regDistrictRouteCount;
    private Region<Integer, PdxInstance> regDistrictRouteTop;


    @Override
    public boolean processEvents(List<AsyncEvent> events) {

        gemFireCache = CacheFactory.getAnyInstance();
//        CacheTransactionManager tm = gemFireCache.getCacheTransactionManager();
//        tm.begin();
//        QueryService queryService = gemFireCache.getQueryService();


        // for block route process
        regRouteCount = gemFireCache.getRegion("RegRouteCount");
        regRouteTop = gemFireCache.getRegion("RegRouteTop");
        regRouteTopTen = gemFireCache.getRegion("RegRouteTopTen");
        RouteProcessor routeProcessor = new RouteProcessor(regRouteCount, regRouteTop, regRouteTopTen);

        // for district process
        regDistrictCount = gemFireCache.getRegion("RegDistrictCount");
        regDropoffDistrictTop = gemFireCache.getRegion("RegDropoffDistrictTop");
        DistrictProcessor districtProcessor = new DistrictProcessor(regDistrictCount, regDropoffDistrictTop);

        // for district route process
        regDistrictRouteCount = gemFireCache.getRegion("RegDistrictRouteCount");
        regDistrictRouteTop = gemFireCache.getRegion("RegDistrictRouteTop");
        DistrictRouteProcessor districtRouteProcessor = new DistrictRouteProcessor(regDistrictRouteCount, regDistrictRouteTop);


        Integer smallestToptenCount = 0;
        Boolean isProcessTopTen = false;

        PdxInstance topTenValue = (PdxInstance)regRouteTopTen.get(1);
        if (topTenValue != null) {
            LinkedList toptenList = (LinkedList)topTenValue.getField("toptenlist");
            if (toptenList.size() != 0) {
//                smallestToptenCount = ((Byte)((PdxInstance)toptenList.getLast()).getField("count")).intValue();
                smallestToptenCount = Integer.parseInt(((PdxInstance)toptenList.getLast()).getField("count").toString());
            }
        }

        Long keyTimestamp = 0L;
        String keyUuid = "";
        String keyRoute = "";
        String keyPickupAddress = "";
        String keyDropoffAddress = "";
        Integer keyRouteCount = 0;
        Boolean incremental = true;

        System.out.println("new events, events.size:" + events.size());
        try {
            for (AsyncEvent event : events) {

                ///////////////////////////////////////////////////////////////////////////
                //////////////////////////// get event data ///////////////////////////////
                ///////////////////////////////////////////////////////////////////////////
                Operation operation = event.getOperation();
                Integer countDiff = 0;

                System.out.println("RawAsyncEventListener operation: " + operation);

                if (operation.equals(Operation.PUTALL_CREATE)) {
                    countDiff = 1;
                }
                else if (operation.equals(Operation.EXPIRE_DESTROY)) {
                    countDiff = -1;
                }
                else {
                    System.out.println("unknown operation: " + operation);
                    continue;
                }

                PdxInstance raw = (PdxInstance) event.getDeserializedValue();
                Long newTimestamp = (Long)raw.getField("timestamp");


                ///////////////////////////////////////////////////////////////////////////
                ///////////////////////// block route process /////////////////////////////
                ///////////////////////////////////////////////////////////////////////////
                // get route from the key in JSON format
                String route = (String)raw.getField("route");
                String pickupAddress = (String)raw.getField("pickupAddress");
                String dropoffAddress = (String)raw.getField("dropoffAddress");

                Integer originalRouteCount = 0 ;
                Integer newRouteCount = 0;
                Long originalRouteTimestamp = 0L;

                PdxInstance originRouteCountValue = (PdxInstance)regRouteCount.get(route);

                if(originRouteCountValue==null){
                    newRouteCount = 1;
                }
                else
                {
//                    originalRouteCount = ((Byte)originRouteCountValue.getField("route_count")).intValue();
                    originalRouteCount = Integer.parseInt(originRouteCountValue.getField("route_count").toString());

                    originalRouteTimestamp = (Long)originRouteCountValue.getField("timestamp");
                    newRouteCount = originalRouteCount + countDiff;
                }

//                processRegionCount(regRouteCount, route, originalCount, originalTimestamp, newCount, newTimestamp);
                routeProcessor.processRouteCount(route, pickupAddress, dropoffAddress, originalRouteCount, newRouteCount, newTimestamp);

//                processRegionTop(regionTop, route, originalCount, originalTimestamp, newCount, newTimestamp);
                routeProcessor.processRouteTop(route, pickupAddress, dropoffAddress, originalRouteCount, originalRouteTimestamp, newRouteCount, newTimestamp);

                // Check whether need to refresh topten
                if (newRouteCount > originalRouteCount) {
                    if (newRouteCount >= smallestToptenCount) {
                        isProcessTopTen = Boolean.logicalOr(isProcessTopTen, Boolean.TRUE);

                        if (keyRoute.isEmpty()) {
                            keyTimestamp = (Long)raw.getField("timestamp");
                            keyUuid = (String)raw.getField("uuid");
                            keyRoute = route;
                            keyPickupAddress = pickupAddress;
                            keyDropoffAddress = dropoffAddress;
                            keyRouteCount = newRouteCount;
                            incremental = true;
                        }
                    }
                }
//                else if (newRouteCount < originalRouteCount){
//                    // TODO waiting for expiration destroy opertaion to be published in async event
//                    // https://issues.apache.org/jira/browse/GEODE-1209
//                    if (originalRouteCount >= smallestToptenCount) {
//                        isProcessTopTen = Boolean.logicalOr(isProcessTopTen, Boolean.TRUE);
//                    }
//                }

                ///////////////////////////////////////////////////////////////////////////
                /////////////////////////// district process //////////////////////////////
                ///////////////////////////////////////////////////////////////////////////
                String dropoffDistrict = (String)raw.getField("dropoffDistrict");
                String dropoffDistrictCode = (String)raw.getField("dropoffDistrictCode");
                String pickupDistrict = (String)raw.getField("pickupDistrict");
                String pickupDistrictCode = (String)raw.getField("pickupDistrictCode");

                Integer originalDropoffDistrictCount = 0 ;
                Integer originalDropoffDistrictAsPickupCount = 0 ;
                Integer newDropoffDistrictCount  = 0;

                PdxInstance originalDropoffDistrict = (PdxInstance)regDistrictCount.get(dropoffDistrictCode);

                if(originalDropoffDistrict == null){
                    newDropoffDistrictCount = 1;
                }
                else
                {
//                    originalDropoffDistrictCount = ((Byte)originalDropoffDistrict.getField("dropoffCount")).intValue();
                    originalDropoffDistrictCount = Integer.parseInt(originalDropoffDistrict.getField("dropoffCount").toString());
                    newDropoffDistrictCount = originalDropoffDistrictCount + countDiff;
//                    originalDropoffDistrictAsPickupCount = ((Byte)originalDropoffDistrict.getField("pickupCount")).intValue();
                    originalDropoffDistrictAsPickupCount = Integer.parseInt(originalDropoffDistrict.getField("pickupCount").toString());
                }


                districtProcessor.processDropoffDistrictCount(dropoffDistrictCode, dropoffDistrict, originalDropoffDistrictCount, originalDropoffDistrictAsPickupCount, newDropoffDistrictCount, newTimestamp);

                Integer originalPickupDistrictCount = 0 ;
                Integer originalPickupDistrictAsDropoffCount = 0 ;
                Integer newPickupDistrictCount  = 0;

                PdxInstance originalPickupDistrict = (PdxInstance)regDistrictCount.get(pickupDistrictCode);

                if(originalPickupDistrict == null){
                    newPickupDistrictCount = 1;
                }
                else
                {
//                    originalPickupDistrictCount = ((Byte)originalPickupDistrict.getField("pickupCount")).intValue();
                    originalPickupDistrictCount = Integer.parseInt(originalPickupDistrict.getField("pickupCount").toString());
                    newPickupDistrictCount = originalPickupDistrictCount + countDiff;
//                    originalPickupDistrictAsDropoffCount = ((Byte)originalPickupDistrict.getField("dropoffCount")).intValue();
                    originalPickupDistrictAsDropoffCount = Integer.parseInt(originalPickupDistrict.getField("dropoffCount").toString());
                }

                districtProcessor.processPickupDistrictCount(pickupDistrictCode, pickupDistrict, originalPickupDistrictCount, originalPickupDistrictAsDropoffCount, newPickupDistrictCount, newTimestamp);

                ///////////////////////////////////////////////////////////////////////////
                ///////////////////////// district route process //////////////////////////
                ///////////////////////////////////////////////////////////////////////////
                String districtRoute = pickupDistrictCode + "_" + dropoffDistrictCode;

                Integer originalDistrictRouteCount = 0 ;
                Integer newDistrictRouteCount = 0;
                Long originalDistrictRouteTimestamp = 0L;

                PdxInstance originDistrictRouteCountValue = (PdxInstance)regDistrictRouteCount.get(districtRoute);

                if(originDistrictRouteCountValue==null){
                    newDistrictRouteCount = 1;
                }
                else
                {
                    originalRouteCount = Integer.parseInt(originDistrictRouteCountValue.getField("route_count").toString());

                    originalDistrictRouteTimestamp = (Long)originDistrictRouteCountValue.getField("timestamp");
                    newDistrictRouteCount = originalDistrictRouteCount + countDiff;
                }

                districtRouteProcessor.processRouteCount(districtRoute, pickupDistrict, dropoffDistrict,
                        originalRouteCount, newDistrictRouteCount, newTimestamp);
            }

            ///////////////////////////////////////////////////////////////////////////
            ///////////////////////// top block routes process ////////////////////////
            ///////////////////////////////////////////////////////////////////////////
            if (isProcessTopTen) {
                routeProcessor.processRouteTopTen(keyRoute, keyPickupAddress, keyDropoffAddress, keyUuid, keyRouteCount, keyTimestamp, incremental);
            }

            ///////////////////////////////////////////////////////////////////////////
            /////////////////////////// top district process //////////////////////////
            ///////////////////////////////////////////////////////////////////////////
            districtProcessor.processDropoffDistrictTop(keyTimestamp);
//            tm.commit();

//        } catch(CommitConflictException e){
//            e.printStackTrace();
//            tm.rollback();
//            System.out.println("-----------------------------CommitConflictException");
//            return false;
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