/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.store.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.api.ServiceBcsResource
import com.tencent.devops.store.config.ExtServiceBcsConfig
import com.tencent.devops.store.config.ExtServiceBcsNameSpaceConfig
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.dao.common.StoreReleaseDao
import com.tencent.devops.store.pojo.QueryServiceFeatureParam
import com.tencent.devops.store.pojo.common.StoreReleaseCreateRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import io.fabric8.kubernetes.client.internal.readiness.Readiness
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ExtServiceCronService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val extServiceDao: ExtServiceDao,
    private val extServiceFeatureDao: ExtServiceFeatureDao,
    private val extServiceBcsConfig: ExtServiceBcsConfig,
    private val serviceNotifyService: ExtServiceNotifyService,
    private val extServiceBcsService: ExtServiceBcsService,
    private val storeReleaseDao: StoreReleaseDao,
    private val extServiceBcsNameSpaceConfig: ExtServiceBcsNameSpaceConfig
) {

    private val logger = LoggerFactory.getLogger(ExtServiceCronService::class.java)

    private val EXTENSION_RELEASE_SUCCESS_TEMPLATE = "EXTENSION_RELEASE_SUCCESS_TEMPLATE" // 扩展服务发布成功消息通知模板

    private val EXTENSION_RELEASE_FAIL_TEMPLATE = "EXTENSION_RELEASE_FAIL_TEMPLATE" // 扩展服务发布失败消息通知模板

    private final val bcsDeployRedisPrefixKey = "ext:service:deploy"

    @Scheduled(cron = "0 */1 * * * ?")
    fun updateReleaseDeployStatus() {
        // 一次查20条处于“正式发布部署中”状态的扩展服务数据
        val redisLock = RedisLock(redisOperation, "updateReleaseDeployStatus", 60L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("updateReleaseDeployStatus start")
                val serviceRecords = extServiceDao.listServiceByStatus(
                    dslContext = dslContext,
                    serviceStatus = ExtServiceStatusEnum.RELEASE_DEPLOYING,
                    page = 1,
                    pageSize = 20,
                    timeDescFlag = false
                )
                if (serviceRecords == null || serviceRecords.isEmpty()) {
                    return
                }
                val serviceCodes = serviceRecords.map { it.serviceCode }.toSet().joinToString(",")
                // 批量获取扩展服务部署信息
                val serviceDeploymentMap = client.get(ServiceBcsResource::class).getBcsDeploymentInfos(
                    namespaceName = extServiceBcsNameSpaceConfig.namespaceName,
                    deploymentNames = serviceCodes,
                    bcsUrl = extServiceBcsConfig.masterUrl,
                    token = extServiceBcsConfig.token
                ).data
                logger.info("serviceDeploymentMap:$serviceDeploymentMap")
                if (serviceDeploymentMap == null) {
                    return
                }
                val deployTimeOut = extServiceBcsConfig.deployTimeOut.toInt()
                serviceRecords.forEach {
                    val serviceCode = it.serviceCode
                    val deployment = serviceDeploymentMap[serviceCode]
                    val bcsDeployRedisKey = "$bcsDeployRedisPrefixKey:$serviceCode"
                    if (Readiness.isDeploymentReady(deployment)) {
                        it.serviceStatus = ExtServiceStatusEnum.RELEASED.status.toByte()
                        // 发布相关逻辑
                        deployService(serviceCode, it.modifier)
                        it.latestFlag = true
                        redisOperation.delete(bcsDeployRedisKey)
                        // 发送版本发布通知消息
                        serviceNotifyService.sendServiceReleaseNotifyMessage(
                            serviceId = it.id,
                            sendAllAdminFlag = true,
                            templateCode = EXTENSION_RELEASE_SUCCESS_TEMPLATE
                        )
                    } else {
                        val bcsFirstDeployTime = redisOperation.get(bcsDeployRedisKey)
                        if (bcsFirstDeployTime != null) {
                            // 轮询超时则把状态置为部署失败
                            if ((System.currentTimeMillis() - bcsFirstDeployTime.toLong()) > deployTimeOut * 60 * 1000) {
                                it.serviceStatus = ExtServiceStatusEnum.RELEASE_DEPLOY_FAIL.status.toByte()
                                redisOperation.delete(bcsDeployRedisKey)
                                // 发送版本发布邮件
                                serviceNotifyService.sendServiceReleaseNotifyMessage(
                                    serviceId = it.id,
                                    sendAllAdminFlag = false,
                                    templateCode = EXTENSION_RELEASE_FAIL_TEMPLATE
                                )
                            }
                        } else {
                            // 首次部署的时间存入redis
                            redisOperation.set(
                                key = bcsDeployRedisKey,
                                value = System.currentTimeMillis().toString(),
                                expiredInSecond = 3600
                            )
                        }
                    }
                }
                // 批量更新扩展服务的状态
                extServiceDao.batchUpdateService(dslContext, serviceRecords)
            } else {
                logger.info("updateReleaseDeployStatus is running")
            }
        } catch (ignored: Throwable) {
            logger.error("BKSystemErrorMonitor|updateReleaseDeployStatus|error=${ignored.message}", ignored)
        } finally {
            redisLock.unlock()
        }
    }

    private fun deployService(serviceCode: String, userId: String) {
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 清空旧版本LATEST_FLAG
            extServiceDao.cleanLatestFlag(context, serviceCode)
            // 记录发布信息
            val pubTime = LocalDateTime.now()
            storeReleaseDao.addStoreReleaseInfo(
                dslContext = context,
                userId = userId,
                storeReleaseCreateRequest = StoreReleaseCreateRequest(
                    storeCode = serviceCode,
                    storeType = StoreTypeEnum.SERVICE,
                    latestUpgrader = userId,
                    latestUpgradeTime = pubTime
                )
            )
        }
    }

    @Scheduled(cron = "0 */10 * * * ?")
    fun killGrayApp() {
        // 一次查20条处于“停止灰度环境标识为true且停止部署标记时间超过指定时间”状态的扩展服务数据
        val redisLock = RedisLock(redisOperation, "killGrayApp", 60L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("killGrayApp start")
                val serviceFeatureRecords = extServiceFeatureDao.getExtFeatureServices(
                    dslContext = dslContext,
                    queryServiceFeatureParam = QueryServiceFeatureParam(
                        deleteFlag = false,
                        killGrayAppFlag = true,
                        killGrayAppIntervalTime = extServiceBcsConfig.killGrayAppIntervalTime.toLong(),
                        page = 1,
                        pageSize = 20
                    )
                )
                if (serviceFeatureRecords == null || serviceFeatureRecords.isEmpty()) {
                    return
                }
                serviceFeatureRecords.forEach {
                    val serviceCode = it.serviceCode
                    // 停止bcs灰度命名空间的应用
                    val bcsStopAppResult = extServiceBcsService.stopExtService(
                        userId = it.modifier,
                        serviceCode = serviceCode,
                        deploymentName = serviceCode,
                        serviceName = "$serviceCode-service",
                        checkPermissionFlag = false,
                        grayFlag = true
                    )
                    logger.info("service[$serviceCode] bcsStopAppResult is :$bcsStopAppResult")
                    if (bcsStopAppResult.isOk()) {
                        // 灰度环境应用停止部署成功，则把灰度环境停止部署标志更新为null
                        it.killGrayAppFlag = null
                        it.killGrayAppMarkTime = null
                    }
                }
                extServiceFeatureDao.batchUpdateServiceFeature(dslContext, serviceFeatureRecords)
            } else {
                logger.info("killGrayApp is running")
            }
        } catch (ignored: Throwable) {
            logger.error("BKSystemErrorMonitor|killGrayApp|error=${ignored.message}", ignored)
        } finally {
            redisLock.unlock()
        }
    }
}
