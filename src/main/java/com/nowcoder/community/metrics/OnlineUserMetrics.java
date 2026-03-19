package com.nowcoder.community.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class OnlineUserMetrics {



    // 存储在线用户的唯一标识（如用户名、sessionId），线程安全
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    // Prometheus 指标注册器
    private final MeterRegistry meterRegistry;

    // 构造注入 MeterRegistry
    public OnlineUserMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 初始化 Prometheus Gauge 指标，实时监控在线用户数
     */
    @PostConstruct
    public void initOnlineUserGauge() {
        // 定义指标：名称（online_user_count）、描述、标签，值为 onlineUsers 的大小
        Gauge.builder("online_user_count", onlineUsers::size)
                .description("当前系统在线用户数量")
                .tag("module", "security") // 自定义标签，便于分类查询
                .register(meterRegistry);
    }

    /**
     * 监听用户登录成功事件，添加用户到在线列表
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        // 获取用户名（也可以用 sessionId 作为唯一标识）
        String username = auth.getName();
        onlineUsers.add(username);
        System.out.println("用户[" + username + "]登录，当前在线数：" + onlineUsers.size());
    }

    /**
     * 监听 Session 销毁事件（用户登出、Session 超时），移除用户
     */
    @EventListener
    public void onSessionDestroyed(SessionDestroyedEvent event) {
        // 获取销毁的 Session 中的认证信息
        event.getSecurityContexts().forEach(context -> {
            if (context.getAuthentication() != null) {
                String username = context.getAuthentication().getName();
                onlineUsers.remove(username);
                System.out.println("用户[" + username + "]登出，当前在线数：" + onlineUsers.size());
            }
        });
    }
}
