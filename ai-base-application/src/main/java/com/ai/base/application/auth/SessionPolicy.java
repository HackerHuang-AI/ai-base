package com.ai.base.application.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;

@Getter
@AllArgsConstructor
public class SessionPolicy {
    private final Duration ttl;
    private final Duration renewWindow;
}

