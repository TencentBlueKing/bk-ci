package com.tencent.devops.repository.pojo.scm

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工程仓库模型-Svn-创建类型")
class ScmSvnCreate(
        @ApiModelProperty("repo url", required = true)
        val url: String,
        @ApiModelProperty("repo addition path", required = false)
        val additionPath: String?,
        @ApiModelProperty("用户名", required = true)
        val username: String,
        @ApiModelProperty("密码", required = false)
        val password: String?,
        @ApiModelProperty("用户访问私钥(明文)", required = false)
        val privateKey: String?, // Just need one from [password, privateKey]. If protocol is svn+ssh, it's privateKey, otherwise it's password
        @ApiModelProperty("Private Key Passphrase", required = false)
        val passPhrase: String?
)