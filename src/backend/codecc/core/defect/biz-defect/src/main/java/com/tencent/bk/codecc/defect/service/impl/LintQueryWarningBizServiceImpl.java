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

import static com.tencent.devops.common.constant.ComConstants.MASK_STATUS;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.BuildDefectEntity;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerPropsEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.CheckerCustomVO;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.CodeCommentVO;
import com.tencent.bk.codecc.defect.vo.LintDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.LintDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.LintDefectGroupStatisticVO;
import com.tencent.bk.codecc.defect.vo.LintDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.LintDefectVO;
import com.tencent.bk.codecc.defect.vo.LintFileVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectRspVO;
import com.tencent.bk.codecc.defect.vo.TreeNodeVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.GsonUtils;
import com.tencent.devops.common.util.ListSortUtil;
import com.tencent.devops.common.util.PathUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Lint类工具的告警查询实现
 *
 * @version V1.0
 * @date 2019/5/8
 */
@Service("LINTQueryWarningBizService")
@Slf4j
public class LintQueryWarningBizServiceImpl extends AbstractQueryWarningBizService
{
    @Autowired
    private CheckerService multitoolCheckerService;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private BizServiceFactory<TreeService> treeServiceBizServiceFactory;
    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private ToolMetaCacheService toolMetaCache;
    @Autowired
    private NewDefectJudgeService newDefectJudgeService;
    @Autowired
    private BuildDefectRepository buildDefectRepository;
    @Autowired
    private LintStatisticRepository lintStatisticRepository;
    @Autowired
    private CheckerSetRepository checkerSetRepository;

    private static final int FIXED_VAL_IN_DB = DefectStatus.NEW.value() | DefectStatus.FIXED.value();

    @Override
    public int getSubmitStepNum()
    {
        return ComConstants.Step4MutliTool.COMMIT.value();
    }

