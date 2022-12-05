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

package com.tencent.bk.codecc.schedule.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.codecc.defect.api.ServiceReportTaskLogRestResource;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.schedule.component.ConcurrentAnalyzeConfigCache;
import com.tencent.bk.codecc.schedule.constant.ScheduleConstants;
import com.tencent.bk.codecc.schedule.dao.redis.AnalyzeHostPoolDao;
import com.tencent.bk.codecc.schedule.model.AnalyzeHostPoolModel;
import com.tencent.bk.codecc.schedule.service.ScheduleService;
import com.tencent.bk.codecc.schedule.vo.FreeVO;
import com.tencent.bk.codecc.schedule.vo.PushVO;
import com.tencent.bk.codecc.schedule.vo.TailLogRspVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.web.RpcClient;
import com.tencent.devops.common.web.mq.ConstantsKt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 调度服务实现
 *
 * @version V1.0
 * @date 2019/11/4
 */
@Service
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private Client client;
    @Autowired
    private AnalyzeHostPoolDao analyzeHostPoolDao;
    @Autowired
    private ConcurrentAnalyzeConfigCache concurrentAnalyzeConfigCache;
    @Value("${result.log.path:#{null}}")
    private String resultLogPath;

    /**
     * VIP项目的优先级
     */
    private final int VIP_PRIORITY = 10;

    /**
     * 普通项目的默认优先级
     */
    private final int DEFAULT_PRIORITY = 3;

    /**
     * VIP项目超过最大并发数后降级后的优先级
     */
    private final int VIP_DOWNGRAGE_PRIORITY = 2;

    /**
     * 普通项目超过最大并发数后降级后的优先级
     */
    private final int DOWNGRAGE_PRIORITY = 1;

    @Override
    public Boolean push(String streamName, String toolName, String buildId, String createFrom, String projectId) {
        PushVO pushVO = new PushVO();
        pushVO.setStreamName(streamName);
        pushVO.setToolName(toolName);
        pushVO.setBuildId(buildId);
        pushVO.setCreateFrom(createFrom);
        pushVO.setProjectId(projectId);

        // createFrom为空表示非工蜂项目
        if (StringUtils.isEmpty(createFrom)) {

            // 获取项目当前并发数
            Integer nowConcurrentCount = analyzeHostPoolDao.getProjectNowConcurrentCount(projectId);

            // 获取项目当前队列等待任务数
            Integer queueCount = analyzeHostPoolDao.getProjectQueueCount(projectId);

            // 当前任务总数包括正在分析的任务数以及队列中的任务数
            int total = nowConcurrentCount + queueCount;

            // 获取项目允许的最大并发数
            Integer projectMaxConcurrent = concurrentAnalyzeConfigCache.getProjectMaxConcurrent(projectId);

            // 获取项目优先级
            int finalPriority = getPriority(projectId, total, projectMaxConcurrent);
            log.info("project: {}, max concurrent: {}, now concurrent：{}, queue count: {}, priority: {}",
                    projectId, projectMaxConcurrent, nowConcurrentCount, queueCount, finalPriority);
            rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_ANALYZE_DISPATCH, ConstantsKt.ROUTE_ANALYZE_DISPATCH, pushVO,
                    message -> {
                        message.getMessageProperties().setPriority(finalPriority);
                        return message;
                    });

            analyzeHostPoolDao.getSetProjectQueueCount(projectId, 1);
        } else {
            rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_ANALYZE_DISPATCH_OPENSOURCE, ConstantsKt.ROUTE_ANALYZE_DISPATCH_OPENSOURCE, pushVO);
        }
        return true;
    }

    /**
     * 获取项目优先级，优先级规则如下：
     * 1.VIP项目没超限之前，取优先级最高10；
     * 2.VIP项目超限之后，优先级将为2；
     * 3.普通项目没超限之前，取默认优先级3；
     * 4.普通项目超限之后，优先级将为1；
     * @param projectId
     * @param concurrentCount
     * @param projectMaxConcurrent
     * @return
     */
    protected int getPriority(String projectId, Integer concurrentCount, Integer projectMaxConcurrent) {
        int priority;
        Set<String> vipProject = concurrentAnalyzeConfigCache.getVipProject();
        if (vipProject.contains(projectId)){
            if (concurrentCount < projectMaxConcurrent) {
                priority = VIP_PRIORITY;
            }else {
                priority = VIP_DOWNGRAGE_PRIORITY;
            }
        }else {
            if (concurrentCount < projectMaxConcurrent) {
                priority = DEFAULT_PRIORITY;
            }else {
                priority = DOWNGRAGE_PRIORITY;
            }
        }
        return priority;
    }

    @Override
    public Boolean dipatch(PushVO pushVO, AnalyzeHostPoolModel mostIdleHost) {
        String serverURL = String.format("http://%s:%s/", mostIdleHost.getIp(), mostIdleHost.getPort());
        String buildId = mostIdleHost.getJobList().get(mostIdleHost.getJobList().size() - 1).getBuildId();
        Object[] params = new Object[]{pushVO.getStreamName(), pushVO.getToolName(), buildId, pushVO.getProjectId()};
        RpcClient<Boolean> rpcClient = new RpcClient();
        Boolean response = rpcClient.doRequest(serverURL, ScheduleConstants.RpcMethod.TRIGGER.methodName(), params);
        return response;
    }

    @Override
    public void abort(PushVO pushVO) {
        List<AnalyzeHostPoolModel> hostList = analyzeHostPoolDao.getAllAnalyzeHosts();
        hostList.forEach(host ->
        {
            List<AnalyzeHostPoolModel.AnalyzeJob> jobList = host.getJobList();
            if (CollectionUtils.isNotEmpty(jobList)) {
                jobList.forEach(analyzeJob ->
                {
                    if (pushVO.getStreamName().equals(analyzeJob.getStreamName()) && pushVO.getToolName().equals(analyzeJob.getToolName())) {
                        String serverURL = String.format("http://%s:%s/", host.getIp(), host.getPort());
                        Object[] params = new Object[]{pushVO.getStreamName(), pushVO.getToolName(), analyzeJob.getBuildId()};
                        RpcClient<Boolean> rpcClient = new RpcClient();
                        Boolean response = rpcClient.doRequest(serverURL, ScheduleConstants.RpcMethod.ABORT.methodName(), params);

                        if (response == null || !response) {
                            log.error("abort analyze job fail, serverURL:{}, params:{}", serverURL, params);
                        }
                        analyzeHostPoolDao.freeHostThread(pushVO.getToolName(), pushVO.getStreamName(), host.getIp(), analyzeJob.getBuildId());

                        // 中断后需要同步更新分析记录
                        uploadAbortTaskLog(pushVO.getStreamName(), pushVO.getToolName(), analyzeJob.getBuildId(), String.format("当前任务被新构建%s中断", pushVO.getBuildId()));
                        return;
                    }
                });
            }
        });
    }

    @Override
    public Boolean free(FreeVO freeVO) {
        return analyzeHostPoolDao.freeHostThread(freeVO.getToolName(), freeVO.getStreamName(), freeVO.getHostIp(), freeVO.getBuildId());
    }

    @Override
    public void checkAnalyzeHostThreadAlive() {
        List<AnalyzeHostPoolModel> hostList = analyzeHostPoolDao.getAllAnalyzeHosts();
        Map<String, List<AnalyzeHostPoolModel.AnalyzeJob>> needFreeHostMap = new HashMap<>();
        for (AnalyzeHostPoolModel analyzeHost : hostList) {
            List<AnalyzeHostPoolModel.AnalyzeJob> jobList = analyzeHost.getJobList();
            if (CollectionUtils.isNotEmpty(jobList)) {
                Object[] params = new Object[]{JsonUtil.INSTANCE.toJson(jobList)};
                String serverURL = String.format("http://%s:%s/", analyzeHost.getIp(), analyzeHost.getPort());
                RpcClient<?> rpcClient = new RpcClient();
                log.info("{} request: {}", ScheduleConstants.RpcMethod.CHECK.methodName(), params);
                Object responseObj = rpcClient.doRequest(serverURL, ScheduleConstants.RpcMethod.CHECK.methodName(), params);
                log.info("{} response: {}", ScheduleConstants.RpcMethod.CHECK.methodName(), JsonUtil.INSTANCE.toJson(responseObj));
                List<Boolean> response = JsonUtil.INSTANCE.to(JsonUtil.INSTANCE.toJson(responseObj),
                        new TypeReference<List<Boolean>>() {});
                if (response == null || response.size() != jobList.size()) {
                    log.error("The result returned by the calling interface is incorrect:\n{}\n{}\n{}",
                            serverURL, ScheduleConstants.RpcMethod.CHECK.methodName(), params);
                    continue;
                }

                List<AnalyzeHostPoolModel.AnalyzeJob> needFreeJobs = new ArrayList<>();
                for (int i = 0; i < response.size(); i++) {
                    Boolean threadExist = response.get(i);
                    if (threadExist != null && !threadExist) {
                        needFreeJobs.add(jobList.get(i));
                    }
                }
                if (CollectionUtils.isNotEmpty(needFreeJobs)) {
                    needFreeHostMap.put(analyzeHost.getIp(), needFreeJobs);
                }
            }
        }

        if (needFreeHostMap.size() > 0) {
            log.info("batch free host thread: {}", ScheduleConstants.RpcMethod.CHECK.methodName(), JsonUtil.INSTANCE.toJson(needFreeHostMap));
            analyzeHostPoolDao.batchFreeHostThread(needFreeHostMap);

            // 中断后需要同步更新分析记录
            needFreeHostMap.forEach((ip, jobList) ->
            {
                jobList.forEach(analyzeJob ->
                {
                    uploadAbortTaskLog(analyzeJob.getStreamName(), analyzeJob.getToolName(), analyzeJob.getBuildId(), String.format("当前任务由于分析服务器[%s]异常而中断", ip));
                });
            });
        }
    }

    @Override
    public TailLogRspVO tailLog(String streamName, String toolName, String buildId, long beginLine) {
        long beginTime = System.currentTimeMillis();
        log.info("begin tail log: {}, {}, {}, {}", streamName, toolName, buildId, beginLine);
        TailLogRspVO tailLogRspVO = new TailLogRspVO();
        if (StringUtils.isEmpty(resultLogPath)) {
            log.error("result log path is empty");
            tailLogRspVO.setContent("result log path is empty");
            tailLogRspVO.setMaxLineNum(beginLine);
            return tailLogRspVO;
        }
        String logFilePath = String.format("%s/%s_%s/%s.log", resultLogPath, streamName, toolName, buildId);
        if (!new File(logFilePath).exists()) {
            log.error("{}不存在", logFilePath);
            tailLogRspVO.setContent("result log has not upload");
            tailLogRspVO.setMaxLineNum(beginLine);
            return tailLogRspVO;
        }
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(logFilePath))) {
            List<String> lines = reader.lines()
                    .skip(beginLine).limit(ComConstants.COMMON_NUM_10000L)
                    .collect(Collectors.toList());

            StringBuffer buffer = new StringBuffer();
            lines.forEach(line -> buffer.append(line).append("\n"));
            tailLogRspVO.setContent(buffer.toString());
            tailLogRspVO.setMaxLineNum(beginLine + lines.size());
        } catch (IOException e) {
            log.error("file [{}] doesn't exist or is not a file", logFilePath, e);
            tailLogRspVO.setContent("log file is not exist.");
            tailLogRspVO.setMaxLineNum(beginLine);
            return tailLogRspVO;
        }
        log.info("end tail log cost: {}, {}", System.currentTimeMillis() - beginTime, buildId);
        return tailLogRspVO;
    }


    public void uploadAbortTaskLog(String streamName, String toolName, String buildId, String msg) {
        try {
            UploadTaskLogStepVO uploadTaskLogStepVO = new UploadTaskLogStepVO();
            uploadTaskLogStepVO.setStreamName(streamName);
            uploadTaskLogStepVO.setToolName(toolName);
            uploadTaskLogStepVO.setStartTime(0L);
            uploadTaskLogStepVO.setEndTime(System.currentTimeMillis());
            uploadTaskLogStepVO.setMsg(msg);
            uploadTaskLogStepVO.setPipelineBuildId(buildId);
            uploadTaskLogStepVO.setPipelineFail(true);
            Result result = client.get(ServiceReportTaskLogRestResource.class).uploadTaskLog(uploadTaskLogStepVO);

            if (result.isNotOk()) {
                log.error("upload TaskLog fail! streamName: {}, toolName: {}, buildId: {}, msg: {}, message: {}",
                        streamName, toolName, buildId, msg, result.getMessage());
            }
        } catch (Throwable t) {
            log.error("upload TaskLog fail! streamName: {}, toolName: {}, buildId: {}, msg: {}, message: {}",
                    streamName, toolName, buildId, msg, t.getMessage());
        }

    }

}
