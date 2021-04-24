package org.buraindo.diploma.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class CollectionsBenchmark {

    @State(Scope.Benchmark)
    public static class HandlerState {
        private final Class<?> clazz = Handler.class;
        private final Class<?> targetClass = Function.class;
    }


    private static class Handler implements Function<String, String> {
        @Override
        public String apply(final String s) {
            return s;
        }
    }

    public static void main(final String[] args) throws Exception {
        var opt = new OptionsBuilder()
                .include(CollectionsBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    @Fork(100)
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void forLoop(final Blackhole blackhole, final HandlerState state) {
        final var genericInterfaces = state.clazz.getGenericInterfaces();
        for (final var i : genericInterfaces) {
            if (!(i instanceof ParameterizedType)) {
                continue;
            }
            final var parameterizedType = (ParameterizedType) i;
            if (parameterizedType.getRawType() == state.targetClass) {
                blackhole.consume(i);
                return;
            }
        }
    }

    @Fork(100)
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void streamAPI(final Blackhole blackhole, final HandlerState state) {
        final var genericInterfaces = state.clazz.getGenericInterfaces();
        final var functionInterface = Arrays.stream(genericInterfaces)
                .filter(i -> i instanceof ParameterizedType && ((ParameterizedType) i)
                        .getRawType()
                        .getTypeName()
                        .equals(state.targetClass.getTypeName()))
                .findFirst();
        blackhole.consume(functionInterface);
    }

}
