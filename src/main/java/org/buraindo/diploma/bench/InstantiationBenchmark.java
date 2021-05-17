package org.buraindo.diploma.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import yandex.cloud.exception.UnsupportedHandlerException;
import yandex.cloud.exception.YcClassNotFoundException;
import yandex.cloud.handler.Handler;
import yandex.cloud.handler.function.JdkFunctionHandler;
import yandex.cloud.handler.yc.YcFunctionHandler;
import yandex.cloud.sdk.functions.YcFunction;

import java.lang.annotation.Annotation;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class InstantiationBenchmark {

    private static final String ENTRY_POINT = "ru.serverless.complex.Handler";

    private static final String SERVLET_CLASS_NAME = "javax.servlet.http.HttpServlet";
    private static final String SPRING_BOOT_APPLICATION_CLASS_NAME = "org.springframework.boot.autoconfigure.SpringBootApplication";

    private static final String SERVLET_HANDLER_CLASS_NAME = "yandex.cloud.handler.proxy.servlet.HttpServletHandler";
    private static final String SPRING_BOOT_HANDLER_CLASS_NAME = "yandex.cloud.handler.proxy.spring.boot.SpringBootHandler";

    public static void main(final String[] args) throws Exception {
        var opt = new OptionsBuilder()
                .include(InstantiationBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    @Fork(1000)
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void reflection(final Blackhole blackhole) throws Throwable {
        try {
            // Class.forName is used mainly for GraalVM, since it intercepts
            // such calls to detect reflection API usage and inlines real classes
            // provided they exist in classpath
            final var clazz = Class.forName(ENTRY_POINT);
            if (Function.class.isAssignableFrom(clazz)) {
                blackhole.consume(new JdkFunctionHandler(clazz));
                return;
            }
            if (YcFunction.class.isAssignableFrom(clazz)) {
                blackhole.consume(new YcFunctionHandler(clazz));
                return;
            }
            try {
                final var servletClass = Class.forName(SERVLET_CLASS_NAME);
                if (servletClass.isAssignableFrom(clazz)) {
                    final var httpServletHandlerClass = Class.forName(SERVLET_HANDLER_CLASS_NAME);
                    blackhole.consume(httpServletHandlerClass.getDeclaredConstructor(Class.class).newInstance(clazz));
                    return;
                }
            } catch (final ClassNotFoundException unused) {
                // it isn't a servlet function, because otherwise an exception would have been thrown earlier
                // do nothing
            }
            try {
                final var annotation = Class.forName(SPRING_BOOT_APPLICATION_CLASS_NAME);
                final var springBootApplicationAnnotation = (Class<? extends Annotation>) annotation;
                if (clazz.getAnnotation(springBootApplicationAnnotation) != null) {
                    final var springBootHandlerClass = Class.forName(SPRING_BOOT_HANDLER_CLASS_NAME);
                    blackhole.consume(springBootHandlerClass.getDeclaredConstructor(Class.class).newInstance(clazz));
                    return;
                }
            } catch (final ClassNotFoundException unused) {
                // it isn't a spring boot function, because otherwise an exception would have been thrown earlier
                // do nothing
            }
            throw new UnsupportedHandlerException();
        } catch (final ClassNotFoundException e) {
            throw new YcClassNotFoundException(e.getLocalizedMessage());
        }
    }

    @Fork(1000)
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void methodHandle(final Blackhole blackhole) throws Throwable {
        try {
            // Class.forName is used mainly for GraalVM, since it intercepts
            // such calls to detect reflection API usage and inlines real classes
            // provided they exist in classpath
            final var clazz = Class.forName(ENTRY_POINT);
            if (Function.class.isAssignableFrom(clazz)) {
                blackhole.consume(new JdkFunctionHandler(clazz));
                return;
            }
            if (YcFunction.class.isAssignableFrom(clazz)) {
                blackhole.consume(new YcFunctionHandler(clazz));
                return;
            }
            final var publicLookup = MethodHandles.publicLookup();
            try {
                final var servletClass = Class.forName(SERVLET_CLASS_NAME);
                if (servletClass.isAssignableFrom(clazz)) {
                    final var httpServletHandlerClass = Class.forName(SERVLET_HANDLER_CLASS_NAME);
                    final var constructorType = MethodType.methodType(void.class, Class.class);
                    final var constructor = publicLookup.findConstructor(httpServletHandlerClass, constructorType);
                    blackhole.consume((Handler) constructor.invokeExact(clazz));
                    return;
                }
            } catch (final ClassNotFoundException unused) {
                // it isn't a servlet function, because otherwise an exception would have been thrown earlier
                // do nothing
            }
            try {
                final var annotation = Class.forName(SPRING_BOOT_APPLICATION_CLASS_NAME);
                final var springBootApplicationAnnotation = (Class<? extends Annotation>) annotation;
                if (clazz.getAnnotation(springBootApplicationAnnotation) != null) {
                    final var springBootHandlerClass = Class.forName(SPRING_BOOT_HANDLER_CLASS_NAME);
                    final var constructorType = MethodType.methodType(void.class, Class.class);
                    final var constructor = publicLookup.findConstructor(springBootHandlerClass, constructorType);
                    blackhole.consume((Handler) constructor.invokeExact(clazz));
                    return;
                }
            } catch (final ClassNotFoundException unused) {
                // it isn't a spring boot function, because otherwise an exception would have been thrown earlier
                // do nothing
            }
            throw new UnsupportedHandlerException();
        } catch (final ClassNotFoundException e) {
            throw new YcClassNotFoundException(e.getLocalizedMessage());
        }
    }

    @Fork(1000)
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void lambdaMetaFactory(final Blackhole blackhole) throws Throwable {
        final var clazz = Class.forName(ENTRY_POINT);
        final var constructorType = MethodType.methodType(void.class);
        final var lookup = MethodHandles.lookup();
        final var constructor = lookup.findConstructor(clazz, constructorType);
        final var callSite = LambdaMetafactory.metafactory(
                // method handle lookup
                lookup,
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
        try {
            // Class.forName is used mainly for GraalVM, since it intercepts
            // such calls to detect reflection API usage and inlines real classes
            // provided they exist in classpath
            if (Function.class.isAssignableFrom(clazz)) {
                blackhole.consume(new JdkFunctionHandler(clazz));
                return;
            }
            if (YcFunction.class.isAssignableFrom(clazz)) {
                blackhole.consume(new YcFunctionHandler(clazz));
                return;
            }
            try {
                final var servletClass = Class.forName(SERVLET_CLASS_NAME);
                if (servletClass.isAssignableFrom(clazz)) {
                    final var supplier = (Supplier) callSite.getTarget().invoke(clazz);
                    final var result = supplier.get();
                    blackhole.consume(result);
                    return;
                }
            } catch (final ClassNotFoundException unused) {
                // it isn't a servlet function, because otherwise an exception would have been thrown earlier
                // do nothing
            }
            try {
                final var annotation = Class.forName(SPRING_BOOT_APPLICATION_CLASS_NAME);
                final var springBootApplicationAnnotation = (Class<? extends Annotation>) annotation;
                if (clazz.getAnnotation(springBootApplicationAnnotation) != null) {
                    final var supplier = (Supplier) callSite.getTarget().invoke(clazz);
                    final var result = supplier.get();
                    blackhole.consume(result);
                    return;
                }
            } catch (final ClassNotFoundException unused) {
                // it isn't a spring boot function, because otherwise an exception would have been thrown earlier
                // do nothing
            }
            throw new UnsupportedHandlerException();
        } catch (final ClassNotFoundException e) {
            throw new YcClassNotFoundException(e.getLocalizedMessage());
        }
    }

}
