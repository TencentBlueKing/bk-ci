/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

package com.tencent.bk.codecc.job.consumer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.model.LintStatisticEntity;
import com.tencent.bk.codecc.defect.vo.ConfigCheckersPkgReqVO;
import com.tencent.bk.codecc.job.dao.defect.query.LintFileQueryRepository;
import com.tencent.bk.codecc.job.dao.defect.query.LintStatisticRepository;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.JsonUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * 规则配置消费者 [ 当前仅仅操作lint类的规则配置 ]
 *
 * @version V1.0
 * @date 2019/6/17
 */
@Component
public class CheckerConfigConsumer
{
    private static Logger logger = LoggerFactory.getLogger(CheckerConfigConsumer.class);

    @Autowired
    private LintFileQueryRepository lintFileQueryRepository;

    @Autowired
    private LintStatisticRepository lintStatisticRepository;

    /**
     * 添加屏蔽规则 [ 加入屏蔽规则列表t_ignore_checker之后, 调用Job模块操作]
     * <p>
     * 1.  根据相同的屏蔽规则更新t_lint_defect
     * 1.1 从规则列表中移除屏蔽规则
     * 1.2 从告警列表中移除屏蔽规则相同的告警
     * 1.3 重新从告警列表中统计作者列表
     * <p>
     * 2. 统计移除之后的告警
     * 2.1 告警总数
     * 2.2 新的告警总数
     * 2.3 历史告警总数
     * <p>
     * 3. 更新数据报表的数据：LintStatisticEntity
     *
     * @param model
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_IGNORE_CHECKER,
            value = @Queue(value = QUEUE_IGNORE_CHECKER, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_TASK_CHECKER_CONFIG, durable = "true", delayed = "true", type = "topic")))
    public void checkerConfig(ConfigCheckersPkgReqVO model)
    {
        logger.info("checker config job start ... data: \n" + JsonUtil.INSTANCE.toJson(model));
        if (CollectionUtils.isEmpty(model.getClosedCheckers()) && CollectionUtils.isEmpty(model.getOpenedCheckers()))
        {
            return;
        }

        Set<String> closeSet = Sets.newHashSet();
        Set<String> opendSet = Sets.newHashSet();
        Optional.ofNullable(model.getClosedCheckers()).orElseGet(ArrayList::new).forEach(checker -> addChecker(closeSet, checker));
        Optional.ofNullable(model.getOpenedCheckers()).orElseGet(ArrayList::new).forEach(checker -> addChecker(opendSet, checker));
        model.setClosedCheckers(Lists.newArrayList(closeSet));
        model.setOpenedCheckers(Lists.newArrayList(opendSet));

        List<LintFileEntity> lintFileEntityList = lintFileQueryRepository.findByTaskIdAndToolNameAndStatus(model.getTaskId(), model.getToolName(), ComConstants.TaskFileStatus.NEW.value());
        if (CollectionUtils.isNotEmpty(lintFileEntityList))
        {

            // 获取前段提交的关闭规则
            List<String> closeCheckers = model.getClosedCheckers();
            List<String> opendCheckers = model.getOpenedCheckers();

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

            logger.info(String.format("update data: oldFileCount: %s, fileChange: %s, oldDefectCount: %s", newLintFileList.size(), oldFileChange, oldFileDefectCount));
            setLintStatisticInfo(model, newLintFileList, oldFileChange, oldFileDefectCount);
        }
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
        lastLintStatisticEntity.setUpdatedDate(System.currentTimeMillis());
        lintStatisticRepository.save(lastLintStatisticEntity);
    }


    /**
     * 添加规则
     *
     * @param closeCheckers
     * @param checker
     */
    private void addChecker(Set<String> closeCheckers, String checker)
    {
        String checkerValue = checker.endsWith("-tosa") ?
                checker.substring(0, checker.indexOf("-tosa")) : checker;
        closeCheckers.add(checkerValue);
    }


}
