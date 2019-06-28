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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.archive

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.NamedType
import com.fasterxml.jackson.databind.module.SimpleModule
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.element.BuildArchiveGetElement
import com.tencent.devops.common.archive.element.CustomizeArchiveGetElement
import com.tencent.devops.common.archive.element.ReportArchiveElement
import com.tencent.devops.common.archive.element.SingleArchiveElement
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class AutoConfiguration {

    private val logger = LoggerFactory.getLogger(AutoConfiguration::class.java)

    @Bean
    fun registerSubtypesObjectMapper(@Autowired(required = false) objectMapper: ObjectMapper?): ObjectMapper {
        val elementSubModule = SimpleModule().registerSubtypes(
            NamedType(BuildArchiveGetElement::class.java, BuildArchiveGetElement.classType),
            NamedType(ReportArchiveElement::class.java, ReportArchiveElement.classType),
            NamedType(SingleArchiveElement::class.java, SingleArchiveElement.classType),
            NamedType(CustomizeArchiveGetElement::class.java, CustomizeArchiveGetElement.classType)
        )
        logger.info("[REGISTER_MODEL_ELEMENT]|BEGIN-$objectMapper")
        logger.info("[REGISTER_MODEL_ELEMENT]|${BuildArchiveGetElement::class.java}")
        logger.info("[REGISTER_MODEL_ELEMENT]|${ReportArchiveElement::class.java}")
        logger.info("[REGISTER_MODEL_ELEMENT]|${SingleArchiveElement::class.java}")
        logger.info("[REGISTER_MODEL_ELEMENT]|${CustomizeArchiveGetElement::class.java}")
        objectMapper?.registerModule(elementSubModule)

        // 工具类也要注册，防止异常
        JsonUtil.registerModule(elementSubModule)

        return objectMapper ?: JsonUtil.getObjectMapper()
    }
}