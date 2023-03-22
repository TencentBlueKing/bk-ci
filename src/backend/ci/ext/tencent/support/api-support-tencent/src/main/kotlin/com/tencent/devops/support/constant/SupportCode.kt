package com.tencent.devops.support.constant

object SupportCode {
    /*const val BK_MESSAGE_ID_ALREADY_EXISTS = "BkMessageIdAlreadyExists" //message id 已存在。
    const val BK_MESSAGE_ID_INSERTED_SUCCESSFULLY = "BkMessageIdInsertedSuccessfully" //message id 插入成功。*/

    const val BK_SESSION_ID = "BkSessionId" //会话ID
    const val BK_GROUP_ID = "BkGroupId" //群ID
    const val BK_GROUP_CHATID = "BkGroupChatid" //本群ChatId
    const val BK_THIS_GROUP_ID = "BkThisGroupId" //本群ID='{0}'。PS:群ID可用于蓝盾平台上任意企业微信群通知。
    const val BK_BLUE_SHIELD_DEVOPS_ROBOT = "BkBlueShieldDevopsRobot" //您好，我是蓝盾DevOps机器人，下面是平台相关链接
    const val BK_PLATFORM_ENTRANCE = "BkPlatformEntrance" //平台入口
    const val BK_DOCUMENT_ENTRY = "BkDocumentEntry" //文档入口
    const val BK_CAN_DO_FOLLOWING = "BkCanDoFollowing" //可以进行以下操作
    const val BK_QUERY_PIPELINE_LIST = "BkQueryPipelineList" //查询流水线列表
    const val BK_YOU_CAN_CLICK = "BkYouCanClick" //如有需要可以点击
    const val BK_MANUAL_CUSTOMER_SERVICE = "BkManualCustomerService" //人工客服
    const val BK_GROUP_BOUND_PROJECT = "BkGroupBoundProject" //本群已绑定【{0}】项目，如需修改请点击：
    const val BK_MODIFY_ROJECT = "BkModifyRoject" //修改项目
    const val BK_NOT_EXECUTION_PERMISSION = "BkNotExecutionPermission" //{0}暂时还没有【{1}】流水线的执行权限，请点击申请执行权限：
    const val BK_APPLICATION_ADDRESS = "BkApplicationAddress" //申请地址
    const val BK_PIPELINE_STARTED_SUCCESSFULLY = "BkPipelineStartedSuccessfully" //流水线【{0}】启动成功，{1}可以点击查看
    const val BK_PIPELINE_EXECUTION_DETAILS = "BkPipelineExecutionDetails" //流水线执行详情
    const val BK_FAILED_START_PIPELINE = "BkFailedStartPipeline" //{0}启动流水线【{1}】失败。
    const val BK_THERE_NO_ITEMS_VIEW = "BkThereNoItemsView" //在蓝盾平台DevOps中没有可以查看的项目
    const val BK_ITEMS_CAN_VIEWED = "BkItemsCanViewed" //下面是{0}在蓝盾DevOps平台中可以查看的项目
    const val BK_AUTOMATICALLY_BIND_RELEVANT_PROJECT = "BkAutomaticallyBindRelevantProject" //PS:选择项目后，本群会自动绑定相关的项目,该消息只允许{0}点击执行
    const val BK_CONSULTING_GROUP = "BkConsultingGroup" //蓝盾DevOps平台咨询群
    const val BK_PLEASE_DESCRIBE_YOUR_PROBLEM = "BkPleaseDescribeYourProblem" //请描述您的问题，并带上相关的URL地址
    const val BK_NEW_CONSULTING_GROUP_PULLED_UP = "BkNewConsultingGroupPulledUp" //已为您拉起新的咨询群，请关注会话列表。
    const val BK_NO_PIPELINE_VIEW = "BkNoPipelineView" //{0}在【{1}】项目中没有可以查看的流水线
    const val BK_FOLLOWING_PIPELINE_CAN_VIEW = "BkFollowingPipelineCanView" //下面是{0}在【{1}】项目中可以查看的流水线
    const val BK_EXECUTION = "BkExecution" //执行
    const val BK_MESSAGE_ALLOWS_CLICK = "BkMessageAllowsClick" //该消息只允许{0}点击执行。
}