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

package com.tencent.devops.common.web.handler

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.MESSAGE
import com.tencent.devops.common.api.constant.PATTERN_STYLE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.web.annotation.BkExceptionMapper
import com.tencent.devops.common.web.constant.BkStyleEnum
import org.slf4j.LoggerFactory
import java.text.MessageFormat
import javax.validation.ConstraintViolationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper

@BkExceptionMapper
class BkFieldExceptionMapper : ExceptionMapper<ConstraintViolationException> {

    companion object {
        private val logger = LoggerFactory.getLogger(BkFieldExceptionMapper::class.java)
    }

    override fun toResponse(exception: ConstraintViolationException): Response {
        val constraintViolations = exception.constraintViolations
        var errorResult: Result<Any> = MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        if (constraintViolations.isNotEmpty()) {
            constraintViolations.forEach { constraintViolation ->
                val constraintDescriptor = constraintViolation.constraintDescriptor
                val attributes = constraintDescriptor.attributes
                val patternStyle = attributes[PATTERN_STYLE] as BkStyleEnum // 获取接口参数校验的正则表达式
                val defaultMessage = attributes[MESSAGE] as String // 获取接口参数校验的默认错误描述
                // 获取接口参数正则表达式对应的错误描述信息
                val patternStyleMessage = MessageCodeUtil.getCodeLanMessage(patternStyle.name, defaultMessage)
                // 获取接口参数在接口资源路径对应方法中path路径
                val propertyPath = constraintViolation.propertyPath.toString()
                val pathList = propertyPath.split(".")
                val pathSize = pathList.size
                // 展示给客户端的路径需去掉方法名和bean类型参数名称
                val propertyShowPath = when {
                    pathSize > 2 -> propertyPath.substring(pathList[0].length + pathList[1].length + 2)
                    pathSize == 2 -> propertyPath.substring(pathList[0].length + 1)
                    else -> propertyPath
                }
                // 获取path路径对应的描述信息，如果没有配置则给前端展示去掉方法名的path
                val parameterName = MessageCodeUtil.getCodeLanMessage(propertyPath, propertyShowPath)
                // 生成错误信息
                errorResult = MessageCodeUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_VALIDATE_ERROR,
                    params = arrayOf(parameterName, patternStyleMessage),
                    data = null,
                    defaultMessage = MessageFormat(patternStyleMessage).format(arrayOf(parameterName))
                )
                logger.info("field:$propertyPath errorResult is；$errorResult")
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(errorResult).build()
            }
        }
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE)
            .entity(errorResult).build()
    }
}
