package com.tencent.devops.common.ci.service

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.ci.SERVICE_TYPE
import com.tencent.devops.common.ci.task.ServiceJobDevCloudInput
import javax.ws.rs.core.Response

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = SERVICE_TYPE)
@JsonSubTypes(
    JsonSubTypes.Type(value = MysqlService::class, name = MysqlService.type)
)

abstract class AbstractService(
    open val image: String,
    open val user: String?,
    open val password: String?,
    open val paramNamespace: String?
) {
    abstract fun getType(): String

    abstract fun getServiceParamNameSpace(): String

    abstract fun getServiceInput(repoUrl: String, repoUsername: String, repoPwd: String, env: String): ServiceJobDevCloudInput

    fun parseImage(): Pair<String, String> {
        val list = image.split(":")
        if (list.size != 2) {
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "GITCI Service镜像格式非法")
        }
        return Pair(list[0], list[1])
    }
}