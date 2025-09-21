package org.example.loadbalancers;

import org.example.model.BackendServer;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IPHashingLoadBalancerTest {

    private InetSocketAddress backend1;
    private InetSocketAddress backend2;

    @BeforeEach
    void setUp() {
        backend1 = new InetSocketAddress("127.0.0.1", 9001);
        backend2 = new InetSocketAddress("127.0.0.1", 9002);
    }

    @Test
    void testAddServerAndGetServers_usesMocks() throws NoSuchAlgorithmException {
        try (MockedConstruction<BackendServer> mocked = mockConstruction(BackendServer.class,
                (mock, context) -> {
                    when(mock.getAddress()).thenReturn((InetSocketAddress) context.arguments().get(0));
                })) {

            IPHashingLoadBalancer lb = new IPHashingLoadBalancer(List.of(backend1, backend2));

            List<BackendServer> servers = lb.getBackendServers();
            assertEquals(2, servers.size());

            // Verify that BackendServer was constructed twice
            assertEquals(2, mocked.constructed().size());

            // Verify methods on mocks behave
            assertEquals(backend1, mocked.constructed().get(0).getAddress());
            assertEquals(backend2, mocked.constructed().get(1).getAddress());
        }
    }

    @Test
    void testDeterministicClientMapping() throws NoSuchAlgorithmException {
        IPHashingLoadBalancer lb = new IPHashingLoadBalancer(List.of(backend1, backend2));
        SocketAddress client = new InetSocketAddress("192.168.1.50", 1234);
        BackendServer first = lb.getNextAvailableBackendServer(client);
        BackendServer second = lb.getNextAvailableBackendServer(client);
        assertSame(first, second, "Same client must always map to the same backend");
    }

    @Test
    void testRemoveServer() throws NoSuchAlgorithmException {
        IPHashingLoadBalancer lb = new IPHashingLoadBalancer(List.of(backend1, backend2));
        lb.removeServer(backend1);
        List<BackendServer> servers = lb.getBackendServers();
        assertEquals(1, servers.size());
        assertEquals(backend2, servers.get(0).getAddress());

    }

    @Test
    void testThrowsWhenNoServerAvailable() throws NoSuchAlgorithmException {
        try (MockedConstruction<BackendServer> ignored = mockConstruction(BackendServer.class)) {
            IPHashingLoadBalancer lb = new IPHashingLoadBalancer(List.of());

            SocketAddress client = new InetSocketAddress("10.0.0.1", 1111);
            assertThrows(RuntimeException.class,
                    () -> lb.getNextAvailableBackendServer(client));
        }
    }
}

