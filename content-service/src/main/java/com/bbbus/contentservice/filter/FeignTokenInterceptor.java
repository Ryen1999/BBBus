package com.bbbus.contentservice.filter;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 *  拦截器，内容中心所有 Feign 调用都会自动带上 Token，不用改任何接口代码。
 */
@Configuration
public class FeignTokenInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 从当前请求的 ThreadLocal 里取 Token
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String token = request.getHeader("X-Token");   // 网关透传进来的
            if (token != null) {
                template.header("X-Token", token);  // 自动加到 Feign 请求头
            }
        }
    }
}