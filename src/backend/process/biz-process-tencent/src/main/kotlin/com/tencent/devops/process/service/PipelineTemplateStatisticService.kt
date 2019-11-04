/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.pojo.statistic.PipelineAndTemplateStatistic
import com.tencent.devops.process.util.exception.OrganizationTypeNotSupported
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 统计流水线与模板信息
 */
@Service
class PipelineTemplateStatisticService @Autowired constructor(
    private val dslContext: DSLContext,
    private val templateDao: TemplateDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val templatePipelineDao: TemplatePipelineDao,
    private val client: Client
) {

    /**
     * 通过组织信息获取流水线总数
     */
    fun getPipelineAndTemplateStatistic(
        userId: String,
        organizationType: String,
        organizationId: Int,
        deptName: String?,
        centerName: String?,
        interfaceName: String? = "Anon interface"
    ): PipelineAndTemplateStatistic {
        logger.info("$interfaceName:getPipelineAndTemplateStatistic:Input:($userId,$organizationType,$organizationId,$deptName,$centerName)")
        // 1.参数校验与预处理
        // 当前接口只支持BG的ID传递
        if (organizationType != AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG) {
            throw OrganizationTypeNotSupported("organizationType:$organizationType, is not supported yet,supported types are [ $AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG ]")
        }
        // 2.根据组织信息查询所有项目id
        // 调用TOF接口根据ID获取部门名称
        val projectsResult = client.get(ServiceTxProjectResource::class).getProjectEnNamesByOrganization(
            userId = userId,
            bgId = organizationId.toLong(),
            deptName = deptName?.trim(),
            centerName = centerName?.trim()
        )
        val projectIds = projectsResult.data?.toSet() ?: emptySet()
        logger.info("$interfaceName:getPipelineAndTemplateStatistic:Inner:projectIds=$projectIds")
        // 3.1 根据项目id集合查询流水线数量
        // 流水线总数
        val pipelineNum = pipelineInfoDao.countByProjectIds(dslContext, projectIds, ChannelCode.BS)
        // 实例化流水线总数
        val instancedPipelineNum = templatePipelineDao.countPipelineInstancedByTemplate(dslContext, projectIds).value1()
        // 模板总数
        val templateNum = templateDao.countTemplateByProjectIds(
            dslContext = dslContext,
            projectIds = projectIds,
            includePublicFlag = null,
            templateType = null,
            templateName = null,
            storeFlag = null
        )
        // 实例化模板总数
        val instancedTemplateNum = templatePipelineDao.countTemplateInstanced(dslContext, projectIds).value1()
        // 原始模板总数
        var srcTemplateIds: Set<String> = mutableSetOf()
        templateDao.getCustomizedTemplate(dslContext, projectIds).forEach {
            srcTemplateIds = srcTemplateIds.plus(it.value1())
        }
        templateDao.getOriginalTemplate(dslContext, projectIds).forEach {
            srcTemplateIds = srcTemplateIds.plus(it.value1())
        }
        val srcTemplateNum = srcTemplateIds.size
        // 实例化原始模板总数
        val instancedSrcTemplateNum = templatePipelineDao.countSrcTemplateInstanced(dslContext, srcTemplateIds).value1()
        logger.info("$interfaceName:getPipelineAndTemplateStatistic:Output:($pipelineNum,$instancedPipelineNum,$templateNum,$instancedTemplateNum,$srcTemplateNum,$instancedSrcTemplateNum)")
        return PipelineAndTemplateStatistic(
            pipelineNum = pipelineNum,
            instancedPipelineNum = instancedPipelineNum,
            templateNum = templateNum,
            instancedTemplateNum = instancedTemplateNum,
            srcTemplateNum = srcTemplateNum,
            instancedSrcTemplateNum = instancedSrcTemplateNum
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateStatisticService::class.java)
    }
}