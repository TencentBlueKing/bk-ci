/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.scm.handler

import com.tencent.devops.common.web.JerseyConfig
import com.tencent.devops.common.web.annotation.BkExceptionMapper
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Primary
@Configuration
class ScmJerseyConfig : JerseyConfig() {

    override fun afterPropertiesSet() {
        super.afterPropertiesSet()
        logger.info("scm|JerseyConfig-BkExceptionMapper-Reflect-find-start")
        val reflections = Reflections("com.tencent.devops.scm.handler.exception")
        val handlerClasses = reflections.getTypesAnnotatedWith(BkExceptionMapper::class.java)
        handlerClasses?.forEach { handlerClazz ->
            if (!Modifier.isAbstract(handlerClazz.modifiers)) {
                logger.info("scm|Reflect-BkExceptionMapper: $handlerClazz")
                register(handlerClazz)
            }
        }
        logger.info("scm|JerseyConfig-ExceptionMapper-register-end")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScmJerseyConfig::class.java)
    }
}
