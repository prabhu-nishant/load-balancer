package org.example.service;

import org.example.loadbalancers.LoadBalancer;
import org.example.model.BackendServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.verification.VerificationMode;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import static org.mockito.Mockito.*;

class ServerPoolManagerTest {

    private LoadBalancer loadBalancer;
    private BackendServer healthyBackend;
    private BackendServer unhealthyBackend;
    private List<BackendServer> masterList;


    @BeforeEach
    void setUp() {
        loadBalancer = mock(LoadBalancer.class);

        healthyBackend = mock(BackendServer.class);
        when(healthyBackend.getAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 9001));
        when(healthyBackend.isHealthy()).thenReturn(true);

        unhealthyBackend = mock(BackendServer.class);
        when(unhealthyBackend.getAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 9002));
        when(unhealthyBackend.isHealthy()).thenReturn(false);

        masterList = List.of(healthyBackend, unhealthyBackend);
    }

    @Test
    void testManageBackendServers_addsAndRemovesCorrectly() {
        // Backend list from load balancer contains only healthyBackend
        when(loadBalancer.getBackendServers()).thenReturn(List.of(healthyBackend, unhealthyBackend));
        doNothing().when(loadBalancer).removeServer(any(InetSocketAddress.class));
        doNothing().when(loadBalancer).addServer(any(InetSocketAddress.class));

        ServerPoolManager manager = new ServerPoolManager(masterList, loadBalancer, 1000);

        try {
            var method = ServerPoolManager.class.getDeclaredMethod("manageBackendServers");
            method.setAccessible(true);
            method.invoke(manager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // unhealthyBackend is unhealthy and present in LB → should be removed
        verify(loadBalancer).removeServer(unhealthyBackend.getAddress());

        // healthyBackend is healthy and present in masterList but already in LB → not added again
        verify(loadBalancer, never()).addServer(healthyBackend.getAddress());
    }

    @Test
    void testCheckAndUpdateServerHealth_marsUpAndDown() throws IOException {
        when(loadBalancer.getBackendServers()).thenReturn(List.of(healthyBackend, unhealthyBackend));
        doNothing().when(loadBalancer).removeServer(any(InetSocketAddress.class));
        doNothing().when(loadBalancer).addServer(any(InetSocketAddress.class));

        try (MockedConstruction<Socket> mockedSockets = mockConstruction(Socket.class,
                (mock, context) -> {
                    if (!context.arguments().isEmpty()) {
                        InetSocketAddress addr = (InetSocketAddress) context.arguments().get(0);
                        doNothing().when(mock).connect(any(InetSocketAddress.class), anyInt());
                    } else {
                        // No-arg constructor, just stub connect
                        doNothing().when(mock).connect(any(InetSocketAddress.class), anyInt());
                    }
                })) {
            ServerPoolManager manager = new ServerPoolManager(masterList, loadBalancer, 1000);
            // Call checkAndUpdateServerHealth via reflection
            try {
                var method = ServerPoolManager.class.getDeclaredMethod("checkAndUpdateServerHealth");
                method.setAccessible(true);
                method.invoke(manager);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            verify(healthyBackend, never()).setHealthy(anyBoolean());
            verify(unhealthyBackend).setHealthy(true);
        }
    }
}
