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
import com.tencent.bk.codecc.defect.dao.mongorepository.CLOCDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CLOCDefectDao;
import com.tencent.bk.codecc.defect.model.CLOCDefectEntity;
import com.tencent.bk.codecc.defect.model.DefectJsonFileEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.CLOCUploadStatisticService;
import com.tencent.bk.codecc.defect.vo.CLOCLanguageVO;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadCLOCStatisticVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DUPC告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component("clocDefectCommitConsumer")
@Slf4j
public class CLOCDefectCommitConsumer extends AbstractDefectCommitConsumer
{
    @Autowired
    private CLOCDefectDao clocDefectDao;
    @Autowired
    private CLOCDefectRepository clocDefectRepository;
    @Autowired
    private CLOCUploadStatisticService clocUploadStatisticService;

    @Override
    protected void uploadDefects(CommitDefectVO commitDefectVO, Map<String, ScmBlameVO> fileChangeRecordsMap,
            Map<String, RepoSubModuleVO> codeRepoIdMap) {
        long taskId = commitDefectVO.getTaskId();
        String streamName = commitDefectVO.getStreamName();
        String toolName = commitDefectVO.getToolName();
        String buildId = commitDefectVO.getBuildId();

        // 判断本次是增量还是全量扫描
        ToolBuildStackEntity toolBuildStackEntity =
                toolBuildStackRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        boolean isFullScan = toolBuildStackEntity == null || toolBuildStackEntity.isFullScan();

        // 读取原生（未经压缩）告警文件
        String defectListJson = scmJsonComponent.loadRawDefects(streamName, toolName, buildId);
        DefectJsonFileEntity<CLOCDefectEntity> defectJsonFileEntity =
                JsonUtil.INSTANCE.to(defectListJson, new TypeReference<DefectJsonFileEntity<CLOCDefectEntity>>() {
                });
        List<CLOCDefectEntity> clocDefectEntityList = defectJsonFileEntity.getDefects();

        // 增量告警，获取删除文件列表，设置失效位
        if (!isFullScan && CollectionUtils.isNotEmpty(toolBuildStackEntity.getDeleteFiles())) {
            log.info("start to disable deleted CLOC defect entity, taskId: {} | buildId: {} | stream Name: {}",
                    taskId, buildId, streamName);
            clocDefectDao.batchDisableClocInfoByFileName(taskId, toolBuildStackEntity.getDeleteFiles());
        } else if (isFullScan) {
            // 全量告警将当前任务所有告警设置为失效
            log.info("start to disable all CLOC defect entity, taskId: {} | buildId: {} | stream Name: {}",
                    taskId, buildId, streamName);
            clocDefectDao.batchDisableClocInfo(taskId);
        }
        if (CollectionUtils.isNotEmpty(clocDefectEntityList)) {
            Long currentTime = System.currentTimeMillis();
            clocDefectEntityList.forEach(clocDefectEntity -> {
                clocDefectEntity.setTaskId(taskId);
                clocDefectEntity.setStreamName(streamName);
                clocDefectEntity.setCreatedDate(currentTime);
                clocDefectEntity.setUpdatedDate(currentTime);
            });
        }
        //告警详情再upsert
        clocDefectDao.batchUpsertClocInfo(clocDefectEntityList);
        log.info("start to insert CLOC defect statistic, taskId: {} | buildId: {} | streamName: {}",
                taskId, buildId, streamName);

        // 获取全量告警记录，用于统计信息
        clocDefectEntityList = clocDefectRepository.findByTaskIdAndStatusIsNot(taskId, "DISABLED");
        //上报统计信息
        UploadCLOCStatisticVO uploadCLOCStatisticVO = new UploadCLOCStatisticVO();
        uploadCLOCStatisticVO.setTaskId(taskId);
        uploadCLOCStatisticVO.setStreamName(streamName);
        Map<String, List<CLOCDefectEntity>> clocLanguageMap = clocDefectEntityList.stream()
                .collect(Collectors.groupingBy(CLOCDefectEntity::getLanguage));
        List<CLOCLanguageVO> languageVOList = clocLanguageMap.entrySet()
                .stream()
                .map(stringListEntry -> {
                    CLOCLanguageVO clocLanguageVO = new CLOCLanguageVO();
                    clocLanguageVO.setLanguage(stringListEntry.getKey());
                    List<CLOCDefectEntity> clocInfoVOS = stringListEntry.getValue();
                    clocLanguageVO.setCodeSum(clocInfoVOS.stream()
                            .map(CLOCDefectEntity::getCode)
                            .reduce((o1, o2) -> o1 + o2)
                            .orElse(0L));
                    clocLanguageVO.setBlankSum(clocInfoVOS.stream()
                            .map(CLOCDefectEntity::getBlank)
                            .reduce((o1, o2) -> o1 + o2)
                            .orElse(0L));
                    clocLanguageVO.setCommentSum(clocInfoVOS.stream()
                            .map(CLOCDefectEntity::getComment)
                            .reduce((o1, o2) -> o1 + o2)
                            .orElse(0L));
                    return clocLanguageVO;
                }).collect(Collectors.toList());
        uploadCLOCStatisticVO.setLanguageCodeList(languageVOList);
        uploadCLOCStatisticVO.setLanguages(clocDefectEntityList
                .stream()
                .map(CLOCDefectEntity::getLanguage)
                .distinct()
                .collect(Collectors.toList()));
        clocUploadStatisticService.uploadNewStatistic(uploadCLOCStatisticVO, clocLanguageMap, buildId, streamName);

        // 更新告警快照基准构建ID
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);
    }
}
