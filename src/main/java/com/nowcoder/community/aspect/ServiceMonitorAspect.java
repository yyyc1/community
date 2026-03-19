package com.nowcoder.community.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class ServiceMonitorAspect {

    @Autowired
    private MeterRegistry meterRegistry;

    // 1. 定义切点：拦截所有Service类的所有方法（根据你的实际包名调整）
    @Pointcut("execution(* com.nowcoder.community.service.*Service.*(..))")
    public void serviceTimePointcut() {}

    // 2. 环绕通知：仅统计耗时，不修改原有逻辑
    @Around("serviceTimePointcut()")
    public Object monitorServiceTime(ProceedingJoinPoint joinPoint) throws Throwable {
        // 2.1 获取方法元数据（和原有计数指标的标签保持一致）
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // module：UserService → user，LikeService → like（和原有逻辑一致）
        String module = method.getDeclaringClass().getSimpleName().replace("Service", "").toLowerCase();
        // operate：方法名，如register、like、findLetters（和原有逻辑一致）
        String operate = method.getName();
        // result：默认success，抛异常则为fail（和你手动计数的result逻辑对齐）
        String result = "success";

        // 2.2 记录方法开始时间
        long startTime = System.currentTimeMillis();

        try {
            // 执行原有业务方法（完全不干预，包括你的手动计数逻辑）
            return joinPoint.proceed();
        } catch (Exception e) {
            // 方法抛异常 → 标记为fail（和你手动计数的fail场景对齐）
            result = "fail";
            // 重新抛出异常，保证原有业务的异常处理逻辑不变
            throw e;
        } finally {
            // 2.3 计算耗时（毫秒）
            long durationMs = System.currentTimeMillis() - startTime;


            // 2.5 注册耗时指标（仅新增time指标，不触碰计数逻辑）
            Timer.builder("discuss.operate.timer") // 耗时指标名，和之前约定的一致
                    .tags("module", module, "operate", operate, "result", result)
                    .register(meterRegistry)
                    .record(durationMs, TimeUnit.MILLISECONDS);
        }
    }
}
