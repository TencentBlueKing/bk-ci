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

package com.tencent.bk.codecc.defect.component;

import com.google.common.io.Files;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity;
import com.tencent.bk.codecc.defect.pojo.AggregateDefectInputModel;
import com.tencent.bk.codecc.defect.pojo.AggregateDispatchFileName;
import com.tencent.bk.codecc.defect.pojo.FileMD5SingleModel;
import com.tencent.bk.codecc.defect.pojo.FileMD5TotalModel;
import com.tencent.bk.codecc.defect.service.IMessageQueueBizService;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


/**
 * 新版告警跟踪抽象类
 *
 * @version V1.0
 * @date 2020/4/26
 */
@Slf4j
public abstract class AbstractDefectTracingComponent<T>
{
    @Autowired
    public ScmJsonComponent scmJsonComponent;

    @Autowired
    protected BizServiceFactory<IMessageQueueBizService> messageBizServiceFactory;

    /**
     * 抽象告警跟踪方法
     * @return
     */
    abstract Future<Boolean> defectTracing(
            CommitDefectVO commitDefectVO,
            TaskDetailVO taskVO, Set<String> filterPath,
            BuildEntity buildEntity,
            int chunkNo,
            List<T> originalFileList,
            List<T> currentFileList,
            List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList);

    /**
     * 分批执行聚类跟踪
     *
     * @param taskVO
     * @param toolName
     * @param buildId
     * @param chunkNo
     * @param inputList
     * @return
     */
    public Pair<String, AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> executeCluster(
            TaskDetailVO taskVO,
            String toolName,
            String buildId,
            int chunkNo,
            List<AggregateDefectInputModel> inputList)
    {
        String inputFileName = String.format("%s_%s_%s_%s_aggregate_input_data.json", taskVO.getNameEn(), toolName, buildId, chunkNo);
        String inputFilePath = scmJsonComponent.index(inputFileName, ScmJsonComponent.AGGREGATE);
        log.info("aggregate inputFilePath : {}", inputFilePath);
        File inputFile = new File(inputFilePath);

        String outputFileName = String.format("%s_%s_%s_%s_aggregate_output_data.json", taskVO.getNameEn(), toolName, buildId, chunkNo);
        String outputFilePath = scmJsonComponent.index(outputFileName, ScmJsonComponent.AGGREGATE);
        log.info("aggregate outputFilePath : {}", outputFilePath);
        File outputFile = new File(outputFilePath);
        try
        {
            //写入输入数据
            if (!inputFile.exists())
            {
                inputFile.createNewFile();
            }
            if (outputFile.exists())
            {
                outputFile.delete();
            }

            Files.write(JsonUtil.INSTANCE.toJson(inputList).getBytes(), inputFile);

            AggregateDispatchFileName aggregateFileName = new AggregateDispatchFileName(inputFilePath, outputFilePath);

            AsyncRabbitTemplate.RabbitConverterFuture<Boolean> asyncMsgFuture;

            // 区分创建来源为工蜂项目，创建对应处理器
            IMessageQueueBizService messageQueueBizService = messageBizServiceFactory.createBizService(
                    taskVO.getCreateFrom(),ComConstants.BusinessType.MESSAGE_QUEUE.value(),IMessageQueueBizService.class);
            asyncMsgFuture = messageQueueBizService.messageAsyncMsgFuture(aggregateFileName);

            Pair<String, AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> asyncResult = Pair.of(outputFilePath, asyncMsgFuture);
            return asyncResult;
        }
        catch (Exception e)
        {
            log.warn("cluster fail! {}", outputFilePath, e);
        }
        return null;
    }

    @NotNull
    public Map<String, String> getFIleMd5Map(String streamName, String toolName, String buildId)
    {
        FileMD5TotalModel fileMD5TotalModel = scmJsonComponent.loadFileMD5(streamName, toolName, buildId);
        if (fileMD5TotalModel == null || CollectionUtils.isEmpty(fileMD5TotalModel.getFileList()))
        {
            log.warn("md5 file is empty: {}_{}_{}_md5.json", streamName, toolName, buildId);
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }
        return fileMD5TotalModel.getFileList().stream().collect(Collectors.toMap(FileMD5SingleModel::getFilePath, FileMD5SingleModel::getMd5));
    }


    /**
     * 检查聚类output文件是否存在
     * @param outputFile
     * @throws InterruptedException
     */
    protected void checkOutputFileExists(String outputFile) throws InterruptedException
    {
        File file = new File(outputFile);
        int i = 0;
        while (!file.exists() && i < 10)
        {
            Thread.sleep(3000L);
            i++;
        }
    }
}
