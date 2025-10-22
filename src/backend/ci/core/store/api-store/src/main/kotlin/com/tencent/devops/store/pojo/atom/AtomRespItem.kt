/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.pojo.atom

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nSourceEnum
import com.tencent.devops.store.pojo.common.honor.HonorInfo
import com.tencent.devops.store.pojo.common.label.Label
import com.tencent.devops.store.pojo.common.index.StoreIndexInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线-插件信息")
data class AtomRespItem(
    @get:Schema(title = "插件名称", required = true)
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @get:Schema(title = "插件代码", required = true)
    val atomCode: String,
    @get:Schema(title = "插件版本", required = true)
    val version: String,
    @get:Schema(title = "插件默认版本号", required = true)
    val defaultVersion: String,
    @get:Schema(title = "插件大类（插件市场发布的插件分为有marketBuild：构建环境和marketBuildLess：无构建环境）", required = true)
    val classType: String,
    @get:Schema(title = "服务范围", required = true)
    val serviceScope: List<String>,
    @get:Schema(title = "支持的操作系统", required = true)
    val os: List<String>,
    @get:Schema(title = "插件logo", required = false)
    val logoUrl: String?,
    @get:Schema(title = "插件图标", required = false)
    val icon: String?,
    @get:Schema(title = "所属分类编码", required = true)
    val classifyCode: String,
    @get:Schema(title = "所属分类名称", required = true)
    val classifyName: String,
    @get:Schema(title = "插件所属范畴，TRIGGER：触发器类插件 TASK：任务类插件", required = true)
    val category: String,
    @get:Schema(title = "插件简介", required = false)
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val summary: String?,
    @get:Schema(title = "插件说明文档链接", required = false)
    val docsLink: String?,
    @get:Schema(title = "插件类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = true)
    val atomType: String,
    @get:Schema(title = "插件状态，INIT：初始化|COMMITTING：提交中|BUILDING：构建中|BUILD_FAIL：构建失败|TESTING：测试中" +
        "|AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|UNDERCARRIAGING：下架中" +
        "|UNDERCARRIAGED：已下架", required = true)
    val atomStatus: String,
    @get:Schema(title = "插件描述", required = false)
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val description: String?,
    @get:Schema(title = "发布者")
    @BkFieldI18n(source = I18nSourceEnum.DB, keyPrefixName = "versionInfo")
    val publisher: String?,
    @get:Schema(title = "创建人", required = true)
    val creator: String,
    @get:Schema(title = "修改人")
    val modifier: String,
    @get:Schema(title = "创建时间")
    val createTime: String,
    @get:Schema(title = "修改时间")
    val updateTime: String,
    @get:Schema(title = "是否为默认插件（默认插件默认所有项目可见）true：默认插件 false：普通插件", required = true)
    val defaultFlag: Boolean,
    @get:Schema(title = "是否为最新版本插件 true：最新 false：非最新", required = true)
    val latestFlag: Boolean,
    @get:Schema(title = "前端渲染模板版本（1.0代表历史存量插件渲染模板版本）", required = true)
    val htmlTemplateVersion: String,
    @get:Schema(title = "无构建环境插件是否可以在有构建环境运行标识， TRUE：可以 FALSE：不可以", required = false)
    val buildLessRunFlag: Boolean?,
    @get:Schema(title = "权重（数值越大代表权重越高）", required = false)
    val weight: Int?,
    @get:Schema(title = "是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean?,
    @get:Schema(title = "评分", required = false)
    val score: Double? = null,
    @get:Schema(title = "最近执行次数", required = false)
    val recentExecuteNum: Int? = null,
    @get:Schema(title = "是否能卸载标识", required = false)
    val uninstallFlag: Boolean? = null,
    @get:Schema(title = "标签列表", required = false)
    val labelList: List<Label>? = null,
    @get:Schema(title = "是否有权限安装标识", required = false)
    val installFlag: Boolean? = null,
    @get:Schema(title = "是否已安装", required = false)
    val installed: Boolean? = null,
    @get:Schema(title = "荣誉信息", required = false)
    val honorInfos: List<HonorInfo>? = null,
    @get:Schema(title = "指标信息列表")
    val indexInfos: List<StoreIndexInfo>? = null,
    @get:Schema(title = "hotFlag")
    val hotFlag: Boolean? = null
)
