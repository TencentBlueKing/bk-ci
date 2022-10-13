package com.tencent.devops.turbo.sdk

import com.tencent.devops.common.api.util.OkhttpUtil
import com.tencent.devops.turbo.config.TBSProperties
import com.tencent.devops.web.util.SpringContextHolder
import org.slf4j.LoggerFactory


object BKDistccApi {

    private val logger = LoggerFactory.getLogger(BKDistccApi::class.java)


    /**
     * 查询编译加速工具版本清单
     */
    fun queryBkdistccVersion(): String {
        val properties = SpringContextHolder.getBean<TBSProperties>()
        val url = "${properties.rootPath}/api/v1/disttask/resource/version"
        logger.info("queryBkdistccVersion url: [$url]")
        return OkhttpUtil.doGet(url = url)
    }
}
