package com.tencent.devops.process.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * TAPD 支持服务
 */
@Service
class TapdSupportService constructor() {

    private fun getStoryInfo(
        workspaceId: String,
        storyId: String
    ) {

    }

    companion object {
        private val logger = LoggerFactory.getLogger(TapdSupportService::class.java)
    }
}