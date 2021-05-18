package com.tencent.bk.codecc.task.component

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.GONGFENG_CHECK_CONFIG
import com.tencent.devops.common.util.JsonUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class GongfengCheckConfigCache @Autowired constructor(
    private val baseDataRepository: BaseDataRepository
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GongfengCheckConfigCache::class.java)
    }

    private val cache: LoadingCache<String, Map<String, List<String>>> = CacheBuilder.newBuilder()
            .maximumSize(10)
            .refreshAfterWrite(60, TimeUnit.MINUTES)
            .build(object : CacheLoader<String, Map<String, List<String>>>() {
                override fun load(key: String): Map<String, List<String>> {
                    return loadCheckConfigCache(key)
                }
            })

    private fun loadCheckConfigCache(key: String): Map<String, List<String>> {
        val baseDataEntity = baseDataRepository.findByParamTypeAndParamCode(GONGFENG_CHECK_CONFIG, key) ?: return emptyMap()
        val listMap = JsonUtil.to(baseDataEntity.paramValue,
                object : TypeReference<Map<String, List<String>>>() {})
        logger.info("get cache: $key $baseDataEntity $listMap")
        return listMap
    }

    fun getConfigCache(key: ComConstants.OpenSourceDisableReason): Map<String, List<String>>? {
        return cache.get(key.code.toString())
    }
}
