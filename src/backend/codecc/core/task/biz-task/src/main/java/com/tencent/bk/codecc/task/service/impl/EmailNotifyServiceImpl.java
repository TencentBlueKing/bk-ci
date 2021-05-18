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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.codecc.defect.api.ServiceDefectTreeResource;
import com.tencent.bk.codecc.defect.api.ServiceRepoResource;
import com.tencent.bk.codecc.defect.api.ServiceTaskLogRestResource;
import com.tencent.bk.codecc.defect.vo.CodeRepoVO;
import com.tencent.bk.codecc.quartz.pojo.JobExternalDto;
import com.tencent.bk.codecc.quartz.pojo.OperationType;
import com.tencent.bk.codecc.task.dao.CommonDao;
import com.tencent.bk.codecc.task.dao.mongorepository.EmailNotifyMessageTemplateRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.enums.EmailType;
import com.tencent.bk.codecc.task.model.EmailNotifyMessageTemplateEntity;
import com.tencent.bk.codecc.task.model.NotifyCustomEntity;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.pojo.DailyDataReportReqModel;
import com.tencent.bk.codecc.task.pojo.EmailMessageModel;
import com.tencent.bk.codecc.task.pojo.EmailNotifyModel;
import com.tencent.bk.codecc.task.pojo.NodeDataReportReqModel;
import com.tencent.bk.codecc.task.pojo.NodeServerRespModel;
import com.tencent.bk.codecc.task.pojo.RtxNotifyModel;
import com.tencent.bk.codecc.task.pojo.ToolBaseInfoModel;
import com.tencent.bk.codecc.task.service.DevopsNotifyService;
import com.tencent.bk.codecc.task.service.EmailNotifyService;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.CCNLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.CommonLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.DUPCLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.LintLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.api.util.UUIDUtil;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.OkhttpUtils;
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

