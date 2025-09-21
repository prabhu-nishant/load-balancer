package org.example.service;

import org.example.loadbalancers.LoadBalancer;
import org.example.model.BackendServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;

import static org.mockito.Mockito.*;

class ClientHandlerServiceTest {

    private LoadBalancer loadBalancer;
    private ThreadPoolService threadPoolService;
    private ClientHandlerService clientHandlerService;
    private BackendServer backendServer;

    @BeforeEach
    void setUp() {
        loadBalancer = mock(LoadBalancer.class);
        threadPoolService = mock(ThreadPoolService.class);

        backendServer = mock(BackendServer.class);
        when(backendServer.getAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 9001));

        when(loadBalancer.getBackendServers()).thenReturn(List.of(backendServer));

        clientHandlerService = new ClientHandlerService(8080, 1000, loadBalancer, threadPoolService);
    }

    @Test
    void testHandleClient_withHealthyBackend() throws IOException {
        Socket clientSocket = mock(Socket.class);
        SocketAddress clientAddress = new InetSocketAddress("127.0.0.1", 50000);
        when(clientSocket.getRemoteSocketAddress()).thenReturn(clientAddress);

        when(loadBalancer.getNextAvailableBackendServer(clientAddress)).thenReturn(backendServer);

        // We invoke handleClient directly (it's private, so we can make it package-private or use reflection)
        clientHandlerService.getClass()
                .getDeclaredMethods()[0] // handleClient
                .setAccessible(true);

        try {
            clientHandlerService.getClass()
                    .getDeclaredMethod("handleClient", Socket.class)
                    .invoke(clientHandlerService, clientSocket);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Verify load balancer was called
        verify(loadBalancer).getNextAvailableBackendServer(clientAddress);

        // Verify threadPoolService submitted tasks
        verify(threadPoolService, times(2)).submit(any(Runnable.class));

        // Verify clientSocket.connect not called (we mock backendSocket internally in RequestForwarder)
    }

    @Test
    void testHandleClient_withNoHealthyBackend() throws IOException {
        Socket clientSocket = mock(Socket.class);
        SocketAddress clientAddress = new InetSocketAddress("127.0.0.1", 50001);
        when(clientSocket.getRemoteSocketAddress()).thenReturn(clientAddress);

        when(loadBalancer.getNextAvailableBackendServer(clientAddress)).thenReturn(null);

        try {
            clientHandlerService.getClass()
                    .getDeclaredMethod("handleClient", Socket.class)
                    .setAccessible(true);
            clientHandlerService.getClass()
                    .getDeclaredMethod("handleClient", Socket.class)
                    .invoke(clientHandlerService, clientSocket);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Verify clientSocket.close was called
        verify(clientSocket).close();

        // Verify no tasks submitted to thread pool
        verify(threadPoolService, never()).submit(any(Runnable.class));
    }
}
