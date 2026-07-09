package com.github.jlegmedproject;

import com.sun.net.httpserver.HttpServer;
import io.jexxa.jlegmed.core.JLegMed;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public final class JLegMedMicrometer
{
    static PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    static void main() throws Exception
    {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/metrics", httpExchange -> {
            // Holt die aktuellen Metriken im Prometheus-Textformat ab
            String response = registry.scrape();

            byte[] bytes = response.getBytes();
            httpExchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = httpExchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        // Server im Hintergrund starten
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
