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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.codecc.defect.constant.DefectConstants;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.TaskLogRepository;
import com.tencent.bk.codecc.defect.dao.redis.TaskAnalysisDao;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;
import com.tencent.devops.common.api.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.ToolLastAnalysisResultVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.plugin.codecc.pojo.CodeccBuildInfo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务分析记录服务层
 *
 * @version V1.0
 * @date 2019/5/5
 */
public abstract class AbstractAnalyzeTaskBizService implements IBizService<UploadTaskLogStepVO>
{
    private static Logger logger = LoggerFactory.getLogger(AbstractAnalyzeTaskBizService.class);

    @Autowired
    public TaskAnalysisDao taskAnalysisDao;
    @Autowired
    protected TaskLogRepository taskLogRepository;
    @Autowired
    private BuildRepository buildRepository;
    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;
    @Autowired
    private Client client;
    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TaskLogService taskLogService;

    @Override
    public Result processBiz(UploadTaskLogStepVO uploadTaskLogStepVO)
    {
        if (uploadTaskLogStepVO.getStartTime() != ComConstants.COMMON_NUM_0L)
        {
            uploadTaskLogStepVO.setStartTime(System.currentTimeMillis());
        }
        else if (uploadTaskLogStepVO.getEndTime() != ComConstants.COMMON_NUM_0L)
        {
            uploadTaskLogStepVO.setEndTime(System.currentTimeMillis());
        }

        String streamName = uploadTaskLogStepVO.getStreamName();
        String toolName = uploadTaskLogStepVO.getToolName();

        // 调用task模块的接口获取任务信息
        TaskBaseVO taskVO = thirdPartySystemCaller.getTaskInfo(streamName);

        long taskId = taskVO.getTaskId();
        uploadTaskLogStepVO.setTaskId(taskId);

        TaskLogEntity lastTaskLogEntity = taskLogRepository.findFirstByTaskIdAndToolNameOrderByStartTimeDesc(taskId, toolName);


        //创建新的分析任务条件：（1）该项目首次创建的任务
        // （2）上一个任务已经处于失败状态
        // （3）上一个已经被标记结束时间
        // （4）上一个的最新step大于当前上报的步骤step
        // （5）上一个任务被中断
        if (lastTaskLogEntity == null
                || lastTaskLogEntity.getFlag() == DefectConstants.TASK_FLAG_FAIL
                || lastTaskLogEntity.getEndTime() != ComConstants.COMMON_NUM_0L
                || lastTaskLogEntity.getCurrStep() > uploadTaskLogStepVO.getStepNum()
                || lastTaskLogEntity.getFlag() == DefectConstants.TASK_FLAG_ABORT
                || needCreateTaskWhenSameStep(lastTaskLogEntity, uploadTaskLogStepVO, taskVO))
        {
            logger.info("begin to create new task log");
            //该任务的当前step大于步骤step并且不是标记为失败，需要将该任务设置为 finish，
            if (lastTaskLogEntity != null && lastTaskLogEntity.getCurrStep() > uploadTaskLogStepVO.getStepNum()
                    && lastTaskLogEntity.getFlag() == DefectConstants.TASK_FLAG_PROCESSING)
            {
                int lastIndex = lastTaskLogEntity.getStepArray().size() - 1;
                ToolConfigBaseVO abortToolConfigInfo = updateAbortAnalysisTaskLog(lastTaskLogEntity, toolName);
                pipelineService.handleDevopsCallBack(lastTaskLogEntity, lastTaskLogEntity.getStepArray().get(lastIndex), toolName);
                //发送websocket信息
                sendWebSocketMsg(abortToolConfigInfo, uploadTaskLogStepVO, lastTaskLogEntity, taskId, toolName);
            }

            // 如果第一步开始，需要创建版本号
            String analysisVersion = taskAnalysisDao.generateAnalysisVersion(taskId, toolName);
            //更新工具信息 用于后续websocket推送信息使用
            ToolConfigBaseVO toolConfigBaseVO = new ToolConfigBaseVO();
            TaskLogEntity taskLogEntity = createNewTaskLog(uploadTaskLogStepVO, taskVO, toolConfigBaseVO);

            // 分析开始前告警数据及告警统计数据预处理
            preHandleDefectsAndStatistic(uploadTaskLogStepVO, analysisVersion);
            //发送websocket信息
            sendWebSocketMsg(toolConfigBaseVO, uploadTaskLogStepVO, taskLogEntity, taskId, toolName);

        }
        //否则，将上传步骤信息追加到该任务
        else
        {
            logger.info("begin to mongotemplate task log");
            ToolConfigBaseVO toolConfigBaseVO = updateTaskLog(lastTaskLogEntity, uploadTaskLogStepVO, taskVO);

            // 分析成功时告警数据及告警统计数据后处理
            postHandleDefectsAndStatistic(uploadTaskLogStepVO);
            TaskLogEntity.TaskUnit taskStep = new TaskLogEntity.TaskUnit();
            taskStep.setStepNum(uploadTaskLogStepVO.getStepNum());
            taskStep.setStartTime(uploadTaskLogStepVO.getStartTime());
            taskStep.setEndTime(uploadTaskLogStepVO.getEndTime());
            taskStep.setMsg(uploadTaskLogStepVO.getMsg());
            taskStep.setFlag(uploadTaskLogStepVO.getFlag());
            taskStep.setElapseTime(uploadTaskLogStepVO.getElapseTime());
            pipelineService.handleDevopsCallBack(lastTaskLogEntity, taskStep, toolName);
            sendWebSocketMsg(toolConfigBaseVO, uploadTaskLogStepVO, lastTaskLogEntity, taskId, toolName);
        }

        return new Result(CommonMessageCode.SUCCESS, "upload taskLog ok");
    }


