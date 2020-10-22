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

package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CommonStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.dao.redis.StatisticDao;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity;
import com.tencent.bk.codecc.defect.service.AbstractPlatformUploadDefectBizService;
import com.tencent.bk.codecc.defect.service.IUpdateDefectBizService;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.utils.AuthorTransferUtils;
import com.tencent.bk.codecc.defect.vo.DefectDetailVO;
import com.tencent.bk.codecc.defect.vo.UpdateDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.vo.OpenCheckerVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * pinpoint任务分析记录服务层实现
 *
 * @version V1.0
 * @date 2019/12/9
 */
@Slf4j
@Service("PINPOINTUploadDefectBizService")
public class PinpointUploadDefectBizServiceImpl extends AbstractPlatformUploadDefectBizService
{
    @Autowired
    protected DefectRepository defectRepository;
    @Autowired
    protected StatisticDao statisticDao;
    @Autowired
    private IUpdateDefectBizService updateDefectBizService;
    @Autowired
    protected BuildRepository buildRepository;
    @Autowired
    protected CommonStatisticRepository commonStatisticRepository;
    @Autowired
    protected ScmFileInfoService scmFileInfoService;

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

        List<DefectEntity> allExistDefectList = defectRepository.findByTaskIdAndToolName(taskId, toolName);
        Map<String, DefectEntity> defectEntityMap = allExistDefectList.stream().collect(Collectors.toMap(DefectEntity::getId, defect -> defect, (k1, k2) -> k1));

        // 获取文件作者信息
        Map<String, ScmBlameVO> fileChangeRecordsMap = scmFileInfoService.loadAuthorInfoMap(
            uploadDefectVO.getTaskId(),
            uploadDefectVO.getStreamName(),
            uploadDefectVO.getToolName(),
            uploadDefectVO.getBuildId());

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

