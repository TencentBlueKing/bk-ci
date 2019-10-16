package com.tencent.devops.support.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.op.OpNoticeResource
import com.tencent.devops.support.model.app.NoticeRequest
import com.tencent.devops.support.model.app.pojo.Notice
import com.tencent.devops.support.services.NoticeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpNoticeResourceImpl @Autowired constructor(private val noticeService: NoticeService) : OpNoticeResource {
    override fun updateNotice(id: Long, noticeRequest: NoticeRequest): Result<Int> {
        return Result(data = noticeService.handleNotice(id, noticeRequest))
    }

    override fun addNotice(noticeRequest: NoticeRequest): Result<Int> {
        return Result(data = noticeService.handleNotice(null, noticeRequest))
    }

    override fun getNotice(id: Long): Result<Notice?> {
        return Result(data = noticeService.getNotice(id))
    }

    override fun getAllNotice(): Result<List<Notice>> {
        return Result(data = noticeService.getAllNotice())
    }

    override fun deleteNotice(id: Long): Result<Int> {
        return Result(data = noticeService.deleteNotice(id))
    }
}