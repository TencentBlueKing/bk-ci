package com.tencent.devops.common.auth.utils

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.AncestorsApiReq
import com.tencent.devops.common.auth.api.pojo.EsbCreateApiReq
import com.tencent.devops.common.auth.pojo.CodeCCAuthResourceType
import com.tencent.devops.common.auth.service.IamEsbService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired


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

class CodeCCAuthResourceApi @Autowired constructor(
    val iamEsbService: IamEsbService,
    val iamConfiguration: IamConfiguration
) {
    fun createResource(user: String, resourceType: CodeCCAuthResourceType, projectCode: String, resourceId: String, resourceName: String) {
    }

    fun doCreateResource(user: String, resourceType: CodeCCAuthResourceType, projectCode: String, resourceId: String, resourceName: String) {
        logger.info("v3 createResource projectCode[$projectCode] resourceName[$resourceName]" +
            " resourceId[$resourceId] resourceType[${resourceType.value}]")
        val ancestors = mutableListOf<AncestorsApiReq>()
        ancestors.add(AncestorsApiReq(
            system = iamConfiguration.systemId,
            id = projectCode,
            type = AuthResourceType.PROJECT.value
        ))

        val iamApiReq = EsbCreateApiReq(
            creator = user,
            name = resourceName,
            id = resourceId,
            type = resourceType.value,
            system = iamConfiguration.systemId,
            ancestors = ancestors,
            bk_app_code = "",
            bk_app_secret = "",
            bk_username = user
        )
        iamEsbService.createRelationResource(iamApiReq)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}