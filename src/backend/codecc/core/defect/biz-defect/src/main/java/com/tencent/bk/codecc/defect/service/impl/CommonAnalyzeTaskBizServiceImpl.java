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
import com.tencent.bk.codecc.defect.service.RedLineReportService;
import com.tencent.bk.codecc.defect.service.TaskLogOverviewService;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.vo.CodeFileUrlVO;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.vo.NotifyCustomVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BaseDataCacheService;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.service.utils.BotUtil;
import com.tencent.devops.common.util.*;
import com.tencent.devops.common.web.mq.ConstantsKt;
import java.util.Arrays;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC_OPENSOURCE;

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
    @Value("${bkci.public.url:#{null}}")
    protected String devopsHost;
    @Value("${bkci.public.url:#{null}}")
    protected String codeccHost;
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
    private TaskLogOverviewService taskLogOverviewService;
    @Autowired
    private BaseDataCacheService baseDataCacheService;

    private static Map<Integer, String> BOT_SEVERITY_MSG_MAP = new HashMap<Integer, String>() {{
        put(ComConstants.SERIOUS, "严重告警");
        put(ComConstants.SERIOUS | ComConstants.NORMAL, "严重+一般告警");
        put(ComConstants.SERIOUS | ComConstants.NORMAL | ComConstants.PROMPT, "所有告警");
    }};

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
            commitDefectVO.setCreateFrom(taskVO.getCreateFrom());
            if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(taskVO.getCreateFrom()))
            {
                rabbitTemplate.convertAndSend(ConstantsKt.PREFIX_EXCHANGE_OPENSOURCE_DEFECT_COMMIT + toolName.toLowerCase(),
                        ConstantsKt.PREFIX_ROUTE_OPENSOURCE_DEFECT_COMMIT + toolName.toLowerCase(), commitDefectVO);
            }
            else
            {
                rabbitTemplate.convertAndSend(ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT + toolName.toLowerCase(),
                        ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT + toolName.toLowerCase(), commitDefectVO);
            }
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
        CommonStatisticEntity statisticEntity = CommonStatisticEntity.constructByZeroVal();
        statisticEntity.setTaskId(taskId);
        statisticEntity.setToolName(toolName);
        statisticEntity.setTime(System.currentTimeMillis());
        statisticEntity.setBuildId(buildId);

        BuildEntity buildEntity = buildRepository.findFirstByBuildId(buildId);
        statisticDao.getAndClearDefectStatistic(statisticEntity, buildEntity.getBuildNo());

        statisticEntity = commonStatisticRepository.save(statisticEntity);

        // 异步统计非new状态的告警数
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(taskVO.getCreateFrom())) {
            rabbitTemplate.convertAndSend(EXCHANGE_CLOSE_DEFECT_STATISTIC_OPENSOURCE,
                    ROUTE_CLOSE_DEFECT_STATISTIC_OPENSOURCE, statisticEntity);
        } else {
            rabbitTemplate.convertAndSend(EXCHANGE_CLOSE_DEFECT_STATISTIC, ROUTE_CLOSE_DEFECT_STATISTIC, statisticEntity);
        }

        // 保存首次分析成功时间
        saveFirstSuccessAnalyszeTime(taskId, toolName);

        // 保存本次构建遗留告警告警列表快照
        saveBuildDefects(uploadTaskLogStepVO);

        // 更新告警快照基准构建ID
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);

        // 改由MQ汇总发送 {@link EmailNotifyServiceImpl#sendWeChatBotRemind(RtxNotifyModel, TaskInfoEntity)}
        // 发送群机器人通知
        //sendBotRemind(taskVO, statisticEntity, toolName);

        // 保存质量红线数据
        redLineReportService.saveRedLineData(taskVO, toolName, buildId);

        // 处理md5.json
        scmFileInfoService.parseFileInfo(uploadTaskLogStepVO.getTaskId(),
                uploadTaskLogStepVO.getStreamName(),
                uploadTaskLogStepVO.getToolName(),
                uploadTaskLogStepVO.getPipelineBuildId());

        // 清除强制全量扫描标志
        clearForceFullScan(taskId, toolName);

        // 设置当前工具执行完成
        uploadTaskLogStepVO.setFinish(true);
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
        log.info("start to send robot remind: {}", taskVO.getTaskId());
        if (notifyCustomVO != null && StringUtils.isNotEmpty(notifyCustomVO.getBotWebhookUrl()) && notifyCustomVO.getBotRemindRange() != null
                && notifyCustomVO.getBotRemaindTools() != null && notifyCustomVO.getBotRemaindTools().contains(toolName))
        {
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
                Set<String> authors = new HashSet<>();
                String botRemainMsg = null;
                String taskName = StringUtils.isEmpty(taskVO.getNameCn()) ? taskVO.getNameEn() : taskVO.getNameCn();
                String defectListUrl = NotifyUtils.getBotTargetUrl(taskVO.getProjectId(), taskVO.getNameCn(), taskVO.getTaskId(), toolName, codeccHost, devopsHost);
                String toolDisplayName = toolMetaCacheService.getToolDisplayName(toolName);
                if (ComConstants.BotNotifyRange.NEW.code == notifyCustomVO.getBotRemindRange())
                {
                    authors = statisticEntity.getNewAuthors();
                    botRemainMsg = String.format("%s告警未清零：%s\n本次扫描新增告警   严重 %d，一般 %d，提示 %d\n目前遗留告警 总计 %d\n[告警列表|%s]\n%s处理人如下：",
                            toolDisplayName,
                            taskName,
                            statisticEntity.getNewSeriousCount(),
                            statisticEntity.getNewNormalCount(),
                            statisticEntity.getNewPromptCount(),
                            statisticEntity.getExistCount(),
                            defectListUrl,
                            notifyCustomVO.getBotRemindSeverity() == null ? "" : BOT_SEVERITY_MSG_MAP.get(notifyCustomVO.getBotRemindSeverity()));
                }
                else if (ComConstants.BotNotifyRange.EXIST.code == notifyCustomVO.getBotRemindRange())
                {
                    authors = statisticEntity.getExistAuthors();
                    botRemainMsg = String.format("%s告警未清零：%s\n严重 %d，一般 %d，提示 %d\n[告警列表|%s]\n%s处理人如下：",
                            toolDisplayName,
                            taskName,
                            statisticEntity.getExistSeriousCount(),
                            statisticEntity.getExistNormalCount(),
                            statisticEntity.getExistPromptCount(),
                            defectListUrl,
                        notifyCustomVO.getBotRemindSeverity() == null ? "" : BOT_SEVERITY_MSG_MAP.get(notifyCustomVO.getBotRemindSeverity()));
                }

                log.info("filter by defect severity for task: {}", taskVO.getTaskId());
                if (notifyCustomVO.getBotRemindSeverity() != null) {
                    Set<Object> severityAuthors = new HashSet<>();
                    if ((notifyCustomVO.getBotRemindSeverity() & ComConstants.SERIOUS) > 0) {
                        severityAuthors.addAll(statisticEntity.getSeriousAuthors());
                    }
                    if ((notifyCustomVO.getBotRemindSeverity() & ComConstants.NORMAL) > 0) {
                        severityAuthors.addAll(statisticEntity.getNormalAuthors());
                    }
                    if ((notifyCustomVO.getBotRemindSeverity() & ComConstants.PROMPT) > 0) {
                        severityAuthors.addAll(statisticEntity.getPromptAuthors());
                    }

                    authors.retainAll(severityAuthors);
                }

                log.info("send to robot for task is {}, authors:{}, botRemainMsg:{}", taskVO.getTaskId(), authors, botRemainMsg);
                if (CollectionUtils.isNotEmpty(authors) && StringUtils.isNotEmpty(botRemainMsg))
                {
                    BotUtil.sendMsgToRobot(notifyCustomVO.getBotWebhookUrl(), botRemainMsg, authors);
                }
            }
        }
    }


    /**
     * 发送websocket信息
     *
     * @param toolConfigBaseVO
     * @param uploadTaskLogStepVO
     * @param taskId
     * @param toolName
     */
    @Override
    protected void sendWebSocketMsg(ToolConfigBaseVO toolConfigBaseVO, UploadTaskLogStepVO uploadTaskLogStepVO,
                                    TaskLogEntity taskLogEntity, TaskDetailVO taskDetailVO, long taskId, String toolName)
    {
        //1. 推送消息至任务详情首页面
        TaskOverviewVO.LastAnalysis lastAnalysis = assembleAnalysisResult(toolConfigBaseVO, uploadTaskLogStepVO, toolName);
        //获取告警数量信息
        if (ComConstants.Step4Cov.COMPLETE.value() == uploadTaskLogStepVO.getStepNum() &&
                ComConstants.StepFlag.SUCC.value() == uploadTaskLogStepVO.getFlag())
        {
            ToolLastAnalysisResultVO toolLastAnalysisResultVO = new ToolLastAnalysisResultVO();
            toolLastAnalysisResultVO.setTaskId(taskId);
            toolLastAnalysisResultVO.setToolName(toolName);
            BaseLastAnalysisResultVO lastAnalysisResultVO = taskLogService.getLastAnalysisResult(toolLastAnalysisResultVO, toolName);
            lastAnalysis.setLastAnalysisResult(lastAnalysisResultVO);
        }

        TaskLogVO taskLogVO = new TaskLogVO();
        BeanUtils.copyProperties(taskLogEntity, taskLogVO, "stepArray");
        List<TaskLogEntity.TaskUnit> stepArrayEntity = taskLogEntity.getStepArray();
        List<TaskLogVO.TaskUnit> stepArrayVO = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(stepArrayEntity))
        {
            stepArrayVO = stepArrayEntity.stream().map(taskUnit ->
            {
                TaskLogVO.TaskUnit taskUnitVO = new TaskLogVO.TaskUnit();
                BeanUtils.copyProperties(taskUnit, taskUnitVO);
                return taskUnitVO;
            }).collect(Collectors.toList());
        }
        taskLogVO.setStepArray(stepArrayVO);
        TaskLogOverviewVO taskLogOverviewVO = taskLogOverviewService.getTaskLogOverview(taskId,
                uploadTaskLogStepVO.getPipelineBuildId(),
                null);

        List<TaskLogVO> taskLogVOList = new ArrayList<>();
        BaseDataVO orderToolIds = baseDataCacheService.getToolOrder();
        List<String> toolOrderList = Arrays.asList(orderToolIds.getParamValue().split(","));
        if (taskLogOverviewVO != null && taskLogOverviewVO.getTaskLogVOList() != null) {
            taskLogVOList = taskLogOverviewVO.getTaskLogVOList();
            // 工具展示顺序排序
            taskLogOverviewVO.getTaskLogVOList()
                    .sort(Comparator.comparingInt(it -> toolOrderList.indexOf(it.getToolName())));
        }
        taskLogVOList.removeIf(it -> it.getToolName().equals(taskLogVO.getToolName()));
        taskLogVOList.add(taskLogVO);

        assembleTaskInfo(uploadTaskLogStepVO, taskDetailVO, taskLogEntity);

        WebsocketDTO websocketDTO = new WebsocketDTO(taskLogVO, lastAnalysis, taskDetailVO, taskLogOverviewVO);
        rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_TASKLOG_DEFECT_WEBSOCKET, "",
                websocketDTO);
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
            buildDefectRepository.saveAll(buildDefectEntities);
        }
    }

}
