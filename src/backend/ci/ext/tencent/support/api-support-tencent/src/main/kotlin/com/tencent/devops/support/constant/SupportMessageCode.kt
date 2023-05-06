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

package com.tencent.devops.support.constant

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-环境 06：experience-版本体验 07：image-镜像 08：log-日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店 21： auth-权限 22:sign-签名服务 23:metrics-度量服务 24：external-外部
 *    25：prebuild-预建 26:dispatcher-kubernetes 27：buildless 28: lambda 29: stream  30: worker 31: dispatcher-docker）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2023-3-20
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object SupportMessageCode {
    const val UPLOAD_FILE_TYPE_IS_NOT_SUPPORT = "21170001" //{0}类型文件不支持上传，您可以上传{1}类型文件
    const val UPLOAD_FILE_IS_TOO_LARGE = "21170002" //上传的文件不能超过{0}

    const val BK_GROUP_CHATID = "bkGroupChatid" //本群ChatId
    const val BK_BLUE_SHIELD_DEVOPS_ROBOT = "bkBlueShieldDevopsRobot" //您好，我是蓝盾DevOps机器人，下面是平台相关链接
    const val BK_PLATFORM_ENTRANCE = "bkPlatformEntrance" //平台入口
    const val BK_DOCUMENT_ENTRY = "bkDocumentEntry" //文档入口
    const val BK_CAN_DO_FOLLOWING = "bkCanDoFollowing" //可以进行以下操作
    const val BK_QUERY_PIPELINE_LIST = "bkQueryPipelineList" //查询流水线列表
    const val BK_YOU_CAN_CLICK = "bkYouCanClick" //如有需要可以点击
    const val BK_MANUAL_CUSTOMER_SERVICE = "bkManualCustomerService" //人工客服
    const val BK_GROUP_BOUND_PROJECT = "bkGroupBoundProject" //本群已绑定【{0}】项目，如需修改请点击：
    const val BK_MODIFY_ROJECT = "bkModifyRoject" //修改项目
    const val BK_NOT_EXECUTION_PERMISSION = "bkNotExecutionPermission" //{0}暂时还没有【{1}】流水线的执行权限，请点击申请执行权限：
    const val BK_APPLICATION_ADDRESS = "bkApplicationAddress" //申请地址
    const val BK_PIPELINE_STARTED_SUCCESSFULLY = "bkPipelineStartedSuccessfully" //流水线【{0}】启动成功，{1}可以点击查看
    const val BK_PIPELINE_EXECUTION_DETAILS = "bkPipelineExecutionDetails" //流水线执行详情
    const val BK_FAILED_START_PIPELINE = "bkFailedStartPipeline" //{0}启动流水线【{1}】失败。
    const val BK_THERE_NO_ITEMS_VIEW = "bkThereNoItemsView" //在蓝盾平台DevOps中没有可以查看的项目
    const val BK_ITEMS_CAN_VIEWED = "bkItemsCanViewed" //下面是{0}在蓝盾DevOps平台中可以查看的项目
    const val BK_AUTOMATICALLY_BIND_RELEVANT_PROJECT = "bkAutomaticallyBindRelevantProject" //PS:选择项目后，本群会自动绑定相关的项目,该消息只允许{0}点击执行
    const val BK_CONSULTING_GROUP = "bkConsultingGroup" //蓝盾DevOps平台咨询群
    const val BK_PLEASE_DESCRIBE_YOUR_PROBLEM = "bkPleaseDescribeYourProblem" //请描述您的问题，并带上相关的URL地址
    const val BK_NEW_CONSULTING_GROUP_PULLED_UP = "bkNewConsultingGroupPulledUp" //已为您拉起新的咨询群，请关注会话列表。
    const val BK_NO_PIPELINE_VIEW = "bkNoPipelineView" //{0}在【{1}】项目中没有可以查看的流水线
    const val BK_FOLLOWING_PIPELINE_CAN_VIEW = "bkFollowingPipelineCanView" //下面是{0}在【{1}】项目中可以查看的流水线
    const val BK_EXECUTION = "bkExecution" //执行
    const val BK_MESSAGE_ALLOWS_CLICK = "bkMessageAllowsClick" //该消息只允许{0}点击执行。
}
