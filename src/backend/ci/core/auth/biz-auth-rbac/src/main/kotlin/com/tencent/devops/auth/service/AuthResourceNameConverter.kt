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

package com.tencent.devops.auth.service

import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.IamGroupUtils

/**
 * 蓝盾资源名转换iam资源名
 */
class AuthResourceNameConverter {

    fun generateIamName(
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): String {
        return when (resourceType) {
            // 质量红线规则、质量红线通知组、版本体验组、版本体验、代码检查任务、CodeCC忽略类型
            // 资源名可以重复,创建二级管理员时就会报名称冲突,需要转换
            AuthResourceType.EXPERIENCE_GROUP_NEW.value,
            AuthResourceType.EXPERIENCE_TASK_NEW.value,
            AuthResourceType.QUALITY_RULE.value,
            AuthResourceType.CODECC_TASK.value,
            AuthResourceType.CODECC_IGNORE_TYPE.value,
            AuthResourceType.QUALITY_GROUP_NEW.value ->
                IamGroupUtils.buildSubsetManagerGroupName(
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    resourceName = resourceName
                )
            else ->
                IamGroupUtils.buildSubsetManagerGroupName(
                    resourceType = resourceType,
                    resourceName = resourceName
                )
        }
    }
}
