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

package com.tencent.bk.codecc.codeccjob.service.impl;

import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.model.LintStatisticEntity;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.LintFileQueryRepository;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.codeccjob.service.AbstractFilterPathBizService;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * lint类工具的路径屏蔽
 *
 * @version V1.0
 * @date 2019/11/1
 */
@Service("LINTFilterPathBizService")
@Slf4j
public class LintFilterPathBizServiceImpl extends AbstractFilterPathBizService
{
    @Autowired
    private LintFileQueryRepository lintFileQueryRepository;

    @Autowired
    private LintStatisticRepository lintStatisticRepository;

    @Override
    public CodeCCResult processBiz(FilterPathInputVO filterPathInputVO)
    {
        // Lint类屏蔽路径
        List<LintFileEntity> lintFiles = lintFileQueryRepository.findByTaskIdAndToolName(filterPathInputVO.getTaskId(), filterPathInputVO.getToolName());
        if (!CollectionUtils.isEmpty(lintFiles))
        {
            long currTime = System.currentTimeMillis();
            List<LintFileEntity> needUpdateDefectList = lintFiles.stream()
                    .filter(defectEntity ->
                    {
                        String path = defectEntity.getUrl();
                        if (StringUtils.isEmpty(path))
                        {
                            path = defectEntity.getFilePath();
                        }
                        if (StringUtils.isEmpty(path))
                        {
                            return false;
                        }
                        return PathUtils.checkIfMaskByPath(defectEntity.getRelPath(), filterPathInputVO.getFilterPaths());
                    })
                    .collect(Collectors.toList());
            needUpdateDefectList.forEach(defectEntity ->
            {
                defectEntity.setStatus(getUpdateStatus(filterPathInputVO, defectEntity.getStatus()));
                defectEntity.setExcludeTime(currTime);
                List<LintDefectEntity> defectEntityList = defectEntity.getDefectList();
                if(CollectionUtils.isNotEmpty(defectEntityList))
                {
                    //告警清单也要刷新状态
                    defectEntityList.forEach(defect ->
                        defect.setStatus(getUpdateStatus(filterPathInputVO, defect.getStatus()))
                    );
                }
            });
            lintFileQueryRepository.save(needUpdateDefectList);

            setLintStatisticInfo(filterPathInputVO, lintFiles);
        }
        return new CodeCCResult(CommonMessageCode.SUCCESS);
    }

    /**
     * 更新统计表
     *
     * @param pathInputVO
     * @param lintFiles
     */
    private void setLintStatisticInfo(FilterPathInputVO pathInputVO, List<LintFileEntity> lintFiles)
    {
        int fileCount = 0;
        int newDefectCount = 0;
        int historyDefectCount = 0;
        String toolName = pathInputVO.getToolName();
        // 取出defectList中的status为new告警的文件
        Map<Integer, List<LintDefectEntity>> lintDefectMap = lintFiles.stream()
                .filter(lint -> ComConstants.FileType.NEW.value() == lint.getStatus())
                .filter(lint -> (lint.getToolName().equals(pathInputVO.getToolName()) && CollectionUtils.isNotEmpty(lint.getDefectList())))
                .map(LintFileEntity::getDefectList)
                .flatMap(Collection::stream)
                .filter(lint -> ComConstants.DefectStatus.NEW.value() == lint.getStatus())
                .collect(Collectors.groupingBy(LintDefectEntity::getDefectType));

        // 更新表：LintStatisticEntity
        if (MapUtils.isNotEmpty(lintDefectMap))
        {
            List<LintDefectEntity> newList = lintDefectMap.get(ComConstants.DefectType.NEW.value());
            List<LintDefectEntity> historyList = lintDefectMap.get(ComConstants.DefectType.HISTORY.value());
            newDefectCount = CollectionUtils.isNotEmpty(newList) ? newList.size() : 0;
            historyDefectCount = CollectionUtils.isNotEmpty(historyList) ? historyList.size() : 0;
            fileCount = (int) lintFiles.stream()
                    .filter(lint -> ComConstants.FileType.NEW.value() == lint.getStatus())
                    .filter(lint -> (lint.getToolName().equals(toolName) && CollectionUtils.isNotEmpty(lint.getDefectList())))
                    .count();
        }

        saveLintStatisticInfo(pathInputVO, toolName, fileCount, newDefectCount, historyDefectCount);
    }

    /**
     * 保存忽略规则之后的的统计情况
     *
     * @param pathInputVO        任务ID
     * @param toolName           工具名称
     * @param newDefectCount     新告警个数
     * @param historyDefectCount 历史告警个数
     */
    private void saveLintStatisticInfo(FilterPathInputVO pathInputVO, String toolName, int fileCount, int newDefectCount, int historyDefectCount)
    {
        LintStatisticEntity lastLintStatisticEntity = lintStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(pathInputVO.getTaskId(), toolName);
        if (Objects.isNull(lastLintStatisticEntity))
        {
            lastLintStatisticEntity = new LintStatisticEntity();
        }

        int defectCount = newDefectCount + historyDefectCount;
        int defectChange = defectCount - lastLintStatisticEntity.getDefectCount();
        int fileChange = fileCount - lastLintStatisticEntity.getFileCount();
        lastLintStatisticEntity.setFileCount(fileCount);
        lastLintStatisticEntity.setFileChange(fileChange);
        lastLintStatisticEntity.setDefectCount(defectCount);
        lastLintStatisticEntity.setDefectChange(defectChange);
        lastLintStatisticEntity.setNewDefectCount(newDefectCount);
        lastLintStatisticEntity.setHistoryDefectCount(historyDefectCount);
        lastLintStatisticEntity.setTime(System.currentTimeMillis());
        lintStatisticRepository.save(lastLintStatisticEntity);
    }
}