    /**
     * 发送websocket信息
     *
     * @param toolConfigBaseVO
     * @param uploadTaskLogStepVO
     * @param taskId
     * @param toolName
     */
    private void sendWebSocketMsg(ToolConfigBaseVO toolConfigBaseVO, UploadTaskLogStepVO uploadTaskLogStepVO,
                                  TaskLogEntity taskLogEntity, long taskId, String toolName)
    {
        //1. 推送消息至任务详情首页面
        TaskOverviewVO.LastAnalysis lastAnalysis = new TaskOverviewVO.LastAnalysis();
        lastAnalysis.setToolName(toolName);
        lastAnalysis.setCurStep(toolConfigBaseVO.getCurStep());
        lastAnalysis.setStepStatus(toolConfigBaseVO.getStepStatus());
        long startTime = uploadTaskLogStepVO.getStartTime();
        long endTime = uploadTaskLogStepVO.getEndTime();
        long elapseTime = uploadTaskLogStepVO.getElapseTime();
        if (elapseTime == 0 && endTime != 0)
        {
            elapseTime = endTime - startTime;
        }
        lastAnalysis.setElapseTime(elapseTime);
        lastAnalysis.setLastAnalysisTime(endTime != 0 ? endTime : startTime);
        //获取告警数量信息
        if (ComConstants.Step4MutliTool.COMPLETE.value() == toolConfigBaseVO.getCurStep())
        {
            ToolLastAnalysisResultVO toolLastAnalysisResultVO = new ToolLastAnalysisResultVO();
            toolLastAnalysisResultVO.setTaskId(taskId);
            toolLastAnalysisResultVO.setToolName(toolName);
            BaseLastAnalysisResultVO lastAnalysisResultVO = taskLogService.getLastAnalysisResult(toolLastAnalysisResultVO, toolName);
            lastAnalysis.setLastAnalysisResult(lastAnalysisResultVO);
        }

        try
        {
            simpMessagingTemplate.convertAndSend(String.format("/topic/analysisInfo/taskId/%d", taskId),
                    objectMapper.writeValueAsString(lastAnalysis));
        }
        catch (JsonProcessingException e)
        {
            logger.error("serialize last analysis info failed! task id: {}, tool name: {}", taskId, toolName);
        }


        //2.推送消息至单个任务的详情界面
        try
        {
            simpMessagingTemplate.convertAndSend(String.format("/topic/analysisDetail/taskId/%d/toolName/%s", taskId, toolName), objectMapper.writeValueAsString(taskLogEntity));
        }
        catch (JsonProcessingException e)
        {
            logger.error("serialize last analysis detail failed! task id: {}, tool name: {}", taskId, toolName);
        }
    }

