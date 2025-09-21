package org.example;

import org.example.loadbalancers.LoadBalancer;
import org.example.loadbalancers.RoundRobinLoadBalancer;
import org.example.service.ClientHandlerService;
import org.example.service.ThreadPoolService;

import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class LoadBalancerApplication {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        if (args.length < 3) {
            System.err.println("Usage: java LoadBalancerApplication <listenPort> <healthCheckMillis> <backendHost1:port> [backendHost2:port ...]");
            System.exit(1);
        }

        int listenPort = Integer.parseInt(args[0]);
        long healthCheckMillis = Long.parseLong(args[1]);
        List<InetSocketAddress> backendServersAddressList = new ArrayList<>();

        for (int i = 2; i < args.length; i++) {
            String[] parts = args[i].split(":");
            if (parts.length != 2) {
                System.err.println("Invalid backend: " + args[i]);
                System.exit(2);
            }
            backendServersAddressList.add(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
        }
        LoadBalancer loadBalancer = new RoundRobinLoadBalancer(backendServersAddressList);
        ThreadPoolService threadPoolService = new ThreadPoolService(3);
        new ClientHandlerService(listenPort, healthCheckMillis, loadBalancer, threadPoolService).start();
    }
}