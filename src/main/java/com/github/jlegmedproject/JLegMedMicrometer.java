package com.github.jlegmedproject;

import com.sun.net.httpserver.HttpServer;
import io.jexxa.jlegmed.core.JLegMed;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public final class JLegMedMicrometer
{
    static PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    static void main() throws Exception
    {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/metrics", httpExchange -> {
            String acceptHeader = httpExchange.getRequestHeaders().getFirst("Accept");
            String contentType;
            String response;

            if (acceptHeader != null && acceptHeader.contains("application/openmetrics-text")) {
                contentType = "application/openmetrics-text; version=1.0.0; charset=utf-8";
                response = registry.scrape(contentType);
            } else {
                // Fallback auf das klassische Prometheus-Text-Format
                contentType = "text/plain; version=0.0.4; charset=utf-8";
                response = registry.scrape(contentType);
            }

            httpExchange.getResponseHeaders().set("Content-Type", contentType);

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            httpExchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = httpExchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.start();
        var jLegMed = new JLegMed(JLegMedMicrometer.class);

        jLegMed.newFlowGraph("HelloWorld")
                .every(1, TimeUnit.SECONDS)

                .receive(String.class).from( () -> "Hello " )
                .and().processWith(data -> data + "World" )
                .and().processWith(JLegMedMicrometer::incrementCounter)
                .and().consumeWith(data -> getLogger(JLegMedMicrometer.class).info(data));

        jLegMed.run();
    }

    public static <T> T incrementCounter(T value)
    {
        var counter = registry.counter("hello.world.total");
        counter.increment();
        getLogger(JLegMedMicrometer.class).info("Number of greetings {}", counter.count());
        return value;
    }
}
