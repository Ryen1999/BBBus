package com.bbbus.contentservice.aop;

import com.bbbus.contentservice.enums.ErrorStatusEnum;
import com.bbbus.contentservice.exception.AuthenticationException;
import com.bbbus.contentservice.security.jwt.JwtOperator;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
public class AuthCheckAspect {

    @Autowired
    private JwtOperator jwtOperator;


    /**
     * 认证、授权
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.bbbus.contentservice.aop.annotation.CheckAuthorization) || " +
            "@within(com.bbbus.contentservice.aop.annotation.CheckAuthorization)")
    public Object checkAuthorization(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null)
        {
            throw new AuthenticationException(500,"请先登录");
        }


        HttpServletRequest request = attributes.getRequest();

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            throw new AuthenticationException(500,"请先登录");
        }

        try {
            Claims claims = jwtOperator.getClaimsFromToken(token);
            if (!jwtOperator.validateToken(token)) {
                throw new AuthenticationException("Token无效或已过期");
            }

             String role =claims.get("role").toString();

            //TODO 角色权限校验 不区分大小写
            if (!role.equalsIgnoreCase("ADMIN")) {
                throw new AuthenticationException(ErrorStatusEnum.UNAUTHORIZED.getCode(),ErrorStatusEnum.UNAUTHORIZED.getMessage());
            }


        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Token校验失败: {}", e.getMessage());
            throw new AuthenticationException("Token无效或已过期");
        }


        return joinPoint.proceed();
    }

    private String extractToken(HttpServletRequest request) {
//        String authorization = request.getHeader("Authorization");
//        if (StringUtils.hasText(authorization)) {
//            if (authorization.startsWith("Bearer ")) {
//                return authorization.substring(7).trim();
//            }
//            return authorization.trim();
//        }
        String token = request.getHeader("X-Token");
        return StringUtils.hasText(token) ? token.trim() : null;
    }
}
