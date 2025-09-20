package org.example.service;

import org.example.loadbalancers.LoadBalancer;
import org.example.model.BackendServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandlerService {

    private final int listenPort;
    private final long healthIntervalMs;
    private final LoadBalancer loadBalancer;

    public ClientHandlerService(int listenPort, long healthIntervalMs, LoadBalancer loadBalancer) {
        this.listenPort = listenPort;
        this.healthIntervalMs = healthIntervalMs;
        this.loadBalancer = loadBalancer;
    }

    public void start() {
        List<BackendServer> masterBackendServerList = new ArrayList<>(loadBalancer.getBackendServers());
        new Thread(new ServerHealthChecker(masterBackendServerList, loadBalancer, healthIntervalMs), "HealthChecker").start();

        try (ServerSocket serverSocket = new ServerSocket(listenPort)) {
            System.out.println("Load balancer listening on port " + listenPort);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // accept new client
                new Thread(() -> handleClient(clientSocket)).start(); // each client gets its own thread
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleClient(Socket clientSocket) {
        BackendServer backendServer = loadBalancer.getNextAvailableBackendServer(clientSocket.getRemoteSocketAddress());
        if (backendServer == null) {
            try {
                System.out.println("No healthy backend; closing client " + clientSocket.getRemoteSocketAddress());
                clientSocket.close();
            } catch (IOException ignored) {}
            return;
        }

        try {
            Socket backendSocket = new Socket();
            backendSocket.connect(backendServer.getAddress());

            System.out.println("Client " + clientSocket.getRemoteSocketAddress()
                    + " connected to Backend " + backendServer.getAddress());

            Thread clientToBackendThread = new Thread(new RequestForwarder(clientSocket, backendSocket), "ClientToBackend");
            Thread backendToClientThread = new Thread(new RequestForwarder(backendSocket, clientSocket), "BackendToClient");
            clientToBackendThread.start();
            backendToClientThread.start();

        } catch (IOException e) {
            System.out.println("Failed to connect to backend " + backendServer.getAddress() + ": " + e.getMessage());
            try { clientSocket.close(); } catch (IOException ignored) {}
        }
    }
}
