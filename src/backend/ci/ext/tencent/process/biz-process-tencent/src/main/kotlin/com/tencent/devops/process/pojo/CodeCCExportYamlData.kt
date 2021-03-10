/*
 * Tencent is pleased to support the  source community by making BK-CI 蓝鲸持续集成平台 available.
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

package com.tencent.devops.process.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

/**
 * 流水线导出为yaml时针对CodeCC插件做简化而额外生成的数据类
 * 注释掉的是不需要被导出的
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeCCExportYamlData(
    // codeCC 基础配置参数
    @JsonDeserialize(using = AnyDeserializer::class)
    var script: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var codeCCTaskName: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var codeCCTaskCnName: Any? = "", // 暂时没用
    @JsonDeserialize(using = AnyDeserializer::class)
    var codeCCTaskId: Any? = "", // 调用接口用到
    @JsonDeserialize(using = AnyDeserializer::class)
    var checkerSetType: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var languages: Any? = "", // [PYTHON,KOTLIN]
    @JsonDeserialize(using = AnyDeserializer::class)
    var asynchronous: Any? = "",
//    var asyncTask: Boolean? = false,
//    var asyncTaskId: Long? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var scanType: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var path: Any? = "",
//    var tools: Any? = "", // [TOOL1,TOOL2]
    @JsonDeserialize(using = AnyDeserializer::class)
    var openScanPrj: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var pyVersion: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var eslintRc: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var phpcsStandard: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var goPath: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var projectBuildType: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var projectBuildCommand: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var ccnThreshold: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var needCodeContent: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var coverityToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var klocworkToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var cpplintToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var eslintToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var pylintToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var gometalinterToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var checkStyleToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var styleCopToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var detektToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var phpcsToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var sensitiveToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var occheckToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var ripsToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var gociLintToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var woodpeckerToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var horuspyToolSetId: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var pinpointToolSetId: Any? = "",

    // 非页面参数
    // 如果指定_CODECC_FILTER_TOOLS，则只做_CODECC_FILTER_TOOLS的扫描
    @JsonProperty("_CODECC_FILTER_TOOLS")
    @JsonDeserialize(using = AnyDeserializer::class)
    var filterTools: Any? = "", // [TOOL1,TOOL2]

    @JsonProperty("pipeline.start.channel")
    @JsonDeserialize(using = AnyDeserializer::class)
    var channelCode: Any? = "",

    // CodeCC V3 版本参数
    // 1.基础设置tab
    @JsonDeserialize(using = AnyDeserializer::class)
    var languageRuleSetMap: Any? = "", // 规则集

    // 2.通知报告tab
    @JsonDeserialize(using = AnyDeserializer::class)
    var rtxReceiverType: Any? = "", // rtx接收人类型：0-所有项目成员；1-接口人；2-自定义；3-无
    @JsonDeserialize(using = AnyDeserializer::class)
    var rtxReceiverList: Any? = "", // rtx接收人列表，rtxReceiverType=2时，自定义的接收人保存在该字段
    @JsonDeserialize(using = AnyDeserializer::class)
    var emailReceiverType: Any? = "", // 邮件收件人类型：0-所有项目成员；1-接口人；2-自定义；3-无
    @JsonDeserialize(using = AnyDeserializer::class)
    var emailReceiverList: Any? = "", // 邮件收件人列表，当emailReceiverType=2时，自定义的收件人保存在该字段
    @JsonDeserialize(using = AnyDeserializer::class)
    var emailCCReceiverList: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var reportStatus: Any? = "", // 定时报告任务的状态，有效：1，暂停：2 (目前看了都是1)
    @JsonDeserialize(using = AnyDeserializer::class)
    var reportDate: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var reportTime: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var instantReportStatus: Any? = "", // 即时报告状态，有效：1，暂停：2
    @JsonDeserialize(using = AnyDeserializer::class)
    var reportTools: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var botWebhookUrl: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var botRemindSeverity: Any? = "", // 7-总告警数； 3-严重 + 一般告警数；1-严重告警数
    @JsonDeserialize(using = AnyDeserializer::class)
    var botRemaindTools: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var botRemindRange: Any? = "", // 1-新增 2-遗留

    // 3.扫描配置tab
    @JsonDeserialize(using = AnyDeserializer::class)
    var toolScanType: Any? = "", // 对应接口的scanType, 1：增量；0：全量 2: diff模式
    @JsonDeserialize(using = AnyDeserializer::class)
    var newDefectJudgeFromDate: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var newDefectJudgeBy: Any? = "", // 判定方式1：按日期；2：按构建(目前都填1)
    @JsonDeserialize(using = AnyDeserializer::class)
    var transferAuthorList: Any? = "",
    @JsonDeserialize(using = AnyDeserializer::class)
    var mrCommentEnable: Any? = "",

    // 路径白名单
    @JsonDeserialize(using = AnyDeserializer::class)
    var pathList: Any? = "",
    // 4.路径屏蔽tab
    @JsonDeserialize(using = AnyDeserializer::class)
    var whileScanPaths: Any? = "", // 目前暂时不用
    @JsonDeserialize(using = AnyDeserializer::class)
    var pathType: Any? = "", // CUSTOM - 自定义 ； DEFAULT - 系统默认（目前之用CUSTOM）
    @JsonDeserialize(using = AnyDeserializer::class)
    var customPath: Any? = "", // 黑名单，添加后的代码路径将不会产生告警
    @JsonDeserialize(using = AnyDeserializer::class)
    var filterDir: Any? = "", // 暂时不用
    @JsonDeserialize(using = AnyDeserializer::class)
    var filterFile: Any? = "", // 暂时不用
    @JsonDeserialize(using = AnyDeserializer::class)
    var scanTestSource: Any? = "", // 是否扫描测试代码，true-扫描，false-不扫描，默认不扫描

    @JsonProperty("BK_CI_REPO_WEB_HOOK_HASHID")
    @JsonDeserialize(using = AnyDeserializer::class)
    var hookRepoId: Any? = "",

    @JsonProperty("BK_CI_REPO_GIT_WEBHOOK_SOURCE_BRANCH")
    @JsonDeserialize(using = AnyDeserializer::class)
    var hookMrSourceBranch: Any? = "",

    @JsonProperty("BK_CI_REPO_GIT_WEBHOOK_TARGET_BRANCH")
    @JsonDeserialize(using = AnyDeserializer::class)
    var hookMrTargetBranch: Any? = ""
)