        // 需要更新的遗留告警
        List<DefectEntity> needUpdateExistDefectList = new ArrayList<>();
        long currTime = System.currentTimeMillis();
        Iterator<DefectEntity> it = defectList.iterator();
        while (it.hasNext())
        {
            DefectEntity defectEntity = it.next();
            defectEntity.setTaskId(uploadDefectVO.getTaskId());
            defectEntity.setToolName(uploadDefectVO.getToolName());
            defectEntity.setStreamName(uploadDefectVO.getStreamName());
            defectEntity.setPlatformBuildId(uploadDefectVO.getReportId());
            defectEntity.setPlatformProjectId(uploadDefectVO.getPlatformProjectId());

            setAuthor(defectEntity, fileChangeRecordsMap);

            DefectEntity oldDefect = defectEntityMap.get(defectEntity.getId());

            // 1.已存在的告警
            if (oldDefect != null)
            {
                it.remove();
                needUpdateExistDefectList.add(oldDefect);
                int status = oldDefect.getStatus();
                int codeccStatus = status;
//                Integer platformStatus = defectEntity.getStatus();
//
//                // 1.1 在codecc上new（即不被忽略、不被路径或规则屏蔽），在platform上已修复的告警，要标志为已修复
//                if (codeccStatus == ComConstants.DefectStatus.NEW.value() && (platformStatus & ComConstants.DefectStatus.FIXED.value()) > 0)
//                {
//                    status = codeccStatus | ComConstants.DefectStatus.FIXED.value();
//                    oldDefect.setFixedTime(currTime);
//                    oldDefect.setFixedBuildNumber(buildNum);
//                    closeCount++;
//                    fixedCount++;
//                }
                // 1.2 在codecc上已修复，在platform上未修复的告警，要标志为未修复
//                else if ((codeccStatus & ComConstants.DefectStatus.FIXED.value()) > 0 && (platformStatus & ComConstants.DefectStatus.FIXED.value()) == 0)
                if ((codeccStatus & ComConstants.DefectStatus.FIXED.value()) > 0)
                {
                    status = status - ComConstants.DefectStatus.FIXED.value();
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

                oldDefect.setLineNumber(defectEntity.getLineNumber());
                oldDefect.setDisplayCategory(defectEntity.getDisplayCategory());
                oldDefect.setDisplayType(defectEntity.getDisplayType());
                oldDefect.setFilePathname(defectEntity.getFilePathname());
                oldDefect.setPlatformBuildId(defectEntity.getPlatformBuildId());
                oldDefect.setDefectInstances(defectEntity.getDefectInstances());
            }
            // 新增的告警
            else
            {
                defectEntity.setTaskId(taskId);
                defectEntity.setToolName(toolName);
                defectEntity.setStreamName(streamName);
                defectEntity.setCreateBuildNumber(buildNum);
                defectEntity.setCreateTime(currTime);

                // 根据处理人转换关系转换告警处理人，如果告警处理人为空，则默认取任务接口人作为告警处理人
                List<String> members = new ArrayList<String>() {{
                    add(taskDetailVO.getCreatedBy());
                }};
                Set<String> newAuthorList = AuthorTransferUtils.authorTrans(defectEntity.getAuthorList(),
                    members,
                    transferAuthorList);
                defectEntity.setAuthorList(newAuthorList);

                int status = defectEntity.getStatus();
                if (status == 0)
                {
                    status = 1;
                }

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

        List<Pair<ComConstants.StaticticItem, Integer>> defectCountList = Arrays.asList(
            Pair.of(ComConstants.StaticticItem.NEW, newCount),
            Pair.of(ComConstants.StaticticItem.EXIST, existCount),
            Pair.of(ComConstants.StaticticItem.CLOSE, closeCount),
            Pair.of(ComConstants.StaticticItem.EXCLUDE, excludeCount),
            Pair.of(ComConstants.StaticticItem.FIXED, fixedCount),
            Pair.of(ComConstants.StaticticItem.EXIST_PROMPT, existPromptCount),
            Pair.of(ComConstants.StaticticItem.EXIST_NORMAL, existNormalCount),
            Pair.of(ComConstants.StaticticItem.EXIST_SERIOUS, existSeriousCount),
            Pair.of(ComConstants.StaticticItem.NEW_PROMPT, existPromptCount),
            Pair.of(ComConstants.StaticticItem.NEW_NORMAL, existNormalCount),
            Pair.of(ComConstants.StaticticItem.NEW_SERIOUS, existSeriousCount));
        statisticDao.increaseDefectCountByStatusBatch(taskId, toolName, buildNum, defectCountList);
        statisticDao.addNewAndExistAuthors(taskId, toolName, buildNum, newAuthors, newAuthors);

        // 写入规则统计数据
        statisticDao.increaseDefectCheckerCountBatch(taskId, toolName, buildNum, checkerCountMap);

        List<DefectEntity> allDefectList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(defectList))
        {
            allDefectList.addAll(defectList);
        }
        if (CollectionUtils.isNotEmpty(needUpdateExistDefectList))
        {
            allDefectList.addAll(needUpdateExistDefectList);
        }
        defectRepository.save(allDefectList);
        log.info("save defect size: {}", allDefectList.size());

        //平台已有，工具侧未上报告警
        List<DefectDetailVO> needToUpdateDefectList = allExistDefectList.stream().filter(defectEntity ->
                needUpdateExistDefectList.stream().noneMatch(defect -> defect.getId().equalsIgnoreCase(defectEntity.getId()))).map(defectEntity ->
        {
            DefectDetailVO defectDetailVO = new DefectDetailVO();
            BeanUtils.copyProperties(defectEntity, defectDetailVO);
            defectDetailVO.setStatus(ComConstants.DefectStatus.FIXED.value());
            return defectDetailVO;
        }).collect(Collectors.toList());

        UpdateDefectVO updateDefectStatusVO = new UpdateDefectVO();
        updateDefectStatusVO.setTaskId(uploadDefectVO.getTaskId());
        updateDefectStatusVO.setDefectList(needToUpdateDefectList);
        updateDefectStatusVO.setToolName(uploadDefectVO.getToolName());
        updateDefectStatusVO.setBuildId(uploadDefectVO.getBuildId());
        updateDefectBizService.updateDefectStatus(updateDefectStatusVO);
        log.info("update defect status");
    }

    /**
     * 填入作者信息
     *
     * @param defectEntity
     * @param fileChangeRecordsMap
     */
    private void setAuthor(DefectEntity defectEntity, Map<String, ScmBlameVO> fileChangeRecordsMap)
    {
        if (MapUtils.isNotEmpty(fileChangeRecordsMap) && fileChangeRecordsMap.get(defectEntity.getFilePathname()) != null)
        {
            ScmBlameVO fileLineAuthorInfo = fileChangeRecordsMap.get(defectEntity.getFilePathname());
            List<ScmBlameChangeRecordVO> changeRecords = fileLineAuthorInfo.getChangeRecords();
            if (CollectionUtils.isNotEmpty(changeRecords))
            {
                int defectLine = defectEntity.getLineNumber();
                for (ScmBlameChangeRecordVO changeRecord : changeRecords)
                {
                    boolean isFound = false;
                    List<Object> lines = changeRecord.getLines();
                    if (lines != null && lines.size() > 0)
                    {
                        for (Object line : lines)
                        {
                            if (line instanceof Integer && defectLine == (int) line)
                            {
                                isFound = true;
                            }
                            else if (line instanceof List)
                            {
                                List<Integer> lineScope = (List<Integer>) line;
                                if (CollectionUtils.isNotEmpty(lineScope) && lineScope.size() > 1)
                                {
                                    if (lineScope.get(0) <= defectLine && lineScope.get(lineScope.size() - 1) >= defectLine)
                                    {
                                        isFound = true;
                                    }
                                }
                            }
                            if (isFound)
                            {
                                String authCur = changeRecord.getAuthor();
                                if (null != authCur && authCur.length() > 0)
                                {
                                    // 去掉中文名
                                    int keyIndex = authCur.indexOf("(");
                                    if (keyIndex != -1)
                                    {
                                        authCur = authCur.substring(0, keyIndex);
                                    }
                                }
                                Set<String> authorList = new HashSet<>();
                                authorList.add(authCur);
                                defectEntity.setAuthorList(authorList);
                                break;
                            }
                        }
                    }
                    if (isFound)
                    {
                        defectEntity.setRevision(fileLineAuthorInfo.getRevision());
                        break;
                    }
                }
            }
        }
    }
}
