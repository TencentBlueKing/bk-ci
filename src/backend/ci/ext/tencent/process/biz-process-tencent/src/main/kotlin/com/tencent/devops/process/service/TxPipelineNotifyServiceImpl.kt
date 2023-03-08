package com.tencent.devops.process.service

import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.engine.service.PipelineNotifyService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.notify.command.NotifyCmd
import com.tencent.devops.process.service.notify.TxNotifySendGroupMsgCmdImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxPipelineNotifyServiceImpl @Autowired constructor(
    override val buildVariableService: BuildVariableService,
    override val pipelineRepositoryService: PipelineRepositoryService
) : PipelineNotifyService(
    buildVariableService,
    pipelineRepositoryService
) {

    override fun addExtCmd(): MutableList<NotifyCmd>? {
        val cmdList = mutableListOf<NotifyCmd>()
        // 内部版扩展发送企业微信群通知
        cmdList.add(SpringContextUtil.getBean(TxNotifySendGroupMsgCmdImpl::class.java))
        return cmdList
    }

    companion object {
        val logger = LoggerFactory.getLogger(TxPipelineNotifyServiceImpl::class.java)
    }
}
