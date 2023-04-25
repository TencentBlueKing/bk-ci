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

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("织云-推送文件", description = ZhiyunPushFileElement.classType)
data class ZhiyunPushFileElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "推送文件到织云",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("业务名", required = true)
    val product: String = "",
    @ApiModelProperty("包名", required = true)
    val packageName: String = "",
    @ApiModelProperty("版本描述", required = true)
    val description: String = "",
    @ApiModelProperty("是否先删除上一个版本的包的所有文件", required = true)
    val clean: Boolean,
    @ApiModelProperty("文件来源（PIPELINE-流水线仓库、CUSTOMIZE-自定义仓库）", required = true)
    val fileSource: String = "",
    @ApiModelProperty("文件路径，支持正则表达式(不支持逗号分隔多个文件)", required = true)
    val filePath: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "zhiyunPushFileElement"
    }

    override fun getTaskAtom(): String {
        return "zhiYunPushFileTaskAtom"
    }

    override fun getClassType() = classType
}
