package com.github.jlegmedproject;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.micrometer.MicrometerPlugin;
import io.jexxa.jlegmed.core.JLegMed;
import io.jexxa.jlegmed.core.filter.FilterContext;
import io.jexxa.jlegmed.core.flowgraph.FlowGraph;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
                .and().processWith(JLegMedMicrometer::failEverySecondCall1 ).withoutProperties()
                .and().processWith(data -> data).onError(data -> getLogger(JLegMedMicrometer.class).info("HANDLED ERROR {}", data.originalMessage()))
                .and().processWith(JLegMedMicrometer::failEverySecondCall2 ).withoutProperties()
                .and().consumeWith(data -> getLogger(JLegMedMicrometer.class).info(data));

        initMonitor(registry, jLegMed);

        jLegMed.run();

        app.stop();
    }

    static AtomicInteger FAILED_COUNTER_1 = new AtomicInteger(0);
    static AtomicInteger FAILED_COUNTER_2 = new AtomicInteger(0);

    static <T> T failEverySecondCall1(T attribute, FilterContext filterContext) {
        if ( FAILED_COUNTER_1.incrementAndGet() % 2 == 0) {
            throw new RuntimeException("Failed Iteration");
        }
        return attribute;
    }

    static <T> T failEverySecondCall2(T attribute, FilterContext filterContext) {
        if ( FAILED_COUNTER_2.incrementAndGet() % 4 == 0) {
            throw new RuntimeException("Failed Iteration");
        }
        return attribute;
    }

    private static void initMonitor(PrometheusMeterRegistry registry, JLegMed jLegMed) {
        var flowGraphs = jLegMed.getFlowGraphs();
        flowGraphs.forEach( flowGraphId -> initMonitor(registry, jLegMed.getFlowGraph(flowGraphId)));
    }

    private static void initMonitor(PrometheusMeterRegistry registry, FlowGraph flowGraph) {
        FunctionCounter.builder("flowgraph.messages.total", flowGraph, value -> value.processingStats().forwardedMessages().doubleValue())
                .description("Total processed messages")
                .tag("flow_graph", flowGraph.flowGraphID())
                .tag("result", "success")
                .tag("error_type", "none")
                .register(registry);

        FunctionCounter.builder("flowgraph.messages.total", flowGraph, value -> value.processingStats().handledProcessingErrors().doubleValue())
                .description("Total handled error messages")
                .tag("result", "error")
                .tag("error_type", "handled")
                .register(registry);

        FunctionCounter.builder("flowgraph.messages.total", flowGraph, value -> value.processingStats().unhandledProcessingErrors().doubleValue())
                .description("Total unhandled error messages")
                .tag("result", "error")
                .tag("error_type", "unhandled")
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
