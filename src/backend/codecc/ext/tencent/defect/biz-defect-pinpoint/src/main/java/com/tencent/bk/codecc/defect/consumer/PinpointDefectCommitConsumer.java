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

package com.tencent.bk.codecc.defect.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.service.impl.PinpointUploadDefectBizServiceImpl;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pinpoint告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class PinpointDefectCommitConsumer extends AbstractDefectCommitConsumer
{
    @Autowired
    @Qualifier("PINPOINTUploadDefectBizService")
    private PinpointUploadDefectBizServiceImpl pinpointUploadDefectService;

    @Override
    protected void uploadDefects(CommitDefectVO commitDefectVO, Map<String, ScmBlameVO> fileChangeRecordsMap, Map<String, RepoSubModuleVO> codeRepoIdMap)
    {
        long taskId = commitDefectVO.getTaskId();
        String streamName = commitDefectVO.getStreamName();
        String toolName = commitDefectVO.getToolName();
        String buildId = commitDefectVO.getBuildId();

        // 读取原生（未经压缩）告警文件
        String defectListJson = scmJsonComponent.loadRawDefects(streamName, toolName, buildId);
        PinpointDefectJsonFileEntity<DefectEntity> defectJsonFileEntity = JsonUtil.INSTANCE.to(defectListJson, new TypeReference<PinpointDefectJsonFileEntity<DefectEntity>>()
        {
        });
        List<DefectEntity> defectList = defectJsonFileEntity.getDefects();
        if (CollectionUtils.isEmpty(defectList))
        {
            log.error("defect list is empty.");
        }

        BuildEntity buildEntity = buildDao.getAndSaveBuildInfo(buildId);
        String buildNum = buildEntity.getBuildNo();

        TaskDetailVO taskDetailVO = thirdPartySystemCaller.getTaskInfoWithoutToolsByTaskId(taskId);
        Set<String> filterPathSet = getFilterPaths(taskDetailVO);
        TransferAuthorEntity transferAuthorEntity = transferAuthorRepository.findByTaskId(taskId);
        List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList = null;
        if (transferAuthorEntity != null)
        {
            transferAuthorList = transferAuthorEntity.getTransferAuthorList();
        }

        UploadDefectVO uploadDefectVO = new UploadDefectVO();
        uploadDefectVO.setTaskId(taskId);
        uploadDefectVO.setStreamName(streamName);
        uploadDefectVO.setToolName(toolName);
        uploadDefectVO.setBuildId(buildId);
        uploadDefectVO.setPlatformProjectId(defectJsonFileEntity.getProjectId());
        uploadDefectVO.setReportId(defectJsonFileEntity.getReportId());
        log.info("begin to save defect");
        pinpointUploadDefectService.saveAndStatisticDefect(uploadDefectVO, defectList, taskDetailVO, filterPathSet, transferAuthorList, buildNum);
    }
}
