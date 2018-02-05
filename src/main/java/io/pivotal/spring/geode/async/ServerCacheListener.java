package io.pivotal.spring.geode.async;

import com.gemstone.gemfire.cache.*;
import com.gemstone.gemfire.cache.util.CacheListenerAdapter;
import com.gemstone.gemfire.pdx.PdxInstance;
import io.pivotal.spring.geode.async.lib.RouteProcessor;
import io.pivotal.spring.geode.async.lib.DistrictProcessor;

import java.util.LinkedList;
import java.util.Properties;

public class ServerCacheListener<K,V> extends CacheListenerAdapter<K,V> implements Declarable {

    private GemFireCache gemFireCache;

    // block route process
    private Region regRouteCount;
    private Region<Integer, PdxInstance> regRouteTop;
    private Region regRouteTopTen;

    // district process
    private Region regDistrictCount;
    private Region regDropoffDistrictTop;

    public void afterDestroy(EntryEvent<K,V> e) {
        try {

            Operation operation = e.getOperation();

            System.out.println("ServerCacheListener operation: " + operation);

            if (operation.equals(Operation.EXPIRE_DESTROY)) {

                PdxInstance raw;
                Object oldValue = e.getOldValue();
                if (oldValue instanceof PdxInstance) {
                    raw = (PdxInstance)oldValue;
                } else {
                    throw new Exception("oldValue class:" + oldValue.getClass().getName());
                }


                Integer countDiff = -1;

                // gemFireCache = CacheFactory.getAnyInstance();
                // regRouteCount = gemFireCache.getRegion("RegRouteCount");
                // regRouteTop = gemFireCache.getRegion("RegRouteTop");
                // regRouteTopTen = gemFireCache.getRegion("RegRouteTopTen");

                // RouteProcessor routeProcessor = new RouteProcessor(regRouteCount, regRouteTop, regRouteTopTen);

                gemFireCache = CacheFactory.getAnyInstance();

                // for block route process
                regRouteCount = gemFireCache.getRegion("RegRouteCount");
                regRouteTop = gemFireCache.getRegion("RegRouteTop");
                regRouteTopTen = gemFireCache.getRegion("RegRouteTopTen");
                RouteProcessor routeProcessor = new RouteProcessor(regRouteCount, regRouteTop, regRouteTopTen);

                // for district process
                regDistrictCount = gemFireCache.getRegion("RegDistrictCount");
                regDropoffDistrictTop = gemFireCache.getRegion("RegDropoffDistrictTop");
                DistrictProcessor districtProcessor = new DistrictProcessor(regDistrictCount, regDropoffDistrictTop);

                ///////////////////////////////////////////////////////////////////////////
                ///////////////////////// block route process /////////////////////////////
                ///////////////////////////////////////////////////////////////////////////
                // count & top process
                String route = (String)raw.getField("route");
                String pickupAddress = (String)raw.getField("pickupAddress");
                String dropoffAddress = (String)raw.getField("dropoffAddress");
                Long newTimestamp = (Long)raw.getField("timestamp");

                Integer originalCount = 0 ;
                Integer newCount = 0;
                Long originalTimestamp = 0L;

                PdxInstance originCountValue = (PdxInstance)regRouteCount.get(route);

                if(originCountValue==null){
                    newCount = 1;
                }
                else
                {
                    originalCount = ((Byte)originCountValue.getField("route_count")).intValue();
                    originalTimestamp = (Long)originCountValue.getField("timestamp");
                    newCount = originalCount + countDiff;
                }

                // top ten process
                Integer smallestToptenCount = 0;
                PdxInstance topTenValue = (PdxInstance)regRouteTopTen.get(1);
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
                Boolean incremental = false;

                routeProcessor.processRouteCount(route, pickupAddress, dropoffAddress, originalCount, newCount, newTimestamp);
                routeProcessor.processRouteTop(route, pickupAddress, dropoffAddress, originalCount, originalTimestamp, newCount, newTimestamp);

                if (newCount < originalCount) {

                    if (originalCount >= smallestToptenCount) {
                        keyTimestamp = (Long)raw.getField("timestamp");
                        keyUuid = (String)raw.getField("uuid");
                        keyRoute = route;
                        keyPickupAddress = pickupAddress;
                        keyDropoffAddress = dropoffAddress;
                        keyCount = newCount;
                        incremental = false;

                        routeProcessor.processRouteTopTen(keyRoute, keyPickupAddress, keyDropoffAddress, keyUuid, keyCount, keyTimestamp, incremental);
                    }
                }

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
                    originalDropoffDistrictCount = Integer.parseInt(originalDropoffDistrict.getField("dropoffCount").toString());
                    newDropoffDistrictCount = originalDropoffDistrictCount + countDiff;
                    originalDropoffDistrictAsPickupCount = Integer.parseInt(originalDropoffDistrict.getField("pickupCount").toString());
                }


                districtProcessor.processDropoffDistrictCount(dropoffDistrictCode, dropoffDistrict, originalDropoffDistrictCount, originalDropoffDistrictAsPickupCount, newDropoffDistrictCount, newTimestamp);
                districtProcessor.processDropoffDistrictTop(keyTimestamp);

            }
            else {
                throw new Exception("operation:" + e.getOperation().toString());
            }

        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void init(Properties props) {
        // do nothing
    }
}
