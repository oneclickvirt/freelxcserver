package com.lxc.framework.config.properties;


import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.hutool.core.util.ReUtil;
import com.lxc.common.annotation.Anonymous;
import com.lxc.common.utils.spring.SpringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

/**
 * 设置注解允许匿名访问的url
 *
 * @author Lion Li
 * @deprecated 将在后续版本使用Sa-Token注解 {@link SaIgnore} 代替，
 * 底层过滤方法详见 {@link SaInterceptor#preHandle(HttpServletRequest, HttpServletResponse, Object)}
 */
@Deprecated
@Lazy
@Component
public class ExcludeUrlProperties implements InitializingBean {
    //匹配路径参数？
    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");

    @Getter
    private final List<String> excludes = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        String asterisk = "*";
        //无需手动创建 Bean 实例，从而更加方便地利用 Spring 的依赖注入和 IoC（控制反转）特性
        RequestMappingHandlerMapping mapping = SpringUtils.getBean("requestMappingHandlerMapping",RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo,HandlerMethod> map = mapping.getHandlerMethods();

        map.keySet().forEach(info -> {
            HandlerMethod handlerMethod = map.get(info);

            // 获取方法上边的注解 替代path variable 为 * ,把路径模式集合的元素中的路径参数部分替换成*,扩大适配的权限范围（匿名访问）
            Anonymous method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(),Anonymous.class);
            Optional.ofNullable(method).ifPresent(anonymous -> {
                Set<PathPattern> patterns = info.getPathPatternsCondition().getPatterns();
                patterns.forEach(url -> {
                    excludes.add(ReUtil.replaceAll(url.getPatternString(),PATTERN,asterisk));
                });
            });

            // 获取类上边的注解, 替代path variable 为 *
            Anonymous controller = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Anonymous.class);
            Optional.ofNullable(controller).ifPresent(anonymous -> {
                Set<PathPattern> patterns = info.getPathPatternsCondition().getPatterns();
                patterns.forEach(url -> {
                    excludes.add(ReUtil.replaceAll(url.getPatternString(), PATTERN, asterisk));
                });
            });
        });
    }

}
