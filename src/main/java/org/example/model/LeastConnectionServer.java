package org.example.model;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class LeastConnectionServer extends BackendServer{

    private final AtomicInteger load;

    public LeastConnectionServer(InetSocketAddress serverAddress, AtomicInteger load) {
        super(serverAddress);
        this.load = load;
    }

    public void incrementLoad(){
        this.load.incrementAndGet();
    }

    public void decrementLoad(){
        this.load.decrementAndGet();
    }

    public AtomicInteger getLoad() {
        return load;
    }
}
