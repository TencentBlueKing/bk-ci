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

package com.tencent.devops.store.pojo.ideatom

import com.tencent.devops.store.pojo.common.Category
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomStatusEnum
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "IDE插件信息")
data class OpIdeAtomItem(
    @get:Schema(title = "插件ID", required = true)
    val atomId: String,
    @get:Schema(title = "插件名称", required = true)
    val atomName: String,
    @get:Schema(title = "插件代码", required = true)
    val atomCode: String,
    @get:Schema(title = "插件类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = false)
    val atomType: IdeAtomTypeEnum?,
    @get:Schema(title = "版本号", required = true)
    val atomVersion: String,
    @get:Schema(
        title = "插件状态，INIT：初始化|AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|" +
                "GROUNDING_SUSPENSION：上架中止|UNDERCARRIAGED：已下架",
        required = false
    )
    val atomStatus: IdeAtomStatusEnum,
    @get:Schema(title = "需管理员操作的最新插件ID", required = false)
    var opAtomId: String? = null,
    @get:Schema(title = "需管理员操作的最新插件版本号", required = false)
    var opAtomVersion: String? = null,
    @get:Schema(
        title = "需管理员操作的最新插件状态，INIT：初始化|AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|" +
                "GROUNDING_SUSPENSION：上架中止|UNDERCARRIAGED：已下架",
        required = false
    )
    var opAtomStatus: IdeAtomStatusEnum? = null,
    @get:Schema(title = "所属分类代码", required = false)
    val classifyCode: String?,
    @get:Schema(title = "所属分类名称", required = false)
    val classifyName: String?,
    @get:Schema(title = "范畴列表", required = false)
    val categoryList: List<Category>?,
    @get:Schema(title = "发布者", required = true)
    val publisher: String,
    @get:Schema(title = "发布时间", required = false)
    val pubTime: String?,
    @get:Schema(title = "是否为最新版本插件 true：最新 false：非最新", required = true)
    val latestFlag: Boolean,
    @get:Schema(title = "是否为公共插件 true：公共插件 false：普通插件", required = false)
    val publicFlag: Boolean?,
    @get:Schema(title = "是否推荐， TRUE：是 FALSE：不是", required = false)
    val recommendFlag: Boolean?,
    @get:Schema(title = "权重（数值越大代表权重越高）", required = false)
    val weight: Int?,
    @get:Schema(title = "插件安装包名称", required = false)
    val pkgName: String?,
    @get:Schema(title = "创建人", required = true)
    val creator: String,
    @get:Schema(title = "创建时间", required = true)
    val createTime: String,
    @get:Schema(title = "修改人", required = true)
    val modifier: String,
    @get:Schema(title = "修改时间", required = true)
    val updateTime: String
)
