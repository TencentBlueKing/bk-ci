package com.tencent.devops.openapi.filter.manager.impl

import com.tencent.devops.openapi.filter.manager.ApiFilterFlowState
import com.tencent.devops.openapi.filter.manager.ApiFilterManager
import com.tencent.devops.openapi.filter.manager.FilterContext
import javax.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ApiPathFilter : ApiFilterManager {
    companion object {
        private val logger = LoggerFactory.getLogger(ApiPathFilter::class.java)
    }

    enum class ApiType(val startContextPath: String, val verify: Boolean) {
        DEFAULT("/api/apigw/", true),
        USER("/api/apigw-user/", true),
        APP("/api/apigw-app/", true),
        OP("/api/op/", false),
        SWAGGER("/api/swagger.json", false);

        companion object {
            fun parseType(path: String): ApiType? {
                values().forEach { type ->
                    if (path.contains(other = type.startContextPath, ignoreCase = true)) {
                        return type
                    }
                }
                return null
            }
        }
    }

    /*返回true时执行check逻辑*/
    override fun canExecute(requestContext: FilterContext): Boolean {
        return true
    }

    override fun verify(requestContext: FilterContext): ApiFilterFlowState {
        // path为为空的时候，直接退出
        val path = requestContext.requestContext.uriInfo.requestUri.path

        logger.info("FILTER| url=$path")
        // 判断是否为合法的路径
        val apiType = ApiType.parseType(path) ?: run {
            requestContext.requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Devops OpenAPI Auth fail: path($path) illegal!")
                    .build()
            )
            return ApiFilterFlowState.BREAK
        }
        // 如果是op的接口访问直接跳过后续权限认证
        if (!apiType.verify) return ApiFilterFlowState.BREAK
        requestContext.apiType = apiType
        return ApiFilterFlowState.CONTINUE
    }
}
