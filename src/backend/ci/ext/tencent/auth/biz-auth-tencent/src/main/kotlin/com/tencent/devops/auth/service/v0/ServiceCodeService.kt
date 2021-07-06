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

package com.tencent.devops.auth.service.v0

import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.BSCommonAuthServiceCode
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import com.tencent.devops.common.auth.code.ExperienceAuthServiceCode
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.auth.code.QualityAuthServiceCode
import com.tencent.devops.common.auth.code.RepoAuthServiceCode
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ServiceCodeService @Autowired constructor(
    val pipelineAuthServiceCode: PipelineAuthServiceCode,
    val ticketAuthServiceCode: TicketAuthServiceCode,
    val environmentAuthServiceCode: EnvironmentAuthServiceCode,
    val qualityAuthServiceCode: QualityAuthServiceCode,
    val repoAuthServiceCode: RepoAuthServiceCode,
    val experienceAuthServiceCode: ExperienceAuthServiceCode,
    @Autowired(required = false)
    val commonAuthServiceCode: BSCommonAuthServiceCode?
) {
    fun getServiceCodeByResource(authResourceType: String): AuthServiceCode {
        val serviceCode = when (authResourceType) {
            AuthResourceType.PIPELINE_DEFAULT.value -> pipelineAuthServiceCode
            AuthResourceType.EXPERIENCE_GROUP.value -> experienceAuthServiceCode
            AuthResourceType.EXPERIENCE_TASK.value -> experienceAuthServiceCode
            AuthResourceType.ENVIRONMENT_ENV_NODE.value -> environmentAuthServiceCode
            AuthResourceType.ENVIRONMENT_ENVIRONMENT.value -> environmentAuthServiceCode
            AuthResourceType.QUALITY_RULE.value -> qualityAuthServiceCode
            AuthResourceType.QUALITY_GROUP.value -> qualityAuthServiceCode
            AuthResourceType.TICKET_CREDENTIAL.value -> ticketAuthServiceCode
            AuthResourceType.TICKET_CERT.value -> ticketAuthServiceCode
            AuthResourceType.CODE_REPERTORY.value -> repoAuthServiceCode
            else -> commonAuthServiceCode!!
        }
        return serviceCode
    }
}
