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

package com.tencent.devops.project.service

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.project.tables.records.TNoticeRecord
import com.tencent.devops.project.dao.NoticeDao
import com.tencent.devops.project.pojo.Notice
import com.tencent.devops.project.pojo.NoticeRequest
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class NoticeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val noticeDao: NoticeDao
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(NoticeService::class.java)
    }

    fun getValidNotice(): Notice? {
        val notice = noticeDao.getValidNotice(dslContext)
        return if (notice == null) {
            null
        } else {
            convertNotice(notice)
        }
    }

    private fun handleNoticeList(notices: Result<TNoticeRecord>?, noticeList: MutableList<Notice>) {
        notices?.forEach {
            noticeList.add(
                Notice(
                    id = it.id,
                    noticeTitle = it.noticeTitle,
                    effectDate = it.effectDate.timestampmilli(),
                    invalidDate = it.invalidDate.timestampmilli(),
                    createDate = it.createDate.timestampmilli(),
                    updateDate = it.updateDate.timestampmilli(),
                    noticeContent = it.noticeContent,
                    redirectUrl = it.redirectUrl,
                    noticeType = it.noticeType.toInt(),
                    noticeService = if (it.serviceName.isNullOrBlank()) null else it.serviceName?.split(",")
                )
            )
        }
    }

    fun getAllNotice(): List<Notice> {
        val noticeList = mutableListOf<Notice>()
        val notices = noticeDao.getAllNotice(dslContext)
        handleNoticeList(notices, noticeList)
        return noticeList
    }

    fun getNotice(id: Long): Notice? {
        val notice = noticeDao.getNotice(dslContext, id)
        return if (notice == null) {
            null
        } else {
            convertNotice(notice)
        }
    }

    fun handleNotice(id: Long? = null, noticeRequest: NoticeRequest): Int {
        return noticeDao.handleNotice(dslContext, id, noticeRequest)
    }

    fun deleteNotice(id: Long): Int {
        return noticeDao.deleteNotice(dslContext, id)
    }

    fun convertNotice(noticeRecord: TNoticeRecord): Notice {
        return Notice(
            id = noticeRecord.id,
            noticeTitle = noticeRecord.noticeTitle,
            effectDate = noticeRecord.effectDate.timestampmilli(),
            invalidDate = noticeRecord.invalidDate.timestampmilli(),
            createDate = noticeRecord.createDate.timestampmilli(),
            updateDate = noticeRecord.updateDate.timestampmilli(),
            noticeContent = noticeRecord.noticeContent,
            redirectUrl = noticeRecord.redirectUrl,
            noticeType = noticeRecord.noticeType.toInt(),
            noticeService = if (noticeRecord.serviceName.isNullOrBlank()) null
            else noticeRecord.serviceName?.split(",")
        )
    }
}
