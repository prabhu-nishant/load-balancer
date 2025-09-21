package org.example.loadbalancers;

import org.example.model.BackendServer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoundRobinLoadBalancerTest {

    @Test
    void testAddServerAndGetServers_withMocks() {
        InetSocketAddress backend1 = new InetSocketAddress("127.0.0.1", 9001);
        InetSocketAddress backend2 = new InetSocketAddress("127.0.0.1", 9002);

        try (MockedConstruction<BackendServer> mocked = mockConstruction(BackendServer.class,
                (mock, context) -> when(mock.getAddress()).thenReturn((InetSocketAddress) context.arguments().get(0)))) {

            RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer(List.of(backend1, backend2));

            List<BackendServer> servers = lb.getBackendServers();
            assertEquals(2, servers.size(), "Should contain 2 servers after construction");

            assertEquals(2, mocked.constructed().size(), "Two BackendServer instances should be constructed");
            assertEquals(backend1, mocked.constructed().get(0).getAddress());
            assertEquals(backend2, mocked.constructed().get(1).getAddress());
        }
    }

    @Test
    void testGetNextAvailableBackendServer_roundRobin() {
        InetSocketAddress backend1 = new InetSocketAddress("127.0.0.1", 9001);
        InetSocketAddress backend2 = new InetSocketAddress("127.0.0.1", 9002);

        try (MockedConstruction<BackendServer> mocked = mockConstruction(BackendServer.class,
                (mock, context) -> when(mock.getAddress()).thenReturn((InetSocketAddress) context.arguments().get(0)))) {

            RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer(List.of(backend1, backend2));

            SocketAddress client1 = new InetSocketAddress("10.0.0.1", 1000);
            SocketAddress client2 = new InetSocketAddress("10.0.0.2", 2000);
            SocketAddress client3 = new InetSocketAddress("10.0.0.3", 3000);

            BackendServer first = lb.getNextAvailableBackendServer(client1);
            BackendServer second = lb.getNextAvailableBackendServer(client2);
            BackendServer third = lb.getNextAvailableBackendServer(client3);

            // Round-robin sequence: 0 -> 1 -> 0
            assertSame(mocked.constructed().get(0), first);
            assertSame(mocked.constructed().get(1), second);
            assertSame(mocked.constructed().get(0), third);
        }
    }

    @Test
    void testRemoveServer() {
        InetSocketAddress backend1 = new InetSocketAddress("127.0.0.1", 9001);
        RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer(List.of(backend1));
        lb.removeServer(backend1);
        assertTrue(lb.getBackendServers().isEmpty(), "Backend list should be empty after removal");
    }

    @Test
    void testThrowsWhenNoServerAvailable() {
        RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer(List.of());

        SocketAddress client = new InetSocketAddress("10.0.0.1", 1000);

        assertThrows(RuntimeException.class,
                () -> lb.getNextAvailableBackendServer(client),
                "Should throw when no backend servers exist");
    }
}
