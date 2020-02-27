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

package com.tencent.devops.process.engine.utils

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

object PipelineUtils {

    private val logger = LoggerFactory.getLogger(PipelineUtils::class.java)

    private const val ENGLISH_NAME_PATTERN = "[A-Za-z_][A-Za-z_0-9]+"

    fun checkPipelineName(name: String) {
        if (name.toCharArray().size > 64) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_TOO_LONG,
                defaultMessage = "Pipeline's name is too long"
            )
        }
    }

    fun checkPipelineParams(params: List<BuildFormProperty>) {
        params.forEach {
            if (!Pattern.matches(ENGLISH_NAME_PATTERN, it.id)) {
                logger.warn("Pipeline's start params Name is iregular")
                throw OperationException(MessageCodeUtil.getCodeLanMessage(ProcessMessageCode.ERROR_PIPELINE_PARAMS_NAME_ERROR))
            }
        }
    }
}
