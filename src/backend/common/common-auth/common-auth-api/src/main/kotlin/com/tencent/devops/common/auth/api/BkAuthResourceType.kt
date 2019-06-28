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

package com.tencent.devops.common.auth.api

enum class BkAuthResourceType(val value: String) {
    BCS_DEV_IMAGE("dev_image"),     // bcs服务开发镜像
    BCS_PROD_IMAGE("prod_image"),   // bcs服务生产镜像

    CODE_REPERTORY("repertory"), // code代码仓库

    PIPELINE_DEFAULT("pipeline"), // 流水线默认类型

    ARTIFACTORY_CUSTOM_DIR("custom_dir"), // 版本仓库自定义目录

    TICKET_CREDENTIAL("credential"),        // 凭证服务凭据
    TICKET_CERT("cert"),                    // 凭证服务证书

    ENVIRONMENT_ENVIRONMENT("environment"), // 环境
    ENVIRONMENT_ENV_NODE("env_node"),       // 环境节点

    EXPERIENCE_TASK("task"),                // 体验任务
    EXPERIENCE_GROUP("group"),              // 体验组

    SCAN_TASK("scan_task"), // 扫描任务

    QUALITY_RULE("rule"),                   // 质量红线规则
    QUALITY_GROUP("group"),                 // 质量红线用户组

    WETEST_TASK("task"), // 体验任务
    WETEST_EMAIL_GROUP("email_group"), // 体验组

    PROJECT("project"); // 项目管理

    companion object {
        fun get(value: String): BkAuthResourceType {
            BkAuthResourceType.values().forEach {
                if (value == it.value) return it
            }
            throw IllegalArgumentException("No enum for constant $value")
        }
    }
}