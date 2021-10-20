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

package com.tencent.devops.project.service.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.project.tables.records.TServiceRecord
import com.tencent.devops.project.dao.FavoriteDao
import com.tencent.devops.project.dao.ServiceDao
import com.tencent.devops.project.dao.ServiceTypeDao
import com.tencent.devops.project.pojo.Result
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
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletRequest

@Suppress("UNUSED")
@Service
class UserProjectServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val serviceTypeDao: ServiceTypeDao,
    private val serviceDao: ServiceDao,
    private val favoriteDao: FavoriteDao,
    gray: Gray,
    redisOperation: RedisOperation,
    private val tofService: TOFService
) : AbsUserProjectServiceServiceImpl(dslContext, serviceTypeDao, serviceDao, favoriteDao, gray, redisOperation) {

    @Value("\${project.container.url:#{null}}")
    private var containerUrl: String? = null

    @Value("\${project.container.iegUrl:#{null}}")
    private var containerIegUrl: String? = null

    @Value("\${project.container.bgId::#{null}}")
    private var containerbgId: String? = null

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

    override fun listService(userId: String, projectId: String?): Result<ArrayList<ServiceListVO>> {
        logger.info("listService interface:userId[$userId],projectId[$projectId]")

        val startEpoch = System.currentTimeMillis()
        try {
            val serviceListVO = ArrayList<ServiceListVO>()

            val serviceTypeMap = serviceTypeDao.getAllIdAndTitle(dslContext)

            val groupService = serviceDao.getServiceList(dslContext).groupBy { it.serviceTypeId }

            val favorServices = favoriteDao.list(dslContext, userId).map { it.serviceId }.toList()

            logger.info("listService interface containerUrl:$containerUrl")
            logger.info("listService interface containerbgId:$containerbgId")

            val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            val request = attributes?.request
            val bkToken = request?.getHeader(AUTH_HEADER_DEVOPS_BK_TOKEN)
            logger.info("listService interface request :$request")
            logger.info("listService interface bkToken :$bkToken")
            serviceTypeMap.forEach { serviceType ->
                val typeId = serviceType.id
                val typeName = MessageCodeUtil.getMessageByLocale(serviceType.title, serviceType.englishTitle)
                val services = ArrayList<ServiceVO>()

                val s = groupService[typeId]
                s?.forEach { it ->
                    val status = it.status
                    val favor = favorServices.contains(it.id)
//                    var newWindow = false
//                    var newWindowUrl = ""
                    val (newWindow, newWindowUrl) = getNewWindow(
                        it,
                        request,
                        bkToken,
                        userId
                    )
//                    newWindow = pair.first
//                    newWindowUrl = pair.second
                    services.add(
                        ServiceVO(
                            id = it.id,
                            name = MessageCodeUtil.getMessageByLocale(it.name, it.englishName),
                            link = it.link ?: "",
                            linkNew = it.linkNew ?: "",
                            status = status,
                            injectType = it.injectType ?: "",
                            iframeUrl = genUrl(url = it.iframeUrl, grayUrl = it.grayIframeUrl, projectId = projectId),
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
                            newWindow = newWindow,
                            newWindowUrl = newWindowUrl

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

    @SuppressWarnings("ComplexCondition", "ComplexMethod")
    private fun getNewWindow(
        tServiceRecord: TServiceRecord,
        request: HttpServletRequest?,
        bkToken: String?,
        userId: String
    ): Pair<Boolean, String> {
        var newWindow = false
        var newWindowUrl = ""
        if (tServiceRecord.name.contains("容器服务") &&
            tServiceRecord.injectType.toLowerCase().trim() == "iframe" &&
            request != null &&
            bkToken != null &&
            !containerUrl.isNullOrBlank() &&
            !containerbgId.isNullOrBlank()
        ) {
            logger.info("listService interface:enter container.")
            val containerUrlList = containerUrl!!.split("[,;]".toRegex())
            val containerBgIdList = containerbgId!!.split("[,;]".toRegex())
            logger.info("listService interface containerUrlList:$containerUrlList")
            logger.info("listService interface containerBgIdList:$containerBgIdList")
            if (containerBgIdList.isNotEmpty() &&
                containerUrlList.isNotEmpty() &&
                containerUrlList.size == containerBgIdList.size
            ) {
                val userDeptDetail = tofService.getUserDeptDetail(userId)
                run breaking@{
                    containerBgIdList.forEachIndexed { index, bgId ->
                        if (bgId == userDeptDetail.bgId) {
                            newWindowUrl = containerUrlList[index]
                            newWindow = true
                            return@breaking
                        }
                    }
                }
            }

            val originalHost = request.getHeader("Origin")

            logger.info("listService interface original hots:$originalHost")
            if (!containerIegUrl.isNullOrBlank() &&
                !originalHost.isNullOrBlank() &&
                originalHost.contains(containerIegUrl!!)
            ) {
                logger.info("listService interface change newWindow to false")
                newWindow = false
            }
        }
        return Pair(newWindow, newWindowUrl)
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
