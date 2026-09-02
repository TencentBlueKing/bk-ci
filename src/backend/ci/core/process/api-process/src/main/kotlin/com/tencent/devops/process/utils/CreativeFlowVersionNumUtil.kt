package com.tencent.devops.process.utils

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.process.constant.ProcessMessageCode
import org.slf4j.LoggerFactory

object CreativeFlowVersionNumUtil {
    private val logger = LoggerFactory.getLogger(CreativeFlowVersionNumUtil::class.java)
    private val PATTERN = Regex("^[Vv]?(\\d+)$")

    /**
     * "V208" / "208" -> 208；非法抛 ERROR_CREATIVE_FLOW_VERSION_NUM_INVALID
     */
    fun parse(versionNum: String): Int {
        val match = PATTERN.matchEntire(versionNum.trim())
            ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_VERSION_NUM_INVALID,
                params = arrayOf(versionNum)
            )
        val num = match.groupValues[1].toIntOrNull()
            ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_VERSION_NUM_INVALID,
                params = arrayOf(versionNum)
            )
        if (!versionNum.trim().startsWith("V", ignoreCase = false)) {
            logger.warn("CreativeFlowVersionNumUtil|parse|bare number accepted: $versionNum -> $num")
        }
        return num
    }

    /** 208 -> "V208" */
    fun format(versionNum: Int): String = "V$versionNum"
}
