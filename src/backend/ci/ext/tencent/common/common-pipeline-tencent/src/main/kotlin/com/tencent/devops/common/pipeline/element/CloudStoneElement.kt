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

package com.tencent.devops.common.pipeline.element

import com.tencent.devops.common.pipeline.enums.artifactory.SourceType
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@Deprecated("已作废")
@ApiModel("云石分发(IEG专用)", description = CloudStoneElement.classType)
data class CloudStoneElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "云石",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("关联CC业务Id", required = true)
    val ccAppId: String = "",
    @ApiModelProperty("文件上传路径（不支持多个路径），支持正则表达式", required = true)
    val sourcePath: String = "",
    @ApiModelProperty("是否自定义归档(PIPELINE or CUSTOMIZE)", required = true)
    val sourceType: SourceType,
    @ApiModelProperty("文件上传的目标路径(例如: /js_test/codecc.sql)", required = true)
    val targetPath: String = "",
    @ApiModelProperty("发布说明", required = false)
    val releaseNote: String?,
    @ApiModelProperty("版本号", required = true)
    val versionId: String = "",
    @ApiModelProperty("文件类型(server 或者 client)", required = true)
    val fileType: String = "",
    @ApiModelProperty("版本标签", required = false)
    val customFiled: List<String>?
) : Element(name, id, status) {
    companion object {
        const val classType = "cloudStone"
    }

    override fun getTaskAtom() = "cloudStoneTaskAtom"

    override fun getClassType() = classType
}
