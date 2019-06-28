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

package com.tencent.devops.process.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.UserBuildParametersResource
import com.tencent.devops.process.utils.PIPELINE_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_ELEMENT_ID
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PIPELINE_VMSEQ_ID
import com.tencent.devops.process.utils.PROJECT_NAME
import com.tencent.devops.process.utils.REPORT_DYNAMIC_ROOT_URL
import com.tencent.devops.store.pojo.app.BuildEnvParameters

@RestResource
class UserBuildParametersResourceImpl : UserBuildParametersResource {

    override fun getCommonBuildParams(userId: String) =
        Result(
            listOf(
                BuildEnvParameters(PIPELINE_START_USER_NAME, "当前构建的启动人"),
                BuildEnvParameters(PIPELINE_START_TYPE, "当前构建的启动方式，从${StartType.values().joinToString("/") {
                    it.name
                }}中取值"),
                BuildEnvParameters(PIPELINE_BUILD_NUM, "当前构建的唯一标示ID，从1开始自增"),
                BuildEnvParameters(PROJECT_NAME, "项目英文名"),
                BuildEnvParameters(PIPELINE_ID, "流水线ID"),
                BuildEnvParameters(PIPELINE_NAME, "流水线名称"),
                BuildEnvParameters(PIPELINE_BUILD_ID, "当前构建ID"),
                BuildEnvParameters(PIPELINE_VMSEQ_ID, "流水线JOB ID"),
                BuildEnvParameters(PIPELINE_ELEMENT_ID, "流水线Task ID"),
                BuildEnvParameters(REPORT_DYNAMIC_ROOT_URL, "自定义产出物报告的Web根路径")
            )
        )
}