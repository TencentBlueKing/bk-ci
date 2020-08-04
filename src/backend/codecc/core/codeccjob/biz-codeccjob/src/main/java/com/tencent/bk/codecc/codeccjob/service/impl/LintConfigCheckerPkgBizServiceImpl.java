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

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.model.LintStatisticEntity;
import com.tencent.bk.codecc.defect.vo.ConfigCheckersPkgReqVO;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.LintFileQueryRepository;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.LintStatisticRepository;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.IBizService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * lint类工具的规则配置
 *
 * @version V1.0
 * @date 2019/12/2
 */
@Service("LINTConfigCheckerPkgBizService")
@Slf4j
public class LintConfigCheckerPkgBizServiceImpl implements IBizService<ConfigCheckersPkgReqVO>
{
    @Autowired
    private LintFileQueryRepository lintFileQueryRepository;

    @Autowired
    private LintStatisticRepository lintStatisticRepository;

    @Override
    public CodeCCResult processBiz(ConfigCheckersPkgReqVO configCheckersPkgReqVO)
    {
        List<LintFileEntity> lintFileEntityList = lintFileQueryRepository.findByTaskIdAndToolNameAndStatus(
                configCheckersPkgReqVO.getTaskId(), configCheckersPkgReqVO.getToolName(), ComConstants.TaskFileStatus.NEW.value());
        if (CollectionUtils.isNotEmpty(lintFileEntityList))
        {
            // 获取前段提交的关闭规则
            List<String> closeCheckers = configCheckersPkgReqVO.getClosedCheckers();
            List<String> opendCheckers = configCheckersPkgReqVO.getOpenedCheckers();

            int oldFileCount = 0, oldFileDefectCount = 0;
            for (LintFileEntity lintFile : lintFileEntityList)
            {
                /**
                 * 为什么查询的时候不直接把规则为空的文件排除 ?
                 * 答：虽然关闭规则会把文件的规则移除，移除时可能规则列表为空。
                 * 由于status没有规则屏蔽，当打开规则时需要从告警列表中重
                 * 新统计规则，给文件规则字段赋值。所以查出来不能把规则为空的文件排除。
                 */
                /**
                 * 统计文件个数
                 * 1. status = new
                 * 2. 规则列表 > 0
                 *
                 * 统计文件告警个数
                 * 1. file.status = new
                 * 2. 规则列表 > 0
                 * 3. defect_list.status = new
                 */
                if (CollectionUtils.isNotEmpty(lintFile.getCheckerList()))
                {
                    ++oldFileCount;

                    List<LintDefectEntity> defectList = lintFile.getDefectList();
                    if (CollectionUtils.isNotEmpty(defectList))
                    {
                        oldFileDefectCount += (int) defectList.stream()
                                .filter(defect -> ComConstants.DefectStatus.NEW.value() == defect.getStatus())
                                .count();
                    }
                }

                if (CollectionUtils.isNotEmpty(closeCheckers))
                {
                    updateCloseCheckerConfig(lintFile, closeCheckers);
                }

                if (CollectionUtils.isNotEmpty(opendCheckers))
                {
                    updateOpenCheckerConfig(lintFile, opendCheckers);
                }
            }

            // 重新刷新lint文件，因为关闭规则后文件规则数可能为0，即规则屏蔽。
            List<LintFileEntity> newLintFileList = lintFileEntityList.stream()
                    .filter(lint -> ComConstants.FileType.NEW.value() == lint.getStatus())
                    .filter(lint -> CollectionUtils.isNotEmpty(lint.getCheckerList()))
                    .collect(Collectors.toList());
            int newFileCount = newLintFileList.size();
            // 负值为告警下降，正值为告警上升
            int oldFileChange = newFileCount - oldFileCount;

            log.info(String.format("update data: oldFileCount: %s, fileChange: %s, oldDefectCount: %s", newLintFileList.size(), oldFileChange, oldFileDefectCount));
            setLintStatisticInfo(configCheckersPkgReqVO, newLintFileList, oldFileChange, oldFileDefectCount);
        }
        return new CodeCCResult(CommonMessageCode.SUCCESS);
    }


    /**
     * 更新统计表
     *
     * @param model
     * @param lintFileEntityList
     */
    private void setLintStatisticInfo(ConfigCheckersPkgReqVO model, List<LintFileEntity> lintFileEntityList, int fileChange, int oldFileDefectCount)
    {
        int newDefectCount = 0;
        int historyDefectCount = 0;
        int newFileCount = 0;
        if (CollectionUtils.isNotEmpty(lintFileEntityList))
        {
            // 取出defectList中的status为new告警的文件
            Map<Integer, List<LintDefectEntity>> lintDefectMap = lintFileEntityList.stream()
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
                newFileCount = lintFileEntityList.size();
            }
        }

