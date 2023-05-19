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

package com.tencent.devops.common.api.enums

@Suppress("UNUSED")
enum class SystemModuleEnum(val code: String) {
    COMMON("00"), // 公共模块
    PROCESS("01"), // 流水线
    ARTIFACTORY("02"), // 版本仓库
    DISPATCH("03"), // 公共模块
    DOCKERHOST("04"), // DOCKER机器
    ENVIRONMENT("05"), // 环境
    EXPERIENCE("06"), // 版本体验
    IMAGE("07"), // 镜像
    LOG("08"), // 日志
    MEASURE("09"), // 度量
    MONITORING("10"), // 监控
    NOTIFY("11"), // 通知
    OPENAPI("12"), // 开放平台API
    PLUGIN("13"), // 插件
    QUALITY("14"), // 质量红线
    REPOSITORY("15"), // 代码库
    SCM("16"), // 软件配置管理
    SUPPORT("17"), // 支撑服务
    TICKET("18"), // 证书凭据
    PROJECT("19"), // 项目管理
    STORE("20"), // 商店
    AUTH("21"), // 权限
    SIGN("22"), // 签名服务
    METRICS("23"), // 度量服务
    EXTERNAL("24"), // 外部扩展
    PREBUILD("25"), // 预构建
    // DISPATCH-KUBERNETES("26"), // k8s分发服务
    BUILDLESS("27"), // buildless服务
    LAMBDA("28"), // lambda服务
    STREAM("29"), // stream服务
    WORKER("30"), // 度量服务
    // DISPATCH-DOCKER("31"), // docker分发服务
    REMOTEDEV("32"); // 远程开发服务

    companion object {
        fun getSystemModule(code: String): String {
            values().forEach {
                if (it.code == code) {
                    return it.name
                }
            }
            return COMMON.name
        }
    }
}