    /**
     * 分析开始前告警数据及告警统计数据预处理
     *
     * @param uploadTaskLogStepVO
     * @param analysisVersion
     */
    protected abstract void preHandleDefectsAndStatistic(UploadTaskLogStepVO uploadTaskLogStepVO, String analysisVersion);

    /**
     * 分析成功时告警数据及告警统计数据后处理
     *
     * @param uploadTaskLogStepVO
     */
    protected abstract void postHandleDefectsAndStatistic(UploadTaskLogStepVO uploadTaskLogStepVO);

    /**
     * 更分析记录
     *
     * @param taskLogEntity
     * @param uploadTaskLogStepVO
     * @param taskVO
     */
    private ToolConfigBaseVO updateTaskLog(TaskLogEntity taskLogEntity, UploadTaskLogStepVO uploadTaskLogStepVO, TaskBaseVO taskVO)
    {
        appendStepInfo(taskLogEntity, uploadTaskLogStepVO, taskVO);
        taskLogRepository.save(taskLogEntity);

        return updateToolStatus(taskLogEntity);
    }

    /**
     * 判断是否是虚拟流水线（即蓝盾CodecCC服务创建的流水线）的排队步骤
     * 如果是虚拟流水线CodeCC服务手动触发的任务，且步骤为第一步（排队），则不需要创建新任务
     * 因为CodeCC服务手动触发的任务，为了实现触发后页面能立即看到任务步骤，会先调用分析记录上报接口发送排队开始步骤，那时已经创建了新任务
     * 后面流水线调用工具侧分析脚本，工具侧又会报一次上传开始步骤，这里对这个步骤不作处理。
     *
     * @return
     */
    private boolean isQueueStepOfVirtualpipeline(String createFrom, String triggerFrom, int stepNum)
    {
        return ComConstants.BsTaskCreateFrom.BS_CODECC.value().equals(createFrom) && StringUtils.isNotEmpty(triggerFrom)
                && stepNum == ComConstants.Step4MutliTool.QUEUE.value();
    }

