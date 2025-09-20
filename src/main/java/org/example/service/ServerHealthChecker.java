package org.example.service;

import org.example.loadbalancers.LoadBalancer;
import org.example.model.BackendServer;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ServerHealthChecker implements Runnable{

    private final List<BackendServer> masterBackendServerList;
    private final LoadBalancer loadBalancer;
    private final long healthIntervalMs;

    public ServerHealthChecker(List<BackendServer> masterBackendServerList, LoadBalancer loadBalancer,
                               long healthIntervalMs) {
        this.masterBackendServerList = masterBackendServerList;
        this.loadBalancer = loadBalancer;
        this.healthIntervalMs = healthIntervalMs;
    }

    @Override
    public void run() {
        while (true) {
            for (BackendServer backendServer : masterBackendServerList) {
                boolean ok = false;
                try (Socket s = new Socket()) {
                    s.connect(backendServer.getAddress(), 1000);
                    ok = true;
                } catch (IOException exception) {

                }
                if (ok && !backendServer.isHealthy()) {
                    backendServer.setHealthy(true);
                    System.out.println("Backend " + backendServer.getAddress() + " UP");
                } else if (!ok && backendServer.isHealthy()) {
                    backendServer.setHealthy(false);
                    System.out.println("Backend " + backendServer.getAddress() + " DOWN");
                }
            }
            manageBackendServers();
            try {
                Thread.sleep(healthIntervalMs);
            } catch (InterruptedException ignored) {}
        }
    }

    private void manageBackendServers() {
        List<BackendServer> backendServerList = loadBalancer.getBackendServers();
        masterBackendServerList.stream()
                .filter(server -> backendServerList.contains(server) && !server.isHealthy())
                .map(BackendServer::getAddress)
                .forEach(loadBalancer::removeServer);

        masterBackendServerList.stream()
                .filter(server -> !backendServerList.contains(server) && server.isHealthy())
                .map(BackendServer::getAddress)
                .forEach(loadBalancer::addServer);
    }
}
