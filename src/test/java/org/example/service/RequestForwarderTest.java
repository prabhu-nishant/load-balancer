package org.example.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static org.mockito.Mockito.*;

class RequestForwarderTest {

    @Test
    void testRun_forwardsData() throws IOException {
        // Mock input/output streams
        InputStream mockInput = mock(InputStream.class);
        OutputStream mockOutput = mock(OutputStream.class);

        // Mock sockets
        Socket inputSocket = mock(Socket.class);
        Socket outputSocket = mock(Socket.class);

        when(inputSocket.getInputStream()).thenReturn(mockInput);
        when(outputSocket.getOutputStream()).thenReturn(mockOutput);

        // Simulate some data
        when(mockInput.read(any(byte[].class)))
                .thenAnswer(invocation -> {
                    byte[] buf = invocation.getArgument(0);
                    buf[0] = 42;
                    return 1; // read 1 byte
                })
                .thenReturn(-1); // end of stream

        RequestForwarder forwarder = new RequestForwarder(inputSocket, outputSocket);
        forwarder.run();

        // Verify data was written and flushed
        verify(mockOutput).write(argThat(bytes -> bytes[0] == 42), eq(0), eq(1));
        verify(mockOutput).flush();

        // Verify sockets were closed
        verify(inputSocket).close();
        verify(outputSocket).close();
    }

    @Test
    void testRun_handlesIOException() throws IOException {
        Socket inputSocket = mock(Socket.class);
        Socket outputSocket = mock(Socket.class);

        when(inputSocket.getInputStream()).thenThrow(new IOException("Test exception"));

        RequestForwarder forwarder = new RequestForwarder(inputSocket, outputSocket);
        forwarder.run();

        // Verify sockets were still closed despite exception
        verify(inputSocket).close();
        verify(outputSocket).close();
    }
}
