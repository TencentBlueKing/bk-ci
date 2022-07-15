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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.metrics.config.MetricsConfig
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_CODE
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_COUNT
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_MSG
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_TYPE
import com.tencent.devops.metrics.constant.MetricsMessageCode
import com.tencent.devops.metrics.utils.QueryParamCheckUtil.getErrorTypeName
import com.tencent.devops.metrics.dao.AtomFailInfoDao
import com.tencent.devops.metrics.service.AtomFailInfoManageService
import com.tencent.devops.metrics.pojo.`do`.AtomErrorCodeStatisticsInfoDO
import com.tencent.devops.metrics.pojo.`do`.AtomFailDetailInfoDO
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.metrics.pojo.`do`.ErrorCodeInfoDO
import com.tencent.devops.metrics.pojo.dto.QueryAtomFailInfoDTO
import com.tencent.devops.metrics.pojo.qo.QueryAtomFailInfoQO
import com.tencent.devops.metrics.utils.MetricsUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AtomFailInfoServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val atomFailInfoDao: AtomFailInfoDao,
    private val metricsConfig: MetricsConfig
) : AtomFailInfoManageService {

    override fun queryAtomErrorCodeStatisticsInfo(
        queryAtomFailInfoDTO: QueryAtomFailInfoDTO
    ): List<AtomErrorCodeStatisticsInfoDO> {
        //  获取查询记录总数
        val atomErrorCodeCount = atomFailInfoDao.queryAtomErrorCodeOverviewCount(
            dslContext,
            QueryAtomFailInfoQO(
                projectId = queryAtomFailInfoDTO.projectId,
                BaseQueryReqVO(
                    pipelineIds = queryAtomFailInfoDTO.pipelineIds,
                    pipelineLabelIds = queryAtomFailInfoDTO.pipelineLabelIds,
                    startTime = queryAtomFailInfoDTO.startTime,
                    endTime = queryAtomFailInfoDTO.endTime
                ),
                errorTypes = queryAtomFailInfoDTO.errorTypes,
                errorCodes = queryAtomFailInfoDTO.errorCodes,
                atomCodes = queryAtomFailInfoDTO.atomCodes
            )
        )
        // 查询记录过多，提醒用户缩小查询范围
        if (atomErrorCodeCount > metricsConfig.queryCountMax) {
            throw ErrorCodeException(
                errorCode = MetricsMessageCode.QUERY_DETAILS_COUNT_BEYOND,
                params = arrayOf("${metricsConfig.queryCountMax}")
            )
        }
        // 查询插件统计信息
        val result = atomFailInfoDao.queryAtomErrorCodeStatisticsInfo(
            dslContext,
            QueryAtomFailInfoQO(
                projectId = queryAtomFailInfoDTO.projectId,
                BaseQueryReqVO(
                    pipelineIds = queryAtomFailInfoDTO.pipelineIds,
                    pipelineLabelIds = queryAtomFailInfoDTO.pipelineLabelIds,
                    startTime = queryAtomFailInfoDTO.startTime,
                    endTime = queryAtomFailInfoDTO.endTime
                ),
                errorTypes = queryAtomFailInfoDTO.errorTypes,
                errorCodes = queryAtomFailInfoDTO.errorCodes,
                atomCodes = queryAtomFailInfoDTO.atomCodes,
                page = queryAtomFailInfoDTO.page,
                pageSize = queryAtomFailInfoDTO.pageSize
            )
        )
        // 对查询结果处理封装
        val atomErrorCodeStatisticsInfos = result.map {
            val errorType = it[BK_ERROR_TYPE] as Int
            val errorMsg = it[BK_ERROR_MSG]
            AtomErrorCodeStatisticsInfoDO(
                ErrorCodeInfoDO(
                    errorType = errorType,
                    errorTypeName = getErrorTypeName(errorType),
                    errorCode = it[BK_ERROR_CODE] as Int,
                    errorMsg = if (errorMsg != null) errorMsg as String else ""
                ),
                errorCount = (it[BK_ERROR_COUNT] as Int).toLong()
            )
        }
        return atomErrorCodeStatisticsInfos
    }

    override fun queryPipelineFailDetailInfo(
        queryAtomFailInfoDTO: QueryAtomFailInfoDTO
    ): Page<AtomFailDetailInfoDO> {
        // 查询符合查询条件的记录数
        val pipelineFailDetailCount = atomFailInfoDao.queryAtomFailDetailCount(
            dslContext,
            QueryAtomFailInfoQO(
                projectId = queryAtomFailInfoDTO.projectId,
                BaseQueryReqVO(
                    pipelineIds = queryAtomFailInfoDTO.pipelineIds,
                    pipelineLabelIds = queryAtomFailInfoDTO.pipelineLabelIds,
                    startTime = queryAtomFailInfoDTO.startTime,
                    endTime = queryAtomFailInfoDTO.endTime
                ),
                errorTypes = queryAtomFailInfoDTO.errorTypes,
                errorCodes = queryAtomFailInfoDTO.errorCodes,
                atomCodes = queryAtomFailInfoDTO.atomCodes
            )
        )
        // 查询记录过多，提醒用户缩小查询范围
        if (pipelineFailDetailCount > metricsConfig.queryCountMax) {
            throw ErrorCodeException(
                errorCode = MetricsMessageCode.QUERY_DETAILS_COUNT_BEYOND,
                params = arrayOf("${metricsConfig.queryCountMax}")
            )
        }
        //  查询插件失败详情数据
        val result = atomFailInfoDao.queryAtomFailDetailInfo(
            dslContext,
            QueryAtomFailInfoQO(
                projectId = queryAtomFailInfoDTO.projectId,
                BaseQueryReqVO(
                    pipelineIds = queryAtomFailInfoDTO.pipelineIds,
                    pipelineLabelIds = queryAtomFailInfoDTO.pipelineLabelIds,
                    startTime = queryAtomFailInfoDTO.startTime,
                    endTime = queryAtomFailInfoDTO.endTime
                ),
                errorTypes = queryAtomFailInfoDTO.errorTypes,
                errorCodes = queryAtomFailInfoDTO.errorCodes,
                atomCodes = queryAtomFailInfoDTO.atomCodes,
                page = queryAtomFailInfoDTO.page,
                pageSize = queryAtomFailInfoDTO.pageSize
            )
        )
        result.forEach {
            it.errorTypeName = getErrorTypeName(it.errorType!!)
            it.domain = MetricsUtils.getDomain(it.channelCode)
        }
        return Page(
            page = queryAtomFailInfoDTO.page,
            pageSize = queryAtomFailInfoDTO.pageSize,
            count = pipelineFailDetailCount,
            records = result
        )
    }
}