    @Override
    public CommonDefectQueryRspVO processQueryWarningRequest(long taskId, DefectQueryReqVO queryWarningReq, int pageNum, int pageSize, String sortField, Sort.Direction sortType)
    {
        log.info("query task[{}] defect list by {}", taskId, queryWarningReq);
        LintDefectQueryRspVO lintDefectQueryRsp = new LintDefectQueryRspVO();

        // get tool name params
        List<String> toolNameSet = ParamUtils.getToolsByDimension(queryWarningReq.getToolName(), queryWarningReq.getDimension(), taskId);

        if (CollectionUtils.isEmpty(toolNameSet)) {
            return lintDefectQueryRsp;
        }

        //获取任务信息
        Result<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskInfoResult.getData();

        log.info("query task defect list by tool:{}, {}", taskId, toolNameSet);

        if (CollectionUtils.isEmpty(toolNameSet)) {
            return lintDefectQueryRsp;
        }

        // 获取相同包id下的规则集合
        Set<String> pkgChecker = multitoolCheckerService.queryPkgRealCheckers(
            queryWarningReq.getPkgId(), toolNameSet, taskDetailVO);

        // 获取规则集的规则集合
        if (queryWarningReq.getCheckerSet() != null) {
            DefectQueryReqVO.CheckerSet queryCheckerSet = queryWarningReq.getCheckerSet();
            CheckerSetEntity checkerSetItem = checkerSetRepository.findFirstByCheckerSetIdAndVersion(
                queryCheckerSet.getCheckerSetId(), queryCheckerSet.getVersion());
            Set<String> allChecker = checkerSetItem.getCheckerProps().stream()
                .filter((it) -> toolNameSet.contains(it.getToolName()))
                .map(CheckerPropsEntity::getCheckerKey).collect(Collectors.toSet());

            String checker = queryWarningReq.getChecker();
            if (StringUtils.isNotEmpty(checker) && allChecker.contains(checker)) {
                pkgChecker.add(checker);
            } else {
                pkgChecker.addAll(allChecker);
            }

            if (CollectionUtils.isEmpty(pkgChecker)) {
                return lintDefectQueryRsp;
            }
        }

        log.info("get checker for task: {}, {}", taskId, pkgChecker.size());

        // 获取某个构建id下的告警id
        Set<String> defectIdSet = getDefectIdsByBuildId(taskId, toolNameSet, queryWarningReq.getBuildId());

        log.info("get defect id for task: {}, {}", taskId, defectIdSet.size());

        // 获取新旧告警判断时间
        long newDefectJudgeTime = getNewDefectJudgeTime(taskId, queryWarningReq.getDefectType(), taskDetailVO);

        log.info("get new defect judge time for task: {}, {}", taskId, newDefectJudgeTime);

        Set<String> condStatusList = queryWarningReq.getStatus();
        if (CollectionUtils.isEmpty(condStatusList))
        {
            condStatusList = new HashSet<>(1);
            condStatusList.add(String.valueOf(DefectStatus.NEW.value()));
            queryWarningReq.setStatus(condStatusList);
        }

        if (condStatusList.contains(String.valueOf(DefectStatus.PATH_MASK.value()))) {
            condStatusList.addAll(MASK_STATUS);
        }

        lintDefectQueryRsp.setNewDefectJudgeTime(newDefectJudgeTime);

        // 按文件聚类
        String clusterType = queryWarningReq.getClusterType();
        if (StringUtils.isNotEmpty(clusterType) && ComConstants.ClusterType.file.name().equalsIgnoreCase(clusterType))
        {
            log.info("get defect group by file for task: {}", taskId);

            Page<LintFileVO> pageResult = lintDefectV2Dao.findDefectFilePageByCondition(
                taskId, queryWarningReq, defectIdSet, pkgChecker,
                newDefectJudgeTime, pageNum, pageSize, sortField, sortType, toolNameSet);
            lintDefectQueryRsp.setFileList(pageResult);
        }
        // 按问题聚类
        else
        {
            Map<String, Boolean> filedMap = getDefectBaseFieldMap();

            Page<LintDefectV2Entity> result = lintDefectV2Dao.findDefectPageByCondition(
                taskId, queryWarningReq, defectIdSet, pkgChecker,
                newDefectJudgeTime, filedMap, pageNum, pageSize, sortField, sortType, toolNameSet);

            log.info("get defect group by problem for task: {}, {}, {}", taskId, result.getCount(), pkgChecker);

            List<LintDefectVO> defectVOList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(result.getRecords()))
            {
                Map<String, Long> lastAnalyzeTimeMap = getLastAnalyzeTimeMap(taskId, toolNameSet);

                defectVOList = result.getRecords().stream().map(defectV2Entity ->
                {
                    LintDefectVO defectVO = new LintDefectVO();
                    BeanUtils.copyProperties(defectV2Entity, defectVO);
                    defectVO.setMark(
                        convertMarkStatus(defectV2Entity.getMark(),
                            defectV2Entity.getMarkTime(),
                            lastAnalyzeTimeMap.get(defectV2Entity.getToolName())));
                    defectVO.setSeverity(defectVO.getSeverity() == ComConstants.PROMPT_IN_DB
                        ? ComConstants.PROMPT : defectVO.getSeverity());
                    //如果是快照查，需要把已修复变更为待修复
                    if(StringUtils.isNotBlank(queryWarningReq.getBuildId())
                            && defectVO.getStatus() == FIXED_VAL_IN_DB){
                        defectVO.setStatus(DefectStatus.NEW.value());
                    }
                    return defectVO;
                }).collect(Collectors.toList());

                log.info("fill defect vo list for task: {}, {}", taskId, result.getRecords().get(0));
            }
            Page<LintDefectVO> pageResult = new Page<>(result.getCount(), result.getPage(),
                result.getPageSize(), result.getTotalPages(), defectVOList);
            lintDefectQueryRsp.setDefectList(pageResult);
        }

