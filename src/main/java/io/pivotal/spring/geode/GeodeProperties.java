package io.pivotal.spring.geode;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by lei_xu on 8/8/16.
 */
@ConfigurationProperties(prefix="properties")
public class GeodeProperties {
    // private static final Integer DEFAULT_MAX_CONNECTIONS = 100;

    // private static final Boolean DEFAULT_AUTO_STARTUP = true;

    // private static final Integer DEFAULT_CACHE_SERVER_PORT = 40404;

    // severe, error, warning, info, config, fine
    private static final String DEFAULT_LOG_LEVEL = "warning";

    // private static final String DEFAULT_HOSTNAME_FOR_CLIENTS = "localhost";

    // private static final String DEFAULT_JMX_MANAGER_PORT = "1099";

    // private static final String DEFAULT_LOCATOR_ADDRESS = "localhost[10334]";

    // private static final String DEFAULT_START_JMX = "true";

    // private static final String DEFAULT_USE_JMX = "true";

    // private static final String DEFAULT_USE_LOCATOR = "true";

    private Boolean autoStartup;

    private String bindAddress;

    private String hostNameForClients;

    private Integer cacheServerPort;

    private Integer maxConnections;

    private String logLevel;

    private String logFile;

    private String locatorAddress;

    private String jmxManagerPort;

    private String startJmx;

    private String useJmx;

    private String useLocator;

    private Integer batchSize = 20;

    private Integer batchTimeInterval = 1000;

    private Integer dispatcherThreads = 1;

    private Integer timeout = 1200;

    public void setDispatcherThreads(Integer dispatcherThreads) {
        this.dispatcherThreads = dispatcherThreads;
    }

    public Integer getDispatcherThreads() {
        return this.dispatcherThreads;
    }

    public void setBatchTimeInterval(Integer batchTimeInterval) {
        this.batchTimeInterval = batchTimeInterval;
    }

    public Integer getBatchTimeInterval() {
        return this.batchTimeInterval;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Integer getBatchSize() {
        return this.batchSize;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getTimeout() {
        return this.timeout;
    }

    public void setAutoStartup(Boolean autoStartup) {
        this.autoStartup = autoStartup;
    }

    public Boolean getAutoStartup() {
        if (this.autoStartup == null) {
            return true;
        }
        else {
            return this.autoStartup;
        }
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    public String getBindAddress() {
        if (this.bindAddress == null) {
            return "0.0.0.0";
        }
        else {
            return this.bindAddress;
        }
    }

    public void setHostNameForClients(String hostNameForClients) {
        this.hostNameForClients = hostNameForClients;
    }

    public String getHostNameForClients() {
        if (this.hostNameForClients == null) {
            return "0.0.0.0";
        }
        else {
            return this.hostNameForClients;
        }
    }

    public void setCacheServerPort(Integer cacheServerPort) {
        this.cacheServerPort = cacheServerPort;
    }

    public Integer getCacheServerPort() {
        if (this.cacheServerPort == null) {
            return 40404;
        }
        else {
            return this.cacheServerPort;
        }
    }

    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    public Integer getMaxConnections() {
        if (this.maxConnections == null) {
            return 100;
        }
        else {
            return this.maxConnections;
        }
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getLogLevel() {
        if (this.logLevel == null) {
            return DEFAULT_LOG_LEVEL;
        }
        else {
            return this.logLevel;
        }
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public String getLogFile() {
        return this.logFile;
    }

    public void setLocatorAddress(String locatorAddress) {
        this.locatorAddress = locatorAddress;
    }

    public String getLocatorAddress() {
        if (this.locatorAddress == null) {
            return "0.0.0.0[10334]";
        }
        else {
            return this.locatorAddress;
        }
    }

    public void setJmxManagerPort(String jmxManagerPort) {
        this.jmxManagerPort = jmxManagerPort;
    }

    public String getJmxManagerPort() {
        if (this.jmxManagerPort == null) {
            return "1099";
        }
        else {
            return this.jmxManagerPort;
        }
    }

    public void setStartJmx(String startJmx) {
        this.startJmx = startJmx;
    }

    public String getStartJmx() {
        if (this.startJmx == null) {
            return "true";
        }
        else {
            return this.startJmx;
        }
    }

    public void setUseJmx(String useJmx) {
        this.useJmx = useJmx;
    }

    public String getUseJmx() {
        if (this.useJmx == null) {
            return "true";
        }
        else {
            return this.useJmx;
        }
    }

    public void setUseLocator(String useLocator) {
        this.useLocator = useLocator;
    }

    public String getUseLocator() {
        if (this.useLocator == null) {
            return "true";
        }
        else {
            return this.useLocator;
        }
    }
}
