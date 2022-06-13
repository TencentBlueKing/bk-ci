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

package com.tencent.devops.metrics.service.impl

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.db.utils.SnowFlakeUtils
import com.tencent.devops.metrics.constant.Constants.MAX_CREATE_COUNT
import com.tencent.devops.metrics.dao.ProjectInfoDao
import com.tencent.devops.metrics.service.ProjectInfoManageService
import com.tencent.devops.metrics.pojo.`do`.AtomBaseInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineErrorTypeInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineLabelInfo
import com.tencent.devops.metrics.pojo.dto.QueryProjectAtomListDTO
import com.tencent.devops.metrics.pojo.dto.QueryProjectPipelineLabelDTO
import com.tencent.devops.metrics.pojo.qo.QueryProjectInfoQO
import com.tencent.devops.model.metrics.tables.records.TProjectPipelineLabelInfoRecord
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@Service
class ProjectInfoServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectInfoDao: ProjectInfoDao,
    private val client: Client
): ProjectInfoManageService {

    private val atomCodeCache = Caffeine.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(5, TimeUnit.HOURS)
        .build<String, List<AtomBaseInfoDO>>()

    override fun queryProjectAtomList(queryProjectInfoDTO: QueryProjectAtomListDTO): Page<AtomBaseInfoDO> {
        val projectId = queryProjectInfoDTO.projectId
        val page =queryProjectInfoDTO.page
        val pageSize = queryProjectInfoDTO.pageSize
        val records = if (queryProjectInfoDTO.keyword.isNullOrBlank()) {
            // 从缓存中查找插件属性信息
            val atomCodesList = atomCodeCache.getIfPresent("$projectId:$page:$pageSize")
            if (!atomCodesList.isNullOrEmpty()) {
                // 无需从db查数据则直接返回结果数据
                atomCodesList
            } else {
                val result = projectInfoDao.queryProjectAtomList(dslContext, projectId, page, pageSize)
                atomCodeCache.put("$projectId:$page:$pageSize", result)
                result
            }
        } else {
            projectInfoDao.queryProjectAtomList(dslContext, projectId, page, pageSize, queryProjectInfoDTO.keyword)
        }
        logger.info("queryProjectAtomList: $queryProjectInfoDTO,  $records")
        return Page(
            page = queryProjectInfoDTO.page,
            pageSize = queryProjectInfoDTO.pageSize,
            count = projectInfoDao.queryProjectAtomCount(dslContext, queryProjectInfoDTO.projectId),
            records = records
        )
    }

    override fun queryProjectPipelineLabels(
        queryProjectPipelineLabelDTO: QueryProjectPipelineLabelDTO
    ): Page<PipelineLabelInfo> {
        return Page(
            page = queryProjectPipelineLabelDTO.page,
            pageSize = queryProjectPipelineLabelDTO.pageSize,
            count = projectInfoDao.queryProjectPipelineLabelsCount(
                dslContext,
                QueryProjectInfoQO(
                    projectId = queryProjectPipelineLabelDTO.projectId,
                    pipelineIds = queryProjectPipelineLabelDTO.pipelineIds,
                    keyword = queryProjectPipelineLabelDTO.keyword,
                    page = queryProjectPipelineLabelDTO.page,
                    pageSize = queryProjectPipelineLabelDTO.pageSize
                )
            ),
            records = projectInfoDao.queryProjectPipelineLabels(
                dslContext,
                QueryProjectInfoQO(
                    projectId = queryProjectPipelineLabelDTO.projectId,
                    pipelineIds = queryProjectPipelineLabelDTO.pipelineIds,
                    keyword = queryProjectPipelineLabelDTO.keyword,
                    page = queryProjectPipelineLabelDTO.page,
                    pageSize = queryProjectPipelineLabelDTO.pageSize
                )
            )
        )

    }

    override fun queryPipelineErrorTypes(page: Int, pageSize: Int, keyword: String?): Page<PipelineErrorTypeInfoDO> {
        return Page(
            page = page,
            pageSize = pageSize,
            count = projectInfoDao.queryPipelineErrorTypeCount(dslContext, keyword),
            records = projectInfoDao.queryPipelineErrorTypes(
                dslContext,
                page,
                pageSize,
                keyword
            )
        )
    }

    override fun syncPipelineLabelData(userId: String): Int {
        var page = 1
        var totalPages = 1
        var createCount = 0
        do {
            val result = client.get(ServicePipelineResource::class)
                .getPipelineLabelInfos(
                userId,
                page,
                MAX_CREATE_COUNT
            ).data
            result?.let {
                val records = it.records
                val pipelineLabelRelateInfos = records.map { record ->
                    TProjectPipelineLabelInfoRecord(
                        SnowFlakeUtils.getId(SystemModuleEnum.METRICS.code),
                        record.projectId,
                        record.pipelineId,
                        record.labelId,
                        record.name,
                        record.createUser,
                        record.createUser,
                        record.createTime!!,
                        record.createTime!!
                    )
                }
                createCount += projectInfoDao.batchCreatePipelineLabelData(
                    dslContext,
                    pipelineLabelRelateInfos
                )
            }
            page += 1
            totalPages = result?.totalPages?: totalPages
        } while (page <= totalPages)
        return createCount
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectInfoServiceImpl::class.java)
    }
}