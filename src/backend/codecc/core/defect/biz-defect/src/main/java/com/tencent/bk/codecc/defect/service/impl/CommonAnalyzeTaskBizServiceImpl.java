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

package com.tencent.bk.codecc.defect.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CommonStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CodeFileUrlDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.dao.redis.StatisticDao;
import com.tencent.bk.codecc.defect.dto.WebsocketDTO;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.service.AbstractAnalyzeTaskBizService;
import com.tencent.bk.codecc.defect.service.IMessageQueueBizService;
import com.tencent.bk.codecc.defect.service.RedLineReportService;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.utils.BotUtil;
import com.tencent.bk.codecc.defect.utils.CommonKafkaClient;
import com.tencent.bk.codecc.defect.vo.CodeFileUrlVO;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.vo.NotifyCustomVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;
import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.CompressionUtils;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.util.PathUtils;
import com.tencent.devops.common.util.ThreadPoolUtil;
import com.tencent.devops.common.web.mq.ConstantsKt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.devops.common.web.mq.ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT;
import static com.tencent.devops.common.web.mq.ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT;

/**
 * 平台类工具分析记录上报的接口实现
 *
 * @version V1.0
 * @date 2019/10/3
 */
@Slf4j
@Service("CommonAnalyzeTaskBizService")
public class CommonAnalyzeTaskBizServiceImpl extends AbstractAnalyzeTaskBizService
{
    @Value("${devopsGateway.idchost:#{null}}")
    protected String devopsHost;
    @Autowired
    protected StatisticDao statisticDao;
    @Autowired
    protected BuildRepository buildRepository;
    @Autowired
    protected CommonStatisticRepository commonStatisticRepository;
    @Autowired
    protected CodeFileUrlDao codeFileUrlDao;
    @Autowired
    protected ToolMetaCacheService toolMetaCacheService;
    @Autowired
    private DefectRepository defectRepository;
    @Autowired
    private BuildDefectRepository buildDefectRepository;
    @Autowired
    private RedLineReportService redLineReportService;
    @Autowired
    private ToolBuildInfoDao toolBuildInfoDao;
    @Autowired
    private ScmFileInfoService scmFileInfoService;
    @Autowired
    private CommonKafkaClient commonKafkaClient;

    @Override
    protected void postHandleDefectsAndStatistic(UploadTaskLogStepVO uploadTaskLogStepVO, TaskDetailVO taskVO)
    {
        log.info("begin postHandleDefectsAndStatistic...");
        // 如果工具缺陷提交到自带platform成功，则开始启动将告警提交到codecc
        if (uploadTaskLogStepVO.getStepNum() == ComConstants.Step4Cov.COMMIT.value()
                && uploadTaskLogStepVO.getFlag() == ComConstants.StepFlag.SUCC.value()
                && !uploadTaskLogStepVO.isFastIncrement())
        {
            log.info("begin commit defect.");
            String toolName = uploadTaskLogStepVO.getToolName();

            // 通过消息队列通知coverity服务提单
            CommitDefectVO commitDefectVO = new CommitDefectVO();
            commitDefectVO.setTaskId(uploadTaskLogStepVO.getTaskId());
            commitDefectVO.setStreamName(uploadTaskLogStepVO.getStreamName());
            commitDefectVO.setToolName(toolName);
            commitDefectVO.setBuildId(uploadTaskLogStepVO.getPipelineBuildId());
            commitDefectVO.setTriggerFrom(uploadTaskLogStepVO.getTriggerFrom());

            // 区分创建来源，创建对应处理器
            IMessageQueueBizService messageQueueBizService = messageBizServiceFactory.createBizService(
                    taskVO.getCreateFrom(),ComConstants.BusinessType.MESSAGE_QUEUE.value(),IMessageQueueBizService.class);
            messageQueueBizService.messageQueueConvertAndSend(toolName, commitDefectVO);
        }
        else if (uploadTaskLogStepVO.getStepNum() == getSubmitStepNum()
                && uploadTaskLogStepVO.getFlag() == ComConstants.StepFlag.SUCC.value()
                && !uploadTaskLogStepVO.isFastIncrement())
        {
            log.info("begin statistic defect count.");
            handleSubmitSuccess(uploadTaskLogStepVO, taskVO);
        }
    }

    /**
     * 处理提单成功
     *
     * @param uploadTaskLogStepVO
     * @param taskVO
     */
    @Override
    protected void handleSubmitSuccess(UploadTaskLogStepVO uploadTaskLogStepVO, TaskDetailVO taskVO)
    {
        long taskId = uploadTaskLogStepVO.getTaskId();
        String toolName = uploadTaskLogStepVO.getToolName();
        String buildId = uploadTaskLogStepVO.getPipelineBuildId();
        CommonStatisticEntity statisticEntity = new CommonStatisticEntity();
        statisticEntity.setTaskId(taskId);
        statisticEntity.setToolName(toolName);
        statisticEntity.setTime(System.currentTimeMillis());

        BuildEntity buildEntity = buildRepository.findByBuildId(buildId);
        statisticDao.getAndClearDefectStatistic(statisticEntity, buildEntity.getBuildNo());

        commonStatisticRepository.save(statisticEntity);

        //将数据加入数据平台
        commonKafkaClient.pushCommonStatisticToKafka(statisticEntity);

        // 保存首次分析成功时间
        saveFirstSuccessAnalyszeTime(taskId, toolName);

        // 保存本次构建遗留告警告警列表快照
        saveBuildDefects(uploadTaskLogStepVO);

        // 更新告警快照基准构建ID
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);

