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
import com.tencent.bk.codecc.schedule.constant.ScheduleConstants;
import com.tencent.bk.codecc.schedule.dao.redis.AnalyzeHostPoolDao;
import com.tencent.bk.codecc.schedule.model.AnalyzeHostPoolModel;
import com.tencent.bk.codecc.schedule.service.ScheduleService;
import com.tencent.bk.codecc.schedule.vo.FreeVO;
import com.tencent.bk.codecc.schedule.vo.PushVO;
import com.tencent.bk.codecc.schedule.vo.TailLogRspVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 调度服务实现
 * 
 * @date 2019/11/4
 * @version V1.0
 */
@Service
@Slf4j
public class ScheduleServiceImpl implements ScheduleService
{
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private Client client;
    @Autowired
    private AnalyzeHostPoolDao analyzeHostPoolDao;

    @Value("${result.log.path}")
    private String resultLogPath;

    @Override
    public Boolean push(String streamName, String toolName, String buildId, String createFrom)
    {
        PushVO pushVO = new PushVO();
        pushVO.setStreamName(streamName);
        pushVO.setToolName(toolName);
        pushVO.setBuildId(buildId);
        pushVO.setCreateFrom(createFrom);

        // 将分析任务推入消息队列
        if (StringUtils.isEmpty(createFrom))
        {
            rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_ANALYZE_DISPATCH, ConstantsKt.ROUTE_ANALYZE_DISPATCH, pushVO);
        }
        else
        {
            rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_ANALYZE_DISPATCH_OPENSOURCE, ConstantsKt.ROUTE_ANALYZE_DISPATCH_OPENSOURCE, pushVO);
        }
        return true;
    }

    @Override
    public Boolean dipatch(PushVO pushVO, AnalyzeHostPoolModel mostIdleHost)
    {
        String serverURL = String.format("http://%s:%s/", mostIdleHost.getIp(), mostIdleHost.getPort());
        String buildId = mostIdleHost.getJobList().get(mostIdleHost.getJobList().size() - 1).getBuildId();
        Object[] params = new Object[]{pushVO.getStreamName(), pushVO.getToolName(), buildId};
        RpcClient<Boolean> rpcClient = new RpcClient();
        Boolean response = rpcClient.doRequest(serverURL, ScheduleConstants.RpcMethod.TRIGGER.methodName(), params);
        return response;
    }

    @Override
    public void abort(PushVO pushVO)
    {
        List<AnalyzeHostPoolModel> hostList = analyzeHostPoolDao.getAllAnalyzeHosts();
        hostList.forEach(host ->
        {
            List<AnalyzeHostPoolModel.AnalyzeJob> jobList = host.getJobList();
            if (CollectionUtils.isNotEmpty(jobList))
            {
                jobList.forEach(analyzeJob ->
                {
                    if (pushVO.getStreamName().equals(analyzeJob.getStreamName()) && pushVO.getToolName().equals(analyzeJob.getToolName()))
                    {
                        String serverURL = String.format("http://%s:%s/", host.getIp(), host.getPort());
                        Object[] params = new Object[]{pushVO.getStreamName(), pushVO.getToolName(), analyzeJob.getBuildId()};
                        RpcClient<Boolean> rpcClient = new RpcClient();
                        Boolean response = rpcClient.doRequest(serverURL, ScheduleConstants.RpcMethod.ABORT.methodName(), params);

                        if (response == null || !response)
                        {
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
    public Boolean free(FreeVO freeVO)
    {
        return analyzeHostPoolDao.freeHostThread(freeVO.getToolName(), freeVO.getStreamName(), freeVO.getHostIp(), freeVO.getBuildId());
    }

    @Override
    public void checkAnalyzeHostThreadAlive()
    {
        List<AnalyzeHostPoolModel> hostList = analyzeHostPoolDao.getAllAnalyzeHosts();
        Map<String, List<AnalyzeHostPoolModel.AnalyzeJob>> needFreeHostMap = new HashMap<>();
        for(AnalyzeHostPoolModel analyzeHost: hostList)
        {
            List<AnalyzeHostPoolModel.AnalyzeJob> jobList = analyzeHost.getJobList();
            if (CollectionUtils.isNotEmpty(jobList))
            {
                Object[] params = new Object[]{JsonUtil.INSTANCE.toJson(jobList)};
                String serverURL = String.format("http://%s:%s/", analyzeHost.getIp(), analyzeHost.getPort());
                RpcClient<?> rpcClient = new RpcClient();
                log.info("{} request: {}", ScheduleConstants.RpcMethod.CHECK.methodName(), params);
                Object responseObj = rpcClient.doRequest(serverURL, ScheduleConstants.RpcMethod.CHECK.methodName(), params);
                log.info("{} response: {}", ScheduleConstants.RpcMethod.CHECK.methodName(), JsonUtil.INSTANCE.toJson(responseObj));
                List<Boolean> response = JsonUtil.INSTANCE.to(JsonUtil.INSTANCE.toJson(responseObj), new TypeReference<List<Boolean>>()
                {
                });
                if (response == null || response.size() != jobList.size())
                {
                    log.error("The result returned by the calling interface is incorrect:\n{}\n{}\n{}",
                            serverURL, ScheduleConstants.RpcMethod.CHECK.methodName(), params);
                    continue;
                }

                List<AnalyzeHostPoolModel.AnalyzeJob> needFreeJobs = new ArrayList<>();
                for(int i = 0; i < response.size(); i++)
                {
                    Boolean threadExist = response.get(i);
                    if (threadExist != null && !threadExist)
                    {
                        needFreeJobs.add(jobList.get(i));
                    }
                }
                if (CollectionUtils.isNotEmpty(needFreeJobs))
                {
                    needFreeHostMap.put(analyzeHost.getIp(), needFreeJobs);
                }
            }
        }

        if (needFreeHostMap.size() > 0)
        {
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
    public TailLogRspVO tailLog(String streamName, String toolName, String buildId, long beginLine)
    {
        long beginTime = System.currentTimeMillis();
        log.info("begin tail log: {}, {}, {}, {}", streamName, toolName, buildId, beginLine);
        TailLogRspVO tailLogRspVO = new TailLogRspVO();
        if (StringUtils.isEmpty(resultLogPath))
        {
            log.error("result log path is empty");
            tailLogRspVO.setContent("result log path is empty");
            tailLogRspVO.setMaxLineNum(beginLine);
            return tailLogRspVO;
        }
        String logFilePath = String.format("%s/%s_%s/%s.log", resultLogPath, streamName, toolName, buildId);
        if (!new File(logFilePath).exists())
        {
            log.error("{}不存在", logFilePath);
            tailLogRspVO.setContent("result log has not upload");
            tailLogRspVO.setMaxLineNum(beginLine);
            return tailLogRspVO;
        }
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(logFilePath)))
        {
            List<String> lines = reader.lines()
                    .skip(beginLine).limit(ComConstants.COMMON_NUM_10000L)
                    .collect(Collectors.toList());

            StringBuffer buffer = new StringBuffer();
            lines.forEach(line -> buffer.append(line).append("\n"));
            tailLogRspVO.setContent(buffer.toString());
            tailLogRspVO.setMaxLineNum(beginLine + lines.size());
        }
        catch (IOException e)
        {
            log.error("file [{}] doesn't exist or is not a file", logFilePath, e);
            tailLogRspVO.setContent("log file is not exist.");
            tailLogRspVO.setMaxLineNum(beginLine);
            return tailLogRspVO;
        }

//        try (RandomAccessFile randomFile = new RandomAccessFile(logFilePath, "r"))
//        {
//            randomFile.seek(beginLine);
//            StringBuffer buffer = new StringBuffer();
//            int count = 0;
//            String line;
//            while ((line = randomFile.readLine()) != null && count < ComConstants.COMMON_NUM_10000L)
//            {
//                buffer.append(line).append("\n");
//                count++;
//            }
//
//            tailLogRspVO.setContent(buffer.toString());
//            tailLogRspVO.setMaxLineNum(randomFile.getFilePointer());
//        }
//        catch (FileNotFoundException e)
//        {
//            log.error("file [{}] doesn't exist or is not a file", logFilePath, e);
//            tailLogRspVO.setContent("log file is not exist.");
//            tailLogRspVO.setMaxLineNum(beginLine);
//            return tailLogRspVO;
//        }
//        catch (IOException e)
//        {
//            log.error("read log file fail: {}", logFilePath, e);
//            tailLogRspVO.setContent("read log file fail: " + logFilePath);
//            tailLogRspVO.setMaxLineNum(beginLine);
//            return tailLogRspVO;
//        }
        log.info("end tail log cost: {}, {}", System.currentTimeMillis() - beginTime, buildId);
        return tailLogRspVO;
    }


    public void uploadAbortTaskLog(String streamName, String toolName, String buildId, String msg)
    {
        try
        {
            UploadTaskLogStepVO uploadTaskLogStepVO = new UploadTaskLogStepVO();
            uploadTaskLogStepVO.setStreamName(streamName);
            uploadTaskLogStepVO.setToolName(toolName);
            uploadTaskLogStepVO.setStartTime(0L);
            uploadTaskLogStepVO.setEndTime(System.currentTimeMillis());
            uploadTaskLogStepVO.setMsg(msg);
            uploadTaskLogStepVO.setPipelineBuildId(buildId);
            uploadTaskLogStepVO.setPipelineFail(true);
            CodeCCResult result = client.get(ServiceReportTaskLogRestResource.class).uploadTaskLog(uploadTaskLogStepVO);

            if (result.isNotOk())
            {
                log.error("upload TaskLog fail! streamName: {}, toolName: {}, buildId: {}, msg: {}, message: {}",
                        streamName, toolName, buildId, msg, result.getMessage());
            }
        }
        catch (Throwable t)
        {
            log.error("upload TaskLog fail! streamName: {}, toolName: {}, buildId: {}, msg: {}, message: {}",
                    streamName, toolName, buildId, msg, t.getMessage());
        }

    }

}
