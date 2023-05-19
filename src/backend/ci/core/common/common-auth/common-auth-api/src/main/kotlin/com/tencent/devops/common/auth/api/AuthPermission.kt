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

package com.tencent.devops.common.auth.api

import com.tencent.devops.common.api.constant.CommonMessageCode.MSG_CODE_PERMISSION_PREFIX
import com.tencent.devops.common.api.util.MessageUtil

enum class AuthPermission(val value: String, val alias: String) {
    CREATE("create", "创建"), // 流水线，凭据，证书，代码仓库
    DEPLOY("deploy", "部署"), // 流水线，容器，自定义目录
    EDIT("edit", "编辑"), // 流水线，凭据，证书，代码仓库
    DOWNLOAD("download", "下载"), // 流水线
    DELETE("delete", "删除"), // 流水线，自定义目录，凭据，证书，代码仓库
    VIEW("view", "查看"), // 流水线，自定义目录，凭据，证书，代码仓库
    MOVE("move", "移动"), // 自定义目录
    COPY("copy", "复制"), // 自定义目录
    USE("use", "使用"), // 凭据，证书，代码仓库
    SHARE("share", "分享"), // 流水线，自定义目录
    LIST("list", "列表"), // 流水线，自定义目录，凭据，证书，代码仓库
    EXECUTE("execute", "执行"), // 流水线
    MKDIR("mkdir", "创建目录"), // 自定义目录，容器
    EXPERIENCE("experience", "转体验"), // 版本体验
    ENABLE("enable", "停用/启用"), // 质量红线

    VIEWS_MANAGER("views_manager", "视图管理"), // 项目视图管理
    WEB_CHECK("webcheck", "页面按钮校验"), // 页面按钮校验
    MANAGE("manage", "管理"), // 管理
    VISIT("visit", "访问"), // 项目访问权限

    ANALYZE("analyze", "分析"), // 分析任务
    MANAGE_DEFECT("manage-defect", "问题管理"), // 问题管理
    VIEW_DEFECT("view-defect", "查看问题列表和详情"), // 查看问题列表和详情
    VIEW_REPORT("view-report", "查看报表"), // 查看报表
    SETTING("setting", "设置"); // 设置

    companion object {
        fun get(value: String): AuthPermission {
            values().forEach {
                if (value == it.value) return it
            }
            throw IllegalArgumentException("No enum for constant $value")
        }
    }

    fun getI18n(language: String): String {
        return MessageUtil.getMessageByLocale(
            messageCode = "$MSG_CODE_PERMISSION_PREFIX${this.name}",
            defaultMessage = this.alias,
            language = language
        )
    }
}
