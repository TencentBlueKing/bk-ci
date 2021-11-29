/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.repository.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("代码库模型-Code平台P4")
data class CodeP4Repository(
    @ApiModelProperty("代码库别名", required = true)
    override val aliasName: String,
    @ApiModelProperty("URL", required = true)
    override val url: String,
    @ApiModelProperty("凭据id", required = true)
    override val credentialId: String,
    @ApiModelProperty("项目名称(与aliasName相同)", required = true)
    override val projectName: String,
    @ApiModelProperty("用户名", required = true)
    override var userName: String,
    @ApiModelProperty("项目id", required = true)
    override val projectId: String?,
    @ApiModelProperty("仓库hash id", required = false)
    override val repoHashId: String?
) : Repository {

    companion object {
        const val classType = "codeP4"
    }

    override fun getStartPrefix(): String {
        return ""
    }

    override fun isLegal(): Boolean {
        return true
    }
}
