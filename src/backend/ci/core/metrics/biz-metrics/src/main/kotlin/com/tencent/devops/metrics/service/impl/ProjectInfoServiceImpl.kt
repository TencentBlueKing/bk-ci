/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.common.api.constant.SYSTEM
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.PageUtil.MAX_PAGE_SIZE
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.metrics.config.MetricsConfig
import com.tencent.devops.metrics.dao.ProjectInfoDao
import com.tencent.devops.metrics.pojo.`do`.AtomBaseInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineErrorTypeInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineLabelInfo
import com.tencent.devops.metrics.pojo.dto.QueryProjectAtomListDTO
import com.tencent.devops.metrics.pojo.dto.QueryProjectPipelineLabelDTO
import com.tencent.devops.metrics.pojo.po.SaveProjectAtomRelationDataPO
import com.tencent.devops.metrics.pojo.qo.QueryProjectInfoQO
import com.tencent.devops.metrics.service.ProjectInfoManageService
import com.tencent.devops.metrics.utils.QueryParamCheckUtil.getErrorTypeName
import com.tencent.devops.model.metrics.tables.records.TProjectPipelineLabelInfoRecord
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@Service
class ProjectInfoServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectInfoDao: ProjectInfoDao,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val metricsConfig: MetricsConfig
) : ProjectInfoManageService {

    private val atomCodeCache = Caffeine.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(5, TimeUnit.HOURS)
        .build<String, List<AtomBaseInfoDO>>()

    override fun queryProjectAtomList(queryProjectInfoDTO: QueryProjectAtomListDTO): Page<AtomBaseInfoDO> {
        val projectId = queryProjectInfoDTO.projectId
        val page = queryProjectInfoDTO.page
        val pageSize = queryProjectInfoDTO.pageSize
        val keyword = queryProjectInfoDTO.keyword
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
            projectInfoDao.queryProjectAtomList(dslContext, projectId, page, pageSize, keyword)
        }
        return Page(
            page = queryProjectInfoDTO.page,
            pageSize = queryProjectInfoDTO.pageSize,
            count = projectInfoDao.queryProjectAtomCount(dslContext, queryProjectInfoDTO.projectId, keyword),
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
        val errorTypeInfos = projectInfoDao.queryPipelineErrorTypes(
            dslContext,
            page = page,
            pageSize = pageSize,
            keyWord = keyword
        ).map { PipelineErrorTypeInfoDO(it, getErrorTypeName(it)) }
        return Page(
            page = page,
            pageSize = pageSize,
            count = projectInfoDao.queryPipelineErrorTypeCount(dslContext, keyword),
            records = errorTypeInfos
        )
    }

    override fun syncPipelineLabelData(userId: String): Boolean {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin syncPipelineLabelData")
        var projectMinId = client.get(ServiceProjectResource::class).getMinId().data
        val projectMaxId = client.get(ServiceProjectResource::class).getMaxId().data
        if (projectMinId != null && projectMaxId != null) {

                do {
                    val projectIds = client.get(ServiceProjectResource::class)
                        .getProjectListById(
                            minId = projectMinId,
                            maxId = projectMinId + MAX_PAGE_SIZE
                        ).data?.map { it.englishName }
                    val labelInfosResult = client.get(ServicePipelineResource::class)
                        .getPipelineLabelInfos(userId, projectIds ?: emptyList()).data
                    val pipelineLabelRelateInfos = labelInfosResult?.map {
                        val tProjectPipelineLabelInfoRecord = TProjectPipelineLabelInfoRecord()
                        tProjectPipelineLabelInfoRecord.id = client.get(ServiceAllocIdResource::class)
                            .generateSegmentId("METRICS_PROJECT_PIPELINE_LABEL_INFO").data ?: 0
                        tProjectPipelineLabelInfoRecord.projectId = it.projectId
                        tProjectPipelineLabelInfoRecord.pipelineId = it.pipelineId
                        tProjectPipelineLabelInfoRecord.labelId = it.labelId
                        tProjectPipelineLabelInfoRecord.labelName = it.name
                        tProjectPipelineLabelInfoRecord.creator = it.createUser
                        tProjectPipelineLabelInfoRecord.modifier = it.createUser
                        tProjectPipelineLabelInfoRecord.updateTime = it.createTime!!
                        tProjectPipelineLabelInfoRecord.createTime = it.createTime!!
                        tProjectPipelineLabelInfoRecord
                    }
                    if (!pipelineLabelRelateInfos.isNullOrEmpty()) {
                        projectInfoDao.batchCreatePipelineLabelData(
                            dslContext,
                            pipelineLabelRelateInfos
                        )
                    }
                    projectMinId += (MAX_PAGE_SIZE + 1)
                } while (projectMinId <= projectMaxId)
                logger.info("end syncPipelineLabelData")
            }
        }
        return true
    }

    override fun syncProjectAtomData(userId: String): Boolean {
        val executor = ThreadPoolExecutor(
            metricsConfig.maxThreadHandleProjectNum,
            metricsConfig.maxThreadHandleProjectNum,
            0L,
            TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(10),
            Executors.defaultThreadFactory(),
            ThreadPoolExecutor.DiscardPolicy()
        )
        try {
            logger.info("begin op sync project atom data")
            val projectMinId = client.get(ServiceProjectResource::class).getMinId().data
            val projectMaxId = client.get(ServiceProjectResource::class).getMaxId().data

            if (projectMinId != null && projectMaxId != null) {
                val projectRanges =
                    calculateProjectRanges(projectMinId, projectMaxId, metricsConfig.maxThreadHandleProjectNum)
                projectRanges.forEach { (startProjectPrimaryId, endProjectPrimaryId) ->
                    executor.submit(Callable {
                        val projectIds = client.get(ServiceProjectResource::class)
                            .getProjectListById(
                                minId = startProjectPrimaryId,
                                maxId = endProjectPrimaryId
                            ).data?.map { it.englishName }
                        projectIds?.let { saveProjectAtomInfo(it) }
                    })
                }
            }
        } catch (ignore: Throwable) {
            logger.warn("op ProjectInfoServiceImpl sync project atom data fail", ignore)
        } finally {
            thread {
                executor.shutdown()
            }
        }
        return true
    }

    private fun calculateProjectRanges(
        projectMinId: Long,
        projectMaxId: Long,
        maxThreadHandleProjectNum: Int
    ): List<Pair<Long, Long>> {
        val avgProjectNum = projectMaxId / maxThreadHandleProjectNum
        return (1..maxThreadHandleProjectNum).map { index ->
            val startProjectPrimaryId = (index - 1) * avgProjectNum + 1
            val endProjectPrimaryId = if (index != maxThreadHandleProjectNum) {
                index * avgProjectNum
            } else {
                index * avgProjectNum + projectMinId % maxThreadHandleProjectNum
            }
            Pair(startProjectPrimaryId, endProjectPrimaryId)
        }
    }

    private fun saveProjectAtomInfo(projectIds: List<String>) {
        try {
            var page = PageUtil.DEFAULT_PAGE
            do {
                val projectAtomInfo = projectInfoDao.queryProjectAtomNewNameInfos(
                    dslContext = dslContext,
                    projectIds = projectIds,
                    page = page,
                    pageSize = MAX_PAGE_SIZE
                )
                val saveProjectAtomRelationPOs = mutableListOf<SaveProjectAtomRelationDataPO>()
                projectAtomInfo.forEach {
                    val saveProjectAtomRelationDataPO = SaveProjectAtomRelationDataPO(
                        id = client.get(ServiceAllocIdResource::class)
                            .generateSegmentId("METRICS_PROJECT_ATOM_RELEVANCY_INFO").data ?: 0,
                        projectId = it.value1(),
                        atomCode = it.value2(),
                        atomName = it.value3(),
                        creator = SYSTEM,
                        modifier = SYSTEM
                    )
                    saveProjectAtomRelationPOs.add(saveProjectAtomRelationDataPO)
                }
                if (saveProjectAtomRelationPOs.isNotEmpty()) {
                    projectInfoDao.batchSaveProjectAtomInfo(dslContext, saveProjectAtomRelationPOs)
                }
                page ++
            } while (projectAtomInfo.size == MAX_PAGE_SIZE)
        } catch (ignore: Throwable) {
            logger.warn("ProjectInfoServiceImpl sync project atom data fail", ignore)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectInfoServiceImpl::class.java)
        private var executor: ThreadPoolExecutor? = null
    }
}
