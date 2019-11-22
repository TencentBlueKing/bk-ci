package com.tencent.devops.common.ci.service

import com.tencent.devops.common.ci.task.ServiceJobDevCloudInput

data class MysqlService(
    override val image: String,
    override val user: String?,
    override val password: String?,
    override val paramNamespace: String?
) : AbstractService(image, user, password, paramNamespace) {

    override fun getType(): String {
        return type
    }

    override fun getServiceInput(repoUrl: String, repoUsername: String, repoPwd: String, env: String): ServiceJobDevCloudInput {
        val params = if (password.isNullOrBlank()) {
            "{\"env\":{\"MYSQL_ALLOW_EMPTY_PASSWORD\":\"yes\"}}"
        } else {
            "{\"env\":{\"MYSQL_ROOT_PASSWORD\":\"$password\"}}"
        }

        return ServiceJobDevCloudInput(
            image,
            repoUrl,
            repoUsername,
            repoPwd,
            params,
            env
        )
    }

    override fun getServiceParamNameSpace(): String {
        return if (paramNamespace.isNullOrBlank()) {
            type.toUpperCase()
        } else {
            paramNamespace + "_" + type.toUpperCase()
        }
    }

    companion object {
        const val type = "mysql"
    }
}