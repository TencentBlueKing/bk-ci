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

package com.tencent.devops.worker.common

const val BK_ARCHIVE_PLUGIN_FILE= "bkArchivePluginFile"// 归档插件文件
const val BK_DOWNLOAD_CODECC_TOOL_FAIL = "bkDownloadCodeccToolFail"// 下载Codecc的 {0} 工具失败
const val BK_DOWNLOAD_CODECC_COVERITY_SCRIPT_FAIL = "bkDownloadCodeccCoverityScriptFail"// 下载codecc的coverity的执行脚本失败
const val BK_DOWNLOAD_CODECC_MULTI_TOOL_SCRIPT_FAIL = "bkDownloadCodeccMultiToolScriptFail"// 下载codecc的多工具执行脚本失败
const val BK_UPDATE_IMAGE_MARKET_INFO_FAILED = "bkUpdateImageMarketInfoFailed"// 更新镜像市场信息失败
const val BK_LOGS_END_STATUS_FAILED = "bkLogsEndStatusFailed"// 上报结束状态失败
const val BK_LOGS_REPORT_FAILED = "bkLogsReportFailed"// 上报日志失败
const val BK_LOG_STORAGE_STATUS_FAILED = "bkLogStorageStatusFailed"// 上报日志存储状态失败
const val BK_NOTIFY_SERVER_START_BUILD_FAILED = "bkNotifyServerStartBuildFailed"// 通知服务端启动构建失败
const val BK_RECEIVE_BUILD_MACHINE_TASK_FAILED = "bkReceiveBuildMachineTaskFailed"// 领取构建机任务失败
const val BK_RECEIVE_BUILD_MACHINE_TASK_DETAIL_FAILED = "bkReceiveBuildMachineTaskDetailFailed"// 领取构建机任务详情失败
const val BK_BUILD_FINISH_REQUEST_FAILED = "bkBuildFinishRequestFailed" // 构建完成请求失败
const val BK_REPORT_TASK_FINISH_FAILURE = "bkReportTaskFinishFailure"// 报告任务完成失败
const val BK_HEARTBEAT_FAIL = "bkHeartbeatFail"// 心跳失败
const val BK_GET_BUILD_TASK_DETAILS_FAILURE = "bkGetBuildTaskDetailsFailure"// 获取构建任务详情失败
const val BK_BUILD_TIMEOUT_END_REQUEST_FAILURE = "bkBuildTimeoutEndRequestFailure"// 构建超时结束请求失败
const val BK_GET_TEMPLATE_CROSS_PROJECT_INFO_FAILURE = "bkGetTemplateCrossProjectInfoFailure"// 获取模板跨项目信息失败
const val BK_SAVE_SCRIPT_METADATA_FAILURE = "bkSaveScriptMetadataFailure"// 保存脚本元数据失败
const val BK_REPORT_AGENT_END_STATUS_FAILURE = "bkReportAgentEndStatusFailure"// Quota上报agent运行结束状态失败
const val BK_REPORT_AGENT_START_STATUS_FAILURE = "bkReportAgentStartStatusFailure"// Quota上报agent开始运行失败
const val BK_GET_REPORT_ROOT_PATH_FAILURE = "bkGetReportRootPathFailure"// 获取报告根路径失败
const val BK_CREATE_REPORT_FAIL = "bkCreateReportFail"// 创建报告失败
const val BK_UPLOAD_CUSTOM_REPORT_FAILURE = "bkUploadCustomReportFailure"// 上传自定义报告失败
const val BK_ADD_CODE_BASE_COMMIT_INFO_FAIL = "bkAddCodeBaseCommitInfoFail"// 添加代码库commit信息失败
const val BK_GET_LAST_CODE_BASE_COMMIT_INFO_FAIL = "bkGetLastCodeBaseCommitInfoFail"// 获取最后一次代码commit信息失败
const val BK_ADD_SOURCE_MATERIAL_INFO_FAILURE = "bkAddSourceMaterialInfoFailure"// 添加源材料信息失败
const val BK_GET_OAUTH_INFO_FAIL = "bkGetOauthInfoFail"// 获取oauth认证信息失败
const val BK_GET_CODE_BASE_FAIL = "bkGetCodeBaseFail"// 获取代码库失败
const val BK_GET_CREDENTIAL_FAILED = "bkGetCredentialFailed"// 获取凭证失败
const val BK_START_BUILD_IMAGE_NAME = "bkStartBuildImageName"// 启动构建镜像，镜像名称：
const val BK_WAIT_BUILD_IMAGE_FINISH = "bkWaitBuildImageFinish"// 启动构建镜像成功，等待构建镜像结束，镜像名称：
const val BK_BUILD_IMAGE_FAIL_DETAIL = "bkBuildImageFailDetail"// 构建镜像失败，错误详情：
const val BK_BUILD_IMAGE_SUCCEED = "bkBuildImageSucceed"// 构建镜像成功！
const val BK_DOCKERFILE_FIRST_LINE_CHECK = "bkDockerfileFirstLineCheck"// Dockerfile第一行请确认使用 {0}
const val BK_START_BUILD_IMAGE_FAIL = "bkStartBuildImageFail"// 启动构建镜像失败！请联系【蓝盾助手】
const val BK_QUERY_BUILD_IMAGE_STATUS_FAIL = "bkStartBuildImageFail"// 查询构建镜像状态失败！请联系【蓝盾助手】
const val BK_START_BUILD_FAIL = "bkStartBuildFail"// 启动构建失败！请联系【蓝盾助手】
const val BK_FOLDER_NOT_EXIST = "bkFolderNotExist"// 文件夹{0}不存在
const val BK_ENTRANCE_FILE_NOT_IN_FOLDER = "bkEntranceFileNotInFolder"// 入口文件({0})不在文件夹({1})下
const val BK_ENTRANCE_FILE_CHECK_FINISH = "bkEntranceFileCheckFinish"// 入口文件检测完成
const val BK_UPLOAD_CUSTOM_OUTPUT_SUCCESS = "bkUploadCustomOutputSuccess"// 上传自定义产出物成功，共产生了{0}个文件
const val BK_NO_MATCHING_ARCHIVE_FILE = "bkNoMatchingArchiveFile"// 没有匹配到任何待归档文件，请检查工作空间下面的文件
const val BK_GIT_CREDENTIAL_ILLEGAL = "bkGitCredentialIllegal"// git凭据不合法
const val BK_CODE_REPO_PARAM_NOT_IN_PARAMS = "bkCodeRepoIdNotInParams"// 代码仓库{0}没存在参数中
const val BK_REPORT_START_ERROR_INFO_FAIL = "bkReportStartErrorInfoFail"// 上报启动异常信息失败
