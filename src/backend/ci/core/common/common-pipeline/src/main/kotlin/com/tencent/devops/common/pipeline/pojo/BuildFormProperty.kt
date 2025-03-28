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

package com.tencent.devops.common.pipeline.pojo

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.cascade.BuildCascadeProps
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "构建模型-表单元素属性")
data class BuildFormProperty(
    @get:Schema(title = "元素ID-标识符", required = true)
    var id: String,
    @get:Schema(title = "元素名称", required = true)
    var name: String? = null,
    @get:Schema(title = "是否必须（新前端的入参标识）", required = true)
    var required: Boolean,
    @get:Schema(title = "是否为常量", required = true)
    var constant: Boolean? = false,
    @get:Schema(title = "元素类型", required = true)
    val type: BuildFormPropertyType,
    @get:Schema(title = "默认值", required = true)
    var defaultValue: Any,
    @get:Schema(title = "上次构建的取值", required = true)
    var value: Any? = null,
    @get:Schema(title = "下拉框列表", required = false)
    var options: List<BuildFormValue>?,
    @get:Schema(title = "描述", required = false)
    var desc: String?,
    @get:Schema(title = "分组信息", required = false)
    val category: String? = null,

    // 针对 SVN_TAG 新增字段
    @get:Schema(title = "repoHashId", required = false)
    val repoHashId: String?,
    @get:Schema(title = "relativePath", required = false)
    val relativePath: String?,
    @get:Schema(title = "代码库类型下拉", required = false)
    val scmType: ScmType?,
    @get:Schema(title = "构建机类型下拉", required = false)
    val containerType: BuildContainerType?,

    @get:Schema(title = "自定义仓库通配符", required = false)
    val glob: String?,
    @get:Schema(title = "开启文件版本管理", required = false)
    val enableVersionControl: Boolean? = null,
    @get:Schema(title = "目录随机字符串", required = false)
    val randomStringInPath: String? = null,
    @get:Schema(title = "最新的目录随机字符串", required = false)
    var latestRandomStringInPath: String? = null,
    @get:Schema(title = "文件元数据", required = false)
    val properties: Map<String, String>?,
    @get:Schema(title = "元素标签", required = false)
    var label: String? = null,
    @get:Schema(title = "元素placeholder", required = false)
    var placeholder: String? = null,
    // 区分构建信息、构建版本和流水线参数
    @get:Schema(title = "元素模块", required = false)
    var propertyType: String? = null,

    @get:Schema(title = "搜索url, 当是下拉框选项时，列表值从url获取不再从option获取", required = false)
    var searchUrl: String? = null,
    /**
     * 替换搜索url中的搜素关键字
     *
     * 如searchUrl是aaa/bbb?search={key}, replaceKey的值是{key},则前端在搜索的时候会把{key}替换成用户输入的值.
     * 假设用户输入aaa，那么前端请求就是aaa/bbb?search=aaa
     */
    @get:Schema(title = "替换搜索url中的搜素关键字", required = false)
    var replaceKey: String? = null,
    @get:Schema(title = "是否只读", required = false)
    var readOnly: Boolean? = false,
    @get:Schema(title = "参数值是否必填", required = false)
    val valueNotEmpty: Boolean? = false,
    @get:Schema(title = "页面所需内容，后台仅保存，不做处理", required = false)
    val payload: Any? = null,
    @get:Schema(title = "级联选择器属性", required = false)
    var cascadeProps: BuildCascadeProps? = null
)

@Schema(title = "构建模型-自定义路径拆分的版本控制信息")
data class CustomFileVersionControlInfo(
    @get:Schema(title = "完整目录", required = false)
    var directory: String,
    @get:Schema(title = "最新的目录随机字符串", required = false)
    var latestRandomStringInPath: String
)
