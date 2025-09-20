package org.example.model;

import java.net.InetSocketAddress;
import java.util.Objects;

public class BackendServer {

    private final InetSocketAddress address;
    private volatile boolean healthy = false;

    public BackendServer(InetSocketAddress address) {
        this.address = address;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BackendServer that = (BackendServer) o;
        return Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(address);
    }
}
