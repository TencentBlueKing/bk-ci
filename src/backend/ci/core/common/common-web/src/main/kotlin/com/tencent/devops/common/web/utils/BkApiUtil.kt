package com.tencent.devops.common.web.utils

import com.tencent.devops.common.web.constant.BkApiHandleType
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletRequest

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
     * 获取request对象
     * @return request对象
     */
    fun getHttpServletRequest(): HttpServletRequest? {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return attributes?.request
    }
}
