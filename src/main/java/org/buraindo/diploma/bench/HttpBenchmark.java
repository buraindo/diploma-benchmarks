package org.buraindo.diploma.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class HttpBenchmark {

    public static void main(final String[] args) throws Exception {
        var opt = new OptionsBuilder()
                .include(HttpBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    @Fork(1_000)
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void httpClient() throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest
                .newBuilder(URI.create("https://one.one.one.one"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(request, HttpResponse.BodyHandlers.discarding());
    }

    @Fork(1_000)
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void urlConnection() throws IOException {
        final var url = URI.create("https://one.one.one.one").toURL();
        final var connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("OPTIONS");
        connection.connect();
        connection.disconnect();
    }

}
