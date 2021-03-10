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
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 流水线导出为yaml时针对CodeCC插件做简化而额外生成的数据类
 * 注释掉的是不需要被导出的
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeCCExportYamlData(
    // codeCC 基础配置参数
    var script: Any? = "",

    var codeCCTaskName: Any? = "",
    var codeCCTaskCnName: Any? = null, // 暂时没用
    var codeCCTaskId: Any? = null, // 调用接口用到

    var checkerSetType: Any? = "",
    var languages: Any? = null, // [PYTHON,KOTLIN]
    var asynchronous: Any? = null,
//    var asyncTask: Boolean? = false,
//    var asyncTaskId: Long? = null,
    var scanType: Any? = "",
    var path: Any? = "",
//    var tools: Any? = null, // [TOOL1,TOOL2]
    var openScanPrj: Any? = null,

    var pyVersion: Any? = null,
    var eslintRc: Any? = null,
    var phpcsStandard: Any? = null,
    var goPath: Any? = null,
    var projectBuildType: Any? = null,
    var projectBuildCommand: Any? = null,
    var ccnThreshold: Int? = null,
    var needCodeContent: Any? = null,
    var coverityToolSetId: Any? = null,
    var klocworkToolSetId: Any? = null,
    var cpplintToolSetId: Any? = null,
    var eslintToolSetId: Any? = null,
    var pylintToolSetId: Any? = null,
    var gometalinterToolSetId: Any? = null,
    var checkStyleToolSetId: Any? = null,
    var styleCopToolSetId: Any? = null,
    var detektToolSetId: Any? = null,
    var phpcsToolSetId: Any? = null,
    var sensitiveToolSetId: Any? = null,
    var occheckToolSetId: Any? = null,
    var ripsToolSetId: Any? = null,
    var gociLintToolSetId: Any? = null,
    var woodpeckerToolSetId: Any? = null,
    var horuspyToolSetId: Any? = null,
    var pinpointToolSetId: Any? = null,

    // 非页面参数
    // 如果指定_CODECC_FILTER_TOOLS，则只做_CODECC_FILTER_TOOLS的扫描
    @JsonProperty("_CODECC_FILTER_TOOLS")
    var filterTools: Any? = null, // [TOOL1,TOOL2]

    @JsonProperty("pipeline.start.channel")
    var channelCode: Any? = "",

    // CodeCC V3 版本参数
    // 1.基础设置tab
    var languageRuleSetMap: Any? = "", // 规则集

    // 2.通知报告tab
    var rtxReceiverType: Any? = null, // rtx接收人类型：0-所有项目成员；1-接口人；2-自定义；3-无
    var rtxReceiverList: Any? = null, // rtx接收人列表，rtxReceiverType=2时，自定义的接收人保存在该字段
    var emailReceiverType: Any? = null, // 邮件收件人类型：0-所有项目成员；1-接口人；2-自定义；3-无
    var emailReceiverList: Any? = null, // 邮件收件人列表，当emailReceiverType=2时，自定义的收件人保存在该字段
    var emailCCReceiverList: Any? = null,
    var reportStatus: Any? = null, // 定时报告任务的状态，有效：1，暂停：2 (目前看了都是1)
    var reportDate: Any? = null,
    var reportTime: Any? = null,
    var instantReportStatus: Any? = null, // 即时报告状态，有效：1，暂停：2
    var reportTools: Any? = null,
    var botWebhookUrl: Any? = null,
    var botRemindSeverity: Any? = null, // 7-总告警数； 3-严重 + 一般告警数；1-严重告警数
    var botRemaindTools: Any? = null,
    var botRemindRange: Any? = null, // 1-新增 2-遗留

    // 3.扫描配置tab
    var toolScanType: Any? = null, // 对应接口的scanType, 1：增量；0：全量 2: diff模式
    var newDefectJudgeFromDate: Any? = null,
    var newDefectJudgeBy: Any? = null, // 判定方式1：按日期；2：按构建(目前都填1)
    var transferAuthorList: Any? = null,
    var mrCommentEnable: Boolean? = null,

    // 路径白名单
    var pathList: Any? = null,
    // 4.路径屏蔽tab
    var whileScanPaths: Any? = null, // 目前暂时不用
    var pathType: Any? = "", // CUSTOM - 自定义 ； DEFAULT - 系统默认（目前之用CUSTOM）
    var customPath: Any? = null, // 黑名单，添加后的代码路径将不会产生告警
    var filterDir: Any? = null, // 暂时不用
    var filterFile: Any? = null, // 暂时不用
    var scanTestSource: Any? = null, // 是否扫描测试代码，true-扫描，false-不扫描，默认不扫描

    @JsonProperty("BK_CI_REPO_WEB_HOOK_HASHID")
    var hookRepoId: Any? = null,

    @JsonProperty("BK_CI_REPO_GIT_WEBHOOK_SOURCE_BRANCH")
    var hookMrSourceBranch: Any? = null,

    @JsonProperty("BK_CI_REPO_GIT_WEBHOOK_TARGET_BRANCH")
    var hookMrTargetBranch: Any? = null
)
