package com.lab.file.gateway.filter;

import com.lab.file.common.service.SlidingWindowRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    @Autowired
    private SlidingWindowRateLimiter rateLimiter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String path = request.getURI().getPath();

        // 登录注册接口放行（不限流）
        if (path.contains("/login") || path.contains("/register")) {
            return chain.filter(exchange);
        }

        // 获取限流key（IP + 路径）
        String key = getKey(request);

        // 执行限流（1分钟最多60次）
        boolean allowed = rateLimiter.isAllowed(key, 60, 60);

        if (!allowed) {
            // 返回429
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            String body = "{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}";
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }

        return chain.filter(exchange);
    }

    private String getKey(ServerHttpRequest request) {
        // 获取IP
        String ip = request.getHeaders().getFirst("X-Real-IP");
        if (ip == null) {
            InetSocketAddress address = request.getRemoteAddress();
            ip = address != null ? address.getAddress().getHostAddress() : "unknown";
        }

        // 获取用户ID（如果有）
        String userId = request.getHeaders().getFirst("X-User-Id");

        // 组合key
        if (userId != null) {
            return "user:" + userId;
        }
        return "ip:" + ip;
    }

    @Override
    public int getOrder() {
        return -100; // 优先执行
    }
}