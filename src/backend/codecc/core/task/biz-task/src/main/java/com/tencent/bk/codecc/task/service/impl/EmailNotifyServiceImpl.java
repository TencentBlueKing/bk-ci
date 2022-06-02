/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.task.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.math.IntMath;
import com.tencent.bk.codecc.defect.api.ServiceDefectTreeResource;
import com.tencent.bk.codecc.defect.api.ServiceTaskLogOverviewResource;
import com.tencent.bk.codecc.defect.api.ServiceTaskLogRestResource;
import com.tencent.bk.codecc.defect.api.UserOverviewRestResource;
import com.tencent.bk.codecc.defect.vo.CodeRepoVO;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.defect.vo.TaskPersonalStatisticwVO;
import com.tencent.bk.codecc.quartz.pojo.JobExternalDto;
import com.tencent.bk.codecc.quartz.pojo.OperationType;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.dao.CommonDao;
import com.tencent.bk.codecc.task.dao.mongorepository.CustomProjRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.EmailNotifyMessageTemplateRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.WeChatNotifyMessageTemplateRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao;
import com.tencent.bk.codecc.task.enums.EmailType;
import com.tencent.bk.codecc.task.model.CustomProjEntity;
import com.tencent.bk.codecc.task.model.EmailNotifyMessageTemplateEntity;
import com.tencent.bk.codecc.task.model.NotifyCustomEntity;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.model.WeChatNotifyMessageTemplateEntity;
import com.tencent.bk.codecc.task.pojo.DailyDataReportReqModel;
import com.tencent.bk.codecc.task.pojo.EmailMessageModel;
import com.tencent.bk.codecc.task.pojo.EmailNotifyModel;
import com.tencent.bk.codecc.task.pojo.NodeDataReportReqModel;
import com.tencent.bk.codecc.task.pojo.NodeServerRespModel;
import com.tencent.bk.codecc.task.pojo.RtxNotifyModel;
import com.tencent.bk.codecc.task.pojo.ToolAnalysisBotMsgModel;
import com.tencent.bk.codecc.task.pojo.ToolBaseInfoModel;
import com.tencent.bk.codecc.task.pojo.WeChatMessageModel;
import com.tencent.bk.codecc.task.service.DevopsNotifyService;
import com.tencent.bk.codecc.task.service.EmailNotifyService;
import com.tencent.bk.codecc.task.service.EmailRenderingService;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.bk.codecc.task.vo.CodeLibraryInfoVO;
import com.tencent.bk.codecc.task.vo.TaskCodeLibraryVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.BaseRiskAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.BaseRiskNotRepairedAuthorVO;
import com.tencent.devops.common.api.analysisresult.CCNLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.CommonLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.DUPCLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.LintLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.api.enums.RepositoryType;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.api.util.UUIDUtil;
import com.tencent.devops.common.auth.api.pojo.external.OwnerInfo;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.service.utils.BotUtil;
import com.tencent.devops.common.util.NotifyUtils;
import com.tencent.devops.repository.api.ServiceRepositoryResource;
import com.tencent.devops.repository.pojo.Repository;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CODECC_GENERAL_NOTIFY;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_EXTERNAL_JOB;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_CODECC_BKPLUGINEMAIL_NOTIFY;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_CODECC_BKPLUGINWECHAT_NOTIFY;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_CODECC_EMAIL_NOTIFY;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_CODECC_RTX_NOTIFY;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CODECC_BKPLUGINEMAIL_NOTIFY;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CODECC_BKPLUGINWECHAT_NOTIFY;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CODECC_EMAIL_NOTIFY;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CODECC_RTX_NOTIFY;

/**
 * 邮件通知服务逻辑
 *
 * @version V1.0
 * @date 2019/11/18
 */
@Slf4j
@Service
public class EmailNotifyServiceImpl implements EmailNotifyService {

    @Autowired
    private CustomProjRepository customProjRepository;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EmailNotifyMessageTemplateRepository emailNotifyMessageTemplateRepository;

