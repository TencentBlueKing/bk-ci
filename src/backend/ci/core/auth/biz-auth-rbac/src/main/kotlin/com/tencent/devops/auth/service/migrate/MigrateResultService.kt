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

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.service.AuthVerifyRecordService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.slf4j.LoggerFactory

@Suppress("ALL")
class MigrateResultService constructor(
    private val permissionService: PermissionService,
    private val migrateResourceCodeConverter: MigrateResourceCodeConverter,
    private val authVerifyRecordService: AuthVerifyRecordService,
    private val migrateResourceService: MigrateResourceService,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(MigrateResultService::class.java)
    }

    fun compare(projectCode: String): Boolean {
        logger.info("start to compare policy|$projectCode")
        val startEpoch = System.currentTimeMillis()
        try {
            var offset = 0
            val limit = PageUtil.MAX_PAGE_SIZE
            do {
                val verifyRecordList = authVerifyRecordService.listByProjectCode(
                    projectCode = projectCode,
                    offset = offset,
                    limit = limit
                )
                verifyRecordList.forEach {
                    with(it) {
                        if (resourceCode == "*") return@forEach
                        val rbacResourceCode = migrateResourceCodeConverter.getRbacResourceCode(
                            resourceType = resourceType,
                            migrateResourceCode = resourceCode
                        )
                        val rbacVerifyResult = rbacResourceCode?.let {
                            permissionService.validateUserResourcePermissionByRelation(
                                userId = userId,
                                action = action,
                                projectCode = projectId,
                                resourceCode = rbacResourceCode,
                                resourceType = resourceType,
                                relationResourceType = null
                            )
                        } ?: false
                        if (verifyResult != rbacVerifyResult) {
                            val isSkip = handleVerifyResult(
                                userId = userId,
                                resourceType = resourceType,
                                resourceCode = resourceCode,
                                projectCode = projectCode,
                                action = action
                            )
                            if (isSkip) return@forEach
                        }
                    }
                }
                offset += limit
            } while (verifyRecordList.size == limit)
            return true
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to compare policy|$projectCode"
            )
        }
    }

    private fun handleVerifyResult(
        userId: String,
        resourceType: String,
        resourceCode: String,
        projectCode: String,
        action: String
    ): Boolean {
        // 有可能鉴权表存在数据，但实际资源被删除的情况。所以得先查询资源是否存在，不存在则直接跳过
        migrateResourceService.fetchInstanceInfo(
            resourceType = resourceType,
            projectCode = projectCode,
            ids = listOf(resourceCode)
        ) ?: return true
        // 只对渠道为BS的流水线进行策略对比，因为只有该渠道注册的流水线，才有往权限中心注册
        if (resourceType == AuthResourceType.PIPELINE_DEFAULT.value) {
            val pipelineInfo = client.get(ServicePipelineResource::class)
                .getPipelineInfo(
                    projectId = projectCode,
                    pipelineId = resourceCode,
                    channelCode = ChannelCode.BS
                ).data ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.RESOURCE_NOT_FOUND,
                params = arrayOf(projectCode),
                defaultMessage = "pipeline not found:$resourceCode"
            )
            if (pipelineInfo.channelCode != ChannelCode.BS)
                return true
        }
        logger.warn("compare policy failed:$userId|$action|$projectCode|$resourceType|$resourceCode")
        throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_MIGRATE_AUTH_COMPARE_FAIL,
            params = arrayOf(projectCode),
            defaultMessage = "compare policy failed:" +
                "$userId|$action|$projectCode|$resourceType|$resourceCode"
        )
    }
}
