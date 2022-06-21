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
import com.tencent.bk.codecc.defect.service.statistic.CLOCDefectStatisticService;
import com.tencent.bk.codecc.defect.vo.CLOCLanguageVO;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadCLOCStatisticVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.util.PathUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * DUPC告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component("clocDefectCommitConsumer")
@Slf4j
public class CLOCDefectCommitConsumer extends AbstractDefectCommitConsumer {
    @Autowired
    private CLOCDefectDao clocDefectDao;
    @Autowired
    private CLOCDefectRepository clocDefectRepository;
    @Autowired
    private CLOCUploadStatisticService clocUploadStatisticService;
    @Autowired
    private CLOCDefectStatisticService clocDefectStatisticService;

    @Override
    protected void uploadDefects(CommitDefectVO commitDefectVO, Map<String, ScmBlameVO> fileChangeRecordsMap,
            Map<String, RepoSubModuleVO> codeRepoIdMap) {
        long taskId = commitDefectVO.getTaskId();
        String streamName = commitDefectVO.getStreamName();
        String toolName = commitDefectVO.getToolName();
        String buildId = commitDefectVO.getBuildId();

        // 判断本次是增量还是全量扫描
        ToolBuildStackEntity toolBuildStackEntity =
                toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        boolean isFullScan = toolBuildStackEntity == null || toolBuildStackEntity.isFullScan();

        // 读取原生（未经压缩）告警文件
        String defectListJson = scmJsonComponent.loadRawDefects(streamName, toolName, buildId);
        DefectJsonFileEntity<CLOCDefectEntity> defectJsonFileEntity =
                JsonUtil.INSTANCE.to(defectListJson, new TypeReference<DefectJsonFileEntity<CLOCDefectEntity>>() {
                });
        List<CLOCDefectEntity> clocDefectEntityList = defectJsonFileEntity.getDefects();
        fillDefectInfo(clocDefectEntityList, fileChangeRecordsMap);

        // 增量告警，获取删除文件列表，设置失效位
        if (!isFullScan && CollectionUtils.isNotEmpty(toolBuildStackEntity.getDeleteFiles())) {
            log.info("start to disable deleted {} defect entity, taskId: {} | buildId: {} | stream Name: {}",
                    toolName, taskId, buildId, streamName);
            clocDefectDao.batchDisableClocInfoByFileName(taskId, toolName, toolBuildStackEntity.getDeleteFiles());
        } else if (isFullScan) {
            // 全量告警将当前任务所有告警设置为失效
            log.info("start to disable all {} defect entity, taskId: {} | buildId: {} | stream Name: {}",
                    toolName, taskId, buildId, streamName);
            clocDefectDao.batchDisableClocInfo(taskId, toolName);
        }
        if (CollectionUtils.isNotEmpty(clocDefectEntityList)) {
            Long currentTime = System.currentTimeMillis();
            clocDefectEntityList.forEach(clocDefectEntity -> {
                clocDefectEntity.setTaskId(taskId);
                clocDefectEntity.setToolName(toolName);
                clocDefectEntity.setStreamName(streamName);
                clocDefectEntity.setCreatedDate(currentTime);
                clocDefectEntity.setUpdatedDate(currentTime);
            });
        }
        //告警详情再upsert
        clocDefectDao.batchUpsertClocInfo(clocDefectEntityList);
        log.info("start to insert {} defect statistic, taskId: {} | buildId: {} | streamName: {}",
                toolName, taskId, buildId, streamName);

        // 获取全量告警记录，用于统计信息
        if (Tool.SCC.name().equals(toolName)) {
            clocDefectEntityList = clocDefectRepository.findByTaskIdAndToolNameInAndStatusIsNot(
                    taskId,
                    Collections.singletonList(toolName),
                    "DISABLED");
        } else {
            clocDefectEntityList = clocDefectRepository.findByTaskIdAndToolNameInAndStatusIsNot(
                    taskId,
                    Arrays.asList(toolName, null),
                    "DISABLED");
        }
        //上报统计信息
        UploadCLOCStatisticVO uploadCLOCStatisticVO = new UploadCLOCStatisticVO();
        uploadCLOCStatisticVO.setTaskId(taskId);
        uploadCLOCStatisticVO.setToolName(toolName);
        uploadCLOCStatisticVO.setStreamName(streamName);

        Map<String, List<CLOCDefectEntity>> clocLanguageMap = clocDefectEntityList.stream()
                .collect(Collectors.groupingBy(CLOCDefectEntity::getLanguage));
        // 获取路径黑/白名单
        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(streamName);
        Set<String> filterPaths = filterPathService.getFilterPaths(taskVO, toolName);
        Set<String> pathSet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(taskVO.getWhitePaths())) {
            pathSet.addAll(taskVO.getWhitePaths());
        }
        List<String> pathMaskDefectList = new ArrayList<>();
        List<CLOCLanguageVO> languageVOList = clocLanguageMap.entrySet()
                .stream()
                .map(stringListEntry -> {
                    CLOCLanguageVO clocLanguageVO = new CLOCLanguageVO();
                    clocLanguageVO.setLanguage(stringListEntry.getKey());
                    // 判断过滤路径，被屏蔽的记录需要更新状态
                    List<CLOCDefectEntity> clocInfoVOS
                            = checkMaskByPath(stringListEntry.getValue(),
                            filterPaths,
                            pathSet,
                            pathMaskDefectList);
                    clocLanguageVO.setCodeSum(clocInfoVOS.stream()
                            .map(CLOCDefectEntity::getCode)
                            .reduce(Long::sum)
                            .orElse(0L));
                    clocLanguageVO.setBlankSum(clocInfoVOS.stream()
                            .map(CLOCDefectEntity::getBlank)
                            .reduce(Long::sum)
                            .orElse(0L));
                    clocLanguageVO.setCommentSum(clocInfoVOS.stream()
                            .map(CLOCDefectEntity::getComment)
                            .reduce(Long::sum)
                            .orElse(0L));
                    if (toolName.equals(Tool.SCC.name())) {
                        clocLanguageVO.setEfficientCommentSum(clocInfoVOS.stream()
                                .map(CLOCDefectEntity::getEfficientComment)
                                .reduce(Long::sum)
                                .orElse(0L));
                    }
                    return clocLanguageVO;
                }).collect(Collectors.toList());

