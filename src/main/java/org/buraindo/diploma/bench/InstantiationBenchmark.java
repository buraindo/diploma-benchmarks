package org.buraindo.diploma.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class InstantiationBenchmark {

    private static final MethodHandles.Lookup publicLookup = MethodHandles.lookup();
    private static final String className = "java.lang.StringBuilder";

    public static void main(final String[] args) throws Exception {
        var opt = new OptionsBuilder()
                .include(InstantiationBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    @Fork(1_000)
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void reflection(final Blackhole blackhole) throws Throwable {
        final var clazz = Class.forName(className);
        blackhole.consume((StringBuilder) clazz.getDeclaredConstructor().newInstance());
    }

    @Fork(1_000)
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void methodHandle(final Blackhole blackhole) throws Throwable {
        final var clazz = Class.forName(className);
        final var constructorType = MethodType.methodType(void.class);
        final var constructor = publicLookup.findConstructor(clazz, constructorType);
        blackhole.consume((StringBuilder) constructor.invokeExact());
    }

    @Fork(1_000)
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @SuppressWarnings("unchecked")
    public void lambdaMetaFactory(final Blackhole blackhole) throws Throwable {
        final var clazz = Class.forName(className);
        final var constructorType = MethodType.methodType(void.class);
        final var constructor = publicLookup.findConstructor(clazz, constructorType);
        final var callSite = LambdaMetafactory.metafactory(
                // method handle lookup
                publicLookup,
                // name of the method defined in the target functional interface
                "get",
                // type to be implemented and captured objects
                // in this case the String instance to be trimmed is captured
                MethodType.methodType(Supplier.class),
                // type erasure, Supplier will return an Object
                constructor.type().generic(),
                // method handle to transform
                constructor,
                // Supplier method real signature (reified)
                // trim accepts no parameters and returns String
                constructor.type());
        final var supplier = (Supplier<StringBuilder>) callSite.getTarget().invoke();
        final var result = supplier.get();
        blackhole.consume(result);
    }

}
