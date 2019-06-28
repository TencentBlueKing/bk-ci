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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.archive.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("自定义产出物报告", description = ReportArchiveElement.classType)
data class ReportArchiveElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "python文件编译",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("待上传文件夹）", required = true)
    val fileDir: String = "",
    @ApiModelProperty("入口文件）", required = false)
    val indexFile: String = "",
    @ApiModelProperty("标签别名", required = true)
    val reportName: String = "",
    @ApiModelProperty("开启邮件", required = false)
    val enableEmail: Boolean?,
    @ApiModelProperty("邮件接收者", required = false)
    val emailReceivers: Set<String>?,
    @ApiModelProperty("邮件标题", required = false)
    val emailTitle: String?
) : Element(name, id, status) {
    companion object {
        const val classType = "reportArchive"
    }

    override fun getClassType() = classType
}
// fun main(array: Array<String>) {
//    val e = ReportArchiveElement("python文件编译", "id", "true", "dir", "file"
//    ,"reportName", true, setOf("a","b","d","dd"),"title")
//    val toMutableMap = JsonUtil.toMutableMap(e)
//    val reportArchiveElement = JsonUtil.mapTo(toMutableMap, ReportArchiveElement::class.java)
//    println("$reportArchiveElement")
//    println("map=$toMutableMap")
//    val emailReceivers = toMutableMap["emailReceivers"]
//    val toJson = JsonUtil.toJson(emailReceivers!!)
//    println(toJson::class.java.isAssignableFrom(String::class.java))
//    println(JsonUtil.toJson("abc"))
//    println(JsonUtil.toJson(123))
//    val bean = setOf("dd", "bb")
//    val toJson1 = JsonUtil.toJson(bean)
//    val to = JsonUtil.to<List<String>>("dd,bb")
//    to.forEach {
//        println("{$it}")
//    }
//
//    to.joinToString(",") {
//        if ()
//    }
//
// }
