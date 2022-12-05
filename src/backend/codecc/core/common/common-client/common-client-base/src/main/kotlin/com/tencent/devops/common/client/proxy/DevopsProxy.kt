package com.tencent.devops.common.client.proxy

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import feign.Feign
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import javax.ws.rs.HeaderParam
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam

class DevopsProxy constructor(private val any: Any,
                              private val clz: Class<*>) : InvocationHandler {

    companion object{
        val projectIdThreadLocal = ThreadLocal<Any>()
        private val methodProjectInfoMap = mutableMapOf<String, Int>()
        private val logger = LoggerFactory.getLogger(DevopsProxy::class.java)
    }

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any {
        val argIndex = methodProjectInfoMap[Feign.configKey(clz, method)]
        if (null != argIndex) {
            projectIdThreadLocal.set(args[argIndex])
        } else {
            method.parameters.forEachIndexed { index, parameter ->
                if(parameter.annotations.any {
                        when (it) {
                            is PathParam -> it.value == "projectId"
                            is QueryParam -> it.value == "projectId"
                            is HeaderParam -> it.value == AUTH_HEADER_DEVOPS_PROJECT_ID
                            else -> false
                        }
                    }) {
                    methodProjectInfoMap[Feign.configKey(clz, method)] = index
                    projectIdThreadLocal.set(args[index])
                }
            }
        }

        return try {
            method.invoke(any, *args)
        } catch (t : Throwable){
            logger.error("execute devops service fail, message: ${t.message}")
            throw t.cause ?: t
        } finally {
            projectIdThreadLocal.remove()
        }
    }
}
