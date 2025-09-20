package org.example.loadbalancers;

import org.example.model.BackendServer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements LoadBalancer {

    private final List<BackendServer> backendServerList = new ArrayList<>();
    private final AtomicInteger counter = new AtomicInteger(0);

    public RoundRobinLoadBalancer(List<InetSocketAddress> serverAddressList) {
        serverAddressList.forEach(address -> backendServerList.add(new BackendServer(address)));
    }

    @Override
    public void addServer(InetSocketAddress serverAddress) {
        backendServerList.add(new BackendServer(serverAddress));
    }

    @Override
    public void removeServer(InetSocketAddress serverAddress) {
        backendServerList.remove(new BackendServer(serverAddress));
    }

    @Override
    public synchronized BackendServer getNextAvailableBackendServer(SocketAddress clientSocketAddress) {
        int index = counter.getAndIncrement() % backendServerList.size();
        return backendServerList.get(index);
    }

    @Override
    public List<BackendServer> getBackendServers() {
        return backendServerList;
    }
}
