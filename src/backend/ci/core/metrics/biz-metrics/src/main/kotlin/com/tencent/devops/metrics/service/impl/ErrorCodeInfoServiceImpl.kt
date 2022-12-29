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

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.client.Client
import com.tencent.devops.metrics.dao.AtomFailInfoDao
import com.tencent.devops.metrics.dao.ErrorCodeInfoDao
import com.tencent.devops.metrics.dao.MetricsDataReportDao
import com.tencent.devops.metrics.pojo.`do`.ErrorCodeInfoDO
import com.tencent.devops.metrics.pojo.dto.QueryErrorCodeInfoDTO
import com.tencent.devops.metrics.pojo.po.SaveErrorCodeInfoPO
import com.tencent.devops.metrics.pojo.po.UpdateErrorCodeInfoPO
import com.tencent.devops.metrics.pojo.qo.QueryErrorCodeInfoQO
import com.tencent.devops.metrics.service.ErrorCodeInfoManageService
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.Executors

@Service
class ErrorCodeInfoServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val errorCodeInfoDao: ErrorCodeInfoDao,
    private val atomFailInfoDao: AtomFailInfoDao,
    private val metricsDataReportDao: MetricsDataReportDao,
    private val client: Client
) : ErrorCodeInfoManageService {

    override fun getErrorCodeInfo(queryErrorCodeInfoDTO: QueryErrorCodeInfoDTO): Page<ErrorCodeInfoDO> {
        return Page(
            page = queryErrorCodeInfoDTO.page,
            pageSize = queryErrorCodeInfoDTO.pageSize,
            count = errorCodeInfoDao.getErrorCodeInfoCount(
                dslContext,
                QueryErrorCodeInfoQO(
                    atomCode = queryErrorCodeInfoDTO.atomCode,
                    errorTypes = queryErrorCodeInfoDTO.errorTypes,
                    keyword = queryErrorCodeInfoDTO.keyword,
                    page = queryErrorCodeInfoDTO.page,
                    pageSize = queryErrorCodeInfoDTO.pageSize
                )
            ),
            records = errorCodeInfoDao.getErrorCodeInfo(
                dslContext,
                QueryErrorCodeInfoQO(
                    atomCode = queryErrorCodeInfoDTO.atomCode,
                    errorTypes = queryErrorCodeInfoDTO.errorTypes,
                    keyword = queryErrorCodeInfoDTO.keyword,
                    page = queryErrorCodeInfoDTO.page,
                    pageSize = queryErrorCodeInfoDTO.pageSize
                )
            )
        )
    }

    override fun syncAtomErrorCodeRel(userId: String): Boolean {
        var offset = 0
        val limit = 10
        Executors.newFixedThreadPool(5).submit {
            do {
                val atomCodes = atomFailInfoDao.limitAtomCodes(dslContext, offset, limit)
                atomCodes.forEach { atomCode ->
                    val saveErrorCodeInfoPOs = getAtomErrorInfos(userId, atomCode)
                    saveErrorCodeInfoPOs.forEach {
                        try {
                            metricsDataReportDao.saveErrorCodeInfo(dslContext, it)
                        } catch (ignored: DuplicateKeyException) {
                            logger.warn("fail to update errorCodeInfo:$it", ignored)
                            metricsDataReportDao.updateErrorCodeInfo(
                                dslContext = dslContext,
                                atomCode = atomCode,
                                updateErrorCodeInfoPO = UpdateErrorCodeInfoPO(
                                    errorType = it.errorType,
                                    errorCode = it.errorCode,
                                    errorMsg = it.errorMsg,
                                    modifier = it.modifier,
                                    updateTime = LocalDateTime.now()
                                )
                            )
                        }
                    }
                }
                offset += limit
            } while (atomCodes.size < limit)
        }
        return true
    }

    private fun getAtomErrorInfos(userId: String, atomCode: String): List<SaveErrorCodeInfoPO> {
        val saveErrorCodeInfoPOs = atomFailInfoDao.getAtomErrorInfos(dslContext, atomCode).map {
            SaveErrorCodeInfoPO(
                id = client.get(ServiceAllocIdResource::class)
                    .generateSegmentId("METRICS_ERROR_CODE_INFO").data ?: 0,
                errorCode = it.value1(),
                errorType = it.value2(),
                errorMsg = it.value3(),
                creator = userId,
                modifier = userId,
                createTime = LocalDateTime.now(),
                updateTime = LocalDateTime.now(),
                atomCode = atomCode
            )
        }
        return saveErrorCodeInfoPOs
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ErrorCodeInfoManageService::class.java)
    }

}
