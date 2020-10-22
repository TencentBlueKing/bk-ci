package com.tencent.devops.common.auth.service.impl

import com.tencent.devops.common.auth.service.GongfengAuthTaskService
import com.tencent.devops.common.auth.pojo.GongfengBaseInfo
import org.springframework.stereotype.Service

@Service
class MockGongfengAuthTaskService: GongfengAuthTaskService {
    override fun getGongfengProjInfo(taskId: Long): GongfengBaseInfo? {
        return null
    }

    override fun getGongfengCIProjInfo(gongfengId: Int): GongfengBaseInfo? {
        return null
    }
}