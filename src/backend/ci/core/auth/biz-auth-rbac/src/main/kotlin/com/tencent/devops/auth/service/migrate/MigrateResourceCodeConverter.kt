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
 *
 */

package com.tencent.devops.auth.service.migrate

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServicePipelineResource

/**
 * 资源code转换
 */
class MigrateResourceCodeConverter constructor(
    private val client: Client
) {

    fun getRbacResourceCode(projectCode: String, resourceType: String, migrateResourceCode: String): String? {
        return when (resourceType) {
            // v3流水线使用的是流水线自增Id，rbac需获取具体的pipelineId
            AuthResourceType.PIPELINE_DEFAULT.value -> {
                getPipelineId(projectCode = projectCode, resourceCode = migrateResourceCode)
            }
            // v3代码库使用的是代码库自增ID，rbac使用的是hashId
            AuthResourceType.CODE_REPERTORY.value -> {
                HashUtil.encodeOtherLongId(migrateResourceCode.toLong())
            }
            else -> migrateResourceCode
        }
    }

    private fun getPipelineId(projectCode: String, resourceCode: String): String? {
        return try {
            val pipelineInfo = client.get(ServicePipelineResource::class)
                .getPipelineInfobyAutoId(projectId = projectCode, id = resourceCode.toLong()).data
            pipelineInfo?.pipelineId
        } catch (ignore: Exception) {
            resourceCode
        }
    }
}
