package com.tencent.devops.sign.service

import com.tencent.devops.sign.api.pojo.IpaSignInfo
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.io.File

@Component
class AsyncSignService(
    private val signService: SignService,
    private val signInfoService: SignInfoService
) {

    @Async
    fun asyncSign(
        resignId: String,
        ipaSignInfo: IpaSignInfo,
        ipaFile: File,
        taskExecuteCount: Int
    ) {
        try {
            logger.info("[$resignId] asyncSign|ipaSignInfo=$ipaSignInfo|taskExecuteCount=$taskExecuteCount")
            signService.signIpaAndArchive(resignId, ipaSignInfo, ipaFile, taskExecuteCount)
        } catch (e: Exception) {
            // 失败结束签名逻辑
            signInfoService.failResign(resignId, ipaSignInfo, taskExecuteCount)
            // 异步处理，所以无需抛出异常
            logger.error("[$resignId] asyncSign failed: $e")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AsyncSignService::class.java)
    }
}
