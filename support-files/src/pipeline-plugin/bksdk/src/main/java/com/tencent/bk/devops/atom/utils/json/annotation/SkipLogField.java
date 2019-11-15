/*
 * Copyright (c) 2017. Tencent BlueKing
 */

package com.tencent.bk.devops.atom.utils.json.annotation;

import com.fasterxml.jackson.annotation.JsonFilter;

import java.lang.annotation.*;

/**
 * 过滤不想在日志中打印出来的字段，一般比如Bean中有Password密钥等敏感信息，避免在toJsonString时不想输出在日志中可以如此
 * <p>
 * 将字段名称设置进入value，并给value设置一个唯一的标识，默认注解在字段上，如果不设置则直接以字段名作为标识过滤
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JsonFilter("SkipLogField")
public @interface SkipLogField {
    /**
     * 要过滤的字段名称 -- 用于字段
     *
     * @return
     */
    String value() default "";
}
