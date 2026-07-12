package com.github.jlegmedproject;

import io.jexxa.jlegmed.core.JLegMed;
import io.jexxa.jlegmed.core.filter.FilterContext;
import io.jexxa.jlegmed.micrometer.MetricsCollector;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

public final class MyJLegMedMicrometer
{
    static void main()
    {

        var jLegMed = new JLegMed(MyJLegMedMicrometer.class)
                .registerService(MetricsCollector::micrometerCollector);


        jLegMed.newFlowGraph("HelloWorld")
                .every(1, TimeUnit.SECONDS)

                .receive(String.class).from( () -> "Hello " )
                .and().processWith(data -> data + "World" )
                .and().processWith(MyJLegMedMicrometer::failEverySecondCall1 ).withoutProperties()
                .and().processWith(data -> data).onError(data -> getLogger(MyJLegMedMicrometer.class).info("HANDLED ERROR {}", data.originalMessage()))
                .and().processWith(MyJLegMedMicrometer::failEverySecondCall2 ).withoutProperties()
                .and().consumeWith(data -> getLogger(MyJLegMedMicrometer.class).info(data));

        jLegMed.run();
    }

    static AtomicInteger FAILED_COUNTER_1 = new AtomicInteger(0);
    static AtomicInteger FAILED_COUNTER_2 = new AtomicInteger(0);

    static <T> T failEverySecondCall1(T attribute, FilterContext filterContext) {
        if ( FAILED_COUNTER_1.incrementAndGet() % 2 == 0) {
            throw new IllegalArgumentException("Failed Iteration");
        }
        return attribute;
    }

    static <T> T failEverySecondCall2(T attribute, FilterContext filterContext) {
        if ( FAILED_COUNTER_2.incrementAndGet() % 4 == 0) {
            throw new IllegalArgumentException("Failed Iteration");
        }
        return attribute;
    }
}
