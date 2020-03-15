package com.tencent.devops.project.service

import com.tencent.devops.common.api.pojo.Result
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
        val serviceRecodes =  projectServiceDao.getServiceList(dslContext)
        if(serviceRecodes != null) {
            for (serviceRecode in serviceRecodes) {
                serviceList.add(
                    ServiceVO(
                        id = serviceRecode.id,
                        name = serviceRecode.name,
                        link = serviceRecode.link,
                        linkNew = serviceRecode.linkNew,
                        status = serviceRecode.status,
                        injectType = serviceRecode.injectType,
                        iframeUrl = serviceRecode.iframeUrl,
                        cssUrl = serviceRecode.cssUrl,
                        jsUrl = serviceRecode.jsUrl,
                        grayCssUrl = serviceRecode.grayCssUrl,
                        grayJsUrl = serviceRecode.grayJsUrl,
                        showProjectList = serviceRecode.showProjectList,
                        showNav = serviceRecode.showNav,
                        projectIdType = serviceRecode.projectIdType,
                        collected = false,
                        weigHt = serviceRecode.weight,
                        logoUrl = serviceRecode.logoUrl,
                        webSocket = serviceRecode.webSocket,
                        grayIframeUrl = serviceRecode.grayIframeUrl
                    )
                )
            }
        }
        return Result(serviceList)
    }
}