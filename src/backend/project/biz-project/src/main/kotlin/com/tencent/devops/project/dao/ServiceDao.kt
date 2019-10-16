package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TService
import com.tencent.devops.model.project.tables.records.TServiceRecord
import com.tencent.devops.project.pojo.ServiceUpdateUrls
import com.tencent.devops.project.pojo.service.ServiceCreateInfo
import com.tencent.devops.project.pojo.service.ServiceUrlUpdateInfo
import com.tencent.devops.project.pojo.service.ServiceVO
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ServiceDao {

    fun getServiceList(dslContext: DSLContext): Result<TServiceRecord> {
        with(TService.T_SERVICE) {
            return dslContext.selectFrom(this)
                .where(DELETED.eq(false))
                .fetch()
        }
    }

    fun create(
        dslContext: DSLContext,
        userId: String,
        typeId: Long,
        serviceVO: ServiceVO
    ) {
        with(TService.T_SERVICE) {
            dslContext.insertInto(
                this,
                ID,
                NAME,
                SERVICE_TYPE_ID,
                LINK,
                LINK_NEW,
                STATUS,
                INJECT_TYPE,
                IFRAME_URL,
                CSS_URL,
                JS_URL,
                GRAY_CSS_URL,
                GRAY_JS_URL,
                SHOW_PROJECT_LIST,
                SHOW_NAV,
                PROJECT_ID_TYPE,
                CREATED_USER,
                DELETED
            )
                .values(
                    serviceVO.id,
                    serviceVO.name,
                    typeId,
                    serviceVO.link,
                    serviceVO.linkNew,
                    serviceVO.status,
                    serviceVO.injectType,
                    serviceVO.iframeUrl,
                    serviceVO.cssUrl,
                    serviceVO.jsUrl,
                    serviceVO.grayCssUrl,
                    serviceVO.grayJsUrl,
                    serviceVO.showProjectList,
                    serviceVO.showNav,
                    serviceVO.projectIdType,
                    userId,
                    false
                )
                .execute()
        }
    }

    fun create(dslContext: DSLContext, userId: String, serviceCreateInfo: ServiceCreateInfo): TServiceRecord? {
        with(TService.T_SERVICE) {
            return dslContext.insertInto(
                this,
                NAME,
                SERVICE_TYPE_ID,
                LINK,
                LINK_NEW,
                INJECT_TYPE,
                IFRAME_URL,
                CSS_URL,
                JS_URL,
                GRAY_CSS_URL,
                GRAY_JS_URL,
                SHOW_PROJECT_LIST,
                SHOW_NAV,
                PROJECT_ID_TYPE,
                STATUS,
                CREATED_USER,
                CREATED_TIME,
                DELETED,
                LOGO_URL,
                WEB_SOCKET
            ).values(
                serviceCreateInfo.name,
                serviceCreateInfo.serviceTypeId,
                serviceCreateInfo.link,
                serviceCreateInfo.linkNew,
                serviceCreateInfo.injectType,
                serviceCreateInfo.iframeUrl,
                serviceCreateInfo.cssUrl,
                serviceCreateInfo.jsUrl,
                serviceCreateInfo.grayCssUrl,
                serviceCreateInfo.grayJsUrl,
                serviceCreateInfo.showProjectList,
                serviceCreateInfo.showNav,
                serviceCreateInfo.projectIdType,
                serviceCreateInfo.status,
                userId,
                LocalDateTime.now(),
                false,
                serviceCreateInfo.logoUrl,
                serviceCreateInfo.webSocket
            ).returning().fetchOne()
        }
    }

    fun delete(dslContext: DSLContext, userId: String, serviceId: Long): Boolean {
        with(TService.T_SERVICE) {
            val execute = dslContext.update(this)
                .set(DELETED, true)
                .set(UPDATED_USER, userId)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(ID.eq(serviceId)).execute()
            return execute > 0
        }
    }

    fun update(dslContext: DSLContext, userId: String, serviceId: Long, serviceCreateInfo: ServiceCreateInfo): Boolean {
        with(TService.T_SERVICE) {
            val execute = dslContext.update(this)
                .set(NAME, serviceCreateInfo.name)
                .set(SERVICE_TYPE_ID, serviceCreateInfo.serviceTypeId)
                .set(LINK, serviceCreateInfo.link)
                .set(LINK_NEW, serviceCreateInfo.linkNew)
                .set(INJECT_TYPE, serviceCreateInfo.injectType)
                .set(IFRAME_URL, serviceCreateInfo.iframeUrl)
                .set(CSS_URL, serviceCreateInfo.cssUrl)
                .set(JS_URL, serviceCreateInfo.jsUrl)
                .set(GRAY_CSS_URL, serviceCreateInfo.grayCssUrl)
                .set(GRAY_JS_URL, serviceCreateInfo.grayJsUrl)
                .set(SHOW_PROJECT_LIST, serviceCreateInfo.showProjectList)
                .set(SHOW_NAV, serviceCreateInfo.showNav)
                .set(PROJECT_ID_TYPE, serviceCreateInfo.projectIdType)
                .set(STATUS, serviceCreateInfo.status)
                .set(LOGO_URL, serviceCreateInfo.logoUrl)
                .set(WEB_SOCKET, serviceCreateInfo.webSocket)
                .set(UPDATED_USER, userId)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(ID.eq(serviceId))
                .and(DELETED.eq(false))
                .execute()
            return execute > 0
        }
    }

    fun updateUrlByName(dslContext: DSLContext, serviceUrlUpdateInfo: ServiceUrlUpdateInfo): Boolean {
        with(TService.T_SERVICE) {
            val execute = dslContext.update(this)
                    .set(JS_URL, serviceUrlUpdateInfo.jsUrl)
                    .set(CSS_URL, serviceUrlUpdateInfo.cssUrl)
                    .where(NAME.eq(serviceUrlUpdateInfo.name))
                    .and(DELETED.eq(false))
                    .execute()
            return execute > 0
        }
    }

    fun select(dslContext: DSLContext, serviceId: Long): TServiceRecord? {
        with(TService.T_SERVICE) {
            return dslContext.selectFrom(this)
                .where(ID.eq(serviceId))
                .and(DELETED.eq(false))
                .fetchOne()
        }
    }

    fun updateUrls(
        dslContext: DSLContext,
        userId: String,
        name: String,
        serviceUpdateUrls: ServiceUpdateUrls
    ): Boolean {
        with(TService.T_SERVICE) {
            val step = dslContext.update(this)
            if (!serviceUpdateUrls.cssUrl.isNullOrBlank()) {
                step.set(CSS_URL, serviceUpdateUrls.cssUrl)
            }
            if (!serviceUpdateUrls.jsUrl.isNullOrBlank()) {
                step.set(JS_URL, serviceUpdateUrls.jsUrl)
            }
            if (!serviceUpdateUrls.grayCssUrl.isNullOrBlank()) {
                step.set(GRAY_CSS_URL, serviceUpdateUrls.grayCssUrl)
            }
            if (!serviceUpdateUrls.grayJsUrl.isNullOrBlank()) {
                step.set(GRAY_JS_URL, serviceUpdateUrls.grayJsUrl)
            }
            val execute = step.set(UPDATED_USER, userId)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(NAME.eq(name))
                .and(DELETED.eq(false))
                .execute()
            return execute > 0
        }
    }
}