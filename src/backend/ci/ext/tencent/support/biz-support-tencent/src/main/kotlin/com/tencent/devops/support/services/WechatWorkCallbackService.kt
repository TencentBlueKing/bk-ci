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

package com.tencent.devops.support.services

import com.tencent.devops.common.api.constant.I18NConstant.BK_GROUP_ID
import com.tencent.devops.common.api.constant.I18NConstant.BK_SESSION_ID
import com.tencent.devops.common.api.constant.I18NConstant.BK_THIS_GROUP_ID
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.common.wechatwork.model.Constants.PROJECT
import com.tencent.devops.common.wechatwork.model.Constants.SERVICE_HUMAN
import com.tencent.devops.common.wechatwork.model.enums.EventType
import com.tencent.devops.common.wechatwork.model.enums.FromType
import com.tencent.devops.common.wechatwork.model.enums.MsgType
import com.tencent.devops.common.wechatwork.model.enums.ReceiverType
import com.tencent.devops.common.wechatwork.model.sendmessage.Receiver
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextClick
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextClickLink
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextContent
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMentioned
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMentionedMentioned
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMessage
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextTextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextView
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextViewLink
import com.tencent.devops.process.api.user.UserBuildResource
import com.tencent.devops.process.api.user.UserPipelineResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.utils.PIPELINE_VIEW_ALL_PIPELINES
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.support.constant.SupportMessageCode.BK_APPLICATION_ADDRESS
import com.tencent.devops.support.constant.SupportMessageCode.BK_AUTOMATICALLY_BIND_RELEVANT_PROJECT
import com.tencent.devops.support.constant.SupportMessageCode.BK_BLUE_SHIELD_DEVOPS_ROBOT
import com.tencent.devops.support.constant.SupportMessageCode.BK_CAN_DO_FOLLOWING
import com.tencent.devops.support.constant.SupportMessageCode.BK_CONSULTING_GROUP
import com.tencent.devops.support.constant.SupportMessageCode.BK_DOCUMENT_ENTRY
import com.tencent.devops.support.constant.SupportMessageCode.BK_EXECUTION
import com.tencent.devops.support.constant.SupportMessageCode.BK_FAILED_START_PIPELINE
import com.tencent.devops.support.constant.SupportMessageCode.BK_FOLLOWING_PIPELINE_CAN_VIEW
import com.tencent.devops.support.constant.SupportMessageCode.BK_GROUP_BOUND_PROJECT
import com.tencent.devops.support.constant.SupportMessageCode.BK_ITEMS_CAN_VIEWED
import com.tencent.devops.support.constant.SupportMessageCode.BK_MANUAL_CUSTOMER_SERVICE
import com.tencent.devops.support.constant.SupportMessageCode.BK_MESSAGE_ALLOWS_CLICK
import com.tencent.devops.support.constant.SupportMessageCode.BK_MODIFY_ROJECT
import com.tencent.devops.support.constant.SupportMessageCode.BK_NEW_CONSULTING_GROUP_PULLED_UP
import com.tencent.devops.support.constant.SupportMessageCode.BK_NOT_EXECUTION_PERMISSION
import com.tencent.devops.support.constant.SupportMessageCode.BK_NO_PIPELINE_VIEW
import com.tencent.devops.support.constant.SupportMessageCode.BK_PIPELINE_EXECUTION_DETAILS
import com.tencent.devops.support.constant.SupportMessageCode.BK_PIPELINE_STARTED_SUCCESSFULLY
import com.tencent.devops.support.constant.SupportMessageCode.BK_PLATFORM_ENTRANCE
import com.tencent.devops.support.constant.SupportMessageCode.BK_PLEASE_DESCRIBE_YOUR_PROBLEM
import com.tencent.devops.support.constant.SupportMessageCode.BK_QUERY_PIPELINE_LIST
import com.tencent.devops.support.constant.SupportMessageCode.BK_THERE_NO_ITEMS_VIEW
import com.tencent.devops.support.constant.SupportMessageCode.BK_YOU_CAN_CLICK
import com.tencent.devops.support.dao.WechatWorkMessageDAO
import com.tencent.devops.support.dao.WechatWorkProjectDAO
import com.tencent.devops.support.model.wechatwork.enums.EventKeyType
import org.dom4j.Element
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WechatWorkCallbackService @Autowired constructor(
    private val dslContext: DSLContext,
    private val wechatWorkProjectDAO: WechatWorkProjectDAO,
    private val wechatWorkMessageDAO: WechatWorkMessageDAO,
    private val bsAuthProjectApi: AuthProjectApi,
    private val wechatWorkService: WechatWorkService,
    private val pipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val client: Client
) {
    private val logger = LoggerFactory.getLogger(WechatWorkCallbackService::class.java)

    fun callbackPost(signature: String, timestamp: Long, nonce: String, reqData: String?): Boolean {

        logger.info("signature:$signature")
        logger.info("timestamp:$timestamp")
        logger.info("nonce:$nonce")
        logger.info("reqData:$reqData")
        val callbackElement = wechatWorkService.getCallbackInfo(signature, timestamp, nonce, reqData)

        val chatId = callbackElement.chatId
        val receiverType: ReceiverType
        // 用户ID,企业微信自己维护的
        val userId: String
        // 用户名，也就是英文名，rtx名称
        val userName: String

        // 以回调消息类型来做逻辑区分

        var mentionType = "0"
        // 对于在群里面的没有@机器人的都直接忽略：
        if (callbackElement.fromType == FromType.group) {
            try {
                mentionType = (callbackElement.msgElement.elementIterator("MentionedType").next() as Element).text
            } catch (e: Exception) {
                logger.info("This group does not have mention element")
            }
            // 当被没有被@到,而且不为事件的时候，不做任何处理
            if (mentionType == "0" && callbackElement.msgType != MsgType.Event) {
                return true
            }

            // 转换成
            userId = (callbackElement.fromElement.elementIterator("Sender").next() as Element).text
            receiverType = ReceiverType.group
        } else {
            userId = (callbackElement.fromElement.elementIterator("Id").next() as Element).text
            receiverType = ReceiverType.single
        }

        userName = wechatWorkService.getUserNameByUserId(userId)

        // 处理msgid,个人会话，群会发并@，click事件
        if ((receiverType == ReceiverType.group && mentionType != "0") || receiverType == ReceiverType.single || (callbackElement.msgType == MsgType.Event && EventType.valueOf(
                (callbackElement.msgElement.elementIterator("Event").next() as Element).text
            ) == EventType.click)
        ) {
            val msgId = (callbackElement.msgElement.elementIterator("MsgId").next() as Element).text
            if (wechatWorkMessageDAO.exist(dslContext, msgId)) {
                // 已存在则直接返回
                return true
            } else {
                // 还没有存在则插入并往下走
                wechatWorkMessageDAO.insertMassageId(dslContext, msgId)
            }
        }

        // 从这里开始统一处理消息
        when (callbackElement.msgType) {
            MsgType.emotion, MsgType.file, MsgType.forward, MsgType.image, MsgType.vocie -> {
                // 对于@，文件，转发，图片，声音都不做自动回复。
//                autoReplyMessage(receiverType, chatId, userName)
            }

            MsgType.Event -> {
                // 事件类型
                var eventType =
                    EventType.valueOf((callbackElement.msgElement.elementIterator("Event").next() as Element).text)
                var eventKey = ""
                if (eventType == EventType.click) {
                    eventKey = (callbackElement.msgElement.elementIterator("EventKey").next() as Element).text
                }
//                eventKey = "project:freytest:pipeline:list"
                when (eventType) {
                    EventType.click -> processClickEvent(eventKey, receiverType, chatId, userName)
//                    EventType.enter_chat -> autoReplyMessage(receiverType, chatId, userName)
                }
                logger.info("accept a event callback:$eventType")
            }

            MsgType.text -> { // 注意这个块
                val content = (callbackElement.msgElement.elementIterator("Content").next() as Element).text
                // 针对文本内容的@情况进行处理

                // 当被@到,但没有@到机器人的名字的时候也不做处理。 @CI-Notice(蓝盾机器人) @xiaolandebug(小蓝调试)
                if (receiverType == ReceiverType.group && !content.contains("@CI-Notice") && !content.contains("@xiaolandebug")) {
                    return true
                }
                logger.info("content = $content")
                // 返回群会话ID关键词
                if (receiverType == ReceiverType.group && (content.contains(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_SESSION_ID,
                            language = I18nUtil.getLanguage(userId)
                        ), true) || content.contains(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_GROUP_ID,
                            language = I18nUtil.getLanguage(userId)
                        ),
                        true
                    ))
                ) {
                    logger.info("chatId = $chatId")
                    val receiver = Receiver(receiverType, chatId)
                    val richtextContentList = mutableListOf<RichtextContent>()
                    richtextContentList.add(RichtextText(RichtextTextText(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_THIS_GROUP_ID,
                            language = I18nUtil.getLanguage(userId),
                            params = arrayOf(chatId)
                        )
                    )))
                    val richtextMessage = RichtextMessage(receiver, richtextContentList)
                    wechatWorkService.sendRichText(richtextMessage)
                } else {
                    autoReplyMessage(receiverType, chatId, userName)
                }
            }
        }

        return true
    }

    fun callbackGet(signature: String, timestamp: Long, nonce: String, echoStr: String?): String {
        return wechatWorkService.verifyURL(signature, timestamp, nonce, echoStr)
    }

    /*
    * 自动回复功能
    * */
    fun autoReplyMessage(receiverType: ReceiverType, chatId: String, userName: String) {
        val receiver = Receiver(receiverType, chatId)
        val nickName = getNickName(receiverType, userName)
        val richtextContentList = mutableListOf<RichtextContent>()
        // 只有在群的时候才@发信息的人。
        sendMetionMessage(receiverType, chatId, userName)
        // 群里回复不用带介绍信息
        if (receiverType == ReceiverType.single) {
            richtextContentList.add(RichtextText(RichtextTextText(MessageUtil.getMessageByLocale(
                messageCode = BK_BLUE_SHIELD_DEVOPS_ROBOT,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            ) + "：\n")))
            richtextContentList.add(
                RichtextView(
                    RichtextViewLink(
                        "  " + MessageUtil.getMessageByLocale(
                            messageCode = BK_PLATFORM_ENTRANCE,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                        ) + "  ",
                        "${HomeHostUtil.innerServerHost()}",
                        1
                    )
                )
            )
            richtextContentList.add(RichtextView(RichtextViewLink("  " + MessageUtil.getMessageByLocale(
                messageCode = BK_DOCUMENT_ENTRY,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            ) + "  ", "http://docs.devops.oa.com", 1)))
            richtextContentList.add(RichtextText(RichtextTextText("\n")))
        }

        richtextContentList.add(RichtextText(RichtextTextText("${nickName}" +
                MessageUtil.getMessageByLocale(
                    messageCode = BK_CAN_DO_FOLLOWING,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                ) + "：\n")))

        if (receiverType == ReceiverType.single) {
            // 个人会话的时候，每次需要选择项目
            richtextContentList.add(
                RichtextClick(
                    RichtextClickLink(
                        "  " + MessageUtil.getMessageByLocale(
                            messageCode = BK_QUERY_PIPELINE_LIST,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                        ) + "\n",
//                    "stopPipeline"
                        "$userName:$PROJECT"
                    )
                )
            )
        } else {
            // 群会话的时候，需要判断是否已经绑定项目
            val wechatWorkProjectRecord = wechatWorkProjectDAO.getByGroupId(dslContext, chatId)
            if (wechatWorkProjectRecord == null) {
                // 还没有绑定流水线，需要选择
                richtextContentList.add(
                    RichtextClick(
                        RichtextClickLink(
                            "  " + MessageUtil.getMessageByLocale(
                                messageCode = BK_QUERY_PIPELINE_LIST,
                                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                            ) + "\n",
//                        "stopPipeline"
                            "$userName:$PROJECT"
                        )
                    )
                )
            } else {
                val projectId = wechatWorkProjectRecord.projectId
                // 查看项目id下面的流水线
                richtextContentList.add(
                    RichtextClick(
                        RichtextClickLink(
                            "  " + MessageUtil.getMessageByLocale(
                                messageCode = BK_QUERY_PIPELINE_LIST,
                                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                            ) + "\n",
//                        "stopPipeline"
                            "$userName:project:$projectId:pipeline:list"
                        )
                    )
                )
            }
        }

        // 只有个人会话的时候才会有人工客服
        if (receiverType == ReceiverType.single) {
            richtextContentList.add(RichtextText(RichtextTextText(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_YOU_CAN_CLICK,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
            )))
            richtextContentList.add(
                RichtextClick(
                    RichtextClickLink(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_MANUAL_CUSTOMER_SERVICE,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                        ) + "\n",
                        "$userName:$SERVICE_HUMAN"

                    )
                )
            )
        } else {
            // 当群已经绑定项目
            if (wechatWorkProjectDAO.exist(dslContext, chatId)) {
                val wechatWorkProjectRecord = wechatWorkProjectDAO.getByGroupId(dslContext, chatId)
                if (wechatWorkProjectRecord != null) {
                    val projectName = getProjectNameByProjectCode(wechatWorkProjectRecord.projectId)
                    richtextContentList.add(RichtextText(RichtextTextText(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_GROUP_BOUND_PROJECT,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                            params = arrayOf(projectName)
                        )
                    )))
                    richtextContentList.add(
                        RichtextClick(
                            RichtextClickLink(
                                MessageUtil.getMessageByLocale(
                                    messageCode = BK_MODIFY_ROJECT,
                                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                                ) + "\n",
                                "$userName:$PROJECT"
                            )
                        )
                    )
                }
            }
        }

        val richtextMessage = RichtextMessage(
            receiver,
            richtextContentList
        )

        // 发送自动回复信息
        wechatWorkService.sendRichText(richtextMessage)
    }

