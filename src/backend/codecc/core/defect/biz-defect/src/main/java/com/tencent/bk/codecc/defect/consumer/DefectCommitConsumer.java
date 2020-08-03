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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.api.ServiceReportTaskLogRestResource;
import com.tencent.bk.codecc.defect.component.LintDefectTracingComponent;
import com.tencent.bk.codecc.defect.constant.DefectConstants;
import com.tencent.bk.codecc.defect.dao.mongorepository.*;
import com.tencent.bk.codecc.defect.dao.mongotemplate.BuildDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.BuildDefectDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.BuildDefectService;
import com.tencent.bk.codecc.defect.service.RedLineReportService;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.util.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 刷新规则集使用量消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class DefectCommitConsumer
{
    /**
     * 字符串锁前缀
     */
    private static final String UPDATE_LINT_DEFECTS_LOCK_KEY = "UPDATE_LINT_DEFECTS:";

    /**
     * 分布式锁超时时间
     */
    private static final Long LOCK_TIMEOUT = 20L;

    @Autowired
    private LintDefectRepository lintDefectRepository;

    @Autowired
    private LintDefectDao lintDefectDao;

    @Autowired
    private LintStatisticRepository lintStatisticRepository;
    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;
    @Autowired
    private ToolBuildStackRepository toolBuildStackRepository;

    @Autowired
    private BuildDefectRepository buildDefectRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private BuildDefectService buildDefectService;

    @Autowired
    private CodeRepoInfoRepository codeRepoRepository;

    @Autowired
    private RedLineReportService redLineReportService;

    @Autowired
    private NewDefectJudgeService newDefectJudgeService;

    @Autowired
    private LintDefectTracingComponent lintDefectTracingComponent;

    @Autowired
    private BuildDao buildDao;

    @Autowired
    public ToolBuildInfoDao toolBuildInfoDao;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Autowired
    private ServiceReportTaskLogRestResource serviceReportTaskLogRestResource;

    @Autowired
    private BuildDefectDao buildDefectDao;

    /**
     * 告警提交
     *
     * @param commitDefectVO
     */
    public void lintCommitDefect(CommitDefectVO commitDefectVO)
    {
        long beginTime = System.currentTimeMillis();
        try
        {
            log.info("commit defect! {}", commitDefectVO);

            // 发送开始提单的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null);

            try
            {
                updateStatusAndBuildDefects(commitDefectVO);
            }
            catch (Throwable e)
            {
                log.error("commit defect fail!", e);
                // 发送提单失败的分析记录
                uploadTaskLog(commitDefectVO, ComConstants.StepFlag.FAIL.value(), 0, System.currentTimeMillis(), e.getLocalizedMessage());
                return;
            }

            // 发送提单成功的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null);
        }
        catch (Throwable e)
        {
            log.error("commit defect fail!", e);
        }
        finally
        {
            log.info("commit defect cost: {}, {}", System.currentTimeMillis() - beginTime, commitDefectVO);
        }
    }

    public void ccnCommitDefect(CommitDefectVO commitDefectVO)
    {
        try
        {
            log.info("commit defect! {}", commitDefectVO);

            // 发送开始提单的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null);

            try
            {
//                updateStatusAndBuildDefects(commitDefectVO);
            }
            catch (Exception e)
            {
                log.error("commit defect fail!", e);
                // 发送提单失败的分析记录
                uploadTaskLog(commitDefectVO, ComConstants.StepFlag.FAIL.value(), 0, System.currentTimeMillis(), e.getLocalizedMessage());
                return;
            }

            // 发送提单成功的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null);
        }
        catch (Exception e)
        {
            log.error("commit defect fail!", e);
        }
    }

    public void dupcCommitDefect(CommitDefectVO commitDefectVO)
    {
        try
        {
            log.info("commit defect! {}", commitDefectVO);

            // 发送开始提单的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null);

            try
            {
//                updateStatusAndBuildDefects(commitDefectVO);
            }
            catch (Exception e)
            {
                log.error("commit defect fail!", e);
                // 发送提单失败的分析记录
                uploadTaskLog(commitDefectVO, ComConstants.StepFlag.FAIL.value(), 0, System.currentTimeMillis(), e.getLocalizedMessage());
                return;
            }

            // 发送提单成功的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null);
        }
        catch (Exception e)
        {
            log.error("commit defect fail!", e);
        }
    }

    public void clocCommitDefect(CommitDefectVO commitDefectVO)
    {
        try
        {
            log.info("commit defect! {}", commitDefectVO);

            // 发送开始提单的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null);

            try
            {
//                updateStatusAndBuildDefects(commitDefectVO);
            }
            catch (Exception e)
            {
                log.error("commit defect fail!", e);
                // 发送提单失败的分析记录
                uploadTaskLog(commitDefectVO, ComConstants.StepFlag.FAIL.value(), 0, System.currentTimeMillis(), e.getLocalizedMessage());
                return;
            }

            // 发送提单成功的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null);
        }
        catch (Exception e)
        {
            log.error("commit defect fail!", e);
        }
    }

    public void pinpointCommitDefect(CommitDefectVO commitDefectVO)
    {
        try
        {
            log.info("commit defect! {}", commitDefectVO);

            // 发送开始提单的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null);

            try
            {
//                updateStatusAndBuildDefects(commitDefectVO);
            }
            catch (Exception e)
            {
                log.error("commit defect fail!", e);
                // 发送提单失败的分析记录
                uploadTaskLog(commitDefectVO, ComConstants.StepFlag.FAIL.value(), 0, System.currentTimeMillis(), e.getLocalizedMessage());
                return;
            }

            // 发送提单成功的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null);
        }
        catch (Exception e)
        {
            log.error("commit defect fail!", e);
        }
    }

    private void updateStatusAndBuildDefects(CommitDefectVO commitDefectVO)
    {
        // 调用task模块的接口获取任务信息
        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(commitDefectVO.getStreamName());
        long taskId = commitDefectVO.getTaskId();
        String toolName = commitDefectVO.getToolName();

        // 加分布式锁
        RedisLock lock = new RedisLock(redisTemplate, UPDATE_LINT_DEFECTS_LOCK_KEY + taskId + ComConstants.SEPARATOR_SEMICOLON + toolName, LOCK_TIMEOUT);

        try
        {
            long beginTime = System.currentTimeMillis();
            lock.lock();
            log.info("RedisLock cost: {}, {}", System.currentTimeMillis() - beginTime, commitDefectVO);

            // 更新告警状态和构建告警快照
            beginTime = System.currentTimeMillis();
            updateStatusAndBuildDefects(taskId, toolName, commitDefectVO.getBuildId(), taskVO);
            log.info("updateStatusAndBuildDefects cost: {}, {}", System.currentTimeMillis() - beginTime, commitDefectVO);
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }
    }


    private void updateStatusAndBuildDefects(long taskId, String toolName, String buildId, TaskDetailVO taskDetailVO)
    {
        /* 需要统计的信息：
         * 1.本次分析遗留告警总数，文件总数，用于跟上一次分析的结果比较，得到最近一次分析结果，用于项目详情页展示，例如： 告警88247(↑38) 文件1796(↑0)
         * 2.当前遗留新告警数，历史告警数，用于数据报表统计每日告警遗留趋势图
         */
        int defectCount = 0;
        int fileCount = 0;
        int newDefectCount = 0;
        int historyDefectCount = 0;
        int totalNewSerious = 0;
        int totalNewNormal = 0;
        int totalNewPrompt = 0;
        Map<String, NotRepairedAuthorEntity> authorDefectMap = Maps.newHashMap();

        // 查询临时存储的已删除文件列表并入库
        long beginTime = System.currentTimeMillis();
        CodeRepoInfoEntity codeRepoInfoEntity = codeRepoRepository.findByTaskIdAndBuildId(taskId, buildId);
        if (codeRepoInfoEntity != null && CollectionUtils.isNotEmpty(codeRepoInfoEntity.getTempDeleteFiles()))
        {
            toolBuildInfoDao.updateDeleteFiles(taskId, toolName, codeRepoInfoEntity.getTempDeleteFiles(), DefectConstants.UpdateToolDeleteFileType.ADD);
        }

        // 获取工具侧上报的已删除文件
        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        List<String> deleteFiles;
        if (toolBuildStackEntity != null && CollectionUtils.isNotEmpty(toolBuildStackEntity.getDeleteFiles()))
        {
            deleteFiles = toolBuildStackEntity.getDeleteFiles();
        }
        else
        {
            deleteFiles = Lists.newArrayList();
        }
        log.info("t_tool_build_info update and find cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        // 判断本次是增量还是全量扫描
        boolean isFullScan = toolBuildStackEntity != null ? toolBuildStackEntity.isFullScan() : true;

        // 获取本次构建上报的文件列表并入库
        beginTime = System.currentTimeMillis();
        List<LintFileEntity> previousLintFileEntityList = lintDefectRepository.findByTaskIdAndToolName(taskId, toolName);
        log.info("find t_lint_defect list findByTaskIdAndToolName  cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        beginTime = System.currentTimeMillis();
        Set<String> currentBuildFileRelPaths = Sets.newHashSet();
        BuildEntity buildEntity = buildDao.getAndSaveBuildInfo(buildId);
        log.info("getAndSaveBuildInfo t_build cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        beginTime = System.currentTimeMillis();
        List<BuildDefectEntity> buildDefectEntities = buildDefectRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        log.info("find t_build_defect list ByTaskIdAndToolNameAndBuildId  cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);
        if (CollectionUtils.isNotEmpty(buildDefectEntities))
        {
            List<LintFileEntity> currentBuildLintFiles = Lists.newArrayList();
            for (BuildDefectEntity buildDefect : buildDefectEntities)
            {
                if (buildDefect != null && buildDefect.getTempDefectFile() != null)
                {
                    currentBuildFileRelPaths.add(buildDefect.getFileRelPath());
                    currentBuildLintFiles.add(buildDefect.getTempDefectFile());
                }
            }

            // 保存本次上报的告警
            if (CollectionUtils.isNotEmpty(currentBuildLintFiles))
            {
                // 告警跟踪只用于更新本次有上报的文件中的告警
                List<LintFileEntity> originalFileList = Lists.newArrayList();
                if (previousLintFileEntityList != null)
                {
                    for (LintFileEntity previousFile : previousLintFileEntityList)
                    {
                        // 全量扫描或者本次上报中包含的文件，都要加入告警跟踪
                        if (isFullScan || currentBuildFileRelPaths.contains(previousFile.getRelPath()))
                        {
                            originalFileList.add(previousFile);
                        }
                    }
                }

                // 做告警跟踪操作
                log.info("lint previous defect list :{}", originalFileList.size());
                log.info("lint current defect list :{}", currentBuildLintFiles.size());
                beginTime = System.currentTimeMillis();
                List<LintFileEntity> finalLintFileEntityList = lintDefectTracingComponent.defectTracing(taskDetailVO, toolName,
                        buildEntity,
                        originalFileList, currentBuildLintFiles);
                log.info("defec tracing cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

                int finalDefectCount = 0;
                List<String> filterPath = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(taskDetailVO.getFilterPath()))
                {
                    filterPath.addAll(taskDetailVO.getFilterPath());
                }
                if (CollectionUtils.isNotEmpty(taskDetailVO.getDefaultFilterPath()))
                {
                    filterPath.addAll(taskDetailVO.getDefaultFilterPath());
                }

                if (CollectionUtils.isNotEmpty(finalLintFileEntityList))
                {
                    for (LintFileEntity lintFileEntity : finalLintFileEntityList)
                    {
                        if (CollectionUtils.isNotEmpty(lintFileEntity.getDefectList()))
                        {
                            finalDefectCount += lintFileEntity.getDefectList().size();
                        }
                        //在入库前检测一遍屏蔽路径
                        if (CollectionUtils.isNotEmpty(filterPath))
                        {
                            try
                            {
                                if (PathUtils.checkIfMaskByPath(StringUtils.isNotEmpty(lintFileEntity.getRelPath()) ? lintFileEntity.getRelPath() : lintFileEntity.getFilePath(),
                                        new HashSet<>(filterPath)))
                                {
                                    lintFileEntity.setStatus(lintFileEntity.getStatus() | ComConstants.TaskFileStatus.PATH_MASK.value());
                                    if (CollectionUtils.isNotEmpty(lintFileEntity.getDefectList()))
                                    {
                                        lintFileEntity.getDefectList().forEach(defect ->
                                                defect.setStatus(defect.getStatus() | ComConstants.TaskFileStatus.PATH_MASK.value())
                                        );
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                log.info("invalid regex expression for lint, expression: task id: {}", taskId);
                            }
                        }
                    }
                }
                log.error("lint defect trace result: taskId:{}, toolName:{}, fileCount:{}, defectCount:{}", taskId, toolName,
                        finalLintFileEntityList.size(), finalDefectCount);

                beginTime = System.currentTimeMillis();
                lintDefectDao.upsertDefectListByPath(taskId, toolName, finalLintFileEntityList);
                log.info("t_lint_defect upsertDefectListByFileRelPath cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

                // 上报告警信息到数据平台
//                pushToKafka(currentBuildLintFiles);

                for (BuildDefectEntity buildDefect : buildDefectEntities)
                {
                    Set<String> fileDefectIds = Sets.newHashSet();
                    if (buildDefect.getTempDefectFile() != null && CollectionUtils.isNotEmpty(buildDefect.getTempDefectFile().getDefectList()))
                    {
                        for (LintDefectEntity lintDefectEntity : buildDefect.getTempDefectFile().getDefectList())
                        {
                            if (lintDefectEntity.getStatus() == ComConstants.DefectStatus.NEW.value())
                            {
                                fileDefectIds.add(lintDefectEntity.getDefectId());
                            }
                        }
                    }
                    buildDefect.setFileDefectIds(fileDefectIds);

                    // 清除临时文件存储
                    buildDefect.setTempDefectFile(null);
                }
            }

            // 清除临时文件存储
            beginTime = System.currentTimeMillis();
            buildDefectDao.upsertByFilePath(buildDefectEntities);
            log.info("t_build_defect save list cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);
        }

        // 需要更新状态的文件
        beginTime = System.currentTimeMillis();
        List<LintFileEntity> allFileEntityList = lintDefectRepository.findByTaskIdAndToolName(taskId, toolName);
        log.info("t_lint_defect find list cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        beginTime = System.currentTimeMillis();
        List<LintFileEntity> needUpdateFileEntityList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(allFileEntityList))
        {
            // 查询新老告警判定时间
            long newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, toolName, taskDetailVO);
            long currentTime = System.currentTimeMillis();

            for (LintFileEntity fileEntity : allFileEntityList)
            {
                String filePath = fileEntity.getFilePath();

                // 只处理打开状态的告警
                boolean notCurrentBuildUpload = CollectionUtils.isEmpty(currentBuildFileRelPaths)
                        || !currentBuildFileRelPaths.contains(fileEntity.getRelPath());
                if (fileEntity.getStatus() == ComConstants.DefectStatus.NEW.value())
                {
                    /**
                     * 1、如果文件已删除，则设置为已修复状态
                     * 2、如果是全量扫描，且此次分析中没有上报，则设置为已修复状态
                     * 3、如果是增量扫描，且此次分析中没有上报，则不做处理
                     */
                    if (deleteFiles.contains(filePath) || (isFullScan && notCurrentBuildUpload))
                    {

                        fileEntity.setFixedTime(currentTime);
                        //只有告警全部是已修复状态时，才会将文件状态设置为已修复，否则不会将文件状态设置为修复
                        Boolean updateFileFlag = true;
                        if (CollectionUtils.isNotEmpty(fileEntity.getDefectList()))
                        {
                            for (LintDefectEntity lintDefectEntity : fileEntity.getDefectList())
                            {
                                //只有待修复状态的告警才会设置为已修复
                                if (lintDefectEntity.getStatus() == ComConstants.DefectStatus.NEW.value())
                                {
                                    lintDefectEntity.setStatus(lintDefectEntity.getStatus() | ComConstants.DefectStatus.FIXED.value());
                                    lintDefectEntity.setFixedTime(currentTime);
                                    lintDefectEntity.setFixedBuildNumber(buildEntity.getBuildNo());
                                }
                                //否则不动告警,并且只要有不为已修复状态的告警，则不更新文件状态
                                else if ((lintDefectEntity.getStatus() & ComConstants.DefectStatus.FIXED.value()) == 0)
                                {
                                    updateFileFlag = false;
                                }
                            }
                        }

                        if (updateFileFlag)
                        {
                            log.info("need to update file status to fixed, task id: {}, tool name: {}", taskId, toolName);
                            fileEntity.setStatus(fileEntity.getStatus() | ComConstants.DefectStatus.FIXED.value());
                        }
                        needUpdateFileEntityList.add(fileEntity);
                    }
                }
                //如果是已修复状态，要确保每个告警都是已修复
                else if ((fileEntity.getStatus() & ComConstants.DefectStatus.FIXED.value()) > 0L)
                {
                    if (CollectionUtils.isNotEmpty(fileEntity.getDefectList()))
                    {
                        Boolean needToUpdateFlag = false;
                        for (LintDefectEntity lintDefectEntity : fileEntity.getDefectList())
                        {
                            if ((lintDefectEntity.getStatus() & ComConstants.DefectStatus.FIXED.value()) == 0)
                            {
                                lintDefectEntity.setStatus(lintDefectEntity.getStatus() |
                                        ComConstants.DefectStatus.FIXED.value());
                                needToUpdateFlag = true;
                            }
                        }
                        if (needToUpdateFlag)
                        {
                            log.info("need to refresh status of defect list");
                            needUpdateFileEntityList.add(fileEntity);
                        }
                    }
                }

                // 统计本次构建遗留的告警数量和文件数量，以及各作者告警数量
                if (fileEntity.getStatus() == ComConstants.DefectStatus.NEW.value())
                {
                    fileCount++;
                    if (CollectionUtils.isNotEmpty(fileEntity.getDefectList()))
                    {
                        for (LintDefectEntity lintDefectEntity : fileEntity.getDefectList())
                        {
                            if (lintDefectEntity.getStatus() != ComConstants.DefectStatus.NEW.value())
                            {
                                continue;
                            }
                            Long lineUpdateTime = lintDefectEntity.getLineUpdateTime();
                            if (lineUpdateTime == null)
                            {
                                lineUpdateTime = lintDefectEntity.getCreateTime();
                            }
                            if (lineUpdateTime >= newDefectJudgeTime)
                            {
                                // 获取作者告警数统计
                                NotRepairedAuthorEntity notRepairedAuthorEntity = null;
                                if (StringUtils.isNotEmpty(lintDefectEntity.getAuthor()))
                                {
                                    notRepairedAuthorEntity = authorDefectMap.get(lintDefectEntity.getAuthor());
                                    if (notRepairedAuthorEntity == null)
                                    {
                                        notRepairedAuthorEntity = new NotRepairedAuthorEntity();
                                        notRepairedAuthorEntity.setName(lintDefectEntity.getAuthor());
                                        authorDefectMap.put(lintDefectEntity.getAuthor(), notRepairedAuthorEntity);
                                    }
                                }

                                // 统计新增告警数
                                newDefectCount++;

                                // 统计新增严重告警数
                                if (ComConstants.SERIOUS == lintDefectEntity.getSeverity())
                                {
                                    totalNewSerious++;
                                    if (notRepairedAuthorEntity != null)
                                    {
                                        notRepairedAuthorEntity.setSeriousCount(notRepairedAuthorEntity.getSeriousCount() + 1);
                                    }
                                }

                                // 统计新增一般告警数
                                else if (ComConstants.NORMAL == lintDefectEntity.getSeverity())
                                {
                                    totalNewNormal++;
                                    if (notRepairedAuthorEntity != null)
                                    {
                                        notRepairedAuthorEntity.setNormalCount(notRepairedAuthorEntity.getNormalCount() + 1);
                                    }
                                }

                                // 统计新增提示告警数
                                else if (ComConstants.PROMPT_IN_DB == lintDefectEntity.getSeverity())
                                {
                                    totalNewPrompt++;
                                    if (notRepairedAuthorEntity != null)
                                    {
                                        notRepairedAuthorEntity.setPromptCount(notRepairedAuthorEntity.getPromptCount() + 1);
                                    }
                                }
                            }
                            else
                            {
                                historyDefectCount++;
                            }
                            defectCount++;
                        }
                    }
                }
            }
        }
        log.info("handle allFileEntityList cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        // 更新状态的文件
        beginTime = System.currentTimeMillis();
        lintDefectRepository.save(needUpdateFileEntityList);
        log.info("t_lint_defect save list cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        // 作者关联告警统计信息按告警数量排序
        List<NotRepairedAuthorEntity> authorDefects = Lists.newArrayList(authorDefectMap.values());
        Collections.sort(authorDefects, (o1, o2) -> Integer.compare(o2.getTotalCount(), o1.getTotalCount()));

        // 保存本次分析的统计情况
        beginTime = System.currentTimeMillis();
        String baseBuildId;
        if (toolBuildStackEntity == null)
        {
            ToolBuildInfoEntity toolBuildINfoEntity = toolBuildInfoRepository.findByTaskIdAndToolName(taskId, toolName);
            baseBuildId = toolBuildINfoEntity != null && StringUtils.isNotEmpty(toolBuildINfoEntity.getDefectBaseBuildId()) ? toolBuildINfoEntity.getDefectBaseBuildId() : "";
        }
        else
        {
            baseBuildId = StringUtils.isNotEmpty(toolBuildStackEntity.getBaseBuildId()) ? toolBuildStackEntity.getBaseBuildId() : "";
        }
        saveStatisticInfo(taskId, toolName, buildId, fileCount, defectCount, newDefectCount, historyDefectCount, baseBuildId, totalNewSerious, totalNewNormal,
                totalNewPrompt, authorDefects);
        log.info("saveStatisticInfo cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        beginTime = System.currentTimeMillis();
        // 清除已删除的文件列表
        if (CollectionUtils.isNotEmpty(deleteFiles))
        {
            toolBuildInfoDao.updateDeleteFiles(taskId, toolName, deleteFiles, DefectConstants.UpdateToolDeleteFileType.REMOVE);
        }
        log.info("toolBuildInfoDao.updateDeleteFiles cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        beginTime = System.currentTimeMillis();
        // 更新构建告警快照
        buildDefectService.updateBaseBuildDefectsAndClearTemp(taskId, toolName, baseBuildId, buildId, isFullScan, deleteFiles, currentBuildFileRelPaths);
        log.info("buildDefectService.updateBaseBuildDefectsAndClearTemp cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        beginTime = System.currentTimeMillis();
        // 保存质量红线数据
        redLineReportService.saveRedLineData(taskDetailVO, toolName, buildId);
        log.info("redLineReportService.saveRedLineData cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);
    }

    /**
     * 保存本次分析的统计情况
     *
     * @param taskId
     * @param toolName
     * @param buildId
     * @param fileCount
     * @param defectCount
     * @param newDefectCount
     * @param historyDefectCount
     * @param baseBuildId
     */
    private void saveStatisticInfo(long taskId, String toolName, String buildId, int fileCount, int defectCount, int newDefectCount, int historyDefectCount,
                                   String baseBuildId, int totalNewSerious, int totalNewNormal, int totalNewPrompt, List<NotRepairedAuthorEntity> authorDefects)
    {
        int defectChange;
        int fileChange;
        LintStatisticEntity lastLintStatisticEntity = lintStatisticRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, baseBuildId);
        if (lastLintStatisticEntity == null)
        {
            defectChange = defectCount;
            fileChange = fileCount;
        }
        else
        {
            defectChange = defectCount - (lastLintStatisticEntity.getDefectCount() == null ? 0 : lastLintStatisticEntity.getDefectCount());
            fileChange = fileCount - (lastLintStatisticEntity.getFileCount() == null ? 0 : lastLintStatisticEntity.getFileCount());
        }

        LintStatisticEntity lintStatisticEntity = new LintStatisticEntity();
        lintStatisticEntity.setTaskId(taskId);
        lintStatisticEntity.setToolName(toolName);
        lintStatisticEntity.setFileCount(fileCount);
        lintStatisticEntity.setDefectCount(defectCount);
        lintStatisticEntity.setNewDefectCount(newDefectCount);
        lintStatisticEntity.setHistoryDefectCount(historyDefectCount);
        lintStatisticEntity.setDefectChange(defectChange);
        lintStatisticEntity.setFileChange(fileChange);
        lintStatisticEntity.setBuildId(buildId);
        lintStatisticEntity.setTotalNewNormal(totalNewNormal);
        lintStatisticEntity.setTotalNewPrompt(totalNewPrompt);
        lintStatisticEntity.setTotalNewSerious(totalNewSerious);
        lintStatisticEntity.setAuthorStatistic(authorDefects);

        long currentTime = System.currentTimeMillis();
        lintStatisticEntity.setTime(currentTime);
        lintStatisticRepository.save(lintStatisticEntity);

    }

    /**
     * 发送分析记录
     *
     * @param commitDefectVO
     * @param stepFlag
     * @param msg
     */
    private void uploadTaskLog(CommitDefectVO commitDefectVO, int stepFlag, long startTime, long endTime, String msg)
    {
        UploadTaskLogStepVO uploadTaskLogStepVO = new UploadTaskLogStepVO();
        uploadTaskLogStepVO.setTaskId(commitDefectVO.getTaskId());
        uploadTaskLogStepVO.setStreamName(commitDefectVO.getStreamName());
        uploadTaskLogStepVO.setToolName(commitDefectVO.getToolName());
        uploadTaskLogStepVO.setStartTime(startTime);
        uploadTaskLogStepVO.setEndTime(endTime);
        uploadTaskLogStepVO.setFlag(stepFlag);
        uploadTaskLogStepVO.setMsg(msg);
        uploadTaskLogStepVO.setStepNum(ComConstants.Step4MutliTool.COMMIT.value());
        uploadTaskLogStepVO.setPipelineBuildId(commitDefectVO.getBuildId());
        uploadTaskLogStepVO.setTriggerFrom(commitDefectVO.getTriggerFrom());
        serviceReportTaskLogRestResource.uploadTaskLog(uploadTaskLogStepVO);
    }

}
