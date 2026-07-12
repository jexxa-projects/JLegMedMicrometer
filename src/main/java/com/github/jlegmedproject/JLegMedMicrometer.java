package com.github.jlegmedproject;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.micrometer.MicrometerPlugin;
import io.jexxa.jlegmed.core.JLegMed;
import io.jexxa.jlegmed.core.flowgraph.FlowGraph;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public final class JLegMedMicrometer
{
    static PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    static void main()
    {
        Javalin app = Javalin
                .create( JLegMedMicrometer::initJavalin)
                .start(8080);


        var jLegMed = new JLegMed(JLegMedMicrometer.class);

        jLegMed.newFlowGraph("HelloWorld")
                .every(1, TimeUnit.SECONDS)

                .receive(String.class).from( () -> "Hello " )
                .and().processWith(data -> data + "World" )
                .and().consumeWith(data -> getLogger(JLegMedMicrometer.class).info(data));

        initMonitor(registry, jLegMed);

        jLegMed.run();

        app.stop();
    }

    private static void initMonitor(PrometheusMeterRegistry registry, JLegMed jLegMed) {
        var flowGraphs = jLegMed.getFlowGraphs();
        flowGraphs.forEach( flowGraphId -> initMonitor(registry, jLegMed.getFlowGraph(flowGraphId)));
    }

    private static void initMonitor(PrometheusMeterRegistry registry, FlowGraph flowGraph) {
        FunctionCounter.builder("pipe.messages.total", flowGraph, value -> value.processingStats().forwardedMessages().doubleValue())
                .description("Gesamtzahl verarbeiteter Nachrichten")
                .tag("graph", flowGraph.flowGraphID())
                .tag("status", "success")
                .register(registry);

    }


    static void initJavalin(JavalinConfig config)
    {
        // Plugins registrieren
        config.registerPlugin(new MicrometerPlugin(micrometerConfig -> micrometerConfig.registry = registry));
        config.routes.get("/metrics", ctx -> {
            String acceptHeader = ctx.header("Accept");
            if (acceptHeader != null && acceptHeader.contains("application/openmetrics-text")) {
                // OpenMetrics 1.0.0 Format ausgeben
                ctx.contentType("application/openmetrics-text; version=1.0.0; charset=utf-8");
                ctx.result(registry.scrape("application/openmetrics-text"));
            } else {
                // Traditional Prometheus Text-Format 0.0.4 (Fallback)
                ctx.contentType("text/plain; version=0.0.4; charset=utf-8");
                ctx.result(registry.scrape("text/plain"));
            }
        });


    }
}
