/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.store.common.service.impl

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.constant.GOLANG
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.atom.factory.AtomBusHandleFactory
import com.tencent.devops.store.common.dao.StorePkgRunEnvInfoDao
import com.tencent.devops.store.common.service.AbstractDomainService
import com.tencent.devops.store.common.service.StorePkgRunEnvInfoService
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.env.StorePkgRunEnvInfo
import com.tencent.devops.store.pojo.common.env.StorePkgRunEnvRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class StorePkgRunEnvInfoServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storePkgRunEnvInfoDao: StorePkgRunEnvInfoDao
) : StorePkgRunEnvInfoService {

    companion object {
        private val logger = LoggerFactory.getLogger(StorePkgRunEnvInfoServiceImpl::class.java)
    }

    private val pkgRunEnvInfoCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(2, TimeUnit.HOURS)
        .build<String, StorePkgRunEnvInfo>()

    override fun create(userId: String, storePkgRunEnvRequest: StorePkgRunEnvRequest): Boolean {
        storePkgRunEnvInfoDao.add(dslContext, userId, storePkgRunEnvRequest)
        return true
    }

    override fun update(userId: String, id: String, storePkgRunEnvRequest: StorePkgRunEnvRequest): Boolean {
        storePkgRunEnvInfoDao.update(
            dslContext = dslContext,
            id = id,
            userId = userId,
            storePkgRunEnvRequest = storePkgRunEnvRequest
        )
        return true
    }

    override fun delete(userId: String, id: String): Boolean {
        storePkgRunEnvInfoDao.delete(dslContext, id)
        return true
    }

    override fun getStorePkgRunEnvInfo(
        devopsEnv: String?,
        userId: String,
        storeType: StoreTypeEnum,
        language: String,
        osName: String,
        osArch: String,
        runtimeVersion: String
    ): StorePkgRunEnvInfo? {
        // 把用户传的运行时版本号转换成平台适配的版本号
        val versionParts = runtimeVersion.split(".")
        val convertRuntimeVersion = if (versionParts.size > 1) {
            if (language == GOLANG) {
                // Go 版本号保留 major.minor 两段（如 1.23），不截断
                "${versionParts[0]}.${versionParts[1]}"
            } else {
                // 其他语言只需取主版本号去匹配
                "${versionParts[0]}."
            }
        } else {
            runtimeVersion
        }
        logger.info(
            "getStorePkgRunEnvInfo: requested=$runtimeVersion, " +
                "converted=$convertRuntimeVersion, " +
                "language=$language, osName=$osName, osArch=$osArch"
        )
        val atomBusHandleService = AtomBusHandleFactory.createAtomBusHandleService(language)
        val finalOsName = atomBusHandleService.handleOsName(osName)
        val finalOsArch = atomBusHandleService.handleOsArch(osName, osArch)
        val key = "PkgRunEnvInfo:$devopsEnv:$storeType:$language:$finalOsName:$finalOsArch:$convertRuntimeVersion"
        var storePkgRunEnvInfo = pkgRunEnvInfoCache.getIfPresent(key)
        if (storePkgRunEnvInfo != null) {
            logger.info(
                "getStorePkgRunEnvInfo cache hit: key=$key, " +
                    "cached runtimeVersion=${storePkgRunEnvInfo.runtimeVersion}, " +
                    "pkgName=${storePkgRunEnvInfo.pkgName}"
            )
            return storePkgRunEnvInfo
        }
        // 获取安装包运行时环境信息记录，根据运行时版本号未查到就取默认记录
        logger.info(
            "getStorePkgRunEnvInfo db query: storeType=$storeType, language=$language, " +
                "osName=$finalOsName, osArch=$finalOsArch, runtimeVersion=$convertRuntimeVersion"
        )
        val storePkgRunEnvInfoRecord = storePkgRunEnvInfoDao.getStorePkgRunEnvInfo(
            dslContext = dslContext,
            storeType = storeType.type.toByte(),
            language = language,
            osName = finalOsName,
            osArch = finalOsArch,
            runtimeVersion = convertRuntimeVersion
        ) ?: storePkgRunEnvInfoDao.getDefaultStorePkgRunEnvInfo(
            dslContext = dslContext,
            storeType = storeType.type.toByte(),
            language = language,
            osName = finalOsName,
            osArch = finalOsArch
        )
        return if (storePkgRunEnvInfoRecord == null) {
            logger.warn(
                "getStorePkgRunEnvInfo: no record found, " +
                    "requested=$runtimeVersion, converted=$convertRuntimeVersion"
            )
            null
        } else {
            storePkgRunEnvInfo = storePkgRunEnvInfoDao.convert(storePkgRunEnvInfoRecord)
            logger.info(
                "getStorePkgRunEnvInfo db result: " +
                    "db runtimeVersion=${storePkgRunEnvInfo.runtimeVersion}, " +
                    "pkgName=${storePkgRunEnvInfo.pkgName}, " +
                    "pkgDownloadPath=${storePkgRunEnvInfo.pkgDownloadPath}, " +
                    "defaultFlag=${storePkgRunEnvInfo.defaultFlag}"
            )
            if (!devopsEnv.isNullOrBlank()) {
                // 转换域名
                val beanName = "${devopsEnv.uppercase()}_DOMAIN_SERVICE"
                if (SpringContextUtil.isBeanExist(beanName)) {
                    val domainService = SpringContextUtil.getBean(AbstractDomainService::class.java, beanName)
                    storePkgRunEnvInfo.pkgDownloadPath = domainService.convertDomain(storePkgRunEnvInfo.pkgDownloadPath)
                }
            }
            // 把安装包运行时环境信息放入缓存
            pkgRunEnvInfoCache.put(key, storePkgRunEnvInfo)
            storePkgRunEnvInfo
        }
    }
}