    /**
     * 将本次上报的分析记录步骤追加到最近的一次分析记录里面
     *
     * @param taskLogEntity
     * @param uploadTaskLogStepVO
     * @param taskVO
     */
    private void appendStepInfo(TaskLogEntity taskLogEntity, UploadTaskLogStepVO uploadTaskLogStepVO, TaskBaseVO taskVO)
    {
        TaskLogEntity.TaskUnit taskStep = new TaskLogEntity.TaskUnit();
        BeanUtils.copyProperties(uploadTaskLogStepVO, taskStep);

        taskLogEntity.setCurrStep(taskStep.getStepNum());
        if (taskStep.getFlag() == DefectConstants.TASK_FLAG_PROCESSING)
        {
            /*若为蓝盾CodeCC服务手动触发的多工具任务，且步骤为第一步-排队（CodeCC服务手动触发的任务，为了实现触发后页面能立即看到任务步骤，会先调用
              MutiToolAnalyzeTaskAction类发送排队开始步骤，后面流水线调用工具侧分析脚本，工具侧又会报一次上传开始步骤，这里对这个步骤不作处理。*/
            if (ComConstants.BsTaskCreateFrom.BS_CODECC.value().equals(taskVO.getCreateFrom())
                    && taskStep.getStepNum() == ComConstants.Step4MutliTool.QUEUE.value())
            {
                return;
            }

            //该步骤是进行中的状态，直接添加到任务数组里
            taskLogEntity.getStepArray().add(taskStep);
            taskLogEntity.setFlag(DefectConstants.TASK_FLAG_PROCESSING);
        }
        //该步骤不是进行中状态，则为结束，从任务里拿出最后一个步骤进行修改
        else
        {
            TaskLogEntity.TaskUnit lastTaskStep = taskLogEntity.getStepArray().get(taskLogEntity.getStepArray().size() - 1);
            if (lastTaskStep.getStepNum() == taskStep.getStepNum())
            {
                lastTaskStep.setFlag(taskStep.getFlag());
                lastTaskStep.setEndTime(taskStep.getEndTime());
                lastTaskStep.setMsg(taskStep.getMsg());
                lastTaskStep.setElapseTime(taskStep.getEndTime() - lastTaskStep.getStartTime());
            }
            else
            {
                taskLogEntity.getStepArray().add(taskStep);
                lastTaskStep.setFlag(DefectConstants.TASK_FLAG_SUCC);
            }
        }
        //满足以下条件可以将任务设置为结束并计算耗时
        //（1）步骤是失败的
        if (taskStep.getFlag() == DefectConstants.TASK_FLAG_FAIL)
        {
            // 失败发送RTX提醒
//            String title = getMsgTitle(taskStep.getStepNum());
//            sendEmail(toolName, taskLogEntity, taskStep, title);
//            sendRtx(toolName, taskLogEntity, taskStep, title);
            taskFinishSetting(taskLogEntity, taskStep);
        }
        //（2）步骤是最后一步并且设置了结束时间
        else if (taskStep.getFlag() == DefectConstants.TASK_FLAG_SUCC
                && taskStep.getStepNum() == ComConstants.Step4MutliTool.DEFECT_SUBMI.value() && taskStep.getEndTime() != 0)
        {
            taskFinishSetting(taskLogEntity, taskStep);
//            sendRtxAfterAnalyseSucc(toolName, taskLogEntity, taskStep);
        }
    }

    /**
     * 将任务设置为结束并计算耗时
     *
     * @param taskLogEntity
     * @param taskStep
     */
    public void taskFinishSetting(TaskLogEntity taskLogEntity, TaskLogEntity.TaskUnit taskStep)
    {
        taskLogEntity.setEndTime(taskStep.getEndTime());
        taskLogEntity.setFlag(taskStep.getFlag());
        if (taskLogEntity.getStartTime() != ComConstants.COMMON_NUM_0L)
        {
            taskLogEntity.setElapseTime(taskLogEntity.getEndTime() - taskLogEntity.getStartTime());
        }
        else
        {
            for (TaskLogEntity.TaskUnit taskUnit : taskLogEntity.getStepArray())
            {
                if (taskUnit.getStartTime() != ComConstants.COMMON_NUM_0L)
                {
                    taskLogEntity.setElapseTime(taskLogEntity.getEndTime() - taskUnit.getStartTime());
                    taskLogEntity.setStartTime(taskUnit.getStartTime());
                    break;
                }
            }
        }
    }

    private boolean needCreateTaskWhenSameStep(TaskLogEntity lastTask, UploadTaskLogStepVO taskStep, TaskBaseVO taskBaseVO)
    {
        if (lastTask.getCurrStep() != taskStep.getStepNum())
        {
            return false;
        }

        TaskLogEntity.TaskUnit lastTaskStep = lastTask.getStepArray().get(lastTask.getStepArray().size() - 1);

        //若为蓝盾CodeCC服务手动触发的任务，且步骤为第一步-排队，则不需要创建新任务
        if (isQueueStepOfVirtualpipeline(taskBaseVO.getCreateFrom(), lastTask.getTriggerFrom(), lastTaskStep.getStepNum()))
        {
            return false;
        }

        //同时拥有开始时间
        if (lastTaskStep.getStartTime() > 0 && taskStep.getStartTime() > 0)
        {
            return true;
        }

        //同时拥有结束时间
        if (lastTaskStep.getEndTime() > 0 && taskStep.getEndTime() > 0)
        {
            return true;
        }

        //上一个有结束时间，这个有开始时间
        if (lastTaskStep.getEndTime() > 0 && taskStep.getStartTime() > 0)
        {
            return true;
        }

        return false;
    }

