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

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.dao.FavoriteDao
import com.tencent.devops.project.dao.GrayTestDao
import com.tencent.devops.project.dao.ServiceDao
import com.tencent.devops.project.dao.ServiceTypeDao
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.ServiceUpdateUrls
import com.tencent.devops.project.pojo.service.OPPServiceVO
import com.tencent.devops.project.pojo.service.ServiceCreateInfo
import com.tencent.devops.project.pojo.service.ServiceListVO
import com.tencent.devops.project.pojo.service.ServiceVO
import com.tencent.devops.project.service.tof.TOFService
import com.tencent.devops.project.utils.BG_IEG_ID
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class UserProjectServiceImpl @Autowired constructor(
        private val dslContext: DSLContext,
        private val serviceTypeDao: ServiceTypeDao,
        private val serviceDao: ServiceDao,
        private val grayTestDao: GrayTestDao,
        private val favoriteDao: FavoriteDao,
        private val gray: Gray,
        private val redisOperation: RedisOperation,
        private val tofService: TOFService
) : AbsUserProjectServiceServiceImpl(dslContext, serviceTypeDao, serviceDao, favoriteDao, gray, redisOperation) {

    @Value("\${project.container.domain:}")
    private lateinit var containerDomain: String?

    @Value("\${project.container.bgId:}")
    private lateinit var containerbgId: String?

    override fun getService(userId: String, serviceId: Long): Result<ServiceVO> {
        val tServiceRecord = serviceDao.select(dslContext, serviceId)
        if (tServiceRecord != null) {
            val isIEGMember = tofService.getUserDeptDetail(userId).bgId == BG_IEG_ID

            return Result(
                    ServiceVO(
                            id = tServiceRecord.id ?: 0,
                            name = tServiceRecord.name,
                            link = tServiceRecord.link,
                            linkNew = tServiceRecord.linkNew,
                            status = tServiceRecord.status,
                            injectType = tServiceRecord.injectType,
                            iframeUrl = tServiceRecord.iframeUrl,
                            grayIframeUrl = tServiceRecord.grayIframeUrl,
                            cssUrl = tServiceRecord.cssUrl,
                            jsUrl = tServiceRecord.jsUrl,
                            grayCssUrl = tServiceRecord.grayCssUrl,
                            grayJsUrl = tServiceRecord.grayJsUrl,
                            showProjectList = tServiceRecord.showProjectList,
                            showNav = tServiceRecord.showNav,
                            projectIdType = tServiceRecord.projectIdType,
                            collected = favoriteDao.countFavorite(dslContext, userId, tServiceRecord.id) > 0,
                            logoUrl = tServiceRecord.logoUrl,
                            webSocket = tServiceRecord.webSocket,
                            hidden = isServiceHidden(tServiceRecord.name, isIEGMember),
                            weigHt = tServiceRecord.weight ?: 0
                    )
            )
        } else {
            return Result(405, "无限ID,获取服务信息失败")
        }
    }

    override fun updateService(userId: String, serviceId: Long, serviceCreateInfo: ServiceCreateInfo): Result<Boolean> {
        return super.updateService(userId, serviceId, serviceCreateInfo)
    }

    override fun updateServiceUrls(
            userId: String,
            serviceUpdateUrls: List<ServiceUpdateUrls>
    ): Result<Int> {
        return super.updateServiceUrls(userId, serviceUpdateUrls)
    }

    override fun deleteService(userId: String, serviceId: Long): Result<Boolean> {
        return super.deleteService(userId, serviceId)
    }

    override fun listOPService(userId: String): Result<List<OPPServiceVO>> {
        return super.listOPService(userId)
    }

    override fun createService(userId: String, serviceCreateInfo: ServiceCreateInfo): Result<OPPServiceVO> {
        return super.createService(userId, serviceCreateInfo)
    }

    override fun updateCollected(userId: String, serviceId: Long, collector: Boolean): Result<Boolean> {
        return super.updateCollected(userId, serviceId, collector)
    }

    override fun listService(userId: String, projectId: String?, bkToken: String?): Result<ArrayList<ServiceListVO>> {
        logger.info("listService interface:userId[$userId],projectId[$projectId],bkToken[$bkToken]")

        val startEpoch = System.currentTimeMillis()
        try {
            val serviceListVO = ArrayList<ServiceListVO>()

            val serviceTypeMap = serviceTypeDao.getAllIdAndTitle(dslContext)

            val groupService = serviceDao.getServiceList(dslContext).groupBy { it.serviceTypeId }

            val favorServices = favoriteDao.list(dslContext, userId).map { it.serviceId }.toList()

            logger.info("listService interface containerDomain:$containerDomain")
            logger.info("listService interface containerbgId:$containerbgId")
            serviceTypeMap.forEach { serviceType ->
                val typeId = serviceType.id
                val typeName = MessageCodeUtil.getMessageByLocale(serviceType.title, serviceType.englishTitle)
                val services = ArrayList<ServiceVO>()

                val s = groupService[typeId]
                s?.forEach { it ->
                    val status = it.status
                    val favor = favorServices.contains(it.id)
                    var newWindow = false
                    var iframeUrl = genUrl(url = it.iframeUrl, grayUrl = it.grayIframeUrl, projectId = projectId)
                    if(it.name.contains("容器服务") && it.injectType.toLowerCase().trim().equals("iframe") && bkToken != null && containerDomain.isNullOrBlank() && containerbgId.isNullOrBlank()) {
                        logger.info("listService interface:enter container.")

                        val containerDomainList = containerDomain.split(",|;".toRegex())
                        val containerbgIdList = containerbgId.split(",|;".toRegex())
                        logger.info("listService interface containerDomainList:$containerDomainList")
                        logger.info("listService interface containerbgIdList:$containerbgIdList")
                        if(containerbgIdList.isNotEmpty() && containerDomainList.isNotEmpty() && containerDomainList.size == containerbgIdList.size) {
                            val userDeptDetail = tofService.getUserDeptDetail(userId, bkToken)
                            run breaking@ {
                                containerbgIdList.forEachIndexed { index, bgId  ->
                                    if(bgId == userDeptDetail.bgId) {
                                        iframeUrl = containerDomainList[index]
                                        newWindow = true
                                        return@breaking
                                    }
                                }
                            }
                        }

                    }
                    services.add(
                            ServiceVO(
                                    id = it.id,
                                    name = MessageCodeUtil.getMessageByLocale(it.name, it.englishName),
                                    link = it.link ?: "",
                                    linkNew = it.linkNew ?: "",
                                    status = status,
                                    injectType = it.injectType ?: "",
                                    iframeUrl = iframeUrl,
                                    grayIframeUrl = it.grayIframeUrl ?: "",
                                    cssUrl = genUrl(url = it.cssUrl, grayUrl = it.grayCssUrl, projectId = projectId),
                                    jsUrl = genUrl(url = it.jsUrl, grayUrl = it.grayJsUrl, projectId = projectId),
                                    grayCssUrl = it.grayCssUrl ?: "",
                                    grayJsUrl = it.grayJsUrl ?: "",
                                    showProjectList = it.showProjectList ?: false,
                                    showNav = it.showNav ?: false,
                                    projectIdType = it.projectIdType ?: "",
                                    collected = favor,
                                    weigHt = it.weight ?: 0,
                                    logoUrl = it.logoUrl,
                                    webSocket = it.webSocket,
                                    newWindow = newWindow

                            )
                    )
                }

                serviceListVO.add(
                        ServiceListVO(
                                title = typeName,
                                weigHt = serviceType.weight ?: 0,
                                children = services.sortedByDescending { it.weigHt })
                )
            }

            return Result(code = 0, message = "OK", data = serviceListVO)
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list services")
        }
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

    private fun isServiceHidden(serviceName: String, isIEGMember: Boolean): Boolean {
        return !(if (serviceName.contains("(Monitor)", false) || serviceName.contains("(BCS)", false)) {
            isIEGMember
        } else {
            true
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserProjectServiceImpl::class.java)
    }
}
