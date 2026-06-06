package com.florestal.gateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor MVC que registra método, path, status e latência de cada requisição.
 * Registrado no WebMvcConfig.
 */
@Slf4j
@Component
public class LoggingFilter implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "startTime";

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        log.info("[GATEWAY] {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex
    ) {
        Long start = (Long) request.getAttribute(START_TIME_ATTR);
        long latency = start != null ? System.currentTimeMillis() - start : -1;
        log.info("[GATEWAY] {} {} → {} | {}ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                latency);
    }
}
