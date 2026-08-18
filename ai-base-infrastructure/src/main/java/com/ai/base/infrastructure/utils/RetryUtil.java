package com.ai.base.infrastructure.utils;

import java.util.concurrent.Callable;

public final class RetryUtil {
    private RetryUtil() {
    }

    public static <T> T retry(Callable<T> callable, int maxRetries) throws Exception {
        Exception lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return callable.call();
            } catch (Exception e) {
                lastException = e;
            }
        }
        throw lastException;
    }
}

