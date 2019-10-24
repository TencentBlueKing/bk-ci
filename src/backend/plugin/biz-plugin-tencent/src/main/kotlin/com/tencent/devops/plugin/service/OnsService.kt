package com.tencent.devops.plugin.service

import com.omg.ons.NameSvcApi
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.plugin.pojo.ons.OnsNameInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class OnsService @Autowired constructor() {
    companion object {
        private val logger = LoggerFactory.getLogger(OnsService::class.java)
    }

    /**
     * 获取无状态名字信息
     */
    fun getOnsNameInfo(domainName: String): Result<OnsNameInfo?> {
        logger.info("the domainName is:$domainName")
        val ent = NameSvcApi.getHostByKey(domainName)
        logger.info("the hostInfo is:$ent")
        return if (ent.code == 0) {
            Result(OnsNameInfo(ip = ent.ip, port = ent.port))
        } else {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR) // 获取名字服务失败，抛出错误提示
        }
    }
}
