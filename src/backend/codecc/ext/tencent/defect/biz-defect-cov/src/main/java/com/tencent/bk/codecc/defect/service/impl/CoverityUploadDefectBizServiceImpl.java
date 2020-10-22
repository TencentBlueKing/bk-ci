/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.dao.redis.StatisticDao;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity;
import com.tencent.bk.codecc.defect.service.AbstractPlatformUploadDefectBizService;
import com.tencent.bk.codecc.defect.utils.AuthorTransferUtils;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.PathUtils;
import org.apache.commons.lang3.tuple.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tencent.devops.common.constant.ComConstants.StaticticItem;

/**
 * 任务分析记录服务层实现
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Service("COVERITYUploadDefectBizService")
@Slf4j
public class CoverityUploadDefectBizServiceImpl extends AbstractPlatformUploadDefectBizService
{
    @Autowired
    private DefectRepository defectRepository;

    @Autowired
    private StatisticDao statisticDao;

    @Override
    public void saveAndStatisticDefect(
            UploadDefectVO uploadDefectVO,
            List<DefectEntity> defectList,
            TaskDetailVO taskDetailVO,
            Set<String> filterPathSet,
            List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList,
            String buildNum)
    {
        long taskId = uploadDefectVO.getTaskId();
        String toolName = uploadDefectVO.getToolName();
        // 统计新增、关闭、遗留、屏蔽
        int newCount = 0;
        int closeCount = 0;
        int existCount = 0;
        int excludeCount = 0;
        int existPromptCount = 0;
        int existNormalCount = 0;
        int existSeriousCount = 0;
        Set<String> newAuthors = Sets.newHashSet();
        Map<String, Integer> checkerCountMap = new HashMap<>();
        for (DefectEntity defectEntity : defectList)
        {
            defectEntity.setTaskId(taskId);
            defectEntity.setToolName(toolName);
            defectEntity.setStreamName(uploadDefectVO.getStreamName());

            // 根据处理人转换关系转换告警处理人，如果告警处理人为空，则默认取任务接口人作为告警处理人
            Set<String> newAuthorList = AuthorTransferUtils.authorTrans(defectEntity.getAuthorList(),
                Collections.singletonList(taskDetailVO.getCreatedBy()),
                transferAuthorList);
            defectEntity.setAuthorList(newAuthorList);
            defectEntity.setCreateBuildNumber(buildNum);
            if (PathUtils.checkIfMaskByPath(defectEntity.getFilePathname(), filterPathSet))
            {
                int status = defectEntity.getStatus() | ComConstants.DefectStatus.PATH_MASK.value();
                defectEntity.setStatus(status);
                defectEntity.setExcludeTime(System.currentTimeMillis());
                closeCount++;
                excludeCount++;
            }
            else
            {
                newAuthors.addAll(newAuthorList);
                if ((defectEntity.getSeverity() & ComConstants.PROMPT) > 0)
                {
                    existPromptCount++;
                }
                if ((defectEntity.getSeverity() & ComConstants.NORMAL) > 0)
                {
                    existNormalCount++;
                }
                if ((defectEntity.getSeverity() & ComConstants.SERIOUS) > 0)
                {
                    existSeriousCount++;
                }
            }

            // 获取规则数据
            Integer count = checkerCountMap.get(defectEntity.getCheckerName());
            if (count == null) {
                count = 0;
            }
            checkerCountMap.put(defectEntity.getCheckerName(), ++count);
        }
        newCount = defectList.size();
        existCount = defectList.size() - closeCount;

        List<Pair<StaticticItem, Integer>> defectCountList = Arrays.asList(
            Pair.of(StaticticItem.NEW, newCount),
            Pair.of(StaticticItem.EXIST, existCount),
            Pair.of(StaticticItem.CLOSE, closeCount),
            Pair.of(StaticticItem.EXCLUDE, excludeCount),
            Pair.of(StaticticItem.EXIST_PROMPT, existPromptCount),
            Pair.of(StaticticItem.EXIST_NORMAL, existNormalCount),
            Pair.of(StaticticItem.EXIST_SERIOUS, existSeriousCount),
            Pair.of(StaticticItem.NEW_PROMPT, existPromptCount),
            Pair.of(StaticticItem.NEW_NORMAL, existNormalCount),
            Pair.of(StaticticItem.NEW_SERIOUS, existSeriousCount));
        statisticDao.increaseDefectCountByStatusBatch(taskId, toolName, buildNum, defectCountList);
        statisticDao.addNewAndExistAuthors(taskId, toolName, buildNum, newAuthors, newAuthors);

        // 写入规则统计数据
        statisticDao.increaseDefectCheckerCountBatch(taskId, toolName, buildNum, checkerCountMap);

        defectRepository.save(defectList);
    }
}