import java.io.IOException;
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
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_CODECC_EMAIL_NOTIFY;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_CODECC_RTX_NOTIFY;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CODECC_BKPLUGINEMAIL_NOTIFY;
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
    private TaskRepository taskRepository;

    @Autowired
    private EmailNotifyMessageTemplateRepository emailNotifyMessageTemplateRepository;

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

    @Value("${codecc.node.host}")
    private String nodeHost;

    @Value("${devopsGateway.idchost}")
    private String devopsHost;

    @Value("${codecc.classurl:#{null}}")
    private String publicClassUrl;

    private static final String rtx_title = "CodeCC 代码检查中心";
    private static final String rtx_sender = "codecc";


    @Override
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_CODECC_EMAIL_NOTIFY,
            value = @Queue(value = QUEUE_CODECC_EMAIL_NOTIFY, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_CODECC_GENERAL_NOTIFY, durable = "true", delayed = "true", type = "topic")))
    public void sendReport(EmailNotifyModel emailNotifyModel) {
        try {
            Long taskId = emailNotifyModel.getTaskId();
            String buildId = emailNotifyModel.getBuildId();
            //1. 查询任务信息及报告定制信息
            TaskInfoEntity taskInfoEntity = taskRepository.findByTaskId(taskId);
            if(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(taskInfoEntity.getCreateFrom()))
            {
                log.info("gongfeng scan not send email!");
                return;
            }
            if(null != taskInfoEntity.getStatus() &&
                    ComConstants.Status.DISABLE.value() == taskInfoEntity.getStatus())
            {
                log.info("disabled task not send email! task id: {}", taskId);
                return;
            }
            NotifyCustomEntity notifyCustomEntity = taskInfoEntity.getNotifyCustomInfo();
            //2. 获取报表数据
            DailyDataReportReqModel reqModel;
            String title;
            if (EmailType.INSTANT == emailNotifyModel.getEmailType()) {
                log.info("start to send instant email, task id: {}, build id: {}", emailNotifyModel.getTaskId(),
                        emailNotifyModel.getBuildId());
                if(null != notifyCustomEntity && ComConstants.InstantReportStatus.DISABLED.code().
                        equals(notifyCustomEntity.getInstantReportStatus()))
                {
                    log.info("instant email disabled, task id: {}", emailNotifyModel.getTaskId());
                    return;
                }
                reqModel = getReportDataForAll(taskId, taskInfoEntity);
                reqModel.setType(ComConstants.ReportType.I.name());
                title = String.format("请阅: %s(%s)%s即时报告(%s)", taskInfoEntity.getNameCn(), taskInfoEntity.getNameEn(),
                        "所有工具", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            } else if(EmailType.DAILY == emailNotifyModel.getEmailType()){
                log.info("start to send scheduled email, task id: {}, build id: {}", emailNotifyModel.getTaskId(),
                        emailNotifyModel.getBuildId());
                title = String.format("请阅: %s(%s)%s定时报告(%s)", taskInfoEntity.getNameCn(), taskInfoEntity.getNameEn(),
                        "所有工具", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
                if (null != notifyCustomEntity) {
                    Set<String> toolNames = notifyCustomEntity.getReportTools();
                    log.info("test tool name set: {}", toolNames);
                    if(CollectionUtils.isEmpty(toolNames))
                    {
                        log.info("tool name is empty, task id : {}", emailNotifyModel.getTaskId());
                        return;
                    }
                    Set<Integer> reportDate = notifyCustomEntity.getReportDate();
                    Integer reportTime = notifyCustomEntity.getReportTime();
                    if(CollectionUtils.isEmpty(reportDate) || null == reportTime){
                        log.info("report date or time is empty!");
                        return;
                    }
                    reqModel = getReportData(taskId, taskInfoEntity, toolNames);
                    reqModel.setType(ComConstants.ReportType.T.name());
                } else {
                    log.info("email setting is empty, task id : {}", emailNotifyModel.getTaskId());
                    return;
                }
            } else
            {
              log.error("no qualified email type, task id: {}, email type : {}", taskId, emailNotifyModel.getEmailType().name());
              return;
            }
            reqModel.setBsBuildId(buildId);
            String url = String.format("http://%s/dailymail/content", nodeHost);
            String reqStr;
            try {
                reqStr = objectMapper.writeValueAsString(reqModel);
            } catch (JsonProcessingException e) {
                log.error("convert request model to json string fail! task id: {}, build id: {}", taskId, buildId);
                throw new CodeCCException(CommonMessageCode.UTIL_EXECUTE_FAIL);
            }
            //3. 请求web端生成图片
            String result = OkhttpUtils.INSTANCE.doHttpPost(url, reqStr, new HashMap<>());
            NodeServerRespModel nodeServerRespModel;
            try {
                nodeServerRespModel = objectMapper.readValue(result, NodeServerRespModel.class);
                log.info("fetch image info from node server! task id: {}", taskId);
            } catch (IOException e) {
                log.error("parse node server response model fail! task id: {}", taskId);
                throw new CodeCCException(CommonMessageCode.UTIL_EXECUTE_FAIL);
            }

            //4. 发送邮件
            String htmlEmail = nodeServerRespModel.getHtmlEmail();

            if (StringUtils.isBlank(htmlEmail)) {
                log.info("codecc email body is empty: taskId: {} | buildId: {}", taskId, buildId);
                return;
            }

            Map<String, String> attach = nodeServerRespModel.getImgBase64List();
            Set<String> receiverList;
            Set<String> receiverCCList = new HashSet<>();
            if (null == notifyCustomEntity) {
                receiverList = new HashSet<>(taskInfoEntity.getTaskOwner());
            }
            else
            {
                String receiverType = notifyCustomEntity.getEmailReceiverType();
                if(ComConstants.EmailReceiverType.TASK_MEMBER.code().equals(receiverType))
                {
                    List<String> receivers = taskInfoEntity.getTaskMember();
                    receivers.addAll(taskInfoEntity.getTaskOwner());
                    receiverList = new HashSet<>(receivers);
                }
                else if(ComConstants.EmailReceiverType.TASK_OWNER.code().equals(receiverType))
                {
                    receiverList = new HashSet<>(taskInfoEntity.getTaskOwner());
                }
                else
                {
                    receiverList =  notifyCustomEntity.getEmailReceiverList();
                }
                receiverCCList = notifyCustomEntity.getEmailCCReceiverList();
            }

            if(CollectionUtils.isEmpty(receiverList)){
                log.info("receiver list is empty! task id {}", emailNotifyModel.getTaskId());
                return;
            }
            devopsNotifyService.sendMail(rtx_sender, receiverList,
                    null == receiverCCList ? Collections.emptySet() : receiverCCList,
                    Collections.emptySet(), title, htmlEmail, "0", "HTML", attach);
            log.info("send email successfully!");
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
        try{
            TaskInfoEntity taskInfoEntity = taskRepository.findByTaskId(rtxNotifyModel.getTaskId());
            if(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(taskInfoEntity.getCreateFrom()))
            {
                log.info("gongfeng scan not send email!");
                return;
            }
            if(ComConstants.Status.DISABLE.value() == taskInfoEntity.getStatus())
            {
                log.info("task disabled not send email!");
                return;
            }
            String nameCn = taskInfoEntity.getNameCn();
            NotifyCustomEntity notifyCustomEntity = taskInfoEntity.getNotifyCustomInfo();
            Set<String> toolSet = new HashSet<>();
            List<ToolConfigInfoEntity> toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();
            if (CollectionUtils.isNotEmpty(toolConfigInfoEntityList)) {
                toolSet = toolConfigInfoEntityList.stream().filter(toolConfigInfoEntity ->
                        ComConstants.FOLLOW_STATUS.WITHDRAW.value() != toolConfigInfoEntity.getFollowStatus()).
                        map(ToolConfigInfoEntity::getToolName).collect(Collectors.toSet());
            }
            CodeCCResult<List<ToolLastAnalysisResultVO>> toolLastAnalysisCodeCCResult = client.get(ServiceTaskLogRestResource.class).
                    getBatchLatestTaskLog(taskInfoEntity.getTaskId(), toolSet);
            if(toolLastAnalysisCodeCCResult.isNotOk() || CollectionUtils.isEmpty(toolLastAnalysisCodeCCResult.getData()))
            {
                log.info("get analysis statistics result fail! task id: {}", rtxNotifyModel.getTaskId());
                return;
            }
            StringBuffer stringBuffer = new StringBuffer();
            if(rtxNotifyModel.getSuccess())
            {
                stringBuffer.append(String.format("任务%s扫描完成,\n" +
                                "[任务主页|%s]\n" +
                                "工具扫描结果如下:\n",
                        nameCn,
                        String.format("http://%s/console/codecc/%s/task/%s/detail", devopsHost, taskInfoEntity.getProjectId(), taskInfoEntity.getTaskId())));
                List<ToolLastAnalysisResultVO> toolLastAnalysisResultVOList = toolLastAnalysisCodeCCResult.getData();
                stringBuffer.append(doSendRtx(toolLastAnalysisResultVOList));
            }
            else
            {
                stringBuffer.append(String.format("任务%s扫描失败,\n" +
                                "[项目主页|%s]\n",
                        nameCn,
                        String.format("http://%s/console/codecc/%s/", devopsHost, taskInfoEntity.getProjectId())));
            }

            Set<String> receiverList;
            if (null == notifyCustomEntity) {
                receiverList = new HashSet<>(taskInfoEntity.getTaskOwner());
            }
            else
            {
                String receiverType = notifyCustomEntity.getRtxReceiverType();
                if(ComConstants.EmailReceiverType.TASK_MEMBER.code().equals(receiverType))
                {
                    List<String> receivers = taskInfoEntity.getTaskMember();
                    receivers.addAll(taskInfoEntity.getTaskOwner());
                    receiverList = new HashSet<>(receivers);
                }
                else if(ComConstants.EmailReceiverType.TASK_OWNER.code().equals(receiverType))
                {
                    receiverList = new HashSet<>(taskInfoEntity.getTaskOwner());
                }
                else if(ComConstants.EmailReceiverType.CUSTOMIZED.code().equals(receiverType))
                {
                    receiverList =  notifyCustomEntity.getRtxReceiverList();
                }
                else
                {
                    receiverList = new HashSet<>();
                }
            }
            devopsNotifyService.sendRtx(receiverList, stringBuffer.toString(), rtx_sender, rtx_title, "0");
        } catch (Exception e)
        {
            e.printStackTrace();
            log.error("send rtx message fail! task id: {}", rtxNotifyModel.getTaskId(), e);
        }

    }

    private String doSendRtx(List<ToolLastAnalysisResultVO> toolLastAnalysisResultVOList) {
        StringBuffer stringBuffer = new StringBuffer();

        if (toolLastAnalysisResultVOList == null) return "";
        if (toolLastAnalysisResultVOList.isEmpty()) return "";

        toolLastAnalysisResultVOList.forEach(toolLastAnalysisResultVO -> {
            BaseLastAnalysisResultVO baseLastAnalysisResultVO = toolLastAnalysisResultVO.getLastAnalysisResultVO();
            String toolDisplayName = toolMetaCacheService.getToolDisplayName(toolLastAnalysisResultVO.getToolName());

            switch (baseLastAnalysisResultVO.getPattern()){
                case "LINT": {
                    LintLastAnalysisResultVO lintLastAnalysisResultVO = (LintLastAnalysisResultVO) baseLastAnalysisResultVO;
                    stringBuffer.append(String.format("✔ %s分析完毕：" +
                            "告警数 %d %s，文件数 %d %s " +
                            "\r\n任务耗时：%s\n",
                        toolDisplayName,
                        lintLastAnalysisResultVO.getDefectCount(),
                        (null != lintLastAnalysisResultVO.getDefectChange() && lintLastAnalysisResultVO.getDefectChange() >= 0) ? String.format("(↑%d )", lintLastAnalysisResultVO.getDefectChange()) :
                            String.format("(↓%d )", Math.abs(null == lintLastAnalysisResultVO.getDefectChange() ? 0 : lintLastAnalysisResultVO.getDefectChange())),
                        lintLastAnalysisResultVO.getFileCount(),
                        getIntUpDownMessage(lintLastAnalysisResultVO.getFileChange()),
                        calculateTimeConsuming(toolLastAnalysisResultVO)
                    ));
                }
                break;
                case "CCN": {
                    CCNLastAnalysisResultVO ccnLastAnalysisResultVO = (CCNLastAnalysisResultVO) baseLastAnalysisResultVO;
                    stringBuffer.append(String.format("✔ %s分析完毕：风险函数 %d %s 平均圈复杂度 %s %s" +
                            "\r\n任务耗时：%s\n",
                        toolDisplayName,
                        ccnLastAnalysisResultVO.getDefectCount(),
                        (null != ccnLastAnalysisResultVO.getDefectChange() && ccnLastAnalysisResultVO.getDefectChange() >= 0) ? String.format("(↑%d )", ccnLastAnalysisResultVO.getDefectChange()) :
                            String.format("(↓%d )", Math.abs(null == ccnLastAnalysisResultVO.getDefectChange() ? 0 : ccnLastAnalysisResultVO.getDefectChange())),
                        ccnLastAnalysisResultVO.getAverageCCN(),
                        getFloatUpDownMessage(ccnLastAnalysisResultVO.getAverageCCNChange()),
                        calculateTimeConsuming(toolLastAnalysisResultVO)
                    ));
                }
                break;
                case "DUPC": {
                    DUPCLastAnalysisResultVO dupcLastAnalysisResultVO = (DUPCLastAnalysisResultVO) baseLastAnalysisResultVO;
                    stringBuffer.append(String.format("✔ %s分析完毕：重复文件 %d %s 平均重复率 %s 重复率趋势 %s" +
                            "\r\n任务耗时：%s\n",
                        toolDisplayName,
                        dupcLastAnalysisResultVO.getDefectCount(),
                        (null != dupcLastAnalysisResultVO.getDefectChange() && dupcLastAnalysisResultVO.getDefectChange() >= 0) ? String.format("(↑%d )", dupcLastAnalysisResultVO.getDefectChange()) :
                            String.format("(↓%d )", Math.abs(null == dupcLastAnalysisResultVO.getDefectChange() ? 0 : dupcLastAnalysisResultVO.getDefectChange())),
                        dupcLastAnalysisResultVO.getDupRate(),
                        getFloatUpDownMessage(dupcLastAnalysisResultVO.getDupRateChange()),
                        calculateTimeConsuming(toolLastAnalysisResultVO)
                    ));
                }
                break;
                case "COVERITY":
                case "KLOCWORK":
                case "PINPOINT": {
                    CommonLastAnalysisResultVO commonLastAnalysisResultVO = (CommonLastAnalysisResultVO) baseLastAnalysisResultVO;
                    stringBuffer.append(String.format("✔ %s分析完毕：\r\n新增 %d，关闭 %d，遗留 %d\r\n任务耗时：%s\n",
                        toolDisplayName,
                        commonLastAnalysisResultVO.getNewCount(),
                        commonLastAnalysisResultVO.getCloseCount(),
                        commonLastAnalysisResultVO.getExistCount(),
                        calculateTimeConsuming(toolLastAnalysisResultVO)));
                }
                break;
                default:
                    break;
            }
        });
        return stringBuffer.toString();
    }

    private Object getIntUpDownMessage(Integer intData) {
        if (intData == null) return "(0 )";
        return intData >= 0 ? String.format("(↑%d )", intData) :
            String.format("(↓%d )", Math.abs(intData));
    }

    private String getFloatUpDownMessage(Float floatData) {
        if (floatData == null) return "(0 )";
        return floatData >= 0 ? String.format("(↑%s )", floatData) :
            String.format("(↓%s )", Math.abs(floatData));
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
                emailNotifyMessageTemplateRepository.findByTemplateId(
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




        private DailyDataReportReqModel getReportDataForAll(Long taskId, TaskInfoEntity taskInfoEntity) {
        DailyDataReportReqModel dailyDataReportReqModel = new DailyDataReportReqModel();
        dailyDataReportReqModel.setTaskId(taskId);
        dailyDataReportReqModel.setProjectId(taskInfoEntity.getProjectId());
        dailyDataReportReqModel.setUrlRoot(String.format("http://%s/console/codecc/%s/", devopsHost, taskInfoEntity.getProjectId()));
        dailyDataReportReqModel.setStreamName(taskInfoEntity.getNameEn());
        dailyDataReportReqModel.setNameCN(taskInfoEntity.getNameCn());
        Set<String> toolNames = taskInfoEntity.getToolConfigInfoList().stream().filter(toolConfigInfoEntity -> ComConstants.FOLLOW_STATUS.WITHDRAW.value()
                != toolConfigInfoEntity.getFollowStatus()).map(ToolConfigInfoEntity::getToolName).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(toolNames)) {
            return null;
        }

        toolsDataInfo(dailyDataReportReqModel, taskId, toolNames);
        repoUrlInfo(dailyDataReportReqModel, taskId);
        dailyDataReportReqModel.setSummary(taskService.getTaskOverview(taskId).getLastAnalysisResultList());
        toolMapInfo(dailyDataReportReqModel, toolNames);
        return dailyDataReportReqModel;
    }


    private DailyDataReportReqModel getReportData(Long taskId, TaskInfoEntity taskInfoEntity, Set<String> toolNames) {
        DailyDataReportReqModel dailyDataReportReqModel = new DailyDataReportReqModel();
        dailyDataReportReqModel.setTaskId(taskId);
        dailyDataReportReqModel.setProjectId(taskInfoEntity.getProjectId());
        dailyDataReportReqModel.setUrlRoot(String.format("http://%s/console/codecc/%s/", devopsHost, taskInfoEntity.getProjectId()));
        dailyDataReportReqModel.setStreamName(taskInfoEntity.getNameEn());
        dailyDataReportReqModel.setNameCN(taskInfoEntity.getNameCn());
        toolsDataInfo(dailyDataReportReqModel, taskId, toolNames);
        repoUrlInfo(dailyDataReportReqModel, taskId);

        List<TaskOverviewVO.LastAnalysis> lastAnalysisList = taskService.getTaskOverview(taskId).getLastAnalysisResultList();
        dailyDataReportReqModel.setSummary(lastAnalysisList.stream().filter(lastAnalysis ->
                toolNames.contains(lastAnalysis.getToolName())
        ).collect(Collectors.toList()));
        toolMapInfo(dailyDataReportReqModel, toolNames);
        return dailyDataReportReqModel;
    }



    private void toolsDataInfo(DailyDataReportReqModel dailyDataReportReqModel, Long taskId, Set<String> toolNames)
    {
        List<String> toolOrder = Arrays.asList(commonDao.getToolOrder().split(","));
        CodeCCResult<JSONArray> dataReportCodeCCResult = client.get(ServiceDefectTreeResource.class).getBatchDataReports(taskId, toolNames);
        if (dataReportCodeCCResult.isNotOk() || null == dataReportCodeCCResult.getData()) {
            log.error("get data report list fail! task id: {}", taskId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        JSONArray dataReportRspVOList = dataReportCodeCCResult.getData();
        List<NodeDataReportReqModel> nodeReportRspVOList = new ArrayList<>();
        for(Object object : dataReportRspVOList)
        {
            if(null != object)
            {
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


    private void repoUrlInfo(DailyDataReportReqModel dailyDataReportReqModel, Long taskId)
    {
        CodeCCResult<Set<CodeRepoVO>> codeRepoCodeCCResult = client.get(ServiceRepoResource.class).getCodeRepoByTaskIdAndBuildId(taskId, null);
        if(codeRepoCodeCCResult.isNotOk() || CollectionUtils.isEmpty(codeRepoCodeCCResult.getData())){
            log.error("get repo url fail! task id: {}", taskId);
            return;
        }
        Set<CodeRepoVO> codeRepoVOS = codeRepoCodeCCResult.getData();
        dailyDataReportReqModel.setRepoUrls(codeRepoVOS);
    }

    private void toolMapInfo(DailyDataReportReqModel dailyDataReportReqModel, Set<String> toolNames)
    {
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
    private String calculateTimeConsuming(ToolLastAnalysisResultVO toolLastAnalysisResultVO)
    {
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


        if (hours != 0)
        {
            strElapseTime = String.format("%d 时 %d 分 %d 秒", hours, minutes, seconds);
        }
        else
        {
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
}
