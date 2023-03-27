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

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthResourceDao
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

/**
 * 资源code转换器
 *
 * 1. iam 资源ID最大只能36位,蓝盾资源code可能超过36位，如凭证Id是由用户自定义，最长可以支持64位，资源code需要进行转换
 * 2. 太长的资源code会导致返回的权限表达式太大，导致性能下降,如流水线ID，资源code也需要进行转换
 */
class AuthResourceCodeConverter @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val authResourceDao: AuthResourceDao
) {

    companion object {
        private const val AUTH_RESOURCE_ID_TAG = "AUTH_RESOURCE"
    }

    /**
     * 蓝盾资源code转换成iam资源code
     */
    fun generateIamCode(resourceType: String, resourceCode: String): String {
        // 如果是流水线或者凭证,iam资源code自动生成
        return if (needConvert(resourceType)) {
            client.get(ServiceAllocIdResource::class)
                .generateSegmentId(AUTH_RESOURCE_ID_TAG).data.toString()
        } else {
            resourceCode
        }
    }

    /**
     * 蓝盾资源code转换iam code
     */
    fun code2IamCode(projectCode: String, resourceType: String, resourceCode: String): String? {
        return if (needConvert(resourceType)) {
            authResourceDao.get(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode
            )?.iamResourceCode
        } else {
            resourceCode
        }
    }

    /**
     * 批量权限中心资源code转成蓝盾资源code
     */
    fun batchCode2IamCode(projectCode: String, resourceType: String, resourceCodes: List<String>): List<String> {
        return if (needConvert(resourceType)) {
            authResourceDao.getIamCodeByResourceCodes(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCodes = resourceCodes
            )
        } else {
            resourceCodes
        }
    }

    /**
     * 权限中心资源code转成蓝盾资源code
     */
    fun iamCode2Code(projectCode: String, resourceType: String, iamResourceCode: String): String {
        return if (needConvert(resourceType)) {
            authResourceDao.getByIamCode(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                iamResourceCode = iamResourceCode
            )?.resourceCode ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.RESOURCE_NOT_FOUND,
                params = arrayOf(iamResourceCode),
                defaultMessage = "the resource not exists, resourceCode:$iamResourceCode"
            )
        } else {
            iamResourceCode
        }
    }

    /**
     * 批量权限中心资源code转成蓝盾资源code
     */
    fun batchIamCode2Code(projectCode: String, resourceType: String, iamResourceCodes: List<String>): List<String> {
        return if (needConvert(resourceType)) {
            authResourceDao.getResourceCodeByIamCodes(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                iamResourceCodes = iamResourceCodes
            )
        } else {
            iamResourceCodes
        }
    }

    /**
     * 是否由auth生成id
     *
     * 凭证和证书名可能太长,超过iam限制,需要由auth生成
     * 流水线ID太长会导致表达式很长,影响性能,需要由auth生辰
     */
    private fun needConvert(resourceType: String): Boolean {
        return resourceType == AuthResourceType.TICKET_CREDENTIAL.value ||
            resourceType == AuthResourceType.TICKET_CERT.value ||
            resourceType == AuthResourceType.PIPELINE_DEFAULT.value
    }
}