    /**
     * 创建新的分析记录
     *
     * @param uploadTaskLogStepVO
     * @param taskBaseVO
     * @return
     */
    public TaskLogEntity createNewTaskLog(UploadTaskLogStepVO uploadTaskLogStepVO, TaskBaseVO taskBaseVO, ToolConfigBaseVO toolConfigBaseVO)
    {
        if (uploadTaskLogStepVO.getStartTime() == 0)
        {
            uploadTaskLogStepVO.setStartTime(System.currentTimeMillis());
        }
        TaskLogEntity taskLogEntity = new TaskLogEntity();
        taskLogEntity.setTaskId(taskBaseVO.getTaskId());
        taskLogEntity.setStreamName(taskBaseVO.getNameEn());
        taskLogEntity.setToolName(uploadTaskLogStepVO.getToolName());
        taskLogEntity.setStartTime(uploadTaskLogStepVO.getStartTime() == 0 ? System.currentTimeMillis() : uploadTaskLogStepVO.getStartTime());
        taskLogEntity.setCurrStep(uploadTaskLogStepVO.getStepNum());
        taskLogEntity.setFlag(uploadTaskLogStepVO.getFlag());
        if (taskLogEntity.getFlag() == DefectConstants.TASK_FLAG_FAIL)
        {
            taskLogEntity.setEndTime(taskLogEntity.getStartTime());
        }

        //保存蓝盾项目流水线id，构建id，蓝盾codecc服务创建的多工具任务手动触发的触发人
        taskLogEntity.setPipelineId(taskBaseVO.getPipelineId());
        taskLogEntity.setBuildId(uploadTaskLogStepVO.getPipelineBuildId());
        taskLogEntity.setTriggerFrom(uploadTaskLogStepVO.getTriggerFrom());

        TaskLogEntity.TaskUnit taskStep = new TaskLogEntity.TaskUnit();
        BeanUtils.copyProperties(uploadTaskLogStepVO, taskStep);

        List<TaskLogEntity.TaskUnit> stepArray = new ArrayList<>();
        stepArray.add(taskStep);
        taskLogEntity.setStepArray(stepArray);

        // 1.如果是开始构建步骤，需要把触发方式及触发人信息填入步骤信息里面
        setTriggerInfo(taskStep, taskBaseVO);

        // 2.如果是流水线任务，则查询流水线的构建信息并且保存到codecc本地
        String buildNum = saveBuildInfo(taskLogEntity);
        taskLogEntity.setBuildNum(buildNum);

        // 3.保存分析记录
        logger.info("save task log");
        taskLogRepository.save(taskLogEntity);

        // 4.调用task模块的接口更新工具的步骤状态
        ToolConfigBaseVO finalToolConfigBaseVO = updateToolStatus(taskLogEntity);
        BeanUtils.copyProperties(finalToolConfigBaseVO, toolConfigBaseVO);
        return taskLogEntity;
    }

    /**
     * @param taskLogEntity
     */
    private String saveBuildInfo(TaskLogEntity taskLogEntity)
    {
        String buildNum = null;
        String pipeline = taskLogEntity.getPipelineId();
        String buildId = taskLogEntity.getBuildId();
        if (StringUtils.isNotEmpty(pipeline) && StringUtils.isNotEmpty(buildId))
        {
            BuildEntity buildEntity = buildRepository.findByBuildId(buildId);
            if (buildEntity == null)
            {
                CodeccBuildInfo buildInfo = pipelineService.getBuildIdInfo(buildId);
                if (buildInfo != null)
                {
                    buildEntity = new BuildEntity();
                    BeanUtils.copyProperties(buildInfo, buildEntity);
                    buildEntity.setBuildId(buildId);
                    buildRepository.save(buildEntity);
                    buildNum = buildInfo.getBuildNo();
                }
            }
            else
            {
                buildNum = buildEntity.getBuildNo();
            }
            logger.info("save build info finish, buildNum is {}", buildNum);
        }

        return buildNum;
    }