        // 更新表 LintStatisticEntity
        saveLintStatisticInfo(model, newFileCount, oldFileDefectCount, newDefectCount, historyDefectCount, fileChange);
    }


    /**
     * 更新 [ 打开 ] 规则配置之后的文件信息以及统计信息
     *
     * @param lintFile     lint文件
     * @param openCheckers 开启的规则
     */
    private void updateOpenCheckerConfig(LintFileEntity lintFile, List<String> openCheckers)
    {
        boolean updateLintFile = false;
        List<LintDefectEntity> defectList = lintFile.getDefectList();
        for (String openChecker : openCheckers)
        {
            // 如果规则列表中包含此规则，则退出执行下一个
            boolean condition = StringUtils.isBlank(openChecker) ||
                    (CollectionUtils.isNotEmpty(lintFile.getCheckerList()) && lintFile.getCheckerList().contains(openChecker));
            if (condition)
            {
                continue;
            }

            for (LintDefectEntity defect : defectList)
            {
                // 告警列表中存在此规则并且status属于路径屏蔽, 则需要更新LintFileEntity
                if (openChecker.equals(defect.getChecker()) && (defect.getStatus() & ComConstants.DefectStatus.CHECKER_MASK.value()) > 0)
                {
                    // 打开规则[ 减操作 ], 让状态变成new, 即扫描, 本身规则打开的初始状态status等于new(1)
                    defect.setStatus(defect.getStatus() - ComConstants.DefectStatus.CHECKER_MASK.value());
                    updateLintFile = true;
                }
            }
        }

        if (updateLintFile)
        {
            // 更新表：LintFileEntity
            saveLintFile(lintFile, defectList);
        }

    }


    /**
     * 更新 [ 关闭 ] 规则配置之后的文件信息以及统计信息
     *
     * @param lintFile       lint文件
     * @param closedCheckers 关闭的规则
     */
    private void updateCloseCheckerConfig(LintFileEntity lintFile, List<String> closedCheckers)
    {
        // DB规则列表为空或者不包括关闭规则, 不操作
        Set<String> checkerSet = lintFile.getCheckerList();
        if (CollectionUtils.isEmpty(checkerSet) || checkerSet.stream().noneMatch(closedCheckers::contains))
        {
            return;
        }

        List<LintDefectEntity> defectList = lintFile.getDefectList();
        for (String closeChecker : closedCheckers)
        {
            // 过滤忽略的告警规则，设置状态
            for (LintDefectEntity defect : defectList)
            {
                // 关闭的规则肯定是新告警，关闭规则[ 或操作 ]
                if (ComConstants.DefectStatus.NEW.value() == defect.getStatus() && closeChecker.equals(defect.getChecker()))
                {
                    defect.setStatus(defect.getStatus() | ComConstants.DefectStatus.CHECKER_MASK.value());
                }
            }
        }

        saveLintFile(lintFile, defectList);

    }


    /**
     * 设置需要更新的LintFile属性
     *
     * @param lintFile   lintFile 实体对象
     * @param defectList 屏蔽规则之后的 - 告警列表
     */
    private void saveLintFile(LintFileEntity lintFile, List<LintDefectEntity> defectList)
    {
        if (CollectionUtils.isNotEmpty(defectList))
        {
            Set<String> authorSet = Sets.newHashSet();
            Set<String> checkerSet = Sets.newHashSet();
            AtomicInteger newCount = new AtomicInteger();
            AtomicInteger historyCount = new AtomicInteger();

            defectList.stream()
                    .filter(defect -> ComConstants.DefectStatus.NEW.value() == defect.getStatus())
                    .forEach(defect ->
                    {
                        authorSet.add(defect.getAuthor());
                        checkerSet.add(defect.getChecker());
                        if (ComConstants.DefectType.NEW.value() == defect.getStatus())
                        {
                            newCount.incrementAndGet();
                        }
                        else
                        {
                            historyCount.incrementAndGet();
                        }
                    });

            // 重新设置作者列表
            lintFile.setAuthorList(authorSet);
            // 重新设置规则列表
            lintFile.setCheckerList(checkerSet);
            // 重新设置告警列表
            lintFile.setDefectList(defectList);
            // 重新设置新告警数
            lintFile.setNewCount(newCount.get());
            // 重新设置历史告警数
            lintFile.setHistoryCount(historyCount.get());
            // 重新告警总数
            lintFile.setDefectCount(lintFile.getNewCount() + lintFile.getHistoryCount());
            // 保存更新
            lintFileQueryRepository.save(lintFile);
        }
    }

    /**
     * 保存忽略规则之后的的统计情况
     *
     * @param model              前端请求model
     * @param newDefectCount     新告警个数
     * @param historyDefectCount 历史告警个数
     */
    private void saveLintStatisticInfo(ConfigCheckersPkgReqVO model, int fileCount, int oldFileDefectCount, int newDefectCount, int historyDefectCount, int fileChange)
    {
        LintStatisticEntity lastLintStatisticEntity = lintStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(model.getTaskId(), model.getToolName());
        if (Objects.isNull(lastLintStatisticEntity))
        {
            lastLintStatisticEntity = new LintStatisticEntity();
        }

        lastLintStatisticEntity.setFileCount(fileCount);
        lastLintStatisticEntity.setFileChange(fileChange);
        lastLintStatisticEntity.setNewDefectCount(newDefectCount);
        lastLintStatisticEntity.setHistoryDefectCount(historyDefectCount);
        lastLintStatisticEntity.setDefectCount(newDefectCount + historyDefectCount);
        lastLintStatisticEntity.setDefectChange(lastLintStatisticEntity.getDefectCount() - oldFileDefectCount);
        lastLintStatisticEntity.setTime(System.currentTimeMillis());
        lintStatisticRepository.save(lastLintStatisticEntity);
    }
}
