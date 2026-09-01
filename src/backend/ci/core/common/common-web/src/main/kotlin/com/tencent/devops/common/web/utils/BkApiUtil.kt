package com.tencent.devops.common.web.utils

import com.tencent.devops.common.web.constant.BkApiHandleType
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import jakarta.servlet.http.HttpServletRequest

/**
 * API接口工具类
 *
 * @since: 2023-09-12
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object BkApiUtil {

    private val apiPermissionThreadLocal = ThreadLocal<Boolean>()

    /**
     * 获取需要进行api接口权限校验的项目在缓存中的key
     * @return 需要进行api接口权限校验的项目在缓存中的key
     */
    fun getApiAccessLimitProjectsKey(): String {
        return "${BkApiHandleType.PROJECT_API_ACCESS_LIMIT}:projects"
    }

    /**
     * 获取需要进行api接口权限校验的流水线在缓存中的key
     * @return 需要进行api接口权限校验的流水线在缓存中的key
     */
    fun getApiAccessLimitPipelinesKey(): String {
        return "${BkApiHandleType.PIPELINE_API_ACCESS_LIMIT}:pipelines"
    }

    /**
     * 把接口权限校验标识保存到ThreadLocal中
     * @param permissionFlag 当次接口调用权限校验标识
     */
    fun setPermissionFlag(permissionFlag: Boolean) {
        apiPermissionThreadLocal.set(permissionFlag)
    }

    /**
     * 从ThreadLocal中获取当前线程中的接口权限校验标识
     * @return 布尔值
     */
    fun getPermissionFlag(): Boolean? {
        return apiPermissionThreadLocal.get()
    }

    /**
     * 从ThreadLocal中移除当前线程中的接口权限校验标识
     * @return 布尔值
     */
    fun removePermissionFlag() {
        apiPermissionThreadLocal.remove()
    }

    /**
     * 获取request对象
     * @return request对象
     */
    fun getHttpServletRequest(): HttpServletRequest? {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return attributes?.request
    }

    /**
     * 获取正在迁移流水线列表的redis键
     * @param moduleCode 模块标识
     * @return 正在迁移流水线列表的redis键
     */
    fun getMigratingPipelinesRedisKey(moduleCode: String): String {
        return "$moduleCode:migrating:pipelines"
    }

    /**
     * 获取项目下正在迁移流水线列表的redis键
     *
     * 该集合同时充当项目的归档标识：集合不存在（或为空）表示项目下没有流水线处于归档中，
     * 无需再逐条判断流水线的迁移标识；集合非空时其成员即为项目下正在迁移的流水线。
     * 由于标识由集合成员推导而来，同一项目多条流水线并发归档时不会出现标识被提前清除的问题。
     *
     * @param moduleCode 模块标识
     * @param projectId 项目ID
     * @return 项目下正在迁移流水线列表的redis键
     */
    fun getMigratingPipelinesRedisKey(moduleCode: String, projectId: String): String {
        return "$moduleCode:migrating:pipelines:$projectId"
    }
}
