package com.tencent.devops.support.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.NoticeResource
import com.tencent.devops.support.model.app.pojo.Notice
import com.tencent.devops.support.services.NoticeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class NoticeResourceImpl @Autowired constructor(private val noticeService: NoticeService) : NoticeResource {
    override fun getValidNotice(): Result<Notice?> {
        return Result(data = noticeService.getValidNotice())
    }

    override fun getAllNotice(): Result<List<Notice>> {
        return Result(data = noticeService.getAllNotice())
    }
}