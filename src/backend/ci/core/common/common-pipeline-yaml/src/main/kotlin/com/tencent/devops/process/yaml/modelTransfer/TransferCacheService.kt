package com.tencent.devops.process.yaml.modelTransfer

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.client.Client
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.api.image.service.ServiceStoreImageResource
import com.tencent.devops.store.pojo.atom.ElementThirdPartySearchParam
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class TransferCacheService @Autowired constructor(
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TransferCacheService::class.java)
    }

    private val atomDefaultValueCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.DAYS)
        .build<String, Map<String, String>> { key ->
            kotlin.runCatching {
                val (atomCode, version) = key.split("@")
                client.get(ServiceMarketAtomResource::class)
                    .getAtomsDefaultValue(ElementThirdPartySearchParam(atomCode, version)).data
            }.onFailure { logger.warn("get $key default value error.") }.getOrNull() ?: emptyMap()
        }

    private val storeImageInfoCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.DAYS)
        .build<String, ImageRepoInfo?> { key ->
            kotlin.runCatching {
                val (imageCode, imageVersion) = key.split("@@")
                client.get(ServiceStoreImageResource::class)
                    .getImageInfoByCodeAndVersion(
                        imageCode = imageCode,
                        imageVersion = imageVersion
                    ).data
            }.onFailure { logger.warn("get $key default value error.") }.getOrNull()
        }

    fun getAtomDefaultValue(key: String) = atomDefaultValueCache.get(key) ?: emptyMap()

    fun getStoreImageInfo(imageCode: String, imageVersion: String?) =
        storeImageInfoCache.get("$imageCode@@${imageVersion ?: ""}")
}
