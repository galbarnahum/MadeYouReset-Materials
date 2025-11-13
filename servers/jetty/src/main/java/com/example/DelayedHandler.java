package com.example;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DelayedHandler extends AbstractHandler {
    // Create a scheduler to delay the response.
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        // Mark this request as handled.
        baseRequest.setHandled(true);

        // Start asynchronous processing.
        final AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(1000); // Timeout after 1 second if something goes wrong.

        // Schedule sending the response after a 200ms delay.
        scheduler.schedule(() -> {
            try {
                HttpServletResponse resp = (HttpServletResponse) asyncContext.getResponse();
                resp.setContentType("text/plain;charset=utf-8");
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().print("hello jetty!");
		// Write "1" to the file /tmp/jetty.
                //Path filePath = Paths.get("/tmp/jetty");
                //Files.write(filePath, "1".getBytes(),  StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                asyncContext.complete();
            }
        }, 100, TimeUnit.MILLISECONDS);
    }
}
