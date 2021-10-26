package com.tencent.devops.support.robot

import com.tencent.devops.common.wechatwork.WechatWorkRobotService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WeworkRobotSendMsgService @Autowired constructor(
    val robotCustomConfig: WeworkRobotCustomConfig,
    val wechatWorkRobotService: WechatWorkRobotService
) {
    fun sendTextMsgByRobot(msg: String) {
        val url = "${robotCustomConfig.weworkUrl}/cgi-bin/webhook/send?key=${robotCustomConfig.robotKey}"
        wechatWorkRobotService.send(url, msg)
    }


    companion object {
        val logger = LoggerFactory.getLogger(WeworkRobotSendMsgService::class.java)
    }
}