        // 发送群机器人通知
        sendBotRemind(taskVO, statisticEntity, toolName);

        // 保存质量红线数据
        redLineReportService.saveRedLineData(taskVO, toolName, buildId);

        // 处理md5.json
        scmFileInfoService.parseFileInfo(uploadTaskLogStepVO.getTaskId(),
                uploadTaskLogStepVO.getStreamName(),
                uploadTaskLogStepVO.getToolName(),
                uploadTaskLogStepVO.getPipelineBuildId());

        // 清除强制全量扫描标志
        clearForceFullScan(taskId, toolName);
    }

    @Override
    protected void updateCodeRepository(UploadTaskLogStepVO uploadTaskLogStepVO, TaskLogEntity taskLogEntity)
    {
        if (uploadTaskLogStepVO.getStepNum() == ComConstants.Step4Cov.COMMIT.value()
                && uploadTaskLogStepVO.getFlag() == ComConstants.StepFlag.SUCC.value())
        {
            ThreadPoolUtil.addRunnableTask(() ->
            {
                /*
                    工具侧整理输出的文件路径格式跟工具无关，如下：
                    window：D:/workspace/svnauth_svr/app/utils/jgit_proxy/SSHProxySessionFactory.java
                    linux:  /data/landun/workspace/test/parallel/test-string-decoder-fuzz.js
                    后台存入t_code_file_url表中时，做了如下处理：
                    window下的路径将盘号去掉，变成
                    /workspace/svnauth_svr/app/utils/jgit_proxy/SSHProxySessionFactory.java
                    另外url转换成标准http格式
                 */
                saveCodeFileUrl(uploadTaskLogStepVO);

                // 保存代码仓信息
                saveCodeRepoInfo(uploadTaskLogStepVO);
            });

        }
    }

    /**
     * 保存代码文件的URL信息
     *
     * @param uploadTaskLogStepVO
     */
    private void saveCodeFileUrl(UploadTaskLogStepVO uploadTaskLogStepVO)
    {
        String scmUrlJsonStr = scmJsonComponent.loadRepoFileUrl(uploadTaskLogStepVO.getStreamName(), uploadTaskLogStepVO.getToolName(), uploadTaskLogStepVO.getPipelineBuildId());
        if (StringUtils.isNotEmpty(scmUrlJsonStr))
        {
            CodeFileUrlVO codeFileUrlVO = JsonUtil.INSTANCE.to(scmUrlJsonStr, CodeFileUrlVO.class);
            String fileListStr = codeFileUrlVO.getFileList();
            String codeFileURLJson = CompressionUtils.decodeBase64AndDecompress(fileListStr);

            List<CodeFileUrlEntity> codeFileUrlEntityList = JsonUtil.INSTANCE.to(codeFileURLJson, new TypeReference<List<CodeFileUrlEntity>>()
            {
            });
            long currTime = System.currentTimeMillis();
            codeFileUrlEntityList.forEach(codeFileUrlEntity ->
            {
                String filePath = codeFileUrlEntity.getFile();
                if (StringUtils.isNotEmpty(filePath))
                {
                    filePath = PathUtils.trimWinPathPrefix(filePath);
                    codeFileUrlEntity.setFile(filePath);
                }
                codeFileUrlEntity.setUrl(PathUtils.formatFileRepoUrlToHttp(codeFileUrlEntity.getUrl()));
                codeFileUrlEntity.setUpdatedDate(currTime);
            });

            codeFileUrlDao.upsert(uploadTaskLogStepVO.getTaskId(), codeFileUrlEntityList);
        }
    }

    @Override
    public int getSubmitStepNum()
    {
        return ComConstants.Step4Cov.DEFECT_SYNS.value();
    }

    @Override
    public int getCodeDownloadStepNum()
    {
        return ComConstants.Step4Cov.UPLOAD.value();
    }

    public void sendBotRemind(TaskDetailVO taskVO, CommonStatisticEntity statisticEntity, String toolName)
    {
        // 发送群机器人通知
        NotifyCustomVO notifyCustomVO = taskVO.getNotifyCustomInfo();
        if (notifyCustomVO != null && StringUtils.isNotEmpty(notifyCustomVO.getBotWebhookUrl()) && notifyCustomVO.getBotRemindRange() != null
                && notifyCustomVO.getBotRemaindTools() != null && notifyCustomVO.getBotRemaindTools().contains(toolName))
        {
            log.info("send robot remind url:{}, range:{}, severity:{}", notifyCustomVO.getBotWebhookUrl(), notifyCustomVO.getBotRemindRange(),
                    notifyCustomVO.getBotRemindSeverity());
            boolean matchSeverity = false;
            //如果是新增的tab页，则用新增的告警数进行判断，如果是遗留的tab页，则用遗留的告警数进行判断
            if (ComConstants.BotNotifyRange.NEW.code == notifyCustomVO.getBotRemindRange())
            {
                if ((notifyCustomVO.getBotRemindSeverity() & ComConstants.PROMPT) > 0 && null != statisticEntity.getNewPromptCount() && statisticEntity.getNewPromptCount() > 0)
                {
                    matchSeverity = true;
                }
                if ((notifyCustomVO.getBotRemindSeverity() & ComConstants.NORMAL) > 0 && null != statisticEntity.getNewNormalCount() && statisticEntity.getNewNormalCount() > 0)
                {
                    matchSeverity = true;
                }
                if ((notifyCustomVO.getBotRemindSeverity() & ComConstants.SERIOUS) > 0 && null != statisticEntity.getNewSeriousCount() && statisticEntity.getNewSeriousCount() > 0)
                {
                    matchSeverity = true;
                }
            }
            else if (ComConstants.BotNotifyRange.EXIST.code == notifyCustomVO.getBotRemindRange())
            {
                if ((notifyCustomVO.getBotRemindSeverity() & ComConstants.PROMPT) > 0 && null != statisticEntity.getExistPromptCount() && statisticEntity.getExistPromptCount() > 0)
                {
                    matchSeverity = true;
                }
                if ((notifyCustomVO.getBotRemindSeverity() & ComConstants.NORMAL) > 0 && null != statisticEntity.getExistNormalCount() && statisticEntity.getExistNormalCount() > 0)
                {
                    matchSeverity = true;
                }
                if ((notifyCustomVO.getBotRemindSeverity() & ComConstants.SERIOUS) > 0 && null != statisticEntity.getExistSeriousCount() && statisticEntity.getExistSeriousCount() > 0)
                {
                    matchSeverity = true;
                }
            }
            if (matchSeverity)
            {
                Set<String> authors = null;
                String botRemainMsg = null;
                String taskName = StringUtils.isEmpty(taskVO.getNameCn()) ? taskVO.getNameEn() : taskVO.getNameCn();
                String defectListUrl = "http://" + devopsHost + String.format("/console/codecc/%s/task/%s/defect/compile/%s/list", taskVO.getProjectId(),
                        taskVO.getTaskId(), toolName);
                String toolDisplayName = toolMetaCacheService.getToolDisplayName(toolName);
                if (ComConstants.BotNotifyRange.NEW.code == notifyCustomVO.getBotRemindRange())
                {
                    authors = statisticEntity.getNewAuthors();
                    botRemainMsg = String.format("%s告警未清零：%s\n本次扫描新增告警   严重 %d，一般 %d，提示 %d\n目前遗留告警 总计 %d\n[告警列表|%s]\n积极修复就有机会被月度大比拼表彰喔！",
                            toolDisplayName,
                            taskName,
                            statisticEntity.getNewSeriousCount(),
                            statisticEntity.getNewNormalCount(),
                            statisticEntity.getNewPromptCount(),
                            statisticEntity.getExistCount(),
                            defectListUrl);
                }
                else if (ComConstants.BotNotifyRange.EXIST.code == notifyCustomVO.getBotRemindRange())
                {
                    authors = statisticEntity.getExistAuthors();
                    botRemainMsg = String.format("%s告警未清零：%s\n严重 %d，一般 %d，提示 %d\n[告警列表|%s]\n积极修复就有机会被月度大比拼表彰喔！",
                            toolDisplayName,
                            taskName,
                            statisticEntity.getExistSeriousCount(),
                            statisticEntity.getExistNormalCount(),
                            statisticEntity.getExistPromptCount(),
                            defectListUrl);
                }
                if (CollectionUtils.isNotEmpty(authors) && StringUtils.isNotEmpty(botRemainMsg))
                {
                    log.info("send to robot authors:{}, botRemainMsg:{}", authors, botRemainMsg);
                    BotUtil.sendMsgToRobot(notifyCustomVO.getBotWebhookUrl(), botRemainMsg, authors);
                }
            }
        }
    }

    public void saveBuildDefects(UploadTaskLogStepVO uploadTaskLogStepVO)
    {
        long taskId = uploadTaskLogStepVO.getTaskId();
        String toolName = uploadTaskLogStepVO.getToolName();
        List<DefectEntity> defectList = defectRepository.findByTaskIdAndToolNameAndStatus(taskId, toolName, ComConstants.DefectStatus.NEW.value());
        if (CollectionUtils.isNotEmpty(defectList))
        {
            List<BuildDefectEntity> buildDefectEntities = Lists.newArrayList();
            for (DefectEntity defectEntity : defectList)
            {
                BuildDefectEntity buildDefectEntity = new BuildDefectEntity();
                buildDefectEntity.setTaskId(taskId);
                buildDefectEntity.setToolName(toolName);
                buildDefectEntity.setBuildId(uploadTaskLogStepVO.getPipelineBuildId());
                buildDefectEntity.setDefectId(defectEntity.getId());
                buildDefectEntities.add(buildDefectEntity);
            }
            buildDefectRepository.save(buildDefectEntities);
        }
    }

}
