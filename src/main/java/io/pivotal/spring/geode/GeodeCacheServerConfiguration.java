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
//        gemfireCache.setUseBeanFactoryLocator(false);
        geodeCache.setPdxReadSerialized(false);

        return geodeCache;
    }

    @Bean
    Properties geodeProperties() {
        Properties geodeProperties = new Properties();

        geodeProperties.setProperty("name", BootApplication.class.getSimpleName());
        geodeProperties.setProperty("log-level", properties.getLogLevel());


        if (properties.getUseLocator().equals("true")) {
            geodeProperties.setProperty("mcast-port", "0");
            geodeProperties.setProperty("locators", properties.getLocatorAddress());
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


    // RegionRaw Configurations
    @Bean(name = "RegionRaw")
    PartitionedRegionFactoryBean<String, Object> rawRegion(Cache gemfireCache,
                                                           @Qualifier("rawRegionAttributes") RegionAttributes<String, Object> rawRegionAttributes)
    {
        PartitionedRegionFactoryBean<String, Object> rawRegion = new PartitionedRegionFactoryBean<>();

        rawRegion.setCache(gemfireCache);
        rawRegion.setClose(false);
        rawRegion.setAttributes(rawRegionAttributes);
        rawRegion.setName("RegionRaw");
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

//    @Bean
//    RawChangeListener rawChangeListener() {
//        return new RawChangeListener();
//    }

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

    // RegionCount Configurations
    @Bean(name = "RegionCount")
    PartitionedRegionFactoryBean<String, Integer> countRegion(Cache gemfireCache)
    {
        PartitionedRegionFactoryBean<String, Integer> countRegion = new PartitionedRegionFactoryBean<>();

        countRegion.setCache(gemfireCache);
        countRegion.setClose(false);
        countRegion.setName("RegionCount");
        countRegion.setPersistent(false);

        return countRegion;
    }

    // RegionTop Configurations
    @Bean(name = "RegionTop")
    PartitionedRegionFactoryBean<Integer, Object> topRegion(Cache gemfireCache)
    {
        PartitionedRegionFactoryBean<Integer, Object> topRegion = new PartitionedRegionFactoryBean<>();

        topRegion.setCache(gemfireCache);
//        topRegion.setClose(false);
        topRegion.setName("RegionTop");
        topRegion.setPersistent(false);

        return topRegion;
    }

    // RegionTopTen Configurations
    @Bean(name = "RegionTopTen")
    PartitionedRegionFactoryBean<Integer, Object> topTenRegion(Cache gemfireCache)
    {
        PartitionedRegionFactoryBean<Integer, Object> topTenRegion = new PartitionedRegionFactoryBean<>();

        topTenRegion.setCache(gemfireCache);
//        topTenRegion.setClose(false);
        topTenRegion.setName("RegionTopTen");
        topTenRegion.setPersistent(false);

        return topTenRegion;
    }
}
