package com.tencent.devops.openapi.service.op

import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.openapi.constant.OpenAPIMessageCode
import com.tencent.devops.openapi.dao.AppManagerUserDao
import com.tencent.devops.openapi.pojo.AppManagerInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Service
class AppUserInfoService @Autowired constructor(
    val dslContext: DSLContext,
    val appManagerUserDao: AppManagerUserDao,
    val opAppUserService: OpAppUserService
) {

    private val indexCache = CacheBuilder.newBuilder()
        .maximumSize(200)
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build<String/*appCode*/, String/*managerUser*/>()

    @PostConstruct
    fun init() {
        list()
    }

    fun bindAppManagerUser(userId: String, appManagerInfo: AppManagerInfo): Boolean {
        logger.info("BIND_APP_MANAGER_USER|$userId|$appManagerInfo")
        if (!opAppUserService.checkUser(appManagerInfo.managerUser)) {
            logger.warn("BIND_APP_MANAGER_USER|$userId|appManagerUser check fail")
            throw ParamBlankException(
                I18nUtil.getCodeLanMessage(
                    OpenAPIMessageCode.USER_CHECK_FAIL,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }

        val appManagerRecord = appManagerUserDao.get(dslContext, appManagerInfo.appCode)
        val exist = if (appManagerRecord != null) {
            if (appManagerInfo.managerUser == appManagerRecord.managerId) {
                logger.warn("BIND_APP_MANAGER_USER|$userId|appManagerUser ${appManagerInfo.managerUser} is exist")
                throw ParamBlankException(
                    I18nUtil.getCodeLanMessage(
                        OpenAPIMessageCode.ERROR_USER_EXIST,
                        language = I18nUtil.getLanguage(userId)
                    )
                )
            } else {
                true
            }
        } else {
            false
        }
        return appManagerUserDao.set(
            dslContext = dslContext,
            appCode = appManagerInfo.appCode,
            createUser = userId,
            managerUser = appManagerInfo.managerUser,
            exist = exist
        )
    }

    fun list(): List<AppManagerInfo>? {
        val appManagerInfos = appManagerUserDao.list(dslContext)
        val result = mutableListOf<AppManagerInfo>()
        if (!appManagerInfos.isNullOrEmpty()) {
            appManagerInfos.forEach {
                indexCache.put(it.appCode, it.managerId)
                result.add(AppManagerInfo(
                    appCode = it.appCode,
                    managerUser = it.managerId
                ))
            }
        }
        return result
    }

    fun get(appCode: String): String? {
        if (!indexCache.getIfPresent(appCode).isNullOrEmpty()) {
            return indexCache.getIfPresent(appCode)!!
        }
        val appManagerInfo = appManagerUserDao.get(dslContext, appCode) ?: return null
        indexCache.put(appManagerInfo.appCode, appManagerInfo.managerId)
        return appManagerInfo.managerId
    }

    fun delete(id: Int): Boolean {
        appManagerUserDao.delete(dslContext, id)
        // 数据量较小直接清理cache
        list()
        return true
    }

    companion object {
        val logger = LoggerFactory.getLogger(AppUserInfoService::class.java)
    }
}
