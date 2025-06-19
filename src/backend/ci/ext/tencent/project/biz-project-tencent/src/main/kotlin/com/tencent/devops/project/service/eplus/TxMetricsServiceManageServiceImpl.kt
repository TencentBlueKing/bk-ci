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

package com.tencent.devops.project.service.eplus

import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.pojo.enums.ProjectAuthSecrecyStatus
import com.tencent.devops.project.pojo.service.ServiceVO
import com.tencent.devops.project.service.ServiceManageService
import com.tencent.devops.project.service.eplus.EplusEncrptUtil.Filter
import com.tencent.devops.project.service.eplus.EplusEncrptUtil.JsonData
import com.tencent.devops.project.service.eplus.EplusEncrptUtil.encryptPanelToken
import java.text.MessageFormat
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("METRICS_MANAGE_SERVICE")
class TxMetricsServiceManageServiceImpl(
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao
) : ServiceManageService() {

    @Value("\${eplus.panelFrom:}")
    private val panelFrom: String = ""

    @Value("\${eplus.panelUrl:#{null}}")
    private val panelUrl: String? = null

    @Value("\${eplus.publicKey:#{null}}")
    private val publicKey: String? = null

    @Value("\${eplus.ms.metrics.panelNid:#{null}}")
    private val panelNid: Int? = null

    @Value("\${eplus.ms.metrics.panelPid:#{null}}")
    private val panelPid: Int? = null

    @Value("\${eplus.ms.metrics.performAnalysisPanelPid:#{null}}")
    private val performAnalysisPanelPid: Int? = null

    override fun doSpecBus(
        userId: String,
        serviceVO: ServiceVO,
        projectId: String?,
        pipelineId: String?
    ): ServiceVO {
        if (projectId.isNullOrBlank() || publicKey.isNullOrBlank()) return serviceVO
        if (panelUrl.isNullOrBlank() || panelPid == null || panelNid == null) return serviceVO
        val project = projectDao.getByEnglishName(dslContext, projectId)
        val filter = mutableListOf(Filter(col = "project_id", op = "=", `val` = projectId))
        if (pipelineId.isNullOrBlank()) {
            if (project?.authSecrecy != ProjectAuthSecrecyStatus.PUBLIC.value) return serviceVO
        } else {
            filter.add(Filter(col = "pipeline_id", op = "=", `val` = pipelineId))
        }
        // 非保密项目才跳去eplus页面看统计数据
        return serviceVO.apply {
            val jsonData = JsonData(
                nid = panelNid,
                pid = when {
                    pipelineId.isNullOrBlank() -> panelPid
                    else -> performAnalysisPanelPid ?: return serviceVO
                },
                user = userId,
                filter = filter
            )
            val token = encryptPanelToken(publicKey, jsonData)
            // 生成eplus的跳转页面地址
            val serviceUrl = MessageFormat(panelUrl).format(arrayOf(panelFrom, token))
            iframeUrl = serviceUrl
            grayIframeUrl = serviceUrl
        }
    }
}
