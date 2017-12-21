package io.pivotal.spring.geode;

import java.util.Properties;

import com.gemstone.gemfire.cache.ExpirationAction;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.server.CacheServerFactoryBean;
import org.springframework.data.gemfire.PartitionedRegionFactoryBean;
import org.springframework.data.gemfire.RegionAttributesFactoryBean;
import org.springframework.data.gemfire.ExpirationAttributesFactoryBean;
import org.springframework.data.gemfire.wan.AsyncEventQueueFactoryBean;


import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.RegionAttributes;
import com.gemstone.gemfire.cache.ExpirationAttributes;

import io.pivotal.spring.geode.async.RawAsyncEventListener;
import io.pivotal.spring.geode.async.ServerCacheListener;


/**
 * Created by lei_xu on 8/8/16.
 */
@Configuration
@EnableConfigurationProperties(GeodeProperties.class)
public class GeodeCacheServerConfiguration {
    @Autowired
    private GeodeProperties properties;

    @Bean
    CacheServerFactoryBean geodeCacheServer (Cache gemfireCache) {

        CacheServerFactoryBean geodeCacheServer = new CacheServerFactoryBean();

        geodeCacheServer.setCache(gemfireCache);
        geodeCacheServer.setAutoStartup(properties.getAutoStartup());
        geodeCacheServer.setBindAddress(properties.getBindAddress());
        geodeCacheServer.setHostNameForClients(properties.getHostNameForClients());
        geodeCacheServer.setPort(properties.getCacheServerPort());
        geodeCacheServer.setMaxConnections(properties.getMaxConnections());

        return geodeCacheServer;
    }

    @Bean
    CacheFactoryBean geodeCache(@Qualifier("geodeProperties") Properties geodeProperties) {
        CacheFactoryBean geodeCache = new CacheFactoryBean();

        geodeCache.setClose(true);
        geodeCache.setProperties(geodeProperties);
        // gemfireCache.setUseBeanFactoryLocator(false);
        // geodeCache.setPdxReadSerialized(false);

        return geodeCache;
    }

    @Bean
    Properties geodeProperties() {
        Properties geodeProperties = new Properties();

        geodeProperties.setProperty("name", BootApplication.class.getSimpleName());
        geodeProperties.setProperty("log-level", properties.getLogLevel());


        if (properties.getUseLocator().equals("true")) {
            geodeProperties.setProperty("mcast-port", "0");
            geodeProperties.setProperty("start-locator", properties.getLocatorAddress());
        }

        if (properties.getUseJmx().equals("true")) {
            geodeProperties.setProperty("jmx-manager", properties.getUseJmx());
            geodeProperties.setProperty("jmx-manager-port", properties.getJmxManagerPort());
            geodeProperties.setProperty("jmx-manager-start", properties.getStartJmx());
        }


        String logFile = properties.getLogFile();

        if (logFile != null && !logFile.isEmpty()) {
            geodeProperties.setProperty("log-file", logFile);
        }

        return geodeProperties;
    }


    // RegRaw Configurations
    @Bean(name = "RegRaw")
    PartitionedRegionFactoryBean<String, Object> rawRegion(Cache gemfireCache,
                                                           @Qualifier("rawRegionAttributes") RegionAttributes<String, Object> rawRegionAttributes)
    {
        PartitionedRegionFactoryBean<String, Object> rawRegion = new PartitionedRegionFactoryBean<>();

        rawRegion.setCache(gemfireCache);
        rawRegion.setClose(false);
        rawRegion.setAttributes(rawRegionAttributes);
        rawRegion.setName("RegRaw");
        rawRegion.setPersistent(false);

        return rawRegion;
    }

    @Bean
    @SuppressWarnings("unchecked")
    RegionAttributesFactoryBean rawRegionAttributes(@Qualifier("expirationAttributes") ExpirationAttributes expirationAttributes) {
        RegionAttributesFactoryBean rawRegionAttributes = new RegionAttributesFactoryBean();

        rawRegionAttributes.addAsyncEventQueueId("rawQueue");
        rawRegionAttributes.setKeyConstraint(String.class);
        rawRegionAttributes.setValueConstraint(Object.class);
        rawRegionAttributes.setEntryTimeToLive(expirationAttributes);
        rawRegionAttributes.addCacheListener(serverCacheListener());

        return rawRegionAttributes;
    }

    @Bean
    @SuppressWarnings("unchecked")
    ExpirationAttributesFactoryBean expirationAttributes() {
        ExpirationAttributesFactoryBean expirationAttributes = new ExpirationAttributesFactoryBean();

        expirationAttributes.setTimeout(properties.getTimeout());
        expirationAttributes.setAction(ExpirationAction.DESTROY);

        return expirationAttributes;
    }

    @Bean
    RawAsyncEventListener rawAsyncEventListener() {
        return new RawAsyncEventListener();
    }

    @Bean
    ServerCacheListener serverCacheListener() {
         return new ServerCacheListener();
     }

    @Bean
    AsyncEventQueueFactoryBean asyncEventQueue(Cache gemfireCache) {
        AsyncEventQueueFactoryBean asyncEventQueue = new AsyncEventQueueFactoryBean(gemfireCache, rawAsyncEventListener());
        asyncEventQueue.setName("rawQueue");
        asyncEventQueue.setParallel(false);
        asyncEventQueue.setDispatcherThreads(properties.getDispatcherThreads());
        asyncEventQueue.setBatchTimeInterval(properties.getBatchTimeInterval());
        asyncEventQueue.setBatchSize(properties.getBatchSize());
        asyncEventQueue.setBatchConflationEnabled(true);
        asyncEventQueue.setPersistent(false);
        asyncEventQueue.setDiskSynchronous(false);

        return asyncEventQueue;
    }

    // RegRouteCount Configurations
    @Bean(name = "RegRouteCount")
    PartitionedRegionFactoryBean<String, Integer> routeCountRegion(Cache gemfireCache)
    {
        PartitionedRegionFactoryBean<String, Integer> routeCountRegion = new PartitionedRegionFactoryBean<>();

        routeCountRegion.setCache(gemfireCache);
        routeCountRegion.setClose(false);
        routeCountRegion.setName("RegRouteCount");
        routeCountRegion.setPersistent(false);

        return routeCountRegion;
    }

    // RegRouteTop Configurations
    @Bean(name = "RegRouteTop")
    PartitionedRegionFactoryBean<Integer, Object> routeTopRegion(Cache gemfireCache)
    {
        PartitionedRegionFactoryBean<Integer, Object> routeTopRegion = new PartitionedRegionFactoryBean<>();

        routeTopRegion.setCache(gemfireCache);
//        topRegion.setClose(false);
        routeTopRegion.setName("RegRouteTop");
        routeTopRegion.setPersistent(false);

        return routeTopRegion;
    }

    // RegRouteTopTen Configurations
    @Bean(name = "RegRouteTopTen")
    PartitionedRegionFactoryBean<Integer, Object> routeToptenRegion(Cache gemfireCache)
    {
        PartitionedRegionFactoryBean<Integer, Object> routeToptenRegion = new PartitionedRegionFactoryBean<>();

        routeToptenRegion.setCache(gemfireCache);
//        topTenRegion.setClose(false);
        routeToptenRegion.setName("RegRouteTopTen");
        routeToptenRegion.setPersistent(false);

        return routeToptenRegion;
    }

    // RegDistrictCount Configurations
    @Bean(name = "RegDistrictCount")
    PartitionedRegionFactoryBean<String, Object> districtCountRegion(Cache gemfireCache)
    {
        PartitionedRegionFactoryBean<String, Object> districtCountRegion = new PartitionedRegionFactoryBean<>();

        districtCountRegion.setCache(gemfireCache);
        districtCountRegion.setName("RegDistrictCount");
        districtCountRegion.setPersistent(false);

        return districtCountRegion;
    }

    // RegDropoffDistrictTop Configurations
    @Bean(name = "RegDropoffDistrictTop")
    PartitionedRegionFactoryBean<String, Object> dropoffDistrictTopRegion(Cache gemfireCache)
    {
        PartitionedRegionFactoryBean<String, Object> dropoffDistrictTopRegion = new PartitionedRegionFactoryBean<>();

        dropoffDistrictTopRegion.setCache(gemfireCache);
        dropoffDistrictTopRegion.setName("RegDropoffDistrictTop");
        dropoffDistrictTopRegion.setPersistent(false);

        return dropoffDistrictTopRegion;
    }

    // RegDistrictRouteCount Configurations
    @Bean(name = "RegDistrictRouteCount")
    PartitionedRegionFactoryBean<String, Integer> districtRouteCountRegion(Cache gemfireCache)
    {
        PartitionedRegionFactoryBean<String, Integer> districtRouteCountRegion = new PartitionedRegionFactoryBean<>();

        districtRouteCountRegion.setCache(gemfireCache);
        districtRouteCountRegion.setClose(false);
        districtRouteCountRegion.setName("RegDistrictRouteCount");
        districtRouteCountRegion.setPersistent(false);

        return districtRouteCountRegion;
    }

    // RegDistrictRouteTop Configurations
    @Bean(name = "RegDistrictRouteTop")
    PartitionedRegionFactoryBean<Integer, Object> districtRouteTopRegion(Cache gemfireCache)
    {
        PartitionedRegionFactoryBean<Integer, Object> districtRouteTopRegion = new PartitionedRegionFactoryBean<>();

        districtRouteTopRegion.setCache(gemfireCache);
        districtRouteTopRegion.setName("RegDistrictRouteTop");
        districtRouteTopRegion.setPersistent(false);

        return districtRouteTopRegion;
    }
}
