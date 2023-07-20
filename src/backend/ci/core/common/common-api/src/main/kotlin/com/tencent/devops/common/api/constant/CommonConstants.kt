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

package com.tencent.devops.common.api.constant

const val BCI_CODE_PREFIX = "BCI_CODE_"
const val DEVOPS = "DevOps"
const val NUM_ONE = 1
const val NUM_TWO = 2
const val NUM_THREE = 3
const val NUM_FOUR = 4
const val NUM_FIVE = 5
const val NUM_SIX = 6
const val NUM_SEVEN = 7
const val NUM_EIGHT = 8
const val NUM_NINE = 9
const val INIT_VERSION = "1.0.0" // 初始化版本
const val BEGIN = "begin" // 开始
const val EDIT = "edit" // 提交信息
const val COMMIT = "commit" // 提交
const val BUILD = "build" // 构建
const val CHECK = "check" // 验证
const val TEST = "test" // 测试
const val CODECC = "codecc" // 代码检查
const val APPROVE = "approve" // 审核
const val END = "end" // 结束
const val SUCCESS = "success" // 成功
const val UNDO = "undo" // 未执行
const val DOING = "doing" // 执行中
const val FAIL = "fail" // 失败
const val ONLINE = "online" // 上线
const val TEST_ENV_PREPARE = "testEnvPrepare" // 准备测试环境
const val ING = "ing" // 中
const val LATEST = "latest" // 最新
const val DEVELOP = "develop" // 开发
const val DEPLOY = "deploy" // 部署
const val SECURITY = "security" // 安全
const val NORMAL = "normal" // 正常
const val EXCEPTION = "exception" // 异常
const val REQUIRED = "required" // 必选
const val MIN_LENGTH = "minLength" // 最小长度
const val MAX_LENGTH = "maxLength" // 最大长度
const val DEFAULT = "default" // 默认
const val JAVA = "java" // java
const val PYTHON = "python" // python
const val NODEJS = "nodejs" // nodejs
const val GOLANG = "golang" // golang
const val JS = "js" // js
const val PATTERN_STYLE = "patternStyle" // 正则表达式规则
const val MESSAGE = "message" // 提示信息
const val STATIC = "static" // 静态资源
const val NAME = "name" // 名称
const val FAIL_NUM = "failNum" // 失败数量
const val VERSION = "version" // 版本号
const val TYPE = "type" // 类型
const val OUTPUT_DESC = "description" // 插件输出字段描述
const val COMPONENT = "component" // 组件
const val PIPELINE_URL = "pipelineUrl" // 流水线链接
const val ARTIFACT = "artifact" // 构件
const val REPORT = "report" // 报告
const val VALUE = "value" // 值
const val MULTIPLE_SELECTOR = "multiple" // 多选
const val SINGLE_SELECTOR = "single" // 单选
const val OPTIONS = "options" // 可选项
const val LABEL = "label" // 标签
const val NO_LABEL = "noLabel" // 无标题
const val URL = "url" // url链接
const val PATH = "path" // 路径
const val ARTIFACTORY_TYPE = "artifactoryType" // 归档仓库类型
const val REPORT_TYPE = "reportType" // 报告类型
const val DATA = "data" // 数据
const val STRING = "string" // 字符串
const val LATEST_MODIFIER = "latestModifier" // 最近修改人
const val LATEST_UPDATE_TIME = "latestUpdateTime" // 最近修改时间
const val LATEST_EXECUTOR = "latestExecutor" // 最近执行人
const val LATEST_EXECUTE_TIME = "latestExecuteTime" // 最近执行时间
const val DANG = "dang" // 当
const val AND = "and" // 和
const val OR = "or" // 或
const val TIMETOSELECT = "timetoSelect" // 时必选
const val MASTER = "master" // 主干
const val SYSTEM = "system" // 系统
const val BUILD_RUNNING = "buildRunning" // 运行中
const val BUILD_QUEUE = "buildQueue" // 构建排队中
const val BUILD_REVIEWING = "buildReviewing" // 构建待审核
const val BUILD_STAGE_SUCCESS = "buildStageSuccess" // 构建阶段性完成
const val BUILD_COMPLETED = "buildCompleted" // 构建完成
const val BUILD_CANCELED = "buildCanceled" // 构建已取消
const val BUILD_FAILED = "buildFailed" // 构建失败
const val ID = "id" // id
const val STATUS = "status" // 状态
const val EXECUTE_COUNT = "executeCount"
const val LOCALE_LANGUAGE = "BK_CI_LOCALE_LANGUAGE" // locale国际化语言信息
const val DEFAULT_LOCALE_LANGUAGE = "zh_CN" // 默认语言信息
const val REQUEST_CHANNEL = "BK_CI_REQUEST_CHANNEL" // 请求渠道
const val BK_CREATE = "bkCreate" // 创建
const val BK_REVISE = "bkRevise" // 修改

const val KEY_START_TIME = "startTime"
const val KEY_END_TIME = "endTime"
const val KEY_CHANNEL = "channel"
const val HIDDEN_SYMBOL = "******"
const val KEY_DEFAULT = "default"
const val KEY_INPUT = "vuex-input"
const val KEY_TEXTAREA = "vuex-textarea"
const val KEY_CODE_EDITOR = "atom-ace-editor"
const val KEY_OS = "os"
const val KEY_SUMMARY = "summary"
const val KEY_DOCSLINK = "docsLink"
const val KEY_DESCRIPTION = "description"
const val KEY_WEIGHT = "weight"
const val KEY_ALL = "all"
const val API_ACCESS_TOKEN_PROPERTY = "access_token"
const val TEMPLATE_ACROSS_INFO_ID = "devops_template_across_info_id"
const val KEY_OS_NAME = "osName"
const val KEY_OS_ARCH = "osArch"
const val KEY_INVALID_OS_INFO = "invalidOsInfo"
const val KEY_VALID_OS_NAME_FLAG = "validOsNameFlag"
const val KEY_VALID_OS_ARCH_FLAG = "validOsArchFlag"
const val KEY_SCRIPT = "script"
const val KEY_COMMIT_ID = "commitId"
const val KEY_BRANCH = "branch"
const val KEY_REPOSITORY_HASH_ID = "repositoryHashId"
const val KEY_REPOSITORY_PATH = "repositoryPath"
const val KEY_VERSION = "version"
const val KEY_VERSION_NAME = "versionName"
const val KEY_UPDATED_TIME = "updatedTime"
const val KEY_DEFAULT_LOCALE_LANGUAGE = "defaultLocaleLanguage"

const val BK_BUILD_ENV_START_FAILED = "bkBuildEnvStartFailed" // 构建环境启动失败
const val BK_START_PULL_IMAGE = "bkStartPullImage" // 开始拉取镜像，镜像名称：
const val BK_PULLING_IMAGE = "bkPullingImage" // 正在拉取镜像,第{0}层，进度：{1}
const val BK_PUSH_IMAGE = "bkPushImage" // 正在推送镜像,第{0}层，进度：{1}
const val BK_HUMAN_SERVICE = "bkHumanService" // 人工服务