    /**
     * 处理蓝盾coverity任务详情订制化需求
     * 1、对于蓝盾流水线创建的codecc任务，在构建第一步信息中要标识为：由流水线_XXX触发。
     * 2、对于codecc服务创建的codecc任务，在构建第一步信息中需要标识是定时触发还是手动触发
     *
     * @param taskStep
     * @param taskBaseVO
     */
    private void setTriggerInfo(TaskLogEntity.TaskUnit taskStep, TaskBaseVO taskBaseVO)
    {
        //不是蓝盾项目的任务 或者任务步骤不是第一步不处理
        if (taskStep.getStepNum() != ComConstants.Step4MutliTool.QUEUE.value())
        {
            return;
        }

        String triggerInfo = "";

        //蓝盾流水线创建的codecc任务
        if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equals(taskBaseVO.getCreateFrom()))
        {
            triggerInfo = "流水线（" + taskBaseVO.getNameCn() + "）触发";
        }
        //codecc服务创建的codecc任务
        else if (ComConstants.BsTaskCreateFrom.BS_CODECC.value().equals(taskBaseVO.getCreateFrom()))
        {
            String msg = taskStep.getMsg();
            if (StringUtils.isEmpty(msg))
            {
                triggerInfo = "定时触发";
            }
            else
            {
                triggerInfo = msg;
            }
        }

        taskStep.setMsg(triggerInfo);
    }

    /**
     * 更新中断的分析任务数据
     *
     * @param taskLogEntity
     * @param toolName
     */
    public ToolConfigBaseVO updateAbortAnalysisTaskLog(TaskLogEntity taskLogEntity, String toolName)
    {
        int lastIndex = taskLogEntity.getStepArray().size() - 1;
        taskLogEntity.setFlag(DefectConstants.TASK_FLAG_FAIL);
        TaskLogEntity.TaskUnit lastTaskStep;
        if (lastIndex >= 0)
        {
            lastTaskStep = taskLogEntity.getStepArray().get(lastIndex);
            lastTaskStep.setFlag(DefectConstants.TASK_FLAG_ABORT);
            lastTaskStep.setMsg("由于开始新任务，当前任务被中断");
        }
        taskLogRepository.save(taskLogEntity);

        // 调用task模块的接口更新工具的步骤状态
        return updateToolStatus(taskLogEntity);
    }

    /**
     * 调用task模块的接口更新工具的状态
     *
     * @param taskLogEntity
     */
    private ToolConfigBaseVO updateToolStatus(TaskLogEntity taskLogEntity)
    {
        int curStep = taskLogEntity.getCurrStep();
        int submitStepNum = getSubmitStepNum();
        if (taskLogEntity.getCurrStep() == submitStepNum && taskLogEntity.getEndTime() != 0)
        {
            curStep = submitStepNum + 1;
        }

        ToolConfigBaseVO toolConfigBaseVO = new ToolConfigBaseVO();
        toolConfigBaseVO.setTaskId(taskLogEntity.getTaskId());
        toolConfigBaseVO.setToolName(taskLogEntity.getToolName());

        toolConfigBaseVO.setCurStep(curStep);
        int stepStatus = taskLogEntity.getFlag() == DefectConstants.TASK_FLAG_FAIL ? ComConstants.StepStatus.FAIL.value() : ComConstants.StepStatus.SUCC.value();
        toolConfigBaseVO.setStepStatus(stepStatus);
        client.get(ServiceToolRestResource.class).updateToolStepStatus(toolConfigBaseVO);
        return toolConfigBaseVO;
    }

    /**
     * 获取提单步骤的值，子类必须实现这个方法
     * 普通工具有4个分析步骤：1：代码下载，2、代码下载；3：代码扫描，4：代码缺陷提交
     * Klocwork/Coverity有5个分析步骤：1：上传，2：排队状态，3、分析中；4：缺陷提交，5：提单
     *
     * @return
     */
    public abstract int getSubmitStepNum();
}
