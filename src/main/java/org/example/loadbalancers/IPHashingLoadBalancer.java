package org.example.loadbalancers;

import org.example.model.BackendServer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class IPHashingLoadBalancer implements LoadBalancer {

    private final TreeMap<Long, BackendServer> ring = new TreeMap<>();
    private final MessageDigest md = MessageDigest.getInstance("MD5");

    public IPHashingLoadBalancer(List<InetSocketAddress> serverAddressList) throws NoSuchAlgorithmException {
        serverAddressList.forEach(this::addServer);
    }

    @Override
    public void addServer(InetSocketAddress serverAddress) {
        long hash = generateHash(serverAddress.getAddress().toString() + serverAddress.getPort());
        ring.put(hash, new BackendServer(serverAddress));
    }

    @Override
    public void removeServer(InetSocketAddress serverAddress) {
        long hash = generateHash(serverAddress.getAddress().toString() + serverAddress.getPort());
        ring.remove(hash);
    }

    @Override
    public synchronized BackendServer getNextAvailableBackendServer(SocketAddress clientSocketAddress) {
        if (ring.isEmpty()) {
            throw new RuntimeException("No backend server available as this time");
        }
        long hash = generateHash(clientSocketAddress.toString());
        if (!ring.containsKey(hash)) {
            SortedMap<Long, BackendServer> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        return ring.get(hash);
    }

    @Override
    public List<BackendServer> getBackendServers() {
        return ring.values().stream().toList();
    }

    private long generateHash(String key) {
        md.reset();
        md.update(key.getBytes());
        byte[] digest = md.digest();
        long hash = ((long) (digest[3] & 0xFF) << 24) |
                ((long) (digest[2] & 0xFF) << 16) |
                ((long) (digest[1] & 0xFF) << 8) |
                ((long) (digest[0] & 0xFF));
        return hash;
    }
}
