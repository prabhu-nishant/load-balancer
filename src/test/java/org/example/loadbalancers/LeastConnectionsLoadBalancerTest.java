package org.example.loadbalancers;

import org.example.model.BackendServer;
import org.example.model.LeastConnectionServer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeastConnectionsLoadBalancerTest {

    @Test
    void testAddServerAndGetServers_withMocks() {
        InetSocketAddress backend1 = new InetSocketAddress("127.0.0.1", 9001);
        InetSocketAddress backend2 = new InetSocketAddress("127.0.0.1", 9002);
        LeastConnectionsLoadBalancer lb = new LeastConnectionsLoadBalancer(List.of(backend1, backend2));
        List<BackendServer> servers = lb.getBackendServers();
        assertEquals(2, servers.size(), "Should contain 2 servers after setup");
        assertEquals(backend1, lb.getBackendServers().get(0).getAddress());
        assertEquals(backend2, lb.getBackendServers().get(1).getAddress());
    }

    @Test
    void testGetNextAvailableBackendServer_pollsLowestLoad() {
        InetSocketAddress backend = new InetSocketAddress("127.0.0.1", 9001);

        try (MockedConstruction<LeastConnectionServer> mocked = mockConstruction(LeastConnectionServer.class,
                (mock, context) -> {
                    when(mock.getAddress()).thenReturn((InetSocketAddress) context.arguments().get(0));
                    AtomicInteger load = new AtomicInteger(0);
                    when(mock.getLoad()).thenReturn(load);
                    doAnswer(invocation -> { load.incrementAndGet(); return null; })
                            .when(mock).incrementLoad();
                })) {

            LeastConnectionsLoadBalancer lb = new LeastConnectionsLoadBalancer(List.of(backend));

            SocketAddress client = new InetSocketAddress("10.0.0.1", 1234);
            BackendServer server = lb.getNextAvailableBackendServer(client);

            assertNotNull(server);
            verify(mocked.constructed().get(0), times(1)).incrementLoad();
        }
    }

    @Test
    void testRemoveServer_removesFromList() {
        InetSocketAddress backend = new InetSocketAddress("127.0.0.1", 9001);

        try (MockedConstruction<LeastConnectionServer> mocked = mockConstruction(LeastConnectionServer.class,
                (mock, context) -> when(mock.getAddress()).thenReturn((InetSocketAddress) context.arguments().get(0)))) {

            LeastConnectionsLoadBalancer lb = new LeastConnectionsLoadBalancer(List.of(backend));
            lb.removeServer(backend);

            List<BackendServer> servers = lb.getBackendServers();
            assertTrue(servers.isEmpty(), "After removal, backend list should be empty");
        }
    }

    @Test
    void testThrowsWhenNoServerAvailable() {
        LeastConnectionsLoadBalancer lb = new LeastConnectionsLoadBalancer(List.of());
        SocketAddress client = new InetSocketAddress("10.0.0.1", 1234);

        assertThrows(RuntimeException.class,
                () -> lb.getNextAvailableBackendServer(client));
    }
}
