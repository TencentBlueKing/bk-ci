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

package com.tencent.bkrepo.webhook.service.impl

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.webhook.dao.WebHookLogDao
import com.tencent.bkrepo.webhook.model.TWebHookLog
import com.tencent.bkrepo.webhook.pojo.ListWebHookLogOption
import com.tencent.bkrepo.webhook.pojo.WebHookLog
import com.tencent.bkrepo.webhook.service.LogService
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class LogServiceImpl(
    private val webHookLogDao: WebHookLogDao
) : LogService {

    override fun listLog(webHookId: String, option: ListWebHookLogOption): Page<WebHookLog> {
        with(option) {
            val query = Query(
                Criteria.where(TWebHookLog::webHookId.name).isEqualTo(webHookId)
                    .apply {
                        status?.let { and(TWebHookLog::status.name).isEqualTo(it) }
                        if (startDate != null && endDate != null) {
                            and(TWebHookLog::requestTime.name)
                                .gte(LocalDateTime.parse(startDate))
                                .lte(LocalDateTime.parse(endDate))
                        } else if (startDate != null) {
                            and(TWebHookLog::requestTime.name).gte(LocalDateTime.parse(startDate))
                        } else if (endDate != null) {
                            and(TWebHookLog::requestTime.name).lte(LocalDateTime.parse(endDate))
                        }
                    }
            )
            val pageRequest = Pages.ofRequest(pageNumber, pageSize)
            val sort = Sort.by(Sort.Direction.DESC, TWebHookLog::requestTime.name)
            val totalRecords = webHookLogDao.count(query)
            val records = webHookLogDao.find(query.with(sort).with(pageRequest)).map { transfer(it) }
            return Pages.ofResponse(pageRequest, totalRecords, records)
        }
    }

    override fun deleteLogBeforeDate(date: LocalDateTime): Long {
        return webHookLogDao.deleteByRequestTimeBefore(date).deletedCount
    }

    private fun transfer(tWebHookLog: TWebHookLog): WebHookLog {
        return WebHookLog(
            id = tWebHookLog.id!!,
            webHookUrl = tWebHookLog.webHookUrl,
            triggeredEvent = tWebHookLog.triggeredEvent,
            requestHeaders = tWebHookLog.requestHeaders,
            requestPayload = tWebHookLog.requestPayload,
            status = tWebHookLog.status,
            responseHeaders = tWebHookLog.responseHeaders,
            responseBody = tWebHookLog.responseBody,
            requestDuration = tWebHookLog.requestDuration,
            requestTime = tWebHookLog.requestTime,
            errorMsg = tWebHookLog.errorMsg
        )
    }
}
