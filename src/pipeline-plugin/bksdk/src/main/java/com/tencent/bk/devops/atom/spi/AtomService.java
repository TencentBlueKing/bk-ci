package com.tencent.bk.devops.atom.spi;

import com.tencent.bk.devops.atom.pojo.AtomBaseParam;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AtomService {

    String value() default "";

    /**
     * 排序顺序
     *
     * @return sortNo
     */
    int order() default 0;

    /**
     * 参数类
     */
    Class<? extends AtomBaseParam> paramClass();
}
