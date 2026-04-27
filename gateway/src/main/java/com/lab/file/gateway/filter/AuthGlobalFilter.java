package com.lab.file.gateway.filter;

import com.lab.file.common.service.TokenRedisService;
import com.lab.file.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 全局JWT鉴权过滤器
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    private TokenRedisService tokenRedisService;  // 从 common 注入

    // 定义不需要鉴权的路径白名单
    private static final List<String> WHITE_LIST = Arrays.asList(
        "/api/user/login",
        "/api/user/register",
        "/api/file/chunk/chunk-test",
        "/actuator/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/webjars/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // 获取请求路径
        String path = request.getURI().getPath();
        System.out.println("请求路径: " + path);

        // 检查是否需要鉴权
        if (needAuth(path)) {
            System.out.println("路径需要鉴权: " + path);
            String token = getToken(request);
            if (token == null) {
                System.out.println("未提供Token，拒绝访问: " + path);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                String body = "{\"code\":401,\"message\":\"无权访问\"}";
                DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(buffer));
            }

            // 直接创建JwtUtil实例
            JwtUtil jwtUtil = new JwtUtil();
            Claims claims = jwtUtil.getClaimsFromToken(token);
            if (claims == null) {
                System.out.println("Token解析失败，拒绝访问: " + path);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                String body = "{\"code\":401,\"message\":\"无权访问\"}";
                DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(buffer));
            }

            // 2. 验证Redis中是否存在该Token（核心步骤）
            if (!tokenRedisService.validateToken(token)) {
                System.out.println("Token在Redis中不存在，可能已登出: " + token);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                String body = "{\"code\":401,\"message\":\"无权访问\"}";
                DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(buffer));
            }

            // 3. 刷新Redis中的Token过期时间（续期）
//            tokenRedisService.refreshTokenExpire(token);


            if (jwtUtil.isTokenExpired(token)) {
                System.out.println("Token已过期，拒绝访问: " + path);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                String body = "{\"code\":401,\"message\":\"无权访问\"}";
                DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(buffer));
            }

            // 将用户信息添加到请求头中
            Long userId = claims.get("userId", Long.class);
            String username = claims.getSubject();

            ServerHttpRequest mutableReq = request.mutate()
                    .header("userId", String.valueOf(userId))
                    .header("username", username)
                    .build();

            exchange = exchange.mutate().request(mutableReq).build();
        } else {
            System.out.println("路径在白名单中或非API路径，无需鉴权: " + path);
        }

        // 继续执行后续过滤器和路由
        System.out.println("继续路由: " + path);
        return chain.filter(exchange);
    }

    /**
     * 判断路径是否需要鉴权
     */
    private boolean needAuth(String path) {
        // 检查路径是否在白名单中
        for (String whitePattern : WHITE_LIST) {
            if (pathMatcher.match(whitePattern, path)) {
                return false; // 不需要鉴权
            }
        }

        // 只有以 /api/ 开头的路径才需要鉴权
        // 但要排除白名单中的特定路径
        return path.startsWith("/api/");
    }

    /**
     * 从请求中获取Token
     */
    private String getToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst("Authorization");
        System.out.println(bearerToken);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -98; // 设置较高的优先级
    }
}