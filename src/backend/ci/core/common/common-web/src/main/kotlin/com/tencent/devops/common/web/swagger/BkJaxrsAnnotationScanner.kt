package com.tencent.devops.common.web.swagger

import io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner
import io.swagger.v3.jaxrs2.integration.JaxrsApplicationAndAnnotationScanner
import org.springframework.aop.support.AopUtils

/**
 * 被JerseySwaggerConfig使用到 , 勿删
 */
class BkJaxrsAnnotationScanner : JaxrsAnnotationScanner<JaxrsApplicationAndAnnotationScanner>() {
    override fun classes(): MutableSet<Class<*>> {
        val classes = super.classes()

        val singletons = application.singletons
        if (singletons != null) {
            for (o in singletons) {
                val sourceClass = if (AopUtils.isAopProxy(o)) {
                    AopUtils.getTargetClass(o)
                } else {
                    o.javaClass
                }
                if (!isIgnored(sourceClass.name)) {
                    classes.add(sourceClass)
                }
            }
        }
        return classes
    }
}