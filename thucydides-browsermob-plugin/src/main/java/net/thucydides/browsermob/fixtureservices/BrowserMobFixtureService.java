package net.thucydides.browsermob.fixtureservices;


import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.fixtureservices.FixtureService;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.util.EnvironmentVariables;
import org.apache.commons.lang3.StringUtils;
import org.browsermob.proxy.ProxyServer;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.List;

public class BrowserMobFixtureService implements FixtureService {

    public static final int DEFAULT_PORT = 5555;

    private static final int PORT_RANGE = 1000;
    private static final int MIN_AVAILABLE_PORT = 49152;
    private static final int MAX_AVAILABLE_PORT = MIN_AVAILABLE_PORT + PORT_RANGE;

    private final EnvironmentVariables environmentVariables;

    private int port = 0;

    private ThreadLocal<ProxyServer> threadLocalproxyServer = new  ThreadLocal<ProxyServer>();

    public BrowserMobFixtureService() {
        this(Injectors.getInjector().getInstance(EnvironmentVariables.class));
    }

    public BrowserMobFixtureService(EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @Override
    public void setup() throws Exception {
        if (useBrowserMobProxyManager()) {
            initializeProxy(getAvailablePort());
        }
    }

    protected ProxyServer getProxyServer() {
        return threadLocalproxyServer.get();
    }

    private void initializeProxy(int port) throws Exception {
        setPort(port);
        threadLocalproxyServer.set(new ProxyServer(port));
        threadLocalproxyServer.get().start();
    }

    @Override
    public void shutdown() throws Exception {
        if (threadLocalproxyServer.get() != null) {
            threadLocalproxyServer.get().stop();
            threadLocalproxyServer.remove();
        }
    }

    @Override
    public void addCapabilitiesTo(DesiredCapabilities capabilities) {
        try {
            capabilities.setCapability(CapabilityType.PROXY, threadLocalproxyServer.get().seleniumProxy());
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private boolean useBrowserMobProxyManager() {
        String browserMobFilter = environmentVariables.getProperty(BrowserMobSystemProperties.BROWSER_MOB_FILTER);
        return (StringUtils.isEmpty(browserMobFilter) || shouldActivateBrowserMobWithDriver(browserMobFilter, environmentVariables));
    }

    private boolean shouldActivateBrowserMobWithDriver(String filter, EnvironmentVariables environmentVariables) {
        String currentDriver = environmentVariables.getProperty(ThucydidesSystemProperty.DRIVER);
        List allowedBrowsers = Lists.newArrayList(Splitter.on(",").trimResults().split(filter.toLowerCase()));
        return StringUtils.isEmpty(currentDriver) || allowedBrowsers.contains(currentDriver.toLowerCase());
    }

    private void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    protected int getAvailablePort() {
        int defaultPort = environmentVariables.getPropertyAsInteger(BrowserMobSystemProperties.BROWSER_MOB_PROXY, DEFAULT_PORT);
        if (isAvailable(defaultPort)) {
            return defaultPort;
        } else {
            return nextAvailablePort(MIN_AVAILABLE_PORT);
        }
    }

    private int nextAvailablePort(int portNumber) {
        if (portNumber > MAX_AVAILABLE_PORT) {
            throw new IllegalStateException("No available ports found");
        }
        if (isAvailable(portNumber)) {
            return portNumber;
        } else {
            return nextAvailablePort(portNumber + 1);
        }
    }

    protected boolean isAvailable(int portNumber) {
        ServerSocket socket = null;
        boolean available = false;
        try {
            socket = new ServerSocket(portNumber);
            available = true;
        } catch (IOException e) {
            available = false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
        return available;
    }
}
