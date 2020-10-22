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
import com.tencent.bk.codecc.task.vo.OpenCheckerVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.StaticticItem;

/**
 * 任务分析记录服务层实现
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Service("KLOCWORKUploadDefectBizService")
@Slf4j
public class KlocworkUploadDefectBizServiceImpl extends AbstractPlatformUploadDefectBizService
{
    @Autowired
    protected DefectRepository defectRepository;
    @Autowired
    protected StatisticDao statisticDao;

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
        String streamName = uploadDefectVO.getStreamName();

        // 因为Klocwork告警的文件路径是被klocwork工具自动转为全小写路径，所以匹配的时候需要把屏蔽的路径也转为小写再匹配
        filterPathSet = PathUtils.convertPathsToLowerCase(filterPathSet);

        //查询打开的规则映射
        Map<String, OpenCheckerVO> openCheckerMap = getOpenCheckerMap(taskId, toolName, taskDetailVO);
        // 查询打开的非默认规则
//        Map<String, CheckerDetailVO> openCheckers = checkerService.queryOpenCheckers(taskId, toolName, null, taskDetailVO.getCodeLang());

        Set<String> idSet = defectList.stream().map(DefectEntity::getId).collect(Collectors.toSet());
        List<DefectEntity> existDefectList = defectRepository.findByTaskIdAndToolNameAndIdIn(taskId, toolName, idSet);
        Map<String, DefectEntity> defectEntityMap = existDefectList.stream().collect(Collectors.toMap(DefectEntity::getId, defect -> defect, (k1, k2) -> k1));

        // 统计新增、关闭、遗留、屏蔽
        int newCount = 0;
        int closeCount = 0;
        int existCount = 0;
        int excludeCount = 0;
        int fixedCount = 0;
        int existPromptCount = 0;
        int existNormalCount = 0;
        int existSeriousCount = 0;
        Set<String> newAuthors = Sets.newHashSet();
        Map<String, Integer> checkerCountMap = new HashMap<>();
        long currTime = System.currentTimeMillis();
        Iterator<DefectEntity> it = defectList.iterator();
        while (it.hasNext())
        {
            DefectEntity defectEntity = it.next();

            DefectEntity oldDefect = defectEntityMap.get(defectEntity.getId());

            // 1.已存在的告警
            if (oldDefect != null)
            {
                it.remove();

                int status = oldDefect.getStatus();
                int codeccStatus = status;
                Integer platformStatus = defectEntity.getStatus();

                // 1.1 在codecc上new（即不被忽略、不被路径或规则屏蔽），在platform上已修复的告警，要标志为已修复
                if (codeccStatus == ComConstants.DefectStatus.NEW.value() && (platformStatus & ComConstants.DefectStatus.FIXED.value()) > 0)
                {
                    status = codeccStatus | ComConstants.DefectStatus.FIXED.value();
                    oldDefect.setFixedTime(currTime);
                    oldDefect.setFixedBuildNumber(buildNum);
                    closeCount++;
                    fixedCount++;
                }
                // 1.2 在codecc上已修复，在platform上未修复的告警，要标志为未修复
                else if ((codeccStatus & ComConstants.DefectStatus.FIXED.value()) > 0 && (platformStatus & ComConstants.DefectStatus.FIXED.value()) == 0)
                {
                    status = codeccStatus - ComConstants.DefectStatus.FIXED.value();
                    oldDefect.setFixedTime(0);
                    oldDefect.setFixedBuildNumber(null);
                }

                //如果没有屏蔽的，状态要还原
                if((status & ComConstants.DefectStatus.CHECKER_MASK.value()) > 0L)
                {
                    if(!checkIfMaskByChecker(defectEntity.getCheckerName(), openCheckerMap))
                    {
                        log.info("need to remove check mask status, id: {}", defectEntity.getId());
                        status = status - ComConstants.DefectStatus.CHECKER_MASK.value();
                    }
                }

                if (status == ComConstants.DefectStatus.NEW.value())
                {
                    existCount++;
                    if (CollectionUtils.isNotEmpty(oldDefect.getAuthorList()))
                    {
                        newAuthors.addAll(oldDefect.getAuthorList());
                    }
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

                oldDefect.setStatus(status);

                refreshDefectInfo(oldDefect, defectEntity);
            }
            // 新增的告警
            else
            {
                defectEntity.setTaskId(taskId);
                defectEntity.setToolName(toolName);
                defectEntity.setStreamName(streamName);
                defectEntity.setCreateBuildNumber(buildNum);

                // 根据处理人转换关系转换告警处理人，如果告警处理人为空，则默认取任务接口人作为告警处理人
                List<String> members = new ArrayList<String>() {{
                    add(taskDetailVO.getCreatedBy());
                }};
                Set<String> newAuthorList = AuthorTransferUtils.authorTrans(defectEntity.getAuthorList(),
                    members,
                    transferAuthorList);
                defectEntity.setAuthorList(newAuthorList);

                int status = defectEntity.getStatus();
                // 检查告警是否是已经被修复
                if ((ComConstants.DefectStatus.FIXED.value() & status) > 0)
                {
                    defectEntity.setFixedTime(currTime);
                    fixedCount++;
                }
                else
                {
                    // 检查告警是否过滤路径屏蔽
                    if (PathUtils.checkIfMaskByPath(defectEntity.getFilePathname().toLowerCase(), filterPathSet))
                    {
                        status = status | ComConstants.DefectStatus.PATH_MASK.value();
                    }

                    // 检查告警是否被规则屏蔽
                    if (checkIfMaskByChecker(defectEntity.getCheckerName(), openCheckerMap))
                    {
                        status = status | ComConstants.DefectStatus.CHECKER_MASK.value();
                    }


                    if (status > ComConstants.DefectStatus.NEW.value())
                    {
                        defectEntity.setExcludeTime(currTime);
                        excludeCount++;
                    }
                }

                newCount++;
                if (status > ComConstants.DefectStatus.NEW.value())
                {
                    closeCount++;
                }
                else
                {
                    existCount++;
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

                defectEntity.setStatus(status);
            }


            // 获取规则数据
            Integer count = checkerCountMap.get(defectEntity.getCheckerName());
            if (count == null) {
                count = 0;
            }
            checkerCountMap.put(defectEntity.getCheckerName(), ++count);
        }
        List<Pair<StaticticItem, Integer>> defectCountList = Arrays.asList(
            Pair.of(StaticticItem.NEW, newCount),
            Pair.of(StaticticItem.EXIST, existCount),
            Pair.of(StaticticItem.CLOSE, closeCount),
            Pair.of(StaticticItem.EXCLUDE, excludeCount),
            Pair.of(StaticticItem.FIXED, fixedCount),
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

        if (CollectionUtils.isNotEmpty(defectList))
        {
            existDefectList.addAll(defectList);
        }

        defectRepository.save(existDefectList);
        log.info("save defect size: {}", existDefectList.size());
    }
}
