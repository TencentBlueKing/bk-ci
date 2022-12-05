/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.bkrepo.common.scanner.pojo.scanner.utils.normalizedLevel
import com.tencent.bkrepo.common.scanner.pojo.scanner.utils.removeRootDirPath
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("应用依赖组件信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationItem(
    @ApiModelProperty("组件路径")
    @JsonAlias("FilePath")
    val path: String,

    @ApiModelProperty("组件名")
    @JsonAlias("LibraryName")
    val component: String,

    @ApiModelProperty("组件版本")
    @JsonAlias("LibraryVersion")
    val version: String,

    @ApiModelProperty("证书详情")
    @JsonAlias("license")
    val license: License? = null
) {
    companion object {

        fun normalize(applicationItem: ApplicationItem): ApplicationItem {
            val path = removeRootDirPath(applicationItem.path)
            val license = applicationItem.license?.copy(risk = normalizedLevel(applicationItem.license.risk))
            return applicationItem.copy(path = path, license = license)
        }

        const val TYPE = "APPLICATION_ITEM"
    }
}
