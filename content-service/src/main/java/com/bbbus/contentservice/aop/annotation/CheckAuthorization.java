package com.bbbus.contentservice.aop.annotation;


import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckAuthorization {
    String value();
}
