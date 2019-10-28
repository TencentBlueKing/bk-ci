package com.tencent.devops.support.resources.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.user.UserNoticeResource
import com.tencent.devops.support.model.app.pojo.Notice
import com.tencent.devops.support.services.NoticeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserNoticeResourceImpl @Autowired constructor(private val noticeService: NoticeService) : UserNoticeResource {
    override fun getValidNotice(): Result<Notice?> {
        return Result(data = noticeService.getValidNotice())
    }

    override fun getAllNotice(): Result<List<Notice>> {
        return Result(data = noticeService.getAllNotice())
    }
}