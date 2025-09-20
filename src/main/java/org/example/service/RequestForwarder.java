package org.example.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class RequestForwarder implements Runnable {
    private final Socket input;
    private final Socket output;

    public RequestForwarder(Socket input, Socket output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public void run() {
        try (InputStream is = input.getInputStream();
             OutputStream os = output.getOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                os.write(buf, 0, n);
                os.flush();
            }
        } catch (IOException ignored) {
        } finally {
            try { input.close(); } catch (IOException ignored) {}
            try { output.close(); } catch (IOException ignored) {}
        }
    }
}
