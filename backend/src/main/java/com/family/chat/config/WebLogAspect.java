package com.family.chat.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.ValueFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * Web 请求日志切面。
 *
 * <p>统一记录控制层请求的 URL、方法、客户端 IP、入参与耗时(RT)等信息。
 * 日志内容遵循安全规范：密码、密钥、令牌等敏感字段一律脱敏后输出。
 * 请求级别的 MDC(traceId) 清理由 TraceIdFilter 在 finally 块中统一负责。
 */
@Aspect
@Component
public class WebLogAspect {

    private static final Logger log = LoggerFactory.getLogger(WebLogAspect.class);

    /** 敏感字段脱敏后的占位符 */
    private static final String MASKED_VALUE = "******";

    /** 命中即脱敏的敏感字段关键字(小写匹配) */
    private static final String[] SENSITIVE_KEYWORDS = {"password", "secret", "token", "credential"};

    /** FastJSON 序列化时的敏感字段脱敏过滤器 */
    private static final ValueFilter SENSITIVE_FILTER = (object, name, value) -> {
        if (name != null && value instanceof String && isSensitiveField(name)) {
            return MASKED_VALUE;
        }
        return value;
    };

    // 拦截 com.family.chat.controller 包及其子包下的所有控制层方法
    @Pointcut("execution(* com.family.chat.controller..*.*(..))")
    public void webLog() {}

    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        String url = request != null ? request.getRequestURL().toString() : "Unknown";
        String method = request != null ? request.getMethod() : "Unknown";
        String ip = request != null ? request.getRemoteAddr() : "Unknown";
        Object[] logArgs = filterArgs(joinPoint.getArgs());
        log.info("[HTTP REQUEST] [url={}] [method={}] [client_ip={}] [class_method={}.{}] [args={}]",
                url, method, ip,
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                JSON.toJSONString(logArgs, SENSITIVE_FILTER));
        long startTime = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            // RT (response time) is the key metric for Grafana latency panels and slow-request alerting
            log.info("[HTTP RESPONSE] [url={}] [method={}] [rt={}ms]",
                    url, method, System.currentTimeMillis() - startTime);
        }
    }

    private Object[] filterArgs(Object[] args) {
        if (args == null) {
            return new Object[0];
        }
        Object[] filtered = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                filtered[i] = null;
            } else if (args[i] instanceof HttpServletRequest ||
                    args[i] instanceof HttpServletResponse ||
                    args[i] instanceof HttpSession) {
                filtered[i] = args[i].getClass().getSimpleName();

                // 处理单文件上传，替换为字符串描述，避免 FastJSON 报错
            } else if (args[i] instanceof MultipartFile file) {
                filtered[i] = "[MultipartFile: name=" + file.getOriginalFilename() + ", size=" + file.getSize() + "]";

                // 处理多文件数组上传
            } else if (args[i] instanceof MultipartFile[]) {
                filtered[i] = "[MultipartFile[]]";

            } else {
                filtered[i] = args[i];
            }
        }
        return filtered;
    }

    /**
     * 判断字段名是否属于敏感字段
     */
    private static boolean isSensitiveField(String fieldName) {
        String lowerCaseName = fieldName.toLowerCase(Locale.ROOT);
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (lowerCaseName.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
