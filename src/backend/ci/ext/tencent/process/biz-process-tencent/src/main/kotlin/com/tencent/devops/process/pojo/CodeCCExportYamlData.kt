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

/**
 * 流水线导出为yaml时针对CodeCC插件做简化而额外生成的数据类
 * 注释掉的是不需要被导出的
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeCCExportYamlData(
    // codeCC 基础配置参数

    var script: Any? = "",

    var codeCCTaskName: Any? = "",

    var codeCCTaskCnName: Any? = "", // 暂时没用

    var codeCCTaskId: Any? = "", // 调用接口用到

    var checkerSetType: Any? = "",

    var languages: Any? = "", // [PYTHON,KOTLIN]

    var asynchronous: Any? = "",
//    var asyncTask: Boolean? = false,
//    var asyncTaskId: Long? = "",

    var scanType: Any? = "",

    var path: Any? = "",
//    var tools: Any? = "", // [TOOL1,TOOL2]

    var openScanPrj: Any? = "",

    var pyVersion: Any? = "",

    var eslintRc: Any? = "",

    var phpcsStandard: Any? = "",

    var goPath: Any? = "",

    var projectBuildType: Any? = "",

    var projectBuildCommand: Any? = "",

    var ccnThreshold: Any? = "",

    var needCodeContent: Any? = "",

    var coverityToolSetId: Any? = "",

    var klocworkToolSetId: Any? = "",

    var cpplintToolSetId: Any? = "",

    var eslintToolSetId: Any? = "",

    var pylintToolSetId: Any? = "",

    var gometalinterToolSetId: Any? = "",

    var checkStyleToolSetId: Any? = "",

    var styleCopToolSetId: Any? = "",

    var detektToolSetId: Any? = "",

    var phpcsToolSetId: Any? = "",

    var sensitiveToolSetId: Any? = "",

    var occheckToolSetId: Any? = "",

    var ripsToolSetId: Any? = "",

    var gociLintToolSetId: Any? = "",

    var woodpeckerToolSetId: Any? = "",

    var horuspyToolSetId: Any? = "",

    var pinpointToolSetId: Any? = "",

    // 非页面参数
    // 如果指定_CODECC_FILTER_TOOLS，则只做_CODECC_FILTER_TOOLS的扫描
    @JsonProperty("_CODECC_FILTER_TOOLS")

    var filterTools: Any? = "", // [TOOL1,TOOL2]

    @JsonProperty("pipeline.start.channel")

    var channelCode: Any? = "",

    // CodeCC V3 版本参数
    // 1.基础设置tab

    var languageRuleSetMap: Any? = "", // 规则集

    // 2.通知报告tab

    var rtxReceiverType: Any? = "", // rtx接收人类型：0-所有项目成员；1-接口人；2-自定义；3-无

    var rtxReceiverList: Any? = "", // rtx接收人列表，rtxReceiverType=2时，自定义的接收人保存在该字段

    var emailReceiverType: Any? = "", // 邮件收件人类型：0-所有项目成员；1-接口人；2-自定义；3-无

    var emailReceiverList: Any? = "", // 邮件收件人列表，当emailReceiverType=2时，自定义的收件人保存在该字段

    var emailCCReceiverList: Any? = "",

    var reportStatus: Any? = "", // 定时报告任务的状态，有效：1，暂停：2 (目前看了都是1)

    var reportDate: Any? = "",

    var reportTime: Any? = "",

    var instantReportStatus: Any? = "", // 即时报告状态，有效：1，暂停：2

    var reportTools: Any? = "",

    var botWebhookUrl: Any? = "",

    var botRemindSeverity: Any? = "", // 7-总告警数； 3-严重 + 一般告警数；1-严重告警数

    var botRemaindTools: Any? = "",

    var botRemindRange: Any? = "", // 1-新增 2-遗留

    // 3.扫描配置tab

    var toolScanType: Any? = "", // 对应接口的scanType, 1：增量；0：全量 2: diff模式

    var newDefectJudgeFromDate: Any? = "",

    var newDefectJudgeBy: Any? = "", // 判定方式1：按日期；2：按构建(目前都填1)

    var transferAuthorList: Any? = "",

    var mrCommentEnable: Any? = "",

    // 路径白名单

    var pathList: Any? = "",
    // 4.路径屏蔽tab

    var whileScanPaths: Any? = "", // 目前暂时不用

    var pathType: Any? = "", // CUSTOM - 自定义 ； DEFAULT - 系统默认（目前之用CUSTOM）

    var customPath: Any? = "", // 黑名单，添加后的代码路径将不会产生告警

    var filterDir: Any? = "", // 暂时不用

    var filterFile: Any? = "", // 暂时不用

    var scanTestSource: Any? = "", // 是否扫描测试代码，true-扫描，false-不扫描，默认不扫描

    @JsonProperty("BK_CI_REPO_WEB_HOOK_HASHID")

    var hookRepoId: Any? = "",

    @JsonProperty("BK_CI_REPO_GIT_WEBHOOK_SOURCE_BRANCH")

    var hookMrSourceBranch: Any? = "",

    @JsonProperty("BK_CI_REPO_GIT_WEBHOOK_TARGET_BRANCH")

    var hookMrTargetBranch: Any? = ""
)
