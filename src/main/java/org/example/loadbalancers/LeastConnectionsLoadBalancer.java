package org.example.loadbalancers;

import org.example.model.BackendServer;
import org.example.model.LeastConnectionServer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LeastConnectionsLoadBalancer implements LoadBalancer {

    private final PriorityQueue<LeastConnectionServer> leastConnectionServerPriorityQueue = new PriorityQueue<>(
            Comparator.comparingInt(leastConnectionServer -> leastConnectionServer.getLoad().get()));
    private final Map<InetSocketAddress, BackendServer> leastConnectionServerMap = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public LeastConnectionsLoadBalancer(List<InetSocketAddress> serverAddressList) {
        serverAddressList.forEach(this::addServer);
    }

    @Override
    public void addServer(InetSocketAddress serverAddress) {
        lock.writeLock().lock();
        try {
            var server = new LeastConnectionServer(serverAddress, new AtomicInteger(0));
            leastConnectionServerPriorityQueue.add(server);
            leastConnectionServerMap.put(serverAddress, server);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeServer(InetSocketAddress serverAddress) {
        lock.writeLock().lock();
        try {
            var server = leastConnectionServerMap.get(serverAddress);
            if(server != null){
                leastConnectionServerPriorityQueue.remove(server);
                leastConnectionServerMap.remove(serverAddress);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public synchronized BackendServer getNextAvailableBackendServer(SocketAddress clientSocketAddress) {
        lock.writeLock().lock();
        if(leastConnectionServerPriorityQueue.isEmpty()){
            return null;
        }
        var server = leastConnectionServerPriorityQueue.poll();
        server.incrementLoad();
        leastConnectionServerPriorityQueue.add(server);
        lock.writeLock().unlock();
        return server;
    }

    @Override
    public List<BackendServer> getBackendServers() {
        return leastConnectionServerMap.values().stream().toList();
    }
}