        // 路径屏蔽的告警需要置为失效
        if (CollectionUtils.isNotEmpty(pathMaskDefectList)) {
            clocDefectDao.batchDisableClocInfoByFileName(taskId, toolName, pathMaskDefectList);
        }

        uploadCLOCStatisticVO.setToolName(toolName);
        uploadCLOCStatisticVO.setLanguageCodeList(languageVOList);
        uploadCLOCStatisticVO.setLanguages(clocDefectEntityList
                .stream()
                .map(CLOCDefectEntity::getLanguage)
                .distinct()
                .collect(Collectors.toList()));
        clocDefectStatisticService.statistic(uploadCLOCStatisticVO, clocLanguageMap, buildId, streamName);

        // 更新告警快照基准构建ID
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);
    }

    /**
     * 路基黑/白名单检查
     * 只有存在于 黑名单和白名单 交集中的文件才被记录
     *
     * @param clocDefectEntities 被检测告警
     * @param filterPath 路径黑名单
     * @param pathMaskDefectList 被路径屏蔽的文件，记录下来之后需要更新失效位
     * @param pathSet 路径白名单
     */
    private List<CLOCDefectEntity> checkMaskByPath(List<CLOCDefectEntity> clocDefectEntities,
                                                   Set<String> filterPath,
                                                   Set<String> pathSet,
                                                   List<String> pathMaskDefectList) {
        return clocDefectEntities.stream().filter(it -> {
            // 命中黑名单 或 没有命中白名单
            if (PathUtils.checkIfMaskByPath(it.getFileName(), filterPath)
                    || (CollectionUtils.isNotEmpty(pathSet)
                    && !PathUtils.checkIfMaskByPath(it.getFileName(), pathSet))) {
                pathMaskDefectList.add(it.getFileName());
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    private void fillDefectInfo(List<CLOCDefectEntity> defectList, Map<String, ScmBlameVO> fileChangeRecordsMap) {
        defectList.forEach(defectEntity -> {
            ScmBlameVO scmBlameVO = fileChangeRecordsMap.get(defectEntity.getFileName());
            if (scmBlameVO != null) {
                defectEntity.setRelPath(scmBlameVO.getFileRelPath());
            }
        });
    }
}
