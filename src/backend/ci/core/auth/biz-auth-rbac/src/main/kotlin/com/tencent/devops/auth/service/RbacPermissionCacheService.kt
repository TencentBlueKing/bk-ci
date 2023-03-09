package com.tencent.devops.auth.service

import com.google.common.cache.CacheBuilder
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthActionDao
import com.tencent.devops.auth.dao.AuthResourceTypeDao
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.auth.service.iam.PermissionCacheService
import com.tencent.devops.common.api.exception.ErrorCodeException
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RbacPermissionCacheService @Autowired constructor(
    val dslContext: DSLContext,
    val authResourceTypeDao: AuthResourceTypeDao,
    val authActionDao: AuthActionDao,
) : PermissionCacheService {
    /*获取资源类型下的动作*/
    private val resourceType2ActionCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(7L, TimeUnit.DAYS)
        .build<String/*resourceType*/, List<ActionInfoVo>>()
    private val resourceTypeCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(7L, TimeUnit.DAYS)
        .build<String/*resourceType*/, ResourceTypeInfoVo>()
    private val actionCache = CacheBuilder.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(7L, TimeUnit.DAYS)
        .build<String/*action*/, ActionInfoVo>()


    override fun listResourceTypes(): List<ResourceTypeInfoVo> {
        if (resourceTypeCache.asMap().values.isEmpty()) {
            authResourceTypeDao.list(dslContext).forEach {
                val resourceTypeInfo = ResourceTypeInfoVo(
                    resourceType = it.resourceType,
                    name = it.name,
                    parent = it.parent,
                    system = it.system
                )
                resourceTypeCache.put(it.resourceType, resourceTypeInfo)
            }
        }
        return resourceTypeCache.asMap().values.toList()
    }

    override fun listResourceType2Action(resourceType: String): List<ActionInfoVo> {
        if (resourceType2ActionCache.getIfPresent(resourceType) == null) {
            val actionList = authActionDao.list(dslContext, resourceType)
            if (actionList.isEmpty()) {
                throw ErrorCodeException(
                    errorCode = AuthMessageCode.RESOURCE_ACTION_EMPTY,
                    params = arrayOf(resourceType),
                    defaultMessage = "权限系统：[$resourceType]资源类型关联的动作不存在"
                )
            }
            val actionInfoVoList = actionList.map {
                ActionInfoVo(
                    action = it.action,
                    actionName = it.actionName,
                    resourceType = it.resourceType,
                    relatedResourceType = it.relatedResourceType
                )
            }
            resourceType2ActionCache.put(resourceType, actionInfoVoList)
        }
        return resourceType2ActionCache.getIfPresent(resourceType)!!
    }

    override fun getActionInfo(action: String): ActionInfoVo {
        if (actionCache.getIfPresent(action) == null) {
            val actionRecord = authActionDao.get(dslContext, action)
                ?: throw ErrorCodeException(
                    errorCode = AuthMessageCode.ACTION_NOT_EXIST,
                    params = arrayOf(action),
                    defaultMessage = "权限系统：[$action]操作不存在"
                )
            val actionInfo = ActionInfoVo(
                action = actionRecord.action,
                actionName = actionRecord.actionName,
                resourceType = actionRecord.resourceType,
                relatedResourceType = actionRecord.relatedResourceType
            )
            actionCache.put(action, actionInfo)
        }
        return actionCache.getIfPresent(action)!!
    }

    override fun getResourceTypeInfo(resourceType: String): ResourceTypeInfoVo {
        if (resourceTypeCache.getIfPresent(resourceType) == null) {
            listResourceTypes()
        }
        return resourceTypeCache.getIfPresent(resourceType) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.RESOURCE_TYPE_NOT_FOUND,
            params = arrayOf(resourceType),
            defaultMessage = "权限系统：[$resourceType]资源类型不存在"
        )
    }
}
