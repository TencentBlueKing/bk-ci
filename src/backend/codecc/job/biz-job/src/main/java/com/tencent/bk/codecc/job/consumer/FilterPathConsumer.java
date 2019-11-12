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

package com.tencent.bk.codecc.job.consumer;

import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.job.dao.defect.query.CCNDefectRepository;
import com.tencent.bk.codecc.job.dao.defect.query.DUPCDefectRepository;
import com.tencent.bk.codecc.job.dao.defect.query.LintFileQueryRepository;
import com.tencent.bk.codecc.job.dao.defect.query.LintStatisticRepository;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * 路径屏蔽消费者
 *
 * @version V1.0
 * @date 2019/5/21
 */
@Component
public class FilterPathConsumer
{
    private static Logger logger = LoggerFactory.getLogger(FilterPathConsumer.class);

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Autowired
    private DUPCDefectRepository dupcDefectRepository;

    @Autowired
    private LintFileQueryRepository lintFileQueryRepository;

    @Autowired
    private LintStatisticRepository lintStatisticRepository;

    /**
     * 刪除屏蔽路徑
     *
     * @param filterPathInput
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_DEL_TASK_FILTER_PATH,
            value = @Queue(value = QUEUE_DEL_TASK_FILTER_PATH, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_TASK_FILTER_PATH, durable = "true", delayed = "true", type = "topic")))
    public void delFilterPathMessage(FilterPathInputVO filterPathInput)
    {
        logger.info("delete multitool path ignore!");
        // status設置成NEW
        filterPathInput.setAddFile(Boolean.FALSE);
        doFilterPathMessage(filterPathInput);
    }


    /**
     * 添加屏蔽路徑
     *
     * @param filterPathInput
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_ADD_TASK_FILTER_PATH,
            value = @Queue(value = QUEUE_ADD_TASK_FILTER_PATH, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_TASK_FILTER_PATH, durable = "true", delayed = "true", type = "topic")))
    public void addFilterPathMessage(FilterPathInputVO filterPathInput)
    {
        logger.info("add multitool path ignore!");
        // status設置成EXCLUDED
        filterPathInput.setAddFile(Boolean.TRUE);
        doFilterPathMessage(filterPathInput);
    }


    private void doFilterPathMessage(FilterPathInputVO filterPathInput)
    {
        if (Objects.nonNull(filterPathInput) && Objects.nonNull(filterPathInput.getAddFile()))
        {
            List<String> ignoredPaths = new ArrayList<>();
            // 屏蔽默认路径
            if (ComConstants.PATH_TYPE_DEFAULT.equalsIgnoreCase(filterPathInput.getPathType()))
            {
                ignoredPaths = filterPathInput.getDefaultFilterPath();
            }
            else
            {
                List<String> fileDir = filterPathInput.getFilterDir();
                List<String> filterFile = filterPathInput.getFilterFile();
                List<String> customPath = filterPathInput.getCustomPath();

                // 手工输入路径
                if (!CollectionUtils.isEmpty(customPath))
                {
                    ignoredPaths.addAll(customPath);
                }
                // 选择屏蔽文件
                if (!CollectionUtils.isEmpty(filterFile))
                {
                    ignoredPaths.addAll(filterFile);
                }
                // 选择屏蔽文件夹
                if (!CollectionUtils.isEmpty(fileDir))
                {
                    ignoredPaths.addAll(fileDir);
                }
            }

            if (CollectionUtils.isEmpty(ignoredPaths))
            {
                logger.debug("IgnoredPaths list is empty!");
                return;
            }

            // 现在的路径屏蔽已经移到任务维度，所以工具维度的所有类型告警树都需要更新
            doDefectPath(filterPathInput, ignoredPaths);
        }
    }


    private void doDefectPath(FilterPathInputVO filterPathInput, List<String> ignoredPaths)
    {
        Long taskId = filterPathInput.getTaskId();

        // CCN类屏蔽路径
        List<CCNDefectEntity> ccnFileInfoList = ccnDefectRepository.findByTaskId(taskId);
        if (!CollectionUtils.isEmpty(ccnFileInfoList))
        {
            ccnFileInfoList.stream()
                    .filter(ccn -> checkIfIgnore(ccn.getUrl(), ignoredPaths))
                    .forEach(ccn ->
                    {
                        // 更新缺陷状态及屏蔽时间
                        int status = getUpdateStatus(filterPathInput, ccn.getStatus());
                        ccn.setStatus(status);
                        ccn.setExcludeTime(System.currentTimeMillis());
                        ccnDefectRepository.save(ccn);
                    });
        }

        // DUPC除屏蔽路径
        List<DUPCDefectEntity> dupcFileInfoList = dupcDefectRepository.findByTaskId(taskId);
        if (!CollectionUtils.isEmpty(dupcFileInfoList))
        {
            dupcFileInfoList.stream()
                    .filter(dupc -> checkIfIgnore(dupc.getUrl(), ignoredPaths))
                    .forEach(dupc ->
                    {
                        // 更新缺陷状态及屏蔽时间
                        int status = getUpdateStatus(filterPathInput, dupc.getStatus());
                        dupc.setStatus(status);
                        dupc.setExcludeTime(System.currentTimeMillis());
                        dupcDefectRepository.save(dupc);
                    });

        }

        // Lint类屏蔽路径
        List<LintFileEntity> lintFiles = lintFileQueryRepository.findByTaskId(taskId);
        if (!CollectionUtils.isEmpty(lintFiles))
        {
            // 統計各个工具新的文件数量
            Map<String, Integer> filterToolFileCountMap = new HashMap<>();

            for (LintFileEntity lintFile : lintFiles)
            {
                if (checkIfIgnore(lintFile.getUrl(), ignoredPaths))
                {
                    // 更新缺陷状态及屏蔽时间
                    int status = getUpdateStatus(filterPathInput, lintFile.getStatus());
                    lintFile.setStatus(status);
                    lintFile.setExcludeTime(System.currentTimeMillis());
                    lintFileQueryRepository.save(lintFile);

                    String toolName = lintFile.getToolName();
                    Integer integer = filterToolFileCountMap.get(toolName);
                    int newFileCount = Objects.nonNull(integer) ? ++integer : 1;
                    filterToolFileCountMap.put(toolName, newFileCount);
                }
            }

            setLintStatisticInfo(filterPathInput, lintFiles, filterToolFileCountMap);
        }
    }


    /**
     * 更新统计表
     *
     * @param pathInputVO
     * @param lintFiles
     * @param filterToolFileCountMap
     */
    private void setLintStatisticInfo(FilterPathInputVO pathInputVO, List<LintFileEntity> lintFiles, Map<String, Integer> filterToolFileCountMap)
    {
        for (String toolName : filterToolFileCountMap.keySet())
        {
            int fileCount = 0;
            int newDefectCount = 0;
            int historyDefectCount = 0;

            // 取出defectList中的status为new告警的文件
            Map<Integer, List<LintDefectEntity>> lintDefectMap = lintFiles.stream()
                    .filter(lint -> ComConstants.FileType.NEW.value() == lint.getStatus())
                    .filter(lint -> (lint.getToolName().equals(toolName) && CollectionUtils.isNotEmpty(lint.getDefectList())))
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
    }


    /**
     * 获取更新状态
     *
     * @param filterPathInput
     * @param orgStatus
     * @return
     */
    private int getUpdateStatus(FilterPathInputVO filterPathInput, int orgStatus)
    {
        return filterPathInput.getAddFile() ?
                (orgStatus | ComConstants.TaskFileStatus.PATH_MASK.value()) :
                (orgStatus - ComConstants.TaskFileStatus.PATH_MASK.value());
    }


    /**
     * 检测路径是否匹配某个过滤路径
     *
     * @param path
     * @param ignorePaths
     * @return
     */
    private Boolean checkIfIgnore(String path, List<String> ignorePaths)
    {
        if (StringUtils.isNotBlank(path) && CollectionUtils.isNotEmpty(ignorePaths))
        {
            for (String regrex : ignorePaths)
            {
                if (path.contains(regrex) || path.matches(regrex))
                {
                    return true;
                }
            }
        }
        return false;
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
        lastLintStatisticEntity.setUpdatedDate(System.currentTimeMillis());
        lastLintStatisticEntity.setUpdatedBy(pathInputVO.getUserName());
        lintStatisticRepository.save(lastLintStatisticEntity);
    }


}
