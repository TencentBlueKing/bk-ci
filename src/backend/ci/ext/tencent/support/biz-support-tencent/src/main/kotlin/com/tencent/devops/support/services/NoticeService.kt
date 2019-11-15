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

package com.tencent.devops.support.services

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.support.tables.records.TNoticeRecord
import com.tencent.devops.support.dao.NoticeDao
import com.tencent.devops.support.model.app.NoticeRequest
import com.tencent.devops.support.model.app.pojo.Notice
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
    private val LOG = LoggerFactory.getLogger(NoticeService::class.java)

    fun getValidNotice(): Notice? {
        var notice = noticeDao.getValidNotice(dslContext)
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
                            it.id,
                            it.noticeTitle,
                            it.effectDate.timestampmilli(),
                            it.invalidDate.timestampmilli(),
                            it.createDate.timestampmilli(),
                            it.updateDate.timestampmilli(),
                            it.noticeContent,
                            it.redirectUrl
                    )
            )
        }
    }

    fun getAllNotice(): List<Notice> {
        val noticeList = mutableListOf<Notice>()
        var notices = noticeDao.getAllNotice(dslContext)
        handleNoticeList(notices, noticeList)
        return noticeList
    }

    fun getNotice(id: Long): Notice? {
        var notice = noticeDao.getNotice(dslContext, id)
        LOG.info("the notice is :{}", JsonUtil.getObjectMapper().writeValueAsString(notice))
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
                noticeRecord.id,
                noticeRecord.noticeTitle,
                noticeRecord.effectDate.timestampmilli(),
                noticeRecord.invalidDate.timestampmilli(),
                noticeRecord.createDate.timestampmilli(),
                noticeRecord.updateDate.timestampmilli(),
                noticeRecord.noticeContent,
                noticeRecord.redirectUrl
        )
    }
}