//    /*
//    * 分发纯文本事件。
//    * */
//    fun processTextEvent(content:String,receiverType:ReceiverType, chatId:String, userName: String) {
//        //获取事件类型
//        var eventKeyType = getEventKeyType(eventKey)
//        if(eventKeyType != null) {
//            when(eventKeyType) {
//                EventKeyType.PROJECT_PIPELINE_LIST -> processClickEventPipelineList(eventKey,receiverType,chatId,userName)
//                EventKeyType.SERVICE_HUMAN -> processClickEventServiceHuman(userName)
//                EventKeyType.PROJECT -> processClickEventProject(receiverType,chatId,userName)
//                EventKeyType.PROJECT_PIPELINE_START -> processClickEventPipelineStart(eventKey,receiverType,chatId,userName)
//            }
//
//        }
//    }

    /*
    * 分发不同的事件。
    * */
    fun processClickEvent(eventKey: String, receiverType: ReceiverType, chatId: String, userName: String) {
        // 点击事件是否有权限,没有则直接返回
        val eventKeySplit = eventKey.split(":")
        val clickUsername = eventKeySplit[0]
        if (clickUsername != userName) {
            return
        }
        // 获取事件类型
        var eventKeyType = getEventKeyType(eventKey)
        if (eventKeyType != null) {
            when (eventKeyType) {
                EventKeyType.PROJECT_PIPELINE_LIST -> processClickEventPipelineList(
                    eventKey,
                    receiverType,
                    chatId,
                    userName
                )

                EventKeyType.SERVICE_HUMAN -> processClickEventServiceHuman(userName)
                EventKeyType.PROJECT -> processClickEventProject(receiverType, chatId, userName)
//                EventKeyType.PROJECT_PIPELINE_START ->  logger.info("wechat work pipeline start is off")
                EventKeyType.PROJECT_PIPELINE_START -> processClickEventPipelineStart(
                    eventKey,
                    receiverType,
                    chatId,
                    userName
                )

                else -> {}
            }
        }
    }

    /*
    * 处理启动流水线事件
    *
    * */
    fun processClickEventPipelineStart(eventKey: String, receiverType: ReceiverType, chatId: String, userName: String) {

        // 暂时停止启动流水线的功能
        val nickName = getNickName(receiverType, userName)
        val splitList = eventKey.split(":")
        val projectCode = splitList[2]
        val pipelineId = splitList[4]
        val pipelineName = splitList[6]

        // 组装返回结果
        val receiver = Receiver(receiverType, chatId)
        val richtextContentList = mutableListOf<RichtextContent>()
        // 只有在群的时候才@发信息的人。
        sendMetionMessage(receiverType, chatId, userName)

        val manualStartupInfo: Result<BuildManualStartupInfo>
        // 执行流水线的信息，获取启动参数
        try {
            // 正常获取到执行权限的时候
            manualStartupInfo = client.get(UserBuildResource::class).manualStartupInfo(
                userName,
                projectCode,
                pipelineId
            )
            // 判断是否能够启动
            // 判断是否能够启动
            if (!manualStartupInfo.isOk() || manualStartupInfo.data == null || !(manualStartupInfo.data as BuildManualStartupInfo).canManualStartup) {
                // 不能启动
                val permissionUrl =
                    "${HomeHostUtil.innerServerHost()}/console/perm/apply-subsystem?project_code=$projectCode&client_id=pipeline&req_id=$pipelineId"

                richtextContentList.add(RichtextText(RichtextTextText(
                    MessageUtil.getMessageByLocale(
                        messageCode = BK_NOT_EXECUTION_PERMISSION,
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                        params = arrayOf(nickName, pipelineName)
                    ))))
                richtextContentList.add(RichtextView(RichtextViewLink(
                    MessageUtil.getMessageByLocale(
                        messageCode = BK_APPLICATION_ADDRESS,
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                    ), permissionUrl, 1)))
            } else {
                // 组装启动参数
                val params = mutableMapOf<String, String>()
                val properties = (manualStartupInfo.data as BuildManualStartupInfo).properties
                properties.forEach {
                    params.put(it.id, it.defaultValue.toString())
                }

                // 启动流水线
                val manualStartResult = client.get(UserBuildResource::class).manualStartup(
                    userName,
                    projectCode,
                    pipelineId,
                    params
                )
                if (manualStartResult.isOk()) {
                    richtextContentList.add(RichtextText(RichtextTextText(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_PIPELINE_STARTED_SUCCESSFULLY,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                            params = arrayOf(pipelineName, nickName)
                        ))))
                    val buildId = (manualStartResult.data as BuildId).id
                    val buildUrl =
                        "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectCode/$pipelineId/detail/$buildId"
                    richtextContentList.add(RichtextView(RichtextViewLink(MessageUtil.getMessageByLocale(
                        messageCode = BK_PIPELINE_EXECUTION_DETAILS,
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                    ) + "\n", buildUrl, 1)))
                } else {
                    richtextContentList.add(RichtextText(RichtextTextText(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_FAILED_START_PIPELINE,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                            params = arrayOf(nickName, pipelineName)
                        )
                    )))
                }
            }
        } catch (e: RemoteServiceException) {
            // 没有执行权限的时候
            when (e.httpStatus) {
                403 -> {
                    val permissionUrl =
                        "${HomeHostUtil.innerServerHost()}/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=$projectCode&service_code=pipeline&role_executor=pipeline:$pipelineId"
                    richtextContentList.add(RichtextText(RichtextTextText(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_NOT_EXECUTION_PERMISSION,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                            params = arrayOf(nickName, pipelineName)
                        )
                    )))
                    richtextContentList.add(RichtextView(RichtextViewLink(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_APPLICATION_ADDRESS,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                        ), permissionUrl, 1)))
                }

                else -> {
                    richtextContentList.add(RichtextText(RichtextTextText(e.errorMessage)))
                }
            }
        }

        val richtextMessage = RichtextMessage(
            receiver,
            richtextContentList
        )
        wechatWorkService.sendRichText(richtextMessage)
    }

    /*
    * 处理获取项目列表事件
    *
    * */
    fun processClickEventProject(receiverType: ReceiverType, chatId: String, userName: String) {

        val nickName = getNickName(receiverType, userName)
        // 组装返回结果
        val receiver = Receiver(receiverType, chatId)
        val richtextContentList = mutableListOf<RichtextContent>()
        // 只有在群的时候才@发信息的人。
        sendMetionMessage(receiverType, chatId, userName)

        // 获取流水线列表
        val projectList = bsAuthProjectApi.getUserProjectsAvailable(pipelineAuthServiceCode, userName, null)

        if (projectList.isEmpty()) {
            // 没有可以查看的项目
            richtextContentList.add(RichtextText(RichtextTextText("${nickName}"+
                    MessageUtil.getMessageByLocale(
                        messageCode = BK_THERE_NO_ITEMS_VIEW,
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                    ) + "。\n")))
        } else {
            // 有项目的时候
            richtextContentList.add(RichtextText(RichtextTextText(MessageUtil.getMessageByLocale(
                messageCode = BK_ITEMS_CAN_VIEWED,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                params = arrayOf(nickName)
            ) + ":\n")))
            projectList.forEach {
                richtextContentList.add(
                    RichtextClick(
                        RichtextClickLink(
                            "${it.value}\n",
                            "$userName:project:${it.key}:pipeline:list"
                        )
                    )
                )
            }
        }
        if (receiverType == ReceiverType.group) {
            // 群选择一次之后会自动绑定项目ID
            richtextContentList.add(RichtextText(RichtextTextText(MessageUtil.getMessageByLocale(
                messageCode = BK_AUTOMATICALLY_BIND_RELEVANT_PROJECT,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                params = arrayOf(userName)
            ) + "。\n")))
        }
        val richtextMessage = RichtextMessage(
            receiver,
            richtextContentList
        )
        wechatWorkService.sendRichText(richtextMessage)
    }

    /*
    * 处理人工服务事件
    *
    * */
    fun processClickEventServiceHuman(userName: String) {
        // 逻辑迁移
        val userNameList = listOf("brandonliu", "zanyzhao", userName)
        val newChatId = wechatWorkService.createChatByUserNames(
            MessageUtil.getMessageByLocale(
                messageCode = BK_CONSULTING_GROUP,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            ), userNameList)
        wechatWorkService.sendTextGroup(
            MessageUtil.getMessageByLocale(
                messageCode = BK_PLEASE_DESCRIBE_YOUR_PROBLEM,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            ), newChatId)
        wechatWorkService.sendTextSingle(
            MessageUtil.getMessageByLocale(
                messageCode = BK_NEW_CONSULTING_GROUP_PULLED_UP,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            ), userName)
    }

    /*
    * 处理获取流水线列表事件
    * */
    fun processClickEventPipelineList(eventKey: String, receiverType: ReceiverType, chatId: String, userName: String) {
        val nickName = getNickName(receiverType, userName)

        val splitList = eventKey.split(":")
        val projectCode = splitList[2]
        val projectName = getProjectNameByProjectCode(projectCode)

        // 当会话是群会话，自动绑定最新的项目
        if (receiverType == ReceiverType.group) {
            wechatWorkProjectDAO.setProjectIdforGroupId(dslContext, projectCode, chatId)
        }
        val pipelineList = mutableListOf<Pipeline>()
        // 获取流水线列表
        val userViewResult = client.get(UserPipelineResource::class).listViewPipelines(
            userId = userName,
            projectId = projectCode,
            page = null,
            pageSize = null,
            filterByPipelineName = null,
            filterByCreator = null,
            filterByLabels = null,
            filterByViewIds = null,
            viewId = PIPELINE_VIEW_ALL_PIPELINES,
            collation = PipelineCollation.DEFAULT
        )
        if (userViewResult.data != null && (userViewResult.data as PipelineViewPipelinePage).records.isNotEmpty()) {
            pipelineList.addAll((userViewResult.data as PipelineViewPipelinePage).records)
        }

        // 组装返回结果
        val receiver = Receiver(receiverType, chatId)
        val richtextContentList = mutableListOf<RichtextContent>()

        // 只有在群的时候才@发信息的人。
        sendMetionMessage(receiverType, chatId, userName)
        if (pipelineList.isEmpty()) {
            // 没有可以查看的流水线
            richtextContentList.add(RichtextText(RichtextTextText(MessageUtil.getMessageByLocale(
                messageCode = BK_NO_PIPELINE_VIEW,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                params = arrayOf(nickName, projectName)
            ) + "。\n")))
        } else {
            // 有流水线的时候
            richtextContentList.add(RichtextText(RichtextTextText(MessageUtil.getMessageByLocale(
                messageCode = BK_FOLLOWING_PIPELINE_CAN_VIEW,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                params = arrayOf(nickName, projectName)
            ) + ":\n")))
            pipelineList.forEach {
                val pipelineId = it.pipelineId
                val pipelineName = it.pipelineName
                val hasPermission = it.hasPermission
                val canManualStartup = it.canManualStartup
                val startKey = "$userName:project:$projectCode:pipeline:$pipelineId:pipelineName:$pipelineName:start"
                var getKey = "$userName:project:$projectCode:pipeline:$pipelineId:pipelineName:$pipelineName:get"
                var stopKey = "$userName:project:$projectCode:pipeline:$pipelineId:pipelineName:$pipelineName:stop"
//                richtextContentList.add(RichtextClick(RichtextClickLink(" 查看 ",getKey)))
                if (hasPermission && canManualStartup) {
                    // 拥有权限的可以执行
                    richtextContentList.add(RichtextText(RichtextTextText("  $pipelineName")))
                    richtextContentList.add(RichtextClick(RichtextClickLink("  "+
                            MessageUtil.getMessageByLocale(
                                messageCode = BK_EXECUTION,
                                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                            ) +" ", startKey)))
                    richtextContentList.add(RichtextText(RichtextTextText("\n")))
                } else {
                    // 暂时去掉没有执行权限的流水线
                    // 没有权限的需要去申请
//                    var permissionUrl = "${HomeHostUtil.innerServerHost()}/console/perm/apply-subsystem?project_code=$projectCode&client_id=pipeline&req_id=$pipelineId"
                    //
//                    richtextContentList.add(RichtextView(RichtextViewLink(" 申请权限 ",permissionUrl,1)))
                }
//              richtextContentList.add(RichtextClick(RichtextClickLink(" 停止 ",stopKey)))
            }
        }

        addPromptMessage(receiverType, richtextContentList, userName)

        val richtextMessage = RichtextMessage(
            receiver,
            richtextContentList
        )
        wechatWorkService.sendRichText(richtextMessage)
    }

    /*
    * 根据事件的Key进行分类
    * */
    fun getEventKeyType(eventKey: String): EventKeyType? {
        EventKeyType.values().forEach {
            if (it.reg.matches(eventKey)) {
                return it
            }
        }
        return null
    }

    /*
    * 根据项目code获取项目名称
    * */
    fun getProjectNameByProjectCode(projectCode: String): String {
        val projectInfo = client.get(ServiceProjectResource::class).get(projectCode).data
        return projectInfo?.projectName ?: projectCode
    }

    /*
    * 根据项目code获取项目名称
    * */
    fun sendMetionMessage(receiverType: ReceiverType, chatId: String, userName: String) {
        if (receiverType != ReceiverType.group) {
            return
        }
        val richtextContentList = mutableListOf<RichtextContent>()
        richtextContentList.add(RichtextMentioned(RichtextMentionedMentioned(listOf(userName))))
        val receiver = Receiver(ReceiverType.group, chatId)
        val richtextMessage = RichtextMessage(
            receiver,
            richtextContentList
        )
        wechatWorkService.sendRichText(richtextMessage)
    }

    /*
    * 根据项目code获取项目名称
    * */
    fun addPromptMessage(
        receiverType: ReceiverType,
        richtextContentList: MutableList<RichtextContent>,
        userName: String
    ): List<RichtextContent> {
        if (receiverType == ReceiverType.group) {
            richtextContentList.add(RichtextText(RichtextTextText("\nPS:" + MessageUtil.getMessageByLocale(
                messageCode = BK_MESSAGE_ALLOWS_CLICK,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                params = arrayOf(userName)
            ))))
        }
        return richtextContentList
    }

    /*
    * 获取昵称
    * */
    fun getNickName(receiverType: ReceiverType, userName: String): String {
        return if (receiverType == ReceiverType.single) {
            "You"
        } else {
            "${userName}You"
        }
    }
}
