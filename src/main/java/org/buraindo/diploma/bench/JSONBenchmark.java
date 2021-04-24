package org.buraindo.diploma.bench;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsoniter.JsonIterator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class JSONBenchmark {

    @State(Scope.Benchmark)
    public static class JSONState {
        private final String jsonObject = "{ \"name\": \"Test\", \"age\": 21 }";
    }

    private static class Request {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }

    public static void main(final String[] args) throws Exception {
        var opt = new OptionsBuilder()
                .include(JSONBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    @Fork(1_000)
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void jackson(final Blackhole blackhole, final JSONState state) throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        var result = (Request) objectMapper.readerFor(Request.class).readValue(state.jsonObject);
        blackhole.consume(result);
    }

    @Fork(1_000)
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void jsonIter(final Blackhole blackhole, final JSONState state) {
        var result = JsonIterator.deserialize(state.jsonObject, Request.class);
        blackhole.consume(result);
    }

}
