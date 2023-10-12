package com.tencent.devops.environment.service.job

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("ParseHashListService")
class ParseHashListService {
    fun getDynamicGroupList(envHashIdList: List<String>?): List<String> {
        if (!envHashIdList.isNullOrEmpty()) {
            val trimDynamicGroupList = discardBlankHashId(hashIdList = envHashIdList)

            return emptyList()
        } else {
            logger.warn("[getDynamicGroupList] envHashIdList is null or empty.")
            return emptyList()
        }
    }

    fun getTopoNodeList(nodeHashIdList: List<String>?): List<String> {
        if (!nodeHashIdList.isNullOrEmpty()) {
            val trimDynamicGroupList = discardBlankHashId(hashIdList = nodeHashIdList)

            return emptyList()
        } else {
            logger.warn("[getTopoNodeList] nodeHashIdList is null or empty.")
            return emptyList()
        }
    }

    private fun discardBlankHashId(hashIdList: List<String>): MutableSet<String> {
        val trimBlankHashIdList = mutableSetOf<String>()
        for (hashId in hashIdList) {
            if (hashId.isNotBlank()) {
                trimBlankHashIdList.add(hashId)
            }
        }
        return trimBlankHashIdList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ParseHashListService::class.java)
    }
}