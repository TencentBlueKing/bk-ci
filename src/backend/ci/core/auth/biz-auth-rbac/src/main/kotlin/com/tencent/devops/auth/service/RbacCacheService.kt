package com.tencent.devops.auth.service

import com.google.common.cache.CacheBuilder
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthActionDao
import com.tencent.devops.auth.dao.AuthResourceTypeDao
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.common.api.exception.ErrorCodeException
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RbacCacheService @Autowired constructor(
    val dslContext: DSLContext,
    val authResourceTypeDao: AuthResourceTypeDao,
    val authActionDao: AuthActionDao,
) {
    private val actionListCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(7L, TimeUnit.DAYS)
        .build<String, List<ActionInfoVo>>()
    private val resourceTypeListCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(7L, TimeUnit.DAYS)
        .build<String, List<ResourceTypeInfoVo>>()
    private val actionCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(7L, TimeUnit.DAYS)
        .build<String, ActionInfoVo>()
    private val resourceTypeNameCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(7L, TimeUnit.DAYS)
        .build<String, String>()

    fun listResourceTypes(userId: String): List<ResourceTypeInfoVo> {
        if (resourceTypeListCache.getIfPresent(ALL_RESOURCE) == null) {
            val resourceTypeList = authResourceTypeDao.list(dslContext).map {
                resourceTypeNameCache.put(it.resourceType, it.name)
                ResourceTypeInfoVo(
                    resourceType = it.resourceType,
                    name = it.name,
                    parent = it.parent,
                    system = it.system
                )
            }
            resourceTypeListCache.put(ALL_RESOURCE, resourceTypeList)
        }
        return resourceTypeListCache.getIfPresent(ALL_RESOURCE)!!
    }

    fun listActions(userId: String, resourceType: String): List<ActionInfoVo> {
        if (actionCache.getIfPresent(resourceType) == null) {
            val actionList = authActionDao.list(dslContext, resourceType)
            if (actionList.isEmpty()) {
                throw ErrorCodeException(
                    errorCode = AuthMessageCode.RESOURCE_ACTION_EMPTY,
                    params = arrayOf(resourceType),
                    defaultMessage = "权限系统：[$resourceType]资源类型关联的动作不存在"
                )
            }
            val actionInfoVoList = actionList.map {
                val actionInfoVo = ActionInfoVo(
                    action = it.action,
                    actionName = it.actionName,
                    resourceType = it.resourceType,
                    relatedResourceType = it.relatedResourceType
                )
                actionCache.put(it.action, actionInfoVo)
                actionInfoVo
            }
            actionListCache.put(resourceType, actionInfoVoList)
        }
        return actionListCache.getIfPresent(resourceType)!!

    }

    fun getActionInfo(
        userId: String,
        resourceType: String,
        action: String
    ): ActionInfoVo {
        if (actionCache.getIfPresent(resourceType) == null) {
            listActions(userId, resourceType)
        }
        return actionCache.getIfPresent(action)!!
    }

    fun getResourceTypeName(
        userId: String,
        resourceType: String,
    ): String {
        if (resourceTypeListCache.getIfPresent(ALL_RESOURCE) == null) {
            listResourceTypes(userId)
        }
        return resourceTypeNameCache.getIfPresent(resourceType)!!
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GroupUserService::class.java)
        private const val ALL_RESOURCE = "all_resource"
    }
}
