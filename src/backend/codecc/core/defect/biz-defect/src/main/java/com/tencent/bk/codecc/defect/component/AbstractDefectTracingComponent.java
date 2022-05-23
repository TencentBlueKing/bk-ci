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
import com.tencent.bk.codecc.defect.pojo.AggregateDefectNewInputModel;
import com.tencent.bk.codecc.defect.pojo.DefectClusterDTO;
import com.tencent.bk.codecc.defect.pojo.FileMD5SingleModel;
import com.tencent.bk.codecc.defect.pojo.FileMD5TotalModel;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import java.util.HashSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

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

    /**
     * 分批执行聚类跟踪
     *
     * @param defectClusterDTO
     * @param chunkNo
     * @param inputList
     * @return
     */
    public AsyncRabbitTemplate.RabbitConverterFuture<Boolean> executeCluster(
            DefectClusterDTO defectClusterDTO,
            TaskDetailVO taskDetailVO,
            int chunkNo,
            List<T> inputList,
            Set<String> relPathList,
            Set<String> filePathList,
            Set<String> filterPathSet)
    {
        String inputFileName = String.format("%s_%s_%s_%s_aggregate_input_data.json", taskDetailVO.getNameEn(), defectClusterDTO.getCommitDefectVO().getToolName()
                , defectClusterDTO.getCommitDefectVO().getBuildId(), chunkNo);
        String inputFilePath = scmJsonComponent.index(inputFileName, ScmJsonComponent.AGGREGATE);
        log.info("aggregate inputFilePath : {}", inputFilePath);
        File inputFile = new File(inputFilePath);
        defectClusterDTO.setInputFileName(inputFileName);
        defectClusterDTO.setInputFilePath(inputFilePath);

        try
        {
            //写入输入数据
            if (!inputFile.exists()) {
                inputFile.getParentFile().mkdirs();
                inputFile.createNewFile();
            }
            Set<String> pathSet = new HashSet<>();
            if (CollectionUtils.isNotEmpty(taskDetailVO.getWhitePaths())) {
                pathSet.addAll(taskDetailVO.getWhitePaths());
            }
            AggregateDefectNewInputModel<T> aggregateDefectNewInputModel =
                    new AggregateDefectNewInputModel<>(filePathList,
                            relPathList,
                            filterPathSet,
                            pathSet,
                            inputList);
            Files.write(JsonUtil.INSTANCE.toJson(aggregateDefectNewInputModel).getBytes(), inputFile);
            scmJsonComponent.upload(inputFilePath, inputFileName, ScmJsonComponent.AGGREGATE);
            AsyncRabbitTemplate.RabbitConverterFuture<Boolean> asyncMsgFuture;
            if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(taskDetailVO.getCreateFrom()))
            {
                AsyncRabbitTemplate asyncRabbitTamplte = SpringContextUtil.Companion.getBean(AsyncRabbitTemplate.class, "opensourceAsyncRabbitTamplte");
                asyncMsgFuture = asyncRabbitTamplte.convertSendAndReceive(EXCHANGE_CLUSTER_ALLOCATION_OPENSOURCE, ROUTE_CLUSTER_ALLOCATION_OPENSOURCE, defectClusterDTO);
            }
            else
            {
                AsyncRabbitTemplate asyncRabbitTamplte = SpringContextUtil.Companion.getBean(AsyncRabbitTemplate.class, "clusterAsyncRabbitTamplte");
                asyncMsgFuture = asyncRabbitTamplte.convertSendAndReceive(EXCHANGE_CLUSTER_ALLOCATION, ROUTE_CLUSTER_ALLOCATION, defectClusterDTO);
            }

            return asyncMsgFuture;
        }
        catch (Exception e)
        {
            log.warn("cluster fail! {}", inputFilePath, e);
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
        while (!file.exists() && i < 200)
        {
            Thread.sleep(3000L);
            i++;
        }
    }
}
