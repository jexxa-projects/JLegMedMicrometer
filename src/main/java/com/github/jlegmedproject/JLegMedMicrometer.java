package com.github.jlegmedproject;

import io.jexxa.jlegmed.core.JLegMed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public final class JLegMedMicrometer
{
    static void main()
    {
        var jLegMed = new JLegMed(JLegMedMicrometer.class);

        jLegMed.newFlowGraph("HelloWorld")
                .every(1, TimeUnit.SECONDS)

                .receive(String.class).from( () -> "Hello " )
                .and().processWith(data -> data + "World" )
                .and().processWith(JLegMedMicrometer::incrementCounter)
                .and().consumeWith(data -> getLogger(JLegMedMicrometer.class).info(data));

        jLegMed.run();
    }
    static MeterRegistry registry = new SimpleMeterRegistry();

    public static <T> T incrementCounter(T value)
    {
        var counter = registry.counter("hello.world.total");
        counter.increment();
        getLogger(JLegMedMicrometer.class).info("Number of greetings {}", counter.count());
        return value;
    }
}
