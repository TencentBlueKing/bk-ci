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

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.monitoring.api.service.ServiceAtomMonitorResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineTaskResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreStatisticDailyDao
import com.tencent.devops.store.dao.common.StoreStatisticTotalDao
import com.tencent.devops.store.pojo.atom.AtomPipeline
import com.tencent.devops.store.pojo.atom.AtomPipelineExecInfo
import com.tencent.devops.store.pojo.common.StoreDailyStatisticRequest
import com.tencent.devops.store.pojo.common.StoreStatisticPipelineNumUpdate
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.MarketAtomStatisticService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.Executors

@Suppress("ALL")
@Service
class MarketAtomStatisticServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storeStatisticTotalDao: StoreStatisticTotalDao,
    private val storeStatisticDailyDao: StoreStatisticDailyDao,
    private val client: Client
) : MarketAtomStatisticService {

    companion object {
        private val logger = LoggerFactory.getLogger(MarketAtomStatisticServiceImpl::class.java)
        private const val DEFAULT_PAGE_SIZE = 10
    }

    /**
     * 根据插件标识获取插件关联的所有流水线列表（包括其他项目下）
     */
    override fun getAtomPipelinesByCode(
        atomCode: String,
        username: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<AtomPipeline>> {
        logger.info("getAtomPipelinesByCode: $atomCode | $username | $page | $pageSize")

        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 100

        val pipelineTaskRet =
            client.get(ServicePipelineTaskResource::class).listByAtomCode(
                atomCode = atomCode,
                projectCode = null,
                page = pageNotNull,
                pageSize = pageSizeNotNull
            ).data
        if (pipelineTaskRet == null) {
            return Result(Page(pageNotNull, pageSizeNotNull, 0.toLong(), listOf()))
        } else {
            val pipelines = pipelineTaskRet.records
            val projectCodeList = pipelines.map { it.projectCode }
            val projects = client.get(ServiceProjectResource::class)
                .listByProjectCode(projectCodeList.toSet()).data?.associateBy { it.projectCode }

            val records = pipelines.map {
                val projectCode = it.projectCode
                val project = projects?.get(projectCode)
                AtomPipeline(
                    pipelineId = it.pipelineId,
                    pipelineName = it.pipelineName,
                    atomVersion = it.atomVersion,
                    projectCode = projectCode,
                    projectName = project?.projectName ?: "",
                    bgName = project?.bgName ?: "",
                    deptName = project?.deptName ?: "",
                    centerName = project?.centerName ?: ""
                )
            }
            return Result(Page(pageNotNull, pageSizeNotNull, pipelineTaskRet.count, records))
        }
    }

    /**
     * 根据插件标识获取插件关联的流水线信息（当前项目下）
     */
    override fun getAtomPipelinesByProject(
        userId: String,
        projectCode: String,
        atomCode: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<AtomPipelineExecInfo>> {
        logger.info("getAtomPipelinesByProject: $atomCode | $userId | $projectCode| $page | $pageSize")

        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 100

        val pipelineTaskRet = client.get(ServicePipelineTaskResource::class)
                .listByAtomCode(atomCode, projectCode, pageNotNull, pageSizeNotNull).data
        if (pipelineTaskRet == null) {
            return Result(Page(pageNotNull, pageSizeNotNull, 0.toLong(), listOf()))
        } else {
            val pipelines = pipelineTaskRet.records
            val pipelineIdList = pipelines.map { it.pipelineId }
            val pipelineBuildInfo = client.get(ServiceBuildResource::class)
                .getPipelineLatestBuildByIds(projectCode, pipelineIdList).data
            logger.info("pipelineBuildInfo: $pipelineBuildInfo")

            val records = pipelines.map {
                val pipelineId = it.pipelineId
                val buildInfo = pipelineBuildInfo?.get(pipelineId)
                AtomPipelineExecInfo(
                    pipelineId = pipelineId,
                    pipelineName = it.pipelineName,
                    projectCode = projectCode,
                    owner = buildInfo?.startUser ?: "",
                    latestExecTime = buildInfo?.startTime ?: ""
                )
            }
            return Result(Page(pageNotNull, pageSizeNotNull, pipelineTaskRet.count, records))
        }
    }

    /**
     * 同步使用插件流水线数量到汇总数据统计表
     */
    override fun asyncUpdateStorePipelineNum(): Boolean {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin asyncUpdateStorePipelineNum!!")
            batchUpdatePipelineNum()
            logger.info("end asyncUpdateStorePipelineNum!!")
        }
        return true
    }

    override fun asyncAtomDailyStatisticInfo(
        storeType: Byte,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Boolean {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin asyncAtomDailyStatisticInfo!!")
            batchUpdateAtomDailyStatisticInfo(storeType, startTime, endTime)
            logger.info("end asyncAtomDailyStatisticInfo!!")
        }
        return true
    }

    private fun batchUpdateAtomDailyStatisticInfo(
        storeType: Byte,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ) {
        var page = 1
        do {
            val storeStatistics = storeStatisticTotalDao.getStatisticList(
                dslContext = dslContext,
                storeType = storeType,
                page = page,
                pageSize = DEFAULT_PAGE_SIZE,
                timeDescFlag = false
            )
            storeStatistics?.forEach { storeStatistic ->
                val storeCode = storeStatistic.value1()
                logger.info("batchUpdateAtomDailyStatisticInfo storeCode:$storeCode")
                val atomMonitorStatisticData =
                    client.get(ServiceAtomMonitorResource::class).queryAtomMonitorStatisticData(
                        atomCode = storeCode,
                        startTime = startTime.timestampmilli(),
                        endTime = endTime.timestampmilli()
                    ).data
                val storeDailyStatistic = storeStatisticDailyDao.getDailyStatisticByCode(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    storeType = storeType,
                    statisticsTime = startTime
                )
                val totalFailDetail = atomMonitorStatisticData?.totalFailDetail
                // 统计总的使用量
                val totalDownloads = storeProjectRelDao.countInstallNumByCode(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    storeType = storeType,
                    endTime = endTime
                )
                // 统计当天组件的安装量
                val dailyDownloads = storeProjectRelDao.countInstallNumByCode(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    storeType = storeType,
                    startTime = startTime,
                    endTime = endTime
                )
                val storeDailyStatisticRequest = StoreDailyStatisticRequest(
                    totalDownloads = totalDownloads,
                    dailyDownloads = dailyDownloads,
                    dailySuccessNum = atomMonitorStatisticData?.totalSuccessNum,
                    dailyFailNum = atomMonitorStatisticData?.totalFailNum,
                    dailyFailDetail = if (totalFailDetail != null) JsonUtil.toMap(totalFailDetail) else null,
                    statisticsTime = startTime
                )
                if (storeDailyStatistic != null) {
                    storeStatisticDailyDao.updateDailyStatisticData(
                        dslContext = dslContext,
                        storeCode = storeCode,
                        storeType = storeType,
                        storeDailyStatisticRequest = storeDailyStatisticRequest
                    )
                } else {
                    storeStatisticDailyDao.insertDailyStatisticData(
                        dslContext = dslContext,
                        storeCode = storeCode,
                        storeType = storeType,
                        storeDailyStatisticRequest = storeDailyStatisticRequest
                    )
                }
            }
            page++
        } while (storeStatistics?.size == DEFAULT_PAGE_SIZE)
    }

    private fun batchUpdatePipelineNum() {
        var page = 1
        do {
            // 查询汇总统计表需要同步流水线数量插件的列表
            val statisticList = storeStatisticTotalDao.getStatisticList(
                dslContext = dslContext,
                storeType = StoreTypeEnum.ATOM.type.toByte(),
                page = page,
                pageSize = DEFAULT_PAGE_SIZE
            )
            val atomCodes = statisticList?.map { it.value1() }
            // 批量更汇总统计表使用插件流水线数量
            if (atomCodes?.isNotEmpty() == true) {
                val dataMap = client.get(ServicePipelineTaskResource::class)
                    .listPipelineNumByAtomCodes(null, atomCodes).data
                if (dataMap != null) {
                    val pipelineNumUpdateList = mutableListOf<StoreStatisticPipelineNumUpdate>()
                    atomCodes.forEach { atomCode ->
                        pipelineNumUpdateList.add(
                            StoreStatisticPipelineNumUpdate(
                                storeCode = atomCode,
                                num = dataMap[atomCode]
                            )
                        )
                    }
                    storeStatisticTotalDao.batchUpdatePipelineNum(
                        dslContext = dslContext,
                        pipelineNumUpdateList = pipelineNumUpdateList,
                        storeType = StoreTypeEnum.ATOM.type.toByte()
                    )
                }
            }
            page++
        } while (statisticList?.size == DEFAULT_PAGE_SIZE)
    }
}
