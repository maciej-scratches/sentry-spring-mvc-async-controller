package com.example.springasynccontroller;

import io.sentry.IHub;
import io.sentry.Sentry;
import org.springframework.core.task.TaskDecorator;

public class SentryTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        final IHub oldState = Sentry.getCurrentHub();
        final IHub newHub = Sentry.getCurrentHub().clone();
        return () -> {
            Sentry.setCurrentHub(newHub);
            try {
                runnable.run();
            } finally {
                Sentry.setCurrentHub(oldState);
            }
        };
    }
}
