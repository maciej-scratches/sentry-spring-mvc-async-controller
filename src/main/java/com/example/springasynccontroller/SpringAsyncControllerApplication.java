package com.example.springasynccontroller;

import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

@SpringBootApplication
@EnableAsync
public class SpringAsyncControllerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAsyncControllerApplication.class, args);
    }

    /**
     * If there is a single bean of type {@link TaskDecorator} it will be picked by {@link TaskExecutionAutoConfiguration} and set on the executor running async MVC tasks.
     * If there are more beans of this type, a cystom {@link WebMvcConfigurer} must be defined (look below).
     */
    @Bean
    SentryTaskDecorator sentryTaskDecorator() {
        return new SentryTaskDecorator();
    }
}

/**
 * Alternatively, when more than single bean of type {@link TaskDecorator} is defined or if more fine grained configuration is needed for the MVC async task executor.
 */
//@Configuration
class AsyncConfiguration implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(asyncExecutor());
    }

    public AsyncTaskExecutor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(new SentryTaskDecorator());
        executor.initialize();
        return executor;
    }
}

@RestController
class HelloController {

    // invokes SentryTaskDecorator
    @GetMapping("/callable")
    Callable<String> callable() {
        return () -> {
            Sentry.captureMessage("from callable method");
            return "hello world";
        };
    }

    // invokes SentryTaskDecorator
    @GetMapping(path = "test.txt", produces = {MediaType.TEXT_PLAIN_VALUE})
    ResponseEntity<StreamingResponseBody> streaming() {
        return ResponseEntity.ok()
                .body(outputStream -> {
                    Sentry.captureMessage("from streaming response body");
                    outputStream.write("hello".getBytes(StandardCharsets.UTF_8));
                });
    }

    // does not invoke SentryTaskDecorator
    @GetMapping("/sync")
    String sync() {
        Sentry.captureMessage("from sync method");
        return "hello world";
    }
}
