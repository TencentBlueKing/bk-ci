package com.tencent.devops.project.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.dao.ServiceDao
import com.tencent.devops.project.pojo.service.ServiceVO
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ServiceProjectService @Autowired constructor(
    private val projectServiceDao: ServiceDao,
    private val dslContext: DSLContext
) {
    fun getServiceList(): Result<List<ServiceVO>> {
        val serviceList = mutableListOf<ServiceVO>()
        val serviceRecodes = projectServiceDao.getServiceList(dslContext)
        if (serviceRecodes != null) {
            for (serviceRecode in serviceRecodes) {
                serviceList.add(
                    ServiceVO(
                        id = serviceRecode.id ?: 0,
                        name = MessageCodeUtil.getMessageByLocale(serviceRecode.name, serviceRecode.englishName),
                        link = serviceRecode.link ?: "",
                        linkNew = serviceRecode.linkNew ?: "",
                        status = serviceRecode.status,
                        injectType = serviceRecode.injectType ?: "",
                        iframeUrl = serviceRecode.iframeUrl ?: "",
                        grayIframeUrl = serviceRecode.grayIframeUrl ?: "",
                        cssUrl = serviceRecode.cssUrl ?: "",
                        jsUrl = serviceRecode.jsUrl ?: "",
                        grayCssUrl = serviceRecode.grayCssUrl ?: "",
                        grayJsUrl = serviceRecode.grayJsUrl ?: "",
                        showProjectList = serviceRecode.showProjectList ?: true,
                        showNav = serviceRecode.showNav ?: true,
                        projectIdType = serviceRecode.projectIdType ?: "",
                        collected = true,
                        weigHt = serviceRecode.weight ?: 0,
                        logoUrl = serviceRecode.logoUrl ?: "",
                        webSocket = serviceRecode.webSocket ?: ""
                    )
                )
            }
        }
        return Result(serviceList)
    }
}