        return lintDefectQueryRsp;
    }

    @Override
    public CommonDefectDetailQueryRspVO processQueryWarningDetailRequest(
        long taskId,
        String userId,
        CommonDefectDetailQueryReqVO queryWarningDetailReq,
        String sortField,
        Sort.Direction sortType) {
        log.info("query task{} defects by {}", taskId, GsonUtils.toJson(queryWarningDetailReq));
        if (!(queryWarningDetailReq instanceof LintDefectDetailQueryReqVO))
        {
            log.error("input param class type incorrect!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"queryWarningDetailReq"}, null);
        }

        // get tool name set
        List<String> toolNameSet = ParamUtils.getToolsByDimension(queryWarningDetailReq.getToolName(), queryWarningDetailReq.getDimension(), taskId);

        LintDefectDetailQueryRspVO lintDefectQueryRspVO = new LintDefectDetailQueryRspVO();

        if (CollectionUtils.isEmpty(toolNameSet)) {
            return lintDefectQueryRspVO;
        }

        //获取任务信息
        Result<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskInfoResult.getData();

        LintDefectDetailQueryReqVO lintDefectQueryReqVO = (LintDefectDetailQueryReqVO) queryWarningDetailReq;

        List<LintDefectV2Entity> defectList = new ArrayList<>();

        // 按问题聚类
        String clusterType = lintDefectQueryReqVO.getClusterType();
        if (StringUtils.isNotEmpty(clusterType) && ComConstants.ClusterType.defect.name().equalsIgnoreCase(clusterType))
        {
            LintDefectV2Entity defectEntity = lintDefectV2Repository.findByEntityId(lintDefectQueryReqVO.getEntityId());
            if (defectEntity != null)
            {
                defectList.add(defectEntity);
            }
        }
        // 按文件聚类
        else
        {
            // 获取相同包id下的规则集合
            Set<String> pkgChecker = multitoolCheckerService.queryPkgRealCheckers(lintDefectQueryReqVO.getPkgId(), toolNameSet, taskDetailVO);

            // 获取某个构建id下的告警id
            Set<String> defectIdSet = getDefectIdsByBuildId(taskId, toolNameSet, lintDefectQueryReqVO.getBuildId());

            // 获取新旧告警判断时间
            long newDefectJudgeTime = getNewDefectJudgeTime(taskId, lintDefectQueryReqVO.getDefectType(), taskDetailVO);

            Map<String, Boolean> filedMap = getDefectBaseFieldMap();
            filedMap.put("rel_path", true);
            filedMap.put("url", true);
            filedMap.put("repo_id", true);
            filedMap.put("revision", true);
            filedMap.put("branch", true);
            filedMap.put("sub_module", true);
            filedMap.put("code_comment", true);
            filedMap.put("tool_name", true);

            DefectQueryReqVO queryWarningReq = new DefectQueryReqVO();
            BeanUtils.copyProperties(lintDefectQueryReqVO, queryWarningReq);
            defectList = lintDefectV2Dao.findDefectByCondition(taskId, queryWarningReq, defectIdSet, pkgChecker, newDefectJudgeTime, filedMap, toolNameSet);
        }

        if (CollectionUtils.isEmpty(defectList))
        {
            log.info("defect not found by condition: {}", lintDefectQueryReqVO);
            return lintDefectQueryRspVO;
        }
        Map<String, Long> lastAnalyzeTimeMap = getLastAnalyzeTimeMap(taskId, toolNameSet);

        if (StringUtils.isEmpty(sortField))
        {
            sortField = "lineNum";
        }
        if (null == sortType)
        {
            sortType = Sort.Direction.ASC;
        }
        ListSortUtil.sort(defectList, sortField, sortType.name());

        List<LintDefectVO> defectVOList = defectList.stream().map(defectV2Entity ->
        {
            LintDefectVO defectVO = new LintDefectVO();
            BeanUtils.copyProperties(defectV2Entity, defectVO);
            defectVO.setMark(convertMarkStatus(defectV2Entity.getMark(), defectV2Entity.getMarkTime(), lastAnalyzeTimeMap.get(defectV2Entity.getToolName())));
            //设置告警评论
            if (null != defectV2Entity.getCodeComment() && CollectionUtils.isNotEmpty(defectV2Entity.getCodeComment().getCommentList()))
            {
                CodeCommentVO codeCommentVO = convertCodeComment(defectV2Entity.getCodeComment());
                defectVO.setCodeComment(codeCommentVO);
            }
            return defectVO;
        }).collect(Collectors.toList());

        LintDefectV2Entity defectEntity = defectList.get(0);
        // 赋值告警详情详细信息，包括文件内容，文件路径等
        String relPath = StringUtils.isEmpty(defectEntity.getRelPath()) ? defectEntity.getFilePath().substring(22) : defectEntity.getRelPath();
        String content = getFileContent(taskId,
                taskDetailVO.getProjectId(),
                userId,
                defectEntity.getUrl(),
                defectEntity.getRepoId(),
                relPath,
                defectEntity.getRevision(),
                defectEntity.getBranch(),
                defectEntity.getSubModule());
        if (StringUtils.isBlank(content))
        {
            content = EMPTY_FILE_CONTENT_TIPS;
        }
        lintDefectQueryRspVO.setFileContent(content);

        //4.获取文件相对路径
        String relativePath = PathUtils.getRelativePath(defectEntity.getUrl(), defectEntity.getRelPath());
        lintDefectQueryRspVO.setRelativePath(relativePath);

        //5.获取告警规则详情和规则类型
        getCheckerDetailAndType(defectVOList, toolNameSet, queryWarningDetailReq.getPattern());

        String filePath = defectEntity.getFilePath();

        //获取文件的url
        String url = PathUtils.getFileUrl(defectEntity.getUrl(), defectEntity.getBranch(), defectEntity.getRelPath());
        lintDefectQueryRspVO.setFilePath(StringUtils.isEmpty(url) ? filePath : url);

        lintDefectQueryRspVO.setFileName(defectEntity.getFileName());
        lintDefectQueryRspVO.setLintDefectList(defectVOList);

        return lintDefectQueryRspVO;
    }

    protected long getNewDefectJudgeTime(long taskId, String toolName, Set<String> conditionDefectType, TaskDetailVO taskDetailVO)
    {
        long newDefectJudgeTime = 0;
        if (CollectionUtils.isNotEmpty(conditionDefectType)
                && !conditionDefectType.containsAll(Sets.newHashSet(ComConstants.DefectType.NEW.stringValue(), ComConstants.DefectType.HISTORY.stringValue())))
        {
            // 查询新老告警判定时间
            newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, toolName, taskDetailVO);
        }
        return newDefectJudgeTime;
    }

    protected long getNewDefectJudgeTime(long taskId, Set<String> conditionDefectType, TaskDetailVO taskDetailVO)
    {
        long newDefectJudgeTime = 0;
        if (CollectionUtils.isNotEmpty(conditionDefectType)
            && !conditionDefectType.containsAll(Sets.newHashSet(ComConstants.DefectType.NEW.stringValue(), ComConstants.DefectType.HISTORY.stringValue())))
        {
            // 查询新老告警判定时间
            newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, taskDetailVO);
        }
        return newDefectJudgeTime;
    }

    @Nullable
    protected Set<String> getDefectIdsByBuildId(long taskId, String toolName, String buildId)
    {
        Set<String> defectIdSet = null;
        if (StringUtils.isNotEmpty(buildId) && taskLogService.defectCommitSuccess(taskId, Lists.newArrayList(toolName), buildId, getSubmitStepNum()).get(toolName))
        {
            defectIdSet = new HashSet<>();
            List<BuildDefectEntity> buildFiles = buildDefectRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
            if (CollectionUtils.isNotEmpty(buildFiles))
            {
                for (BuildDefectEntity buildDefectEntity : buildFiles)
                {
                    defectIdSet.addAll(buildDefectEntity.getFileDefectIds());
                }
            }
        }
        return defectIdSet;
    }

    @Nullable
    protected Set<String> getDefectIdsByBuildId(long taskId, List<String> toolNameSet, String buildId)
    {
        Set<String> defectIdSet = new HashSet<>();
        if (StringUtils.isNotEmpty(buildId))
        {
            Map<String, Boolean> commitResult = taskLogService.defectCommitSuccess(taskId, toolNameSet, buildId, getSubmitStepNum());
            List<String> successTools = commitResult.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList());

            List<BuildDefectEntity> buildFiles = buildDefectRepository.findByTaskIdAndToolNameInAndBuildId(taskId, successTools, buildId);
            if (CollectionUtils.isNotEmpty(buildFiles))
            {
                for (BuildDefectEntity buildDefectEntity : buildFiles)
                {
                    defectIdSet.addAll(buildDefectEntity.getFileDefectIds());
                }
            }
        }
        return defectIdSet;
    }

    @NotNull
    protected Map<String, Boolean> getDefectBaseFieldMap()
    {
        Map<String, Boolean> filedMap = new HashMap<>();
        filedMap.put("_id", true);
        filedMap.put("id", true);
        filedMap.put("file_name", true);
        filedMap.put("line_num", true);
        filedMap.put("file_path", true);
        filedMap.put("checker", true);
        filedMap.put("message", true);
        filedMap.put("author", true);
        filedMap.put("severity", true);
        filedMap.put("line_update_time", true);
        filedMap.put("create_time", true);
        filedMap.put("create_build_number", true);
        filedMap.put("status", true);
        filedMap.put("mark", true);
        filedMap.put("mark_time", true);
        filedMap.put("fixed_time", true);
        filedMap.put("ignore_time", true);
        filedMap.put("tool_name", true);
        return filedMap;
    }

    @Override
    public QueryWarningPageInitRspVO processQueryWarningPageInitRequest(Long taskId, String toolName, String dimension,
            Set<String> statusSet, String checkerSet, String buildId) {
        long beginTime = System.currentTimeMillis();

        QueryWarningPageInitRspVO rspVO = new QueryWarningPageInitRspVO();

        List<String> toolNameSet = ParamUtils.getToolsByDimension(toolName, dimension, taskId);

        if (CollectionUtils.isEmpty(toolNameSet)) {
            return rspVO;
        }

        // 快照查补偿处理
        if (StringUtils.isNotEmpty(buildId) && CollectionUtils.isNotEmpty(statusSet)) {
            String newStatusStr = String.valueOf(DefectStatus.NEW.value());
            String fixedStatusStr = String.valueOf(DefectStatus.FIXED.value());

            if (statusSet.contains(newStatusStr)) {
                statusSet.add(newStatusStr);
                statusSet.add(fixedStatusStr);
            } else {
                // 快照查，不存在已修复
                statusSet.remove(newStatusStr);
                statusSet.remove(fixedStatusStr);
            }
        }

        // 根据状态过滤后获取规则，处理人、文件路径
        List<LintFileVO> fileInfos = lintDefectV2Dao.getCheckerAuthorPathForPageInit(taskId, toolNameSet, statusSet);
        log.info("get file info size is: {}, task id: {}, tool name: {}", fileInfos.size(), taskId, toolNameSet);
        Set<String> authors = new HashSet<>();
        Set<String> checkerList = new HashSet<>();
        Set<String> defectPaths = new TreeSet<>();
        if (CollectionUtils.isNotEmpty(fileInfos)) {
            fileInfos.forEach(fileInfo ->
            {
                // 设置作者
                if (CollectionUtils.isNotEmpty(fileInfo.getAuthorList())) {
                    Set<String> authorSet = fileInfo.getAuthorList().stream().filter(StringUtils::isNotEmpty)
                            .collect(Collectors.toSet());
                    authors.addAll(authorSet);
                }

                // 设置规则
                if (CollectionUtils.isNotEmpty(fileInfo.getCheckerList())) {
                    checkerList.addAll(fileInfo.getCheckerList());
                }

                // 获取所有警告文件的相对路径
                String relativePath = PathUtils.getRelativePath(fileInfo.getUrl(), fileInfo.getRelPath());
                if (StringUtils.isNotBlank(relativePath)) {
                    defectPaths.add(relativePath);
                } else {
                    defectPaths.add(fileInfo.getFilePath());
                }
            });
        }

        // 处理文件树
        TreeService treeService = treeServiceBizServiceFactory.createBizService(toolNameSet.get(0),
                ComConstants.BusinessType.TREE_SERVICE.value(), TreeService.class);
        TreeNodeVO treeNode = treeService.getTreeNode(taskId, defectPaths);
        rspVO.setFilePathTree(treeNode);

        rspVO.setAuthorList(authors);
        rspVO.setCheckerList(handleCheckerList(toolNameSet, checkerList, checkerSet));

        log.info("======================getCheckerAuthorPathForPageInit cost: {}",
                System.currentTimeMillis() - beginTime);
        return rspVO;
    }

    @Override
    public QueryWarningPageInitRspVO pageInit(long taskId, DefectQueryReqVO defectQueryReqVO) {
        log.info("begin pageInit taskId: {}, {}", taskId, GsonUtils.toJson(defectQueryReqVO));
        QueryWarningPageInitRspVO rspVO = new QueryWarningPageInitRspVO();

        List<String> toolNameSet =
            ParamUtils.getToolsByDimension(defectQueryReqVO.getToolName(), defectQueryReqVO.getDimension(), taskId);

        log.info("begin pageInit with tool name set for task {}, {}", taskId, toolNameSet);

        if (CollectionUtils.isEmpty(toolNameSet)) {
            return rspVO;
        }

        // 获取任务信息
        Result<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskInfoResult.getData();
        long newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, taskDetailVO);
        rspVO.setNewDefectJudgeTime(newDefectJudgeTime);

        // 获取相同包id下的规则集合
        Set<String> pkgChecker = multitoolCheckerService.queryPkgRealCheckers(
            defectQueryReqVO.getPkgId(), toolNameSet, taskDetailVO);

        log.info("pageInit in query pkg real checkers for task {}, {}", taskId, pkgChecker.size());

        // 获取规则集对应的规则
        if (defectQueryReqVO.getCheckerSet() != null) {
            DefectQueryReqVO.CheckerSet queryCheckerSet = defectQueryReqVO.getCheckerSet();
            CheckerSetEntity checkerSetItem = checkerSetRepository.findFirstByCheckerSetIdAndVersion(
                queryCheckerSet.getCheckerSetId(), queryCheckerSet.getVersion());
            pkgChecker.addAll(checkerSetItem.getCheckerProps().stream()
                .filter((it) -> toolNameSet.contains(it.getToolName()))
                .map(CheckerPropsEntity::getCheckerKey).collect(Collectors.toSet()));
            log.info("get checker for task: {}, {}", taskId, pkgChecker.size());
        }

        Set<String> defectIdSet = getDefectIdsByBuildId(taskId, toolNameSet, defectQueryReqVO.getBuildId());

        log.info("pageInit in get defect id for task {}, {}", taskId, defectIdSet.size());

        defectQueryReqVO.setSeverity(null);
        defectQueryReqVO.setDefectType(null);

        // 默认查询待修复的告警
        Set<String> condStatusList = defectQueryReqVO.getStatus();
        if (CollectionUtils.isEmpty(condStatusList))
        {
            condStatusList = new HashSet<>(1);
            condStatusList.add(String.valueOf(DefectStatus.NEW.value()));
            defectQueryReqVO.setStatus(condStatusList);
        }

        long beginTime = System.currentTimeMillis();
        // 1.根据规则、处理人、快照、路径、日期过滤后计算各状态告警数
        if (ComConstants.StatisticType.STATUS.name().equalsIgnoreCase(defectQueryReqVO.getStatisticType()))
        {
            statisticByStatus(taskId, defectQueryReqVO, pkgChecker, defectIdSet, rspVO, toolNameSet);
        }
        // 2.根据规则、处理人、快照、路径、日期、状态过滤后计算: 各严重级别告警数
        else if (ComConstants.StatisticType.SEVERITY.name().equalsIgnoreCase(defectQueryReqVO.getStatisticType()))
        {
            statisticBySeverity(taskId, defectQueryReqVO, pkgChecker, defectIdSet, rspVO, toolNameSet);
        }
        // 3.根据规则、处理人、快照、路径、日期、状态过滤后计算: 新老告警数
        else if (ComConstants.StatisticType.DEFECT_TYPE.name().equalsIgnoreCase(defectQueryReqVO.getStatisticType()))
        {
            statisticByDefectType(taskId, defectQueryReqVO, pkgChecker, defectIdSet, newDefectJudgeTime, rspVO, toolNameSet);
        }
        else
        {
            log.error("StatisticType is invalid. {}", GsonUtils.toJson(defectQueryReqVO));
        }
        log.info("pageInit finish for task {}", taskId);
        return rspVO;
    }

    protected void statisticBySeverity(long taskId, DefectQueryReqVO defectQueryReqVO, Set<String> pkgChecker,
                                       Set<String> defectIdSet, QueryWarningPageInitRspVO rspVO, List<String> toolNameSet)
    {
        List<LintDefectGroupStatisticVO> groups = lintDefectV2Dao.statisticBySeverity(
            taskId, defectQueryReqVO, defectIdSet, pkgChecker, toolNameSet);
        groups.forEach(it ->
        {
            if (ComConstants.SERIOUS == it.getSeverity())
            {
                rspVO.setSeriousCount(rspVO.getSeriousCount() + it.getDefectCount());
            }
            else if (ComConstants.NORMAL == it.getSeverity())
            {
                rspVO.setNormalCount(rspVO.getNormalCount() + it.getDefectCount());
            }
            else if (ComConstants.PROMPT_IN_DB == it.getSeverity() || ComConstants.PROMPT == it.getSeverity())
            {
                rspVO.setPromptCount(rspVO.getPromptCount() + it.getDefectCount());
            }
        });
    }

    protected void statisticByDefectType(long taskId, DefectQueryReqVO defectQueryReqVO, Set<String> pkgChecker, Set<String> defectIdSet, long newDefectJudgeTime, QueryWarningPageInitRspVO rspVO, List<String> toolNameSet)
    {
        int newDefectCount = 0;
        int historyDefectCount = 0;

        List<LintDefectGroupStatisticVO> newGroups = lintDefectV2Dao.statisticByDefectType(taskId, defectQueryReqVO, defectIdSet, pkgChecker, toolNameSet, newDefectJudgeTime, ComConstants.DefectType.NEW.value());

        for (LintDefectGroupStatisticVO group : newGroups)
        {
            newDefectCount += group.getDefectCount();
        }
        rspVO.setNewCount(newDefectCount);


        log.info("statistic by defect type in history count for task: {}", taskId);

        List<LintDefectGroupStatisticVO> hisGroups = lintDefectV2Dao.statisticByDefectType(taskId, defectQueryReqVO, defectIdSet, pkgChecker, toolNameSet, newDefectJudgeTime, ComConstants.DefectType.HISTORY.value());

        for (LintDefectGroupStatisticVO group : hisGroups)
        {
            historyDefectCount += group.getDefectCount();
        }
        rspVO.setHistoryCount(historyDefectCount);
    }

    protected void statisticByStatus(long taskId, DefectQueryReqVO defectQueryReqVO, Set<String> pkgChecker, Set<String> defectIdSet, QueryWarningPageInitRspVO rspVO, List<String> toolNameSet)
    {
        Set<String> condStatusSet = defectQueryReqVO.getStatus();

        // 只需要查状态为待修复，已修复，已忽略的告警
        /*Set<String> needQueryStatusSet = Sets.newHashSet(
                String.valueOf(DefectStatus.NEW.value()),
                String.valueOf(DefectStatus.NEW.value() | DefectStatus.FIXED.value()),
                String.valueOf(DefectStatus.NEW.value() | DefectStatus.IGNORE.value()));
        defectQueryReqVO.setStatus(needQueryStatusSet);*/
        List<LintDefectGroupStatisticVO> groups = lintDefectV2Dao.statisticByStatus(taskId, defectQueryReqVO, defectIdSet, pkgChecker, toolNameSet);
        groups.forEach(it ->
        {
            if (it.getStatus() == DefectStatus.NEW.value())
            {
                rspVO.setExistCount(rspVO.getExistCount() + it.getDefectCount());
            }
            else if (it.getStatus() == (DefectStatus.NEW.value() | DefectStatus.FIXED.value()))
            {
                rspVO.setFixCount(rspVO.getFixCount() + it.getDefectCount());
            }
            else if (it.getStatus() == (DefectStatus.NEW.value() | DefectStatus.IGNORE.value()))
            {
                rspVO.setIgnoreCount(rspVO.getIgnoreCount() + it.getDefectCount());
            }
            else {
                rspVO.setMaskCount(rspVO.getMaskCount() + it.getDefectCount());
            }
        });

        // 若是快照查，则修正统计；快照查已移除"已修复"状态
        if (StringUtils.isNotEmpty(defectQueryReqVO.getBuildId())) {
            // 已忽略、已屏蔽在多分支下是共享的；而待修复与已修复是互斥的
            rspVO.setExistCount(rspVO.getExistCount() + rspVO.getFixCount());
            rspVO.setFixCount(0);
        }

        defectQueryReqVO.setStatus(condStatusSet);
    }

    /**
     * 获取规则类型
     *
     * @param toolNameSet
     * @param checkerSet
     * @return
     */
    private List<CheckerCustomVO> handleCheckerList(List<String> toolNameSet, Set<String> checkerList, String checkerSet)
    {
        if (CollectionUtils.isEmpty(checkerList))
        {
            return new ArrayList<>();
        }
        // 获取工具对应的所有警告类型 [初始化新增时一定要检查规则名称是否重复]
        Map<String, CheckerDetailVO> checkerDetailVOMap = multitoolCheckerService.queryAllChecker(toolNameSet, checkerSet);


        if (StringUtils.isNotEmpty(checkerSet)) {
            Set<String> checkerKeySet = new HashSet<>();
            checkerSetRepository.findByCheckerSetId(checkerSet).forEach(it -> {
                it.getCheckerProps().forEach(props -> {
                    checkerKeySet.add(props.getCheckerKey());
                });
            });
        }

        if (MapUtils.isNotEmpty(checkerDetailVOMap))
        {
            // 一种规则类型有多个告警规则
            List<CheckerCustomVO> checkerCustomList = new ArrayList<>(checkerDetailVOMap.size());
            checkerList.forEach(checker ->
            {
                // 获取告警名称对应的记录
                CheckerDetailVO checkDetail = checkerDetailVOMap.get(checker);
                CheckerCustomVO checkerCustom = new CheckerCustomVO();

                // 告警文件的规则类型不在初始化的规则类型列表中则为'自定义'
                String checkerType = (Objects.isNull(checkDetail) || StringUtils.isBlank(checkDetail.getCheckerType()))
                        ? "自定义" : checkDetail.getCheckerType();
                checkerCustom.setTypeName(checkerType);

                boolean anyMatchType = checkerCustomList.stream().anyMatch(e -> e.getTypeName().equals(checkerType));
                // 构建一种规则类型有多个告警规则
                if (anyMatchType)
                {
                    checkerCustomList.stream().filter(e -> e.getTypeName().equals(checkerType)).findFirst().get().getCheckers().add(checker);
                }
                else
                {
                    List<String> checkerLists = new ArrayList<>();
                    checkerLists.add(checker);
                    checkerCustom.setCheckers(checkerLists);
                    checkerCustomList.add(checkerCustom);
                }
            });

            return checkerCustomList;
        }

        return new ArrayList<>();
    }


    private void getCheckerDetailAndType(List<LintDefectVO> lintDefectVOList, List<String> toolNameSet, String pattern)
    {
        if (CollectionUtils.isNotEmpty(lintDefectVOList))
        {
            Map<String, CheckerDetailVO> checkers = multitoolCheckerService.queryAllChecker(toolNameSet, null);

            lintDefectVOList.forEach(lintDefectVO ->
            {
                String checker = lintDefectVO.getChecker();
                if (checkers.size() > 0)
                {
                    CheckerDetailVO checkerDetail;
                    String checkerDesc;
                    if (ComConstants.ToolPattern.LINT.name().equals(pattern))
                    {
                        checkerDetail = checkers.get(checker);
                        if (null != checkerDetail)
                        {
                            lintDefectVO.setCheckerType(checkerDetail.getCheckerType());
                            checkerDesc = checkerDetail.getCheckerDesc();
                        }
                        else
                        {
                            lintDefectVO.setCheckerType("自定义");
                            checkerDesc = "该规则为自定义规则，暂无规则描述";
                        }
                    }
                    else
                    {
                        checkerDetail = checkers.get(checker);
                        checkerDesc = checkerDetail.getCheckerDesc();
                    }
                    lintDefectVO.setCheckerDetail(checkerDesc);
                }

            });
        }
    }

    @Override
    public ToolDefectRspVO processToolWarningRequest(Long taskId, DefectQueryReqVO queryWarningReq, Integer pageNum,
                                                     Integer pageSize, String sortField, Sort.Direction sortType)
    {
        log.info("query task[{}] defect list by {}", taskId, queryWarningReq);

        // 排序分页
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 100 : pageSize;

        LintDefectQueryRspVO rsp = (LintDefectQueryRspVO) processQueryWarningRequest(taskId, queryWarningReq, pageNum, pageSize, sortField, sortType);
        ToolDefectRspVO toolDefectRspVO = new ToolDefectRspVO();
        BeanUtils.copyProperties(rsp, toolDefectRspVO, "defectList", "lintDefectList", "lintFileList");
        toolDefectRspVO.setTaskId(taskId);
        toolDefectRspVO.setFirstAnalysisSuccessTime(rsp.getNewDefectJudgeTime());
        toolDefectRspVO.setNewDefectJudgeTime(rsp.getNewDefectJudgeTime());
        // 按问题聚类
        String clusterType = queryWarningReq.getClusterType();
        if (StringUtils.isNotEmpty(clusterType) && ComConstants.ClusterType.file.name().equalsIgnoreCase(clusterType))
        {
            toolDefectRspVO.setLintFileList(rsp.getFileList());
        }
        else
        {
            toolDefectRspVO.setLintDefectList(rsp.getDefectList());
        }

        return toolDefectRspVO;
    }
}


