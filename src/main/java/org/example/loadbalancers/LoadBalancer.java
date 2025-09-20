package org.example.loadbalancers;

import org.example.model.BackendServer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

public interface LoadBalancer {

    public void addServer(InetSocketAddress serverAddress);

    public void removeServer(InetSocketAddress serverAddress);

    public BackendServer getNextAvailableBackendServer(SocketAddress clientSocketAddress);

    public List<BackendServer> getBackendServers();
}
