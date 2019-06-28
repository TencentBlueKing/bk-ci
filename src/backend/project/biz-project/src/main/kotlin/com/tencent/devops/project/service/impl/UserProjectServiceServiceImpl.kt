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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.project.service.impl

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.model.project.tables.records.TServiceRecord
import com.tencent.devops.project.dao.FavoriteDao
import com.tencent.devops.project.dao.ServiceDao
import com.tencent.devops.project.dao.ServiceTypeDao
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.service.OPPServiceVO
import com.tencent.devops.project.pojo.service.ServiceCreateInfo
import com.tencent.devops.project.pojo.service.ServiceListVO
import com.tencent.devops.project.pojo.service.ServiceVO
import com.tencent.devops.project.service.UserProjectServiceService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserProjectServiceServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val serviceTypeDao: ServiceTypeDao,
    private val serviceDao: ServiceDao,
    private val favoriteDao: FavoriteDao,
    private val gray: Gray,
    private val redisOperation: RedisOperation
) : UserProjectServiceService {

    override fun getService(userId: String, serviceId: Long): Result<ServiceVO> {
        val tServiceRecord = serviceDao.select(dslContext, serviceId)
        if (tServiceRecord != null) {
            return Result(
                ServiceVO(
                    tServiceRecord.id ?: 0,
                    tServiceRecord.name,
                    tServiceRecord.link,
                    tServiceRecord.linkNew,
                    tServiceRecord.status, tServiceRecord.injectType,
                    tServiceRecord.iframeUrl,
                    tServiceRecord.cssUrl,
                    tServiceRecord.jsUrl,
                    tServiceRecord.grayCssUrl,
                    tServiceRecord.grayJsUrl,
                    tServiceRecord.showProjectList,
                    tServiceRecord.showNav,
                    tServiceRecord.projectIdType,
                    favoriteDao.countFavorite(dslContext, userId, tServiceRecord.id) > 0,
                    tServiceRecord.weight ?: 0
                )
            )
        } else {
            return Result(405, "无限ID,获取服务信息失败")
        }
    }

    /**
     * 修改服务
     */
    override fun updateService(userId: String, serviceId: Long, serviceCreateInfo: ServiceCreateInfo): Result<Boolean> {
        return Result(serviceDao.update(dslContext, userId, serviceId, serviceCreateInfo))
    }

    /**
     * 删除服务
     */
    override fun deleteService(userId: String, serviceId: Long): Result<Boolean> {
        return Result(serviceDao.delete(dslContext, userId, serviceId))
    }

    /**
     * 服务列表
     */
    override fun listOPService(userId: String): Result<List<OPPServiceVO>> {
        val tServiceList = serviceDao.getServiceList(dslContext)
        val serviceVOList = ArrayList<OPPServiceVO>()
        tServiceList.map { tServiceRecord ->
            serviceVOList.add(
                OPPServiceVO(
                    tServiceRecord.id,
                    tServiceRecord.name ?: "",
                    tServiceRecord.serviceTypeId,
                    tServiceRecord.showProjectList,
                    tServiceRecord.showNav,
                    tServiceRecord.status,
                    tServiceRecord.link,
                    tServiceRecord.linkNew,
                    tServiceRecord.injectType,
                    tServiceRecord.iframeUrl,
                    tServiceRecord.cssUrl,
                    tServiceRecord.jsUrl,
                    tServiceRecord.grayCssUrl,
                    tServiceRecord.grayJsUrl,
                    tServiceRecord.projectIdType,
                    tServiceRecord.createdUser ?: "",
                    DateTimeUtil.toDateTime(tServiceRecord.createdTime),
                    tServiceRecord.updatedUser ?: "",
                    DateTimeUtil.toDateTime(tServiceRecord.updatedTime)
                )
            )
        }

        return Result(serviceVOList)
    }

    /**
     * 添加服务
     */
    override fun createService(userId: String, serviceCreateInfo: ServiceCreateInfo): Result<OPPServiceVO> {
        val tServiceRecord = serviceDao.create(dslContext, userId, serviceCreateInfo)
        if (tServiceRecord != null) {
            return Result(
                OPPServiceVO(
                    tServiceRecord.id,
                    tServiceRecord.name ?: "",
                    tServiceRecord.serviceTypeId,
                    tServiceRecord.showProjectList,
                    tServiceRecord.showNav,
                    tServiceRecord.status,
                    tServiceRecord.link,
                    tServiceRecord.linkNew,
                    tServiceRecord.injectType,
                    tServiceRecord.iframeUrl,
                    tServiceRecord.cssUrl,
                    tServiceRecord.jsUrl,
                    tServiceRecord.grayCssUrl,
                    tServiceRecord.grayJsUrl,
                    tServiceRecord.projectIdType,
                    tServiceRecord.createdUser ?: "",
                    DateTimeUtil.toDateTime(tServiceRecord.createdTime),
                    tServiceRecord.updatedUser ?: "",
                    DateTimeUtil.toDateTime(tServiceRecord.updatedTime)
                )
            )
        }
        return Result(500, "服务添加失败")
    }

    /**
     * 修改服务关注
     */
    override fun updateCollected(userId: String, serviceId: Long, collector: Boolean): Result<Boolean> {
        if (collector) {
            if (favoriteDao.create(dslContext, userId, serviceId) > 0) {
                return Result(0, "服务收藏成功", "", true)
            }
        } else {
            if (favoriteDao.delete(dslContext, userId, serviceId) > 0) {
                return Result(0, "服务取消收藏成功", "", true)
            }
        }
        return Result(false)
    }

    override fun listService(userId: String, projectId: String?): Result<ArrayList<ServiceListVO>> {

        val startEpoch = System.currentTimeMillis()
        try {
            val serviceListVO = ArrayList<ServiceListVO>()

            val serviceTypeMap = serviceTypeDao.getAllIdAndTitle(dslContext)

            val groupService = serviceDao.getServiceList(dslContext).groupBy { it.serviceTypeId }

            val favorServices = favoriteDao.list(dslContext, userId).map { it.serviceId }.toList()

            serviceTypeMap.forEach { serviceType ->
                val typeId = serviceType.id
                val typeName = serviceType.title
                val services = ArrayList<ServiceVO>()

                val s = groupService[typeId]

                s?.forEach {
                    val status = it.status
                    val favor = favorServices.contains(it.id)
                    services.add(
                        ServiceVO(
                            it.id,
                            it.name ?: "",
                            it.link ?: "",
                            it.linkNew ?: "",
                            status,
                            it.injectType ?: "",
                            it.iframeUrl ?: "",
                            getCSSUrl(it, projectId),
                            getJSUrl(it, projectId),
                            it.grayCssUrl ?: "",
                            it.grayJsUrl ?: "",
                            it.showProjectList ?: false,
                            it.showNav ?: false,
                            it.projectIdType ?: "",
                            favor,
                            it.weight ?: 0
                        )
                    )
                }

                serviceListVO.add(
                    ServiceListVO(
                        typeName,
                        serviceType.weight ?: 0,
                        services.sortedByDescending { it.weigHt })
                )
            }

            return Result(0, "OK", serviceListVO)
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list services")
        }
    }

    // 获取CSS URL，包括灰度的
    private fun getCSSUrl(record: TServiceRecord, projectId: String?): String {
        return if (gray.isGray() && !projectId.isNullOrBlank()) {
            if (redisOperation.isMember(gray.getGrayRedisKey(), projectId!!)) {
                record.grayCssUrl ?: record.cssUrl
            } else {
                record.cssUrl
            }
        } else {
            record.cssUrl
        } ?: ""
    }

    // 获取 JS URL， 包括灰度的
    private fun getJSUrl(record: TServiceRecord, projectId: String?): String {
        return if (gray.isGray() && !projectId.isNullOrBlank()) {
            if (redisOperation.isMember(gray.getGrayRedisKey(), projectId!!)) {
                record.grayJsUrl ?: record.jsUrl
            } else {
                record.jsUrl
            }
        } else {
            record.jsUrl
        } ?: ""
    }

    override fun syncService(userId: String, services: List<ServiceListVO>) {
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            services.forEach {
                val type = serviceTypeDao.create(context, userId, it.title, it.weigHt)
                it.children.forEach { s ->
                    serviceDao.create(context, userId, type.id, s)
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserProjectServiceServiceImpl::class.java)
    }
}