    @Autowired
    private WeChatNotifyMessageTemplateRepository weChatNotifyMessageTemplateRepository;

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Autowired
    private DevopsNotifyService devopsNotifyService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private CommonDao commonDao;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Client client;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${bkci.public.url:#{null}}")
    private String devopsHost;

    @Value("${bkci.public.url:#{null}}")
    private String codeccHost;

    @Value("${codecc.classurl:#{null}}")
    private String publicClassUrl;

    private static final String RTX_SENDER = "codecc";
    private static final Map<String, String> TASK_CREATE_FROM_MAP = new ImmutableMap.Builder()
            .put(ComConstants.BsTaskCreateFrom.BS_CODECC.value(), "自建任务")
            .put(ComConstants.BsTaskCreateFrom.BS_PIPELINE.value(), "流水线")
            .put(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value(), "开源治理")
            .put(ComConstants.BsTaskCreateFrom.API_TRIGGER.value(), "API")
            .put(ComConstants.BsTaskCreateFrom.TIMING_SCAN.value(), "开源治理")
            .build();

    private static final Map<Integer, String> BOT_SEVERITY_MSG_MAP = new ImmutableMap.Builder()
            .put(ComConstants.SERIOUS, "严重")
            .put(ComConstants.SERIOUS | ComConstants.NORMAL, "严重+一般")
            .put(ComConstants.SERIOUS | ComConstants.NORMAL | ComConstants.PROMPT, "所有")
            .build();

    @Override
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_CODECC_EMAIL_NOTIFY,
            value = @Queue(value = QUEUE_CODECC_EMAIL_NOTIFY, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_CODECC_GENERAL_NOTIFY, durable = "true", delayed = "true", type = "topic")))
    public void sendReport(EmailNotifyModel emailNotifyModel) {
        try {
            Long taskId = emailNotifyModel.getTaskId();
            String buildId = emailNotifyModel.getBuildId();

            //1. 查询任务信息及报告定制信息
            TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
            if (null != taskInfoEntity.getStatus()
                    && ComConstants.Status.DISABLE.value() == taskInfoEntity.getStatus()) {
                log.info("disabled task not send email! task id: {}", taskId);
                return;
            }

            if (isGongfengScanDefaultNotifySetting(taskInfoEntity)) {
                log.info("gongfeng scan default notify setting not send email! task id: {}", taskId);
                return;
            }

            NotifyCustomEntity notifyCustomEntity = taskInfoEntity.getNotifyCustomInfo();
            Set<String> receiverList = new HashSet<>();
            Set<String> receiverCCList = new HashSet<>();
            if (null == notifyCustomEntity) {
                //这里应该是兼容以前的entity没有notify_custom_info字段
                receiverList = new HashSet<>(taskInfoEntity.getTaskOwner());
            } else {
                String receiverType = notifyCustomEntity.getEmailReceiverType();
                receiverCCList = notifyCustomEntity.getEmailCCReceiverList();

                if (ComConstants.EmailReceiverType.NOT_SEND.code().equals(receiverType)) {
                    log.info("NOT_SEND receiver type not send email! task id: {}", taskId);
                    return;
                } else if (ComConstants.EmailReceiverType.TASK_MEMBER.code().equals(receiverType)) {
                    List<String> receivers = taskInfoEntity.getTaskMember();
                    receivers.addAll(taskInfoEntity.getTaskOwner());
                    receiverList = new HashSet<>(receivers);
                } else if (ComConstants.EmailReceiverType.TASK_OWNER.code().equals(receiverType)) {
                    receiverList = new HashSet<>(taskInfoEntity.getTaskOwner());
                } else if (ComConstants.EmailReceiverType.CUSTOMIZED.code().equals(receiverType)) {
                    receiverList = notifyCustomEntity.getEmailReceiverList();
                } else if (ComConstants.EmailReceiverType.ONLY_AUTHOR.code().equals(receiverType)) {
                    receiverList = getAuthors(taskInfoEntity);
                    log.info("email receiver type 4, list: {}, task id: {}",
                            StringUtils.join(receiverList, ", "), taskId);
                }
            }

            if (CollectionUtils.isEmpty(receiverList)) {
                log.info("receiver list is empty for email! task id {}", emailNotifyModel.getTaskId());
                return;
            }

            //2. 获取报表数据
            String repoNameWithBranch = getRepoNameWithBranch(taskId);
            DailyDataReportReqModel reqModel;
            String title;
            if (EmailType.INSTANT == emailNotifyModel.getEmailType()) {
                log.info("start to send instant email, task id: {}, build id: {}", emailNotifyModel.getTaskId(),
                        emailNotifyModel.getBuildId());
                if (null != notifyCustomEntity && ComConstants.InstantReportStatus.DISABLED.code().
                        equals(notifyCustomEntity.getInstantReportStatus())) {
                    log.info("instant email disabled, task id: {}", emailNotifyModel.getTaskId());
                    return;
                }
                reqModel = getReportDataForAll(taskId, taskInfoEntity);
                reqModel.setType(ComConstants.ReportType.I.name());
                title = String.format("请阅: %s(%s)%s即时报告(%s)", taskInfoEntity.getNameCn(), repoNameWithBranch,
                        "所有工具", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            } else {
                if (EmailType.DAILY == emailNotifyModel.getEmailType()) {
                    log.info("start to send scheduled email, task id: {}, build id: {}", emailNotifyModel.getTaskId(),
                            emailNotifyModel.getBuildId());
                    title = String.format("请阅: %s(%s)%s定时报告(%s)", taskInfoEntity.getNameCn(), repoNameWithBranch,
                            "所有工具", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
                    if (null != notifyCustomEntity) {
                        Set<String> toolNames = notifyCustomEntity.getReportTools();
                        log.info("test tool name set: {}", toolNames);
                        if (CollectionUtils.isEmpty(toolNames)) {
                            log.info("tool name is empty, task id : {}", emailNotifyModel.getTaskId());
                            return;
                        }
                        Set<Integer> reportDate = notifyCustomEntity.getReportDate();
                        Integer reportTime = notifyCustomEntity.getReportTime();
                        if (CollectionUtils.isEmpty(reportDate) || null == reportTime) {
                            log.info("report date or time is empty!");
                            return;
                        }
                        reqModel = getReportData(taskId, taskInfoEntity, toolNames);
                        reqModel.setType(ComConstants.ReportType.T.name());
                    } else {
                        log.info("email setting is empty, task id : {}", emailNotifyModel.getTaskId());
                        return;
                    }
                } else {
                    log.error("no qualified email type, task id: {}, email type : {}",
                            taskId, emailNotifyModel.getEmailType().name());
                    return;
                }
            }

            String createFromCnName = convertTaskCreateFromCnName(taskInfoEntity);
            reqModel.setCreateFromCn(createFromCnName);
            reqModel.setBsBuildId(buildId);
            reqModel.setStreamName(repoNameWithBranch);

            //3. 请求nodejs获取邮件内容
            NodeServerRespModel nodeServerRespModel;
            try {
                nodeServerRespModel = client.getNoneUrlPrefix(EmailRenderingService.class).getEmailContent(reqModel);
            } catch (Exception e) {
                log.error(String.format("fetch email content from node server fail! task id: %d", taskId), e);
                throw new CodeCCException(CommonMessageCode.UTIL_EXECUTE_FAIL);
            }

            //4. 发送邮件
            String htmlEmail = nodeServerRespModel.getHtmlEmail();
            if (StringUtils.isBlank(htmlEmail)) {
                log.info("codecc email body is empty: taskId: {} | buildId: {}", taskId, buildId);
                return;
            }
            Map<String, String> attach = nodeServerRespModel.getImgBase64List();
            String resolveHtmlEmail = NotifyUtils.getResolveHtmlEmail(taskInfoEntity.getProjectId(),
                taskInfoEntity.getNameCn(),
                codeccHost,
                devopsHost,
                htmlEmail);

            if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(taskInfoEntity.getCreateFrom())) {
                log.info("start send email with tof4, task id: {}", taskInfoEntity.getTaskId());
                String finalSender = String.format("%s@%s", RTX_SENDER, "tencent.com");
                devopsNotifyService.sendMailWithTOF4(finalSender, receiverList,
                        null == receiverCCList ? Collections.emptySet() : receiverCCList,
                        Collections.emptySet(), title, resolveHtmlEmail, "0", "HTML", attach);
            } else {
                devopsNotifyService.sendMail(RTX_SENDER, receiverList,
                        null == receiverCCList ? Collections.emptySet() : receiverCCList,
                        Collections.emptySet(), title, resolveHtmlEmail, "0", "HTML", attach);
            }
            log.info("send email successfully! task id: {}", taskId);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("send email fail! task id: {}", emailNotifyModel.getTaskId(), e);
        }
    }

    @Override
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_CODECC_RTX_NOTIFY,
            value = @Queue(value = QUEUE_CODECC_RTX_NOTIFY, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_CODECC_GENERAL_NOTIFY, durable = "true", delayed = "true", type = "topic")))
    public void sendRtx(RtxNotifyModel rtxNotifyModel) {
        try {
            TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(rtxNotifyModel.getTaskId());
            //发送企业微信群机器人消息
            sendWeChatBotRemind(rtxNotifyModel, taskInfoEntity);

            if (ComConstants.Status.DISABLE.value() == taskInfoEntity.getStatus()) {
                log.info("task disabled not send rtx!");
                return;
            }

            if (isGongfengScanDefaultNotifySetting(taskInfoEntity)) {
                log.info("gongfeng scan default notify setting not send rtx! task id: {}", taskInfoEntity.getTaskId());
                return;
            }

            NotifyCustomEntity notifyCustomEntity = taskInfoEntity.getNotifyCustomInfo();
            Set<String> receiverList = new HashSet<>();

            if (null == notifyCustomEntity) {
                receiverList = new HashSet<>(taskInfoEntity.getTaskOwner());
            } else {
                String receiverType = notifyCustomEntity.getRtxReceiverType();

                if (ComConstants.EmailReceiverType.NOT_SEND.code().equals(receiverType)) {
                    log.info("NOT_SEND receiver type not send rtx! task id: {}", rtxNotifyModel.getTaskId());
                    return;
                } else if (ComConstants.EmailReceiverType.TASK_MEMBER.code().equals(receiverType)) {
                    List<String> receivers = taskInfoEntity.getTaskMember();
                    receivers.addAll(taskInfoEntity.getTaskOwner());
                    receiverList = new HashSet<>(receivers);
                } else if (ComConstants.EmailReceiverType.TASK_OWNER.code().equals(receiverType)) {
                    receiverList = new HashSet<>(taskInfoEntity.getTaskOwner());
                } else if (ComConstants.EmailReceiverType.CUSTOMIZED.code().equals(receiverType)) {
                    receiverList = notifyCustomEntity.getRtxReceiverList();
                } else if (ComConstants.EmailReceiverType.ONLY_AUTHOR.code().equals(receiverType)) {
                    receiverList = getAuthors(taskInfoEntity);
                    log.info("rtx receiver type 4, list: {}, task id: {}",
                            StringUtils.join(receiverList, ", "),
                            taskInfoEntity.getTaskId());
                }
            }

            if (CollectionUtils.isEmpty(receiverList)) {
                log.info("receiver list is empty for rtx! task id {}", taskInfoEntity.getTaskId());
                return;
            }

            String title = getRtxCommonTitle(taskInfoEntity, rtxNotifyModel);
            String preBody = getRtxCommonPreBody(taskInfoEntity, rtxNotifyModel);
            StringBuilder sb = new StringBuilder();
            sb.append(preBody);

            if (rtxNotifyModel.getSuccess()) {
                List<ToolLastAnalysisResultVO> toolLastAnalysisResultVOList = getToolLastAnalysisResult(taskInfoEntity);
                if (CollectionUtils.isEmpty(toolLastAnalysisResultVOList)) {
                    return;
                }

                String rtxToolAnalysisDetail = doSendRtx(toolLastAnalysisResultVOList);
                sb.append(rtxToolAnalysisDetail);
            }

            if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(taskInfoEntity.getCreateFrom())) {
                log.info("start send rtx with tof4, task id: {}", taskInfoEntity.getTaskId());
                devopsNotifyService.sendRtxWithTOF4(receiverList, sb.toString(), RTX_SENDER, title, "0");
            } else {
                devopsNotifyService.sendRtx(receiverList, sb.toString(), RTX_SENDER, title, "0");
            }

            log.info("send rtx successfully! task id: {}", taskInfoEntity.getTaskId());
        } catch (Exception e) {
            log.error("send rtx message fail! task id: {}", rtxNotifyModel.getTaskId(), e);
        }
    }

    private String doSendRtx(List<ToolLastAnalysisResultVO> toolLastAnalysisResultVOList) {
        StringBuffer stringBuffer = new StringBuffer();

        if (toolLastAnalysisResultVOList == null) {
            return "";
        }
        if (toolLastAnalysisResultVOList.isEmpty()) {
            return "";
        }

        toolLastAnalysisResultVOList.forEach(toolLastAnalysisResultVO -> {
            BaseLastAnalysisResultVO baseLastAnalysisResultVO = toolLastAnalysisResultVO.getLastAnalysisResultVO();
            if (baseLastAnalysisResultVO == null) {
                return;
            }

            String toolDisplayName = toolMetaCacheService.getToolDisplayName(toolLastAnalysisResultVO.getToolName());
            final String formatter = "✔ %s(%s)：严重 %d，一般 %d，提示 %d \n";

            switch (baseLastAnalysisResultVO.getPattern()) {
                case "LINT": {
                    LintLastAnalysisResultVO lintVO = (LintLastAnalysisResultVO) baseLastAnalysisResultVO;
                    stringBuffer.append(String.format(formatter,
                            toolDisplayName,
                            getUpDownSymbol(lintVO.getDefectChange()),
                            lintVO.getTotalSerious(),
                            lintVO.getTotalNormal(),
                            lintVO.getTotalPrompt())
                    );
                }
                break;
                case "CCN": {
                    CCNLastAnalysisResultVO ccnVO = (CCNLastAnalysisResultVO) baseLastAnalysisResultVO;
                    float avgThousandDefect = ccnVO.getAverageThousandDefect() == null ? 0F
                            : ccnVO.getAverageThousandDefect().floatValue();
                    stringBuffer.append(String.format("✔ %s(%s)：千行超标复杂度 %s，极高风险 %d，高风险 %d，中风险 %d \n",
                            toolDisplayName,
                            getUpDownSymbol(ccnVO.getAverageThousandDefectChange()),
                            keepFloatNPlaceWithRound(avgThousandDefect, 1),
                            ccnVO.getSuperHighCount(),
                            ccnVO.getHighCount(),
                            ccnVO.getMediumCount())
                    );
                }
                break;
                case "DUPC": {
                    DUPCLastAnalysisResultVO dupcVO = (DUPCLastAnalysisResultVO) baseLastAnalysisResultVO;
                    stringBuffer.append(String.format("✔ %s(%s)：平均重复率 %s%%，极高风险 %d，高风险 %d，中风险 %d \n",
                            toolDisplayName,
                            getUpDownSymbol(dupcVO.getDefectChange()),
                            keepFloatNPlaceWithRound(dupcVO.getDupRate(), 1),
                            dupcVO.getSuperHighCount(),
                            dupcVO.getHighCount(),
                            dupcVO.getMediumCount())
                    );
                }
                break;
                case "COVERITY":
                case "KLOCWORK":
                case "PINPOINT": {
                    CommonLastAnalysisResultVO commonVO = (CommonLastAnalysisResultVO) baseLastAnalysisResultVO;
                    stringBuffer.append(String.format(formatter,
                            toolDisplayName,
                            getUpDownSymbol(commonVO.getDefectChange()),
                            commonVO.getExistSeriousCount(),
                            commonVO.getExistNormalCount(),
                            commonVO.getExistPromptCount()
                    ));
                }
                break;
                default:
                    break;
            }
        });

        return stringBuffer.toString();
    }

    /**
     * 获取升降趋势符号
     *
     * @param data
     * @return
     */
    private <T> String getUpDownSymbol(T data) {
        if (data == null) {
            return "";
        }

        if (data instanceof Integer || data instanceof Double) {
            Double val = Double.valueOf(data.toString());
            if (val > 0) {
                return "↑";
            } else if (val == 0) {
                return "-";
            } else {
                return "↓";
            }
        }

        return "";
    }

    private Object getIntUpDownMessage(Integer intData) {
        if (intData == null) {
            return "(0 )";
        }
        return intData >= 0 ? String.format("(↑%d )", intData) :
                String.format("(↓%d )", Math.abs(intData));
    }

    private String getFloatUpDownMessage(Float floatData) {
        if (floatData == null) {
            return "(0 )";
        }
        return floatData >= 0 ? String.format("(↑%s )", floatData) :
                String.format("(↓%s )", Math.abs(floatData));
    }

    private String getFloatUpDownMessageContainPercent(Float floatData) {
        if (floatData == null) {
            return "(0% )";
        }
        return floatData >= 0 ? String.format("(↑%s%% )", floatData) :
                String.format("(↓%s%% )", Math.abs(floatData));
    }

    @Override
    public String addEmailScheduleTask(Long taskId, Set<Integer> week, Integer hour, OperationType operationType, String jobName) {
        String finalJobName;
        if (OperationType.ADD == operationType) {
            finalJobName = String.format("%s_%s", UUIDUtil.INSTANCE.generate(),
                    DigestUtils.md5Hex(String.format("0 0 %d ? * %s", hour,
                            String.join(",", week.stream().map(singleWeek -> String.valueOf((singleWeek % 7) + 1)).collect(Collectors.toList())))));
        } else {
            finalJobName = jobName;
        }
        JobExternalDto jobExternalDto = new JobExternalDto(
                finalJobName,
                String.format("%sSendEmailScheduleTask.java", publicClassUrl),
                "SendEmailScheduleTask",
                String.format("0 0 %d ? * %s", hour, String.join(",", week.stream().map(String::valueOf).collect(Collectors.toList()))),
                new HashMap<String, String>() {{
                    put("taskId", taskId.toString());
                }},
                operationType
        );
        rabbitTemplate.convertAndSend(EXCHANGE_EXTERNAL_JOB, "", jobExternalDto);
        return finalJobName;
    }

    @Override
    @RabbitListener(
            bindings =
            @QueueBinding(
                    key = ROUTE_CODECC_BKPLUGINEMAIL_NOTIFY,
                    value = @Queue(value = QUEUE_CODECC_BKPLUGINEMAIL_NOTIFY, durable = "true"),
                    exchange =
                    @Exchange(
                            value = EXCHANGE_CODECC_GENERAL_NOTIFY,
                            durable = "true",
                            delayed = "true",
                            type = "topic")))
    public void sendEmail(EmailMessageModel emailMessageModel) {
        EmailNotifyMessageTemplateEntity template =
                emailNotifyMessageTemplateRepository.findFirstByTemplateId(
                        emailMessageModel.getTemplate().value());

        if (template == null) {
            log.error("get null email notify template, template code: {}", emailMessageModel.getTemplate());
            return;
        }

        log.info("start to send bkplugin email");
        devopsNotifyService.sendMail(template.getSender(),
                emailMessageModel.getReceivers(),
                emailMessageModel.getCc(),
                emailMessageModel.getBcc(),
                template.getTitle(),
                replaceContentParams(emailMessageModel.getContentParam(), template.getBody()),
                emailMessageModel.getPriority(),
                template.getBodyFormat(),
                Collections.emptyMap());
    }

    @Override
    @RabbitListener(
            bindings =
            @QueueBinding(
                    key = ROUTE_CODECC_BKPLUGINWECHAT_NOTIFY,
                    value = @Queue(value = QUEUE_CODECC_BKPLUGINWECHAT_NOTIFY, durable = "true"),
                    exchange =
                    @Exchange(
                            value = EXCHANGE_CODECC_GENERAL_NOTIFY,
                            durable = "true",
                            delayed = "true")))
    public void sendWeChat(WeChatMessageModel weChatMessageModel) {
        WeChatNotifyMessageTemplateEntity template =
                weChatNotifyMessageTemplateRepository.findFirstByTemplateId(
                        weChatMessageModel.getTemplate().value());

        if (template == null) {
            log.error("get null wechat notify template, template code: {}", weChatMessageModel.getTemplate());
            return;
        }

        log.info("start to send bkplugin email");
        devopsNotifyService.sendWeChat(
                replaceContentParams(weChatMessageModel.getContentParam(), template.getBody()),
                template.getPriority(),
                template.getReceivers(),
                template.getSender(),
                template.getSource());
    }

    /**
     * 为开源扫描打开企业微信实时通知，接收人为"遗留问题处理人"
     *
     * @param bgId
     * @param deptId
     * @param centerId
     * @return
     */
    @Override
    public List<Long> turnOnWechatNotifyForGongFeng(Integer bgId, Integer deptId, Integer centerId) {
        log.info("turnOnWechatNotifyForGongFeng request, bgId: {}, deptId: {}, centerId: {}", bgId, deptId, centerId);

        if (bgId == null && deptId == null && centerId == null) {
            return Collections.emptyList();
        }

        List<Integer> deptIdConditions = new ArrayList<>();
        deptIdConditions.add(deptId);
        List<String> createFromConditions = new ArrayList<>();
        createFromConditions.add(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value());

        List<TaskInfoEntity> taskInfoEntityList = taskDao.queryTaskInfoEntityList(
                TaskConstants.TaskStatus.ENABLE.value(),
                bgId,
                deptIdConditions,
                null,
                createFromConditions,
                null
        );

        //db中center_id没有索引，这里过滤一下
        if (centerId != null) {
            taskInfoEntityList = taskInfoEntityList.stream().filter(taskInfoEntity ->
                    taskInfoEntity.getCenterId() == centerId.intValue()
            ).collect(Collectors.toList());
        }

        if (CollectionUtils.isEmpty(taskInfoEntityList)) {
            return Collections.emptyList();
        }

        //过滤掉API来源的；注意task_id没有索引，走组合查询会导致pipeline_id的索引也失效，故分开查
        List<String> findByPipelineIdsConditions = taskInfoEntityList.stream()
                .filter(taskInfoEntity -> !StringUtils.isEmpty(taskInfoEntity.getPipelineId()))
                .map(TaskInfoEntity::getPipelineId)
                .collect(Collectors.toList());
        Set<String> pipelineIdSet = customProjRepository
                .findByPipelineIdIn(findByPipelineIdsConditions)
                .stream()
                .map(CustomProjEntity::getPipelineId)
                .collect(Collectors.toSet());

        List<Long> findByTaskIdsConditions = taskInfoEntityList.stream()
                .filter(taskInfoEntity -> StringUtils.isEmpty(taskInfoEntity.getPipelineId()))
                .map(TaskInfoEntity::getTaskId)
                .collect(Collectors.toList());
        Set<Long> taskIdSet = customProjRepository
                .findByTaskIdIn(findByTaskIdsConditions)
                .stream()
                .map(CustomProjEntity::getTaskId)
                .collect(Collectors.toSet());

        List<Long> toUpdateTaskIds = taskInfoEntityList.stream()
                .filter(taskInfoEntity -> !pipelineIdSet.contains(taskInfoEntity.getPipelineId())
                        && !taskIdSet.contains(taskInfoEntity.getTaskId()))
                .map(TaskInfoEntity::getTaskId)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(toUpdateTaskIds)) {
            taskDao.updateNotifyReceiverType(toUpdateTaskIds, ComConstants.EmailReceiverType.ONLY_AUTHOR.code(), null);
            log.info("turnOnWechatNotifyForGongFeng updated, task id: {} ", StringUtils.join(toUpdateTaskIds, ", "));
        }

        return toUpdateTaskIds;
    }

    private DailyDataReportReqModel getReportDataForAll(Long taskId, TaskInfoEntity taskInfoEntity) {
        DailyDataReportReqModel dailyDataReportReqModel = new DailyDataReportReqModel();
        dailyDataReportReqModel.setTaskId(taskId);
        dailyDataReportReqModel.setProjectId(taskInfoEntity.getProjectId());
        dailyDataReportReqModel.setStreamName(taskInfoEntity.getNameEn());
        dailyDataReportReqModel.setNameCN(taskInfoEntity.getNameCn());
        Set<String> toolNames = taskInfoEntity.getToolConfigInfoList().stream().filter(toolConfigInfoEntity -> ComConstants.FOLLOW_STATUS.WITHDRAW.value()
                != toolConfigInfoEntity.getFollowStatus()).map(ToolConfigInfoEntity::getToolName).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(toolNames)) {
            return null;
        }

        setEmailReportRootUrl(dailyDataReportReqModel, taskInfoEntity);
        toolsDataInfo(dailyDataReportReqModel, taskId, toolNames);
        repoUrlInfo(dailyDataReportReqModel, taskId);
        dailyDataReportReqModel.setSummary(taskService.getTaskOverview(taskId, null).getLastAnalysisResultList());
        toolMapInfo(dailyDataReportReqModel, toolNames);
        return dailyDataReportReqModel;
    }


    private DailyDataReportReqModel getReportData(Long taskId, TaskInfoEntity taskInfoEntity, Set<String> toolNames) {
        DailyDataReportReqModel dailyDataReportReqModel = new DailyDataReportReqModel();
        dailyDataReportReqModel.setTaskId(taskId);
        dailyDataReportReqModel.setProjectId(taskInfoEntity.getProjectId());
        dailyDataReportReqModel.setStreamName(taskInfoEntity.getNameEn());
        dailyDataReportReqModel.setNameCN(taskInfoEntity.getNameCn());
        toolsDataInfo(dailyDataReportReqModel, taskId, toolNames);
        repoUrlInfo(dailyDataReportReqModel, taskId);
        setEmailReportRootUrl(dailyDataReportReqModel, taskInfoEntity);

        List<TaskOverviewVO.LastAnalysis> lastAnalysisList =
                taskService.getTaskOverview(taskId, null).getLastAnalysisResultList();
        dailyDataReportReqModel.setSummary(lastAnalysisList.stream().filter(lastAnalysis ->
                toolNames.contains(lastAnalysis.getToolName())
        ).collect(Collectors.toList()));
        toolMapInfo(dailyDataReportReqModel, toolNames);
        return dailyDataReportReqModel;
    }

    private void toolsDataInfo(DailyDataReportReqModel dailyDataReportReqModel, Long taskId, Set<String> toolNames) {
        List<String> toolOrder = Arrays.asList(commonDao.getToolOrder().split(","));
        Result<JSONArray> dataReportResult = client.get(ServiceDefectTreeResource.class).getBatchDataReports(taskId, toolNames);
        if (dataReportResult.isNotOk() || null == dataReportResult.getData()) {
            log.error("get data report list fail! task id: {}", taskId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        JSONArray dataReportRspVOList = dataReportResult.getData();
        List<NodeDataReportReqModel> nodeReportRspVOList = new ArrayList<>();
        for (Object object : dataReportRspVOList) {
            if (null != object) {
                JSONObject jsonObject = JSONObject.fromObject(object);
                NodeDataReportReqModel nodeDataReportReqModel = new NodeDataReportReqModel();
                nodeDataReportReqModel.setTaskId(jsonObject.getLong("taskId"));
                nodeDataReportReqModel.setToolName(jsonObject.getString("toolName"));
                jsonObject.discard("taskId");
                nodeDataReportReqModel.setCharts(jsonObject);
                nodeReportRspVOList.add(nodeDataReportReqModel);
            }
        }
        dailyDataReportReqModel.setToolsData(nodeReportRspVOList.stream().filter(Objects::nonNull).
                sorted(Comparator.comparingInt(o -> toolOrder.indexOf(o.getToolName()))).collect(Collectors.toList()));
    }

    private void repoUrlInfo(DailyDataReportReqModel dailyDataReportReqModel, Long taskId) {
        CodeLibraryInfoVO codeLibraryInfo = getCodeLibraryInfo(taskId);
        if (codeLibraryInfo == null) {
            log.error("get repo url fail! task id: {}", taskId);
            return;
        }

        CodeRepoVO codeRepoVO = new CodeRepoVO();
        codeRepoVO.setBranch(codeLibraryInfo.getBranch());
        codeRepoVO.setUrl(codeLibraryInfo.getUrl());
        codeRepoVO.setToolNames(Sets.newHashSet());
        codeRepoVO.setVersion("");
        dailyDataReportReqModel.setRepoUrls(
                new HashSet<CodeRepoVO>() {{
                    add(codeRepoVO);
                }}
        );
    }

    private void toolMapInfo(DailyDataReportReqModel dailyDataReportReqModel, Set<String> toolNames) {
        Map<String, ToolBaseInfoModel> toolInfoMap = new HashMap<>();
        toolNames.forEach(toolName -> {
            ToolMetaDetailVO toolMetaDetailVO = toolMetaCacheService.getToolDetailFromCache(toolName);
            ToolBaseInfoModel toolBaseInfoModel = new ToolBaseInfoModel();
            toolBaseInfoModel.setPattern(toolMetaDetailVO.getPattern());
            toolBaseInfoModel.setDisplayName(toolMetaDetailVO.getDisplayName());
            toolInfoMap.put(toolName, toolBaseInfoModel);
        });
        dailyDataReportReqModel.setToolInfoMap(toolInfoMap);
    }

    /**
     * 计算本次分析的耗时
     *
     * @param toolLastAnalysisResultVO
     * @return
     */
    private String calculateTimeConsuming(ToolLastAnalysisResultVO toolLastAnalysisResultVO) {
        // 计算本次分析的耗时
        long totalMiniSecs = toolLastAnalysisResultVO.getEndTime() - toolLastAnalysisResultVO.getStartTime();

        if (totalMiniSecs < 0) {
            log.error("calculate time consuming fail for tool result vo: " + toolLastAnalysisResultVO);
            totalMiniSecs = System.currentTimeMillis() - toolLastAnalysisResultVO.getStartTime();
        }

        long totalSecs = totalMiniSecs / 1000;
        long hours = totalSecs / 3600;
        long minutes = (totalSecs % 3600) / 60;
        long seconds = totalSecs % 60;
        String strElapseTime = null;


        if (hours != 0) {
            strElapseTime = String.format("%d 时 %d 分 %d 秒", hours, minutes, seconds);
        } else {
            strElapseTime = String.format("%d 分 %d 秒", minutes, seconds);
        }
        return strElapseTime;
    }

    /**
     * 计算分析总耗时
     *
     * @param taskId
     * @param buildId
     * @return
     */
    private String calcTotalTimeConsuming(Long taskId, String buildId) {
        if (StringUtils.isEmpty(buildId)) {
            return "";
        }

        Result<TaskLogOverviewVO> overviewVOResult = client.get(ServiceTaskLogOverviewResource.class)
                .getTaskLogOverview(taskId, buildId, null);
        if (overviewVOResult.isNotOk() || overviewVOResult.getData() == null) {
            log.info("calculateTotalTimeConsuming getTaskLogOverview is fail, task id: {}, build id: {}",
                    taskId, buildId);
            return "";
        }

        TaskLogOverviewVO overviewVO = overviewVOResult.getData();
        //log.info("calculateTotalTimeConsuming overviewVO: {}", overviewVO);
        long totalMiniSecs = overviewVO.getEndTime() - overviewVO.getStartTime();
        long totalSecs = totalMiniSecs / 1000;
        long hours = totalSecs / 3600;
        long minutes = (totalSecs % 3600) / 60;
        long seconds = totalSecs % 60;
        String strElapseTime = null;

        if (hours != 0) {
            strElapseTime = String.format("%d 时 %d 分 %d 秒", hours, minutes, seconds);
        } else {
            strElapseTime = String.format("%d 分 %d 秒", minutes, seconds);
        }

        return strElapseTime;
    }

    private String replaceContentParams(Map<String, String> params, String content) {
        if (params == null || params.isEmpty()) {
            log.error("email content can not be null");
            return content;
        }
        String content1 = content;
        for (String paramName : params.keySet()) {
            String paramValue = params.get(paramName);
            content1 = content1.replace("#{" + paramName + "}", paramValue);
        }
        return content1;
    }

    /**
     * 获取"仓库名@分支名"
     *
     * @param taskId
     * @return
     */
    private String getRepoNameWithBranch(Long taskId) {
        CodeLibraryInfoVO codeLibraryInfo = getCodeLibraryInfo(taskId);
        if (codeLibraryInfo != null) {
            return String.format("%s@%s", codeLibraryInfo.getAliasName(), codeLibraryInfo.getBranch());
        }

        return "";
    }


    /**
     * 获取仓库信息(try-catch包装方法)
     *
     * @param taskId
     * @return
     */
    private CodeLibraryInfoVO getCodeLibraryInfo(Long taskId) {
        try {
            return getCodeLibraryInfoCore(taskId);
        } catch (Exception e) {
            log.error("get code library info fail, task id: {}", taskId, e);
            return null;
        }
    }

    /**
     * 获取仓库信息
     *
     * @param taskId
     * @return
     */
    private CodeLibraryInfoVO getCodeLibraryInfoCore(Long taskId) {
        TaskCodeLibraryVO taskCodeLibraryVO = taskService.getRepoInfo(taskId);

        if (taskCodeLibraryVO == null || CollectionUtils.isEmpty(taskCodeLibraryVO.getCodeInfo())) {
            log.info("task code library info is null, task id: {}", taskId);
            return null;
        }

        //虽然是list，但实现均只存一个item(pipline除外)
        CodeLibraryInfoVO codeLibraryInfoVO = taskCodeLibraryVO.getCodeInfo().get(0);

        //bs_codecc创建的任务，不跑分析无法从分析日志拿git的地址；跑了从t_code_repo_from_analyzelog拿也有覆盖问题，改从蓝盾拿
        if (codeLibraryInfoVO != null && StringUtils.isEmpty(codeLibraryInfoVO.getUrl())) {
            TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
            if (!taskInfoEntity.getCreateFrom().equals(ComConstants.BsTaskCreateFrom.BS_CODECC.value())) {
                return codeLibraryInfoVO;
            }

            if (taskInfoEntity.getProjectId() == null) {
                log.info("ready to get repository info from devops service, but project id is null, task id: {}",
                        taskId);

                return codeLibraryInfoVO;
            }
            if (taskInfoEntity.getRepoHashId() == null) {
                log.info("ready to get repository info from devops service, but repo hash id is null, task id: {}",
                        taskId);

                return codeLibraryInfoVO;
            }

            Result<Repository> repositoryResult = client.getDevopsService(ServiceRepositoryResource.class)
                    .get(taskInfoEntity.getProjectId(), taskInfoEntity.getRepoHashId(), RepositoryType.ID);

            if (repositoryResult.isOk()
                    && repositoryResult.getData() != null
                    && !StringUtils.isEmpty(repositoryResult.getData().getUrl())) {
                codeLibraryInfoVO.setUrl(repositoryResult.getData().getUrl());
            }
        }

        return codeLibraryInfoVO;
    }

    /**
     * 获取任务来源的枚举类型
     *
     * @param taskInfoEntity
     * @return
     */
    private String getTaskCreateFromEnumValue(TaskInfoEntity taskInfoEntity) {
        if (taskInfoEntity == null || taskInfoEntity.getCreateFrom() == null) {
            log.error("task info and createFrom can not be null");
            return "";
        }

        String realCreateFrom = taskInfoEntity.getCreateFrom();

        if (taskInfoEntity.getCreateFrom().equals(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value())) {
            // 如果是 OTEAM 项目的话，设置为和定时触发一样的开源治理项目
            if (taskInfoEntity.getProjectId().equals("CUSTOMPROJ_TEG_CUSTOMIZED")) {
                realCreateFrom = ComConstants.BsTaskCreateFrom.TIMING_SCAN.value();
            } else {
                CustomProjEntity customProjEntity = taskInfoEntity.getPipelineId() != null
                        ? customProjRepository.findFirstByPipelineId(taskInfoEntity.getPipelineId())
                        : customProjRepository.findFirstByTaskId(taskInfoEntity.getTaskId());

                // 如果不是 OTEAM 项目并且是私有API触发项目，则设置为API触发项目
                realCreateFrom = customProjEntity != null
                        ? ComConstants.BsTaskCreateFrom.API_TRIGGER.value()
                        : ComConstants.BsTaskCreateFrom.TIMING_SCAN.value();
            }
        }

        return realCreateFrom;
    }

    /**
     * 获取任务"来源"对外中文文案
     *
     * @param taskInfoEntity
     * @return
     */
    private String convertTaskCreateFromCnName(TaskInfoEntity taskInfoEntity) {
        String realCreateFrom = getTaskCreateFromEnumValue(taskInfoEntity);
        String name = TASK_CREATE_FROM_MAP.get(realCreateFrom);

        if (StringUtils.isEmpty(name)) {
            log.error("task createFrom is invalid, task id: {}, create from: {}",
                    taskInfoEntity.getTaskId(), taskInfoEntity.getCreateFrom());
        }

        return name;
    }

    /**
     * 保留N位小数，直接截断（非四舍五入）
     *
     * @param source 原值
     * @param nPlace 保留的小数位数
     * @return
     */
    private float keepFloatNPlace(float source, int nPlace) {
        int weight = IntMath.pow(10, nPlace);
        float dividend = (float) weight;

        return ((int) (source * weight)) / dividend;
    }


    /**
     * 四舍五入，保留N位小数
     *
     * @param source
     * @param nPlace
     * @return
     */
    private float keepFloatNPlaceWithRound(float source, int nPlace) {
        int weight = IntMath.pow(10, nPlace);
        float dividend = (float) weight;

        return ((int) (source * weight + 0.5)) / dividend;
    }

    /**
     * 获取遗留问题处理人
     *
     * @param taskInfoEntity
     * @return
     */
    private Set<String> getAuthors(TaskInfoEntity taskInfoEntity) {
        Long taskId = taskInfoEntity.getTaskId();
        Result<List<TaskPersonalStatisticwVO>> overviewListResult = client.get(UserOverviewRestResource.class)
                .overviewList(taskId);
        if (overviewListResult.isNotOk() || CollectionUtils.isEmpty(overviewListResult.getData())) {
            log.info("get task personal statistic result fail! task id: {}", taskId);
            return Collections.emptySet();
        }

        //不包括重复率的处理人
        Set<String> authorsSetWithOutGitCheck = overviewListResult.getData().stream().filter(personalStatistic ->
                personalStatistic.getDefectCount() > 0
                || personalStatistic.getSecurityCount() > 0
                || personalStatistic.getStandardCount() > 0
                || personalStatistic.getRiskCount() > 0)
                .map(TaskPersonalStatisticwVO::getUsername)
                .collect(Collectors.toSet());
        log.info("authorsSetWithOutGitCheck, task id: {}, set: {}",
                taskId,
                StringUtils.join(authorsSetWithOutGitCheck, ", "));

        if (CollectionUtils.isEmpty(authorsSetWithOutGitCheck)) {
            return Collections.emptySet();
        }

        //非开源扫描不用校验git权限
        if (!ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(taskInfoEntity.getCreateFrom())) {
            return authorsSetWithOutGitCheck;
        }

        Integer gongfengProjectId = taskInfoEntity.getGongfengProjectId();
        List<OwnerInfo> gitOwnerList = getOwnersByProject(gongfengProjectId);
        if (CollectionUtils.isEmpty(gitOwnerList)) {
            log.info("gitOwnerList list is empty, task id: {}, gong feng project id: {}", taskId, gongfengProjectId);
            return Collections.emptySet();
        }

        Set<String> gitOwnerSet = gitOwnerList.stream().map(OwnerInfo::getUserName).collect(Collectors.toSet());
        log.info("gitOwnerSet, task id: {}, set: {}", taskId, StringUtils.join(gitOwnerSet, ", "));

        Set<String> intersection = gitOwnerSet.stream()
                .filter(authorsSetWithOutGitCheck::contains)
                .collect(Collectors.toSet());
        log.info("authorsWithOutGitCheck with gitOwnerSet intersection, task id: {}, set: {}",
                taskId,
                StringUtils.join(intersection, ", "));

        return intersection;
    }

    public List<OwnerInfo> getOwnersByProject(Integer gongfengProjectId) {
        return new ArrayList<>();
    }

    /**
     * 设置平台页面url的根节点
     *
     * @param dailyDataReportReqModel
     * @param taskInfoEntity
     */
    private void setEmailReportRootUrl(DailyDataReportReqModel dailyDataReportReqModel, TaskInfoEntity taskInfoEntity) {
        String urlRoot = ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(taskInfoEntity.getCreateFrom())
                ? String.format("%s/codecc/%s/", codeccHost, taskInfoEntity.getProjectId())
                : String.format("%s/console/codecc/%s/", devopsHost, taskInfoEntity.getProjectId());

        dailyDataReportReqModel.setUrlRoot(urlRoot);
    }

    /**
     * 是开源扫描默认通知设置吗（不区分api_trigger/timing_scan）
     *
     * @param taskInfoEntity
     * @return
     */
    private boolean isGongfengScanDefaultNotifySetting(TaskInfoEntity taskInfoEntity) {
        return taskInfoEntity.getNotifyCustomInfo() == null
                && ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(taskInfoEntity.getCreateFrom());
    }

    /**
     * 获取各工具最后一次分析结果
     *
     * @param taskInfoEntity
     * @return
     */
    private List<ToolLastAnalysisResultVO> getToolLastAnalysisResult(TaskInfoEntity taskInfoEntity) {
        Set<String> toolSet = new HashSet<>();
        List<ToolConfigInfoEntity> toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();

        if (CollectionUtils.isNotEmpty(toolConfigInfoEntityList)) {
            toolSet = toolConfigInfoEntityList.stream()
                    .filter(toolConfigInfoEntity ->
                            ComConstants.FOLLOW_STATUS.WITHDRAW.value() != toolConfigInfoEntity.getFollowStatus())
                    .map(ToolConfigInfoEntity::getToolName)
                    .collect(Collectors.toSet());
        }

        Result<List<ToolLastAnalysisResultVO>> toolLastAnalysisResult = client.get(ServiceTaskLogRestResource.class)
                .getBatchLatestTaskLog(taskInfoEntity.getTaskId(), toolSet);

        if (toolLastAnalysisResult.isNotOk() || CollectionUtils.isEmpty(toolLastAnalysisResult.getData())) {
            log.info("get analysis statistics result fail! task id: {}", taskInfoEntity.getTaskId());
            return Collections.emptyList();
        }

        List<ToolLastAnalysisResultVO> retList = toolLastAnalysisResult.getData();

        //排序，保障跟前端页面展示的顺序一致
        if (retList.size() > 1) {
            String toolIdsOrder = commonDao.getToolOrder();
            List<String> toolOrderList = Arrays.asList(toolIdsOrder.split(","));
            retList = retList.stream()
                    .filter(vo -> !vo.getToolName().equals(ComConstants.Tool.GITHUBSTATISTIC.name()))
                    .sorted(Comparator.comparing(s -> toolOrderList.indexOf(s.getToolName())))
                    .collect(Collectors.toList());
        }

        return retList;
    }

    /**
     * 获取企业微信消息的公共标题
     *
     * @param taskInfoEntity
     * @param rtxNotifyModel
     * @return
     */
    private String getRtxCommonTitle(TaskInfoEntity taskInfoEntity, RtxNotifyModel rtxNotifyModel) {
        final String formatter = "【CodeCC】%s(%s) 扫描%s";
        String repoNameWithBranch = getRepoNameWithBranch(taskInfoEntity.getTaskId());

        return rtxNotifyModel.getSuccess()
                ? String.format(formatter, taskInfoEntity.getNameCn(), repoNameWithBranch, "完成")
                : String.format(formatter, taskInfoEntity.getNameCn(), repoNameWithBranch, "失败");
    }

    /**
     * 获取企业微信消息的公共前置body
     *
     * @param taskInfoEntity
     * @param rtxNotifyModel
     * @return
     */
    private String getRtxCommonPreBody(TaskInfoEntity taskInfoEntity, RtxNotifyModel rtxNotifyModel) {
        String createFromCnName = convertTaskCreateFromCnName(taskInfoEntity);
        String targetUrl = NotifyUtils.getTargetUrl(taskInfoEntity.getProjectId(),
                taskInfoEntity.getNameCn(),
                taskInfoEntity.getTaskId(),
                codeccHost,
                devopsHost,
                taskInfoEntity.getCreateFrom());

        if (rtxNotifyModel.getSuccess()) {
            String totalTimeConsuming = calcTotalTimeConsuming(taskInfoEntity.getTaskId(), rtxNotifyModel.getBuildId());
            return String.format("%s \n"
                            + "来源：%s \n"
                            + "耗时：%s \n"
                            + "结果：\n",
                    targetUrl,
                    createFromCnName,
                    totalTimeConsuming);
        } else {
            return String.format("%s \n"
                            + "来源：%s\n",
                    targetUrl,
                    createFromCnName);
        }
    }

    /**
     * 发送企业微信群机器人消息
     *
     * @param rtxNotifyModel
     * @param taskInfoEntity
     */
    private void sendWeChatBotRemind(RtxNotifyModel rtxNotifyModel, TaskInfoEntity taskInfoEntity) {
        try {
            sendWeChatBotRemindCore(rtxNotifyModel, taskInfoEntity);
        } catch (Exception e) {
            log.error("send bot remind fail! task id: {}", rtxNotifyModel.getTaskId(), e);
        }
    }

    /**
     * 发送企业微信群机器人消息
     *
     * @param rtxNotifyModel
     * @param taskInfoEntity
     */
    private void sendWeChatBotRemindCore(RtxNotifyModel rtxNotifyModel, TaskInfoEntity taskInfoEntity) {
        if (ComConstants.Status.DISABLE.value() == taskInfoEntity.getStatus()) {
            log.info("task disabled not send bot remind!");
            return;
        }

        NotifyCustomEntity notifyCustomInfo = taskInfoEntity.getNotifyCustomInfo();
        if (notifyCustomInfo == null || StringUtils.isEmpty(notifyCustomInfo.getBotWebhookUrl())
                || notifyCustomInfo.getBotRemindRange() == null
                || notifyCustomInfo.getBotRemindSeverity() == null
                || CollectionUtils.isEmpty(notifyCustomInfo.getBotRemaindTools())) {
            return;
        }

        //1.拼装消息模板
        String title = getRtxCommonTitle(taskInfoEntity, rtxNotifyModel);
        String preBody = getRtxCommonPreBody(taskInfoEntity, rtxNotifyModel);
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        sb.append("\n");
        sb.append(preBody);
        Set<String> atReceiver = Sets.newHashSet();

        if (rtxNotifyModel.getSuccess()) {
            //2.获取最后一次分析数据
            List<ToolLastAnalysisResultVO> analysisResultVOList = getToolLastAnalysisResult(taskInfoEntity);
            if (CollectionUtils.isEmpty(analysisResultVOList)) {
                return;
            }

            //3.分析数据加工处理
            ToolAnalysisBotMsgModel botMsgModel = getToolAnalysisBotMsgModel(analysisResultVOList, notifyCustomInfo);
            if (botMsgModel == null) {
                return;
            }

            if (botMsgModel.getZeroDefectMatch()) {
                log.info("zero defect match, not send bot remind, task id: {}", taskInfoEntity.getTaskId());
                return;
            }

            sb.append(botMsgModel.getBodyContent());
            sb.append(String.format("%s遗留问题处理人如下：",
                    BOT_SEVERITY_MSG_MAP.get(notifyCustomInfo.getBotRemindSeverity())));

            atReceiver = botMsgModel.getAuthorSet();
        }

        BotUtil.sendMsgToRobot(notifyCustomInfo.getBotWebhookUrl(), sb.toString(), atReceiver);

        log.info("send bot remind successfully! task id: {}, receiver: {}", taskInfoEntity.getTaskId(),
                StringUtils.join(atReceiver, ", "));
    }

    /**
     * 获取工具分析的机器人消息model
     */
    private ToolAnalysisBotMsgModel getToolAnalysisBotMsgModel(List<ToolLastAnalysisResultVO> analysisResultVOList,
                                                               NotifyCustomEntity notifyCustomEntity) {
        if (CollectionUtils.isEmpty(analysisResultVOList)) {
            return null;
        }

        boolean unMatchSeverity = true;
        StringBuilder sb = new StringBuilder();
        Set<String> authors = Sets.newHashSet();
        Boolean isNew = ComConstants.BotNotifyRange.NEW.code == notifyCustomEntity.getBotRemindRange();
        final String formatter = "✔ %s(%s)：严重 %d，一般 %d，提示 %d \n";

        for (ToolLastAnalysisResultVO toolLastAnalysisResultVO : analysisResultVOList) {
            BaseLastAnalysisResultVO baseLastAnalysisResultVO = toolLastAnalysisResultVO.getLastAnalysisResultVO();
            if (baseLastAnalysisResultVO == null) {
                continue;
            }

            String toolName = toolLastAnalysisResultVO.getToolName();
            String toolDisplayName = toolMetaCacheService.getToolDisplayName(toolName);
            if (!notifyCustomEntity.getBotRemaindTools().contains(toolName)) {
                continue;
            }

            switch (baseLastAnalysisResultVO.getPattern()) {
                case "COVERITY":
                case "KLOCWORK":
                case "PINPOINT": {
                    CommonLastAnalysisResultVO commonVO = (CommonLastAnalysisResultVO) baseLastAnalysisResultVO;
                    Integer seriousCount = isNew ? commonVO.getNewSeriousCount() : commonVO.getExistSeriousCount();
                    Integer normalCount = isNew ? commonVO.getNewNormalCount() : commonVO.getExistNormalCount();
                    Integer promptCount = isNew ? commonVO.getNewPromptCount() : commonVO.getExistPromptCount();

                    if (unMatchSeverity
                            && checkCovToolSeverityMatch(commonVO, isNew, notifyCustomEntity.getBotRemindSeverity())) {
                        unMatchSeverity = false;
                    }

                    sb.append(String.format(formatter, toolDisplayName, getUpDownSymbol(commonVO.getDefectChange()),
                            seriousCount, normalCount, promptCount
                    ));

                    authors.addAll(
                            getCovToolAnalysisAuthor(commonVO, isNew, notifyCustomEntity.getBotRemindSeverity())
                    );
                }
                break;

                case "LINT": {
                    LintLastAnalysisResultVO lintVO = (LintLastAnalysisResultVO) baseLastAnalysisResultVO;
                    Integer seriousCount = isNew ? lintVO.getTotalNewSerious() : lintVO.getTotalSerious();
                    Integer normalCount = isNew ? lintVO.getTotalNewNormal() : lintVO.getTotalNormal();
                    Integer promptCount = isNew ? lintVO.getTotalNewPrompt() : lintVO.getTotalPrompt();

                    if (unMatchSeverity
                            && checkLintToolSeverityMatch(lintVO, isNew, notifyCustomEntity.getBotRemindSeverity())) {
                        unMatchSeverity = false;
                    }

                    sb.append(String.format(formatter, toolDisplayName, getUpDownSymbol(lintVO.getDefectChange()),
                            seriousCount, normalCount, promptCount
                    ));

                    authors.addAll(
                            getLintToolAnalysisAuthor(lintVO, isNew, notifyCustomEntity.getBotRemindSeverity())
                    );
                }
                break;

                // 对应关系：严重->极高、一般->高、提示->中
                case "CCN": {
                    CCNLastAnalysisResultVO ccnVO = (CCNLastAnalysisResultVO) baseLastAnalysisResultVO;
                    Integer seriousCount = isNew ? ccnVO.getNewSuperHighCount() : ccnVO.getSuperHighCount();
                    Integer normalCount = isNew ? ccnVO.getNewHighCount() : ccnVO.getHighCount();
                    Integer promptCount = isNew ? ccnVO.getNewMediumCount() : ccnVO.getMediumCount();
                    if (unMatchSeverity
                            && checkCCNOrDUPCToolSeverityMatch(ccnVO, isNew, notifyCustomEntity.getBotRemindSeverity())
                    ) {
                        unMatchSeverity = false;
                    }

                    float avgThousandDefect = ccnVO.getAverageThousandDefect() == null ? 0F
                            : ccnVO.getAverageThousandDefect().floatValue();
                    sb.append(String.format("✔ %s(%s)：千行超标复杂度 %s，极高风险 %d，高风险 %d，中风险 %d \n",
                            toolDisplayName, getUpDownSymbol(ccnVO.getAverageThousandDefectChange()),
                            keepFloatNPlaceWithRound(avgThousandDefect, 1),
                            seriousCount, normalCount, promptCount
                    ));

                    authors.addAll(
                            getCCNOrDUPCToolAnalysisAuthor(ccnVO.getNewAuthorStatistic(),
                                    ccnVO.getExistAuthorStatistic(), isNew, notifyCustomEntity.getBotRemindSeverity())
                    );
                }
                break;

                case "DUPC": {
                    DUPCLastAnalysisResultVO dupcVO = (DUPCLastAnalysisResultVO) baseLastAnalysisResultVO;
                    Integer seriousCount = isNew ? dupcVO.getNewSuperHighCount() : dupcVO.getSuperHighCount();
                    Integer normalCount = isNew ? dupcVO.getNewHighCount() : dupcVO.getHighCount();
                    Integer promptCount = isNew ? dupcVO.getNewMediumCount() : dupcVO.getMediumCount();
                    if (unMatchSeverity
                            && checkCCNOrDUPCToolSeverityMatch(dupcVO, isNew, notifyCustomEntity.getBotRemindSeverity())
                    ) {
                        unMatchSeverity = false;
                    }

                    sb.append(String.format("✔ %s(%s)：平均重复率 %s%%，极高风险 %d，高风险 %d，中风险 %d \n",
                            toolDisplayName, getUpDownSymbol(dupcVO.getDefectChange()),
                            keepFloatNPlaceWithRound(dupcVO.getDupRate(), 1),
                            seriousCount, normalCount, promptCount
                    ));

                    authors.addAll(
                            getCCNOrDUPCToolAnalysisAuthor(dupcVO.getNewAuthorStatistic(),
                                    dupcVO.getExistAuthorStatistic(), isNew, notifyCustomEntity.getBotRemindSeverity())
                    );
                }
                break;

                case "CLOC":
                case "STAT":
                default:
                    break;
            }
        }

        ToolAnalysisBotMsgModel toolAnalysisBotMsgModel = new ToolAnalysisBotMsgModel();
        toolAnalysisBotMsgModel.setBodyContent(sb.toString());
        toolAnalysisBotMsgModel.setAuthorSet(authors);
        toolAnalysisBotMsgModel.setZeroDefectMatch(unMatchSeverity);

        return toolAnalysisBotMsgModel;
    }

    /**
     * 校验圈复杂度或重复率的告警匹配情况
     *
     * @param analysisVO
     * @param isNewBotRemindRange
     * @param botRemindSeverity
     * @return
     */
    private boolean checkCCNOrDUPCToolSeverityMatch(BaseRiskAnalysisResultVO analysisVO,
                                                    Boolean isNewBotRemindRange, Integer botRemindSeverity) {
        return isNewBotRemindRange
                ? ((botRemindSeverity & ComConstants.SERIOUS) > 0 && analysisVO.getNewSuperHighCount() > 0)
                || ((botRemindSeverity & ComConstants.NORMAL) > 0 && analysisVO.getNewHighCount() > 0)
                || ((botRemindSeverity & ComConstants.PROMPT) > 0 && analysisVO.getNewMediumCount() > 0)

                : ((botRemindSeverity & ComConstants.SERIOUS) > 0 && analysisVO.getSuperHighCount() > 0)
                || ((botRemindSeverity & ComConstants.NORMAL) > 0 && analysisVO.getHighCount() > 0)
                || ((botRemindSeverity & ComConstants.PROMPT) > 0 && analysisVO.getMediumCount() > 0);
    }

    /**
     * 获取圈复杂度或重复率的问题处理人
     *
     * @param newAuthorList 新增告警处理人
     * @param existAuthorList 存量告警处理人
     */
    private Set<String> getCCNOrDUPCToolAnalysisAuthor(List<BaseRiskNotRepairedAuthorVO> newAuthorList,
                                                       List<BaseRiskNotRepairedAuthorVO> existAuthorList,
                                                       Boolean isNewBotRemindRange,
                                                       Integer botRemindSeverity) {
        Set<String> seriousAuthors = Sets.newHashSet();
        Set<String> normalAuthors = Sets.newHashSet();
        Set<String> promptAuthors = Sets.newHashSet();
        Set<String> retAuthors = Sets.newHashSet();

        if (!CollectionUtils.isEmpty(newAuthorList)) {
            newAuthorList.forEach(newAuthorStatistic -> {
                retAuthors.add(newAuthorStatistic.getName());
                if (newAuthorStatistic.getSuperHighCount() > 0) {
                    seriousAuthors.add(newAuthorStatistic.getName());
                }
                if (newAuthorStatistic.getHighCount() > 0) {
                    normalAuthors.add(newAuthorStatistic.getName());
                }
                if (newAuthorStatistic.getMediumCount() > 0) {
                    promptAuthors.add(newAuthorStatistic.getName());
                }
            });
        }

        if (!isNewBotRemindRange && !CollectionUtils.isEmpty(existAuthorList)) {
            existAuthorList.forEach(existAuthorStatistic -> {
                retAuthors.add(existAuthorStatistic.getName());
                if (existAuthorStatistic.getSuperHighCount() > 0) {
                    seriousAuthors.add(existAuthorStatistic.getName());
                }
                if (existAuthorStatistic.getHighCount() > 0) {
                    normalAuthors.add(existAuthorStatistic.getName());
                }
                if (existAuthorStatistic.getMediumCount() > 0) {
                    promptAuthors.add(existAuthorStatistic.getName());
                }
            });
        }

        return getAuthorsRetainWithSeverity(retAuthors, seriousAuthors, normalAuthors, promptAuthors,
                botRemindSeverity);
    }

    /**
     * 校验LINT类型工具的告警匹配情况
     *
     * @param lintAnalysisVO
     * @param isNewBotRemindRange
     * @param botRemindSeverity
     * @return
     */
    private boolean checkLintToolSeverityMatch(LintLastAnalysisResultVO lintAnalysisVO,
                                               Boolean isNewBotRemindRange, Integer botRemindSeverity) {
        return isNewBotRemindRange
                ? ((botRemindSeverity & ComConstants.SERIOUS) > 0 && lintAnalysisVO.getTotalNewSerious() > 0)
                || ((botRemindSeverity & ComConstants.NORMAL) > 0 && lintAnalysisVO.getTotalNewNormal() > 0)
                || ((botRemindSeverity & ComConstants.PROMPT) > 0 && lintAnalysisVO.getTotalNewPrompt() > 0)

                : ((botRemindSeverity & ComConstants.SERIOUS) > 0 && lintAnalysisVO.getTotalSerious() > 0)
                || ((botRemindSeverity & ComConstants.NORMAL) > 0 && lintAnalysisVO.getTotalNormal() > 0)
                || ((botRemindSeverity & ComConstants.PROMPT) > 0 && lintAnalysisVO.getTotalPrompt() > 0);
    }

    /**
     * 获取LINT类型工具的问题处理人
     */
    private Set<String> getLintToolAnalysisAuthor(LintLastAnalysisResultVO lintAnalysisVO, Boolean isNewBotRemindRange,
                                                  Integer botRemindSeverity) {
        if (lintAnalysisVO == null) {
            return Sets.newHashSet();
        }

        Set<String> seriousAuthors = Sets.newHashSet();
        Set<String> normalAuthors = Sets.newHashSet();
        Set<String> promptAuthors = Sets.newHashSet();
        Set<String> retAuthors = Sets.newHashSet();

        if (!CollectionUtils.isEmpty(lintAnalysisVO.getAuthorStatistic())) {
            lintAnalysisVO.getAuthorStatistic().forEach(newAuthorStatistic -> {
                retAuthors.add(newAuthorStatistic.getName());

                if (newAuthorStatistic.getSeriousCount() > 0) {
                    seriousAuthors.add(newAuthorStatistic.getName());
                }
                if (newAuthorStatistic.getNormalCount() > 0) {
                    normalAuthors.add(newAuthorStatistic.getName());
                }
                if (newAuthorStatistic.getPromptCount() > 0) {
                    promptAuthors.add(newAuthorStatistic.getName());
                }
            });
        }

        if (!isNewBotRemindRange && !CollectionUtils.isEmpty(lintAnalysisVO.getExistAuthorStatistic())) {
            lintAnalysisVO.getExistAuthorStatistic().forEach(existAuthorStatistic -> {
                retAuthors.add(existAuthorStatistic.getName());

                if (existAuthorStatistic.getSeriousCount() > 0) {
                    seriousAuthors.add(existAuthorStatistic.getName());
                }
                if (existAuthorStatistic.getNormalCount() > 0) {
                    normalAuthors.add(existAuthorStatistic.getName());
                }
                if (existAuthorStatistic.getPromptCount() > 0) {
                    promptAuthors.add(existAuthorStatistic.getName());
                }
            });
        }

        return getAuthorsRetainWithSeverity(retAuthors, seriousAuthors, normalAuthors, promptAuthors,
                botRemindSeverity);
    }

    /**
     * 校验COVERITY类型工具的告警匹配情况
     *
     * @param commonAnalysisVO
     * @param isNewBotRemindRange
     * @param botRemindSeverity
     * @return
     */
    private boolean checkCovToolSeverityMatch(CommonLastAnalysisResultVO commonAnalysisVO,
                                              Boolean isNewBotRemindRange, Integer botRemindSeverity) {
        return isNewBotRemindRange
                ? ((botRemindSeverity & ComConstants.SERIOUS) > 0 && commonAnalysisVO.getNewSeriousCount() > 0)
                || ((botRemindSeverity & ComConstants.NORMAL) > 0 && commonAnalysisVO.getNewNormalCount() > 0)
                || ((botRemindSeverity & ComConstants.PROMPT) > 0 && commonAnalysisVO.getNewPromptCount() > 0)

                : ((botRemindSeverity & ComConstants.SERIOUS) > 0 && commonAnalysisVO.getExistSeriousCount() > 0)
                || ((botRemindSeverity & ComConstants.NORMAL) > 0 && commonAnalysisVO.getExistNormalCount() > 0)
                || ((botRemindSeverity & ComConstants.PROMPT) > 0 && commonAnalysisVO.getExistPromptCount() > 0);
    }

    /**
     * 获取COVERITY类型工具的问题处理人
     */
    private Set<String> getCovToolAnalysisAuthor(CommonLastAnalysisResultVO commonAnalysisVO,
                                                 Boolean isNewBotRemindRange, Integer botRemindSeverity) {
        Set<String> seriousAuthors = Sets.newHashSet();
        Set<String> normalAuthors = Sets.newHashSet();
        Set<String> promptAuthors = Sets.newHashSet();
        seriousAuthors.addAll(commonAnalysisVO.getSeriousAuthors());
        normalAuthors.addAll(commonAnalysisVO.getNormalAuthors());
        promptAuthors.addAll(commonAnalysisVO.getPromptAuthors());

        Set<String> retAuthors = Sets.newHashSet();
        retAuthors.addAll(commonAnalysisVO.getNewAuthors());

        if (!isNewBotRemindRange) {
            retAuthors.addAll(commonAnalysisVO.getExistAuthors());
            seriousAuthors.addAll(commonAnalysisVO.getExistSeriousAuthors());
            normalAuthors.addAll(commonAnalysisVO.getExistNormalAuthors());
            promptAuthors.addAll(commonAnalysisVO.getExistPromptAuthors());
        }

        return getAuthorsRetainWithSeverity(retAuthors, seriousAuthors, normalAuthors, promptAuthors,
                botRemindSeverity);
    }

    /**
     * 结合严重等级，获取处理人交集
     *
     * @param source
     * @param seriousAuthors
     * @param normalAuthors
     * @param promptAuthors
     * @param botRemindSeverity
     * @return
     */
    private Set<String> getAuthorsRetainWithSeverity(Set<String> source, Set<String> seriousAuthors,
                                                     Set<String> normalAuthors, Set<String> promptAuthors,
                                                     Integer botRemindSeverity) {
        Set<String> severityAuthors = Sets.newHashSet();
        if ((botRemindSeverity & ComConstants.SERIOUS) > 0) {
            severityAuthors.addAll(seriousAuthors);
        }
        if ((botRemindSeverity & ComConstants.NORMAL) > 0) {
            severityAuthors.addAll(normalAuthors);
        }
        if ((botRemindSeverity & ComConstants.PROMPT) > 0) {
            severityAuthors.addAll(promptAuthors);
        }

        source.retainAll(severityAuthors);

        return source;
    }
}
