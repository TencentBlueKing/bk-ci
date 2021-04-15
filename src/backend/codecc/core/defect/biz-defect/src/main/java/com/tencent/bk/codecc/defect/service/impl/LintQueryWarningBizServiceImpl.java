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
import com.tencent.bk.codecc.defect.dao.ToolMetaCacheServiceImpl;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.BuildDefectEntity;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.StatisticEntity;
import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.defect.vo.common.*;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.GsonUtils;
import com.tencent.devops.common.util.ListSortUtil;
import com.tencent.devops.common.util.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
    private ToolMetaCacheServiceImpl toolMetaCache;
    @Autowired
    private NewDefectJudgeService newDefectJudgeService;
    @Autowired
    private BuildDefectRepository buildDefectRepository;
    @Autowired
    private LintStatisticRepository lintStatisticRepository;

    @Override
    public int getSubmitStepNum()
    {
        return ComConstants.Step4MutliTool.COMMIT.value();
    }

    @Override
    public CommonDefectQueryRspVO processQueryWarningRequest(long taskId, DefectQueryReqVO queryWarningReq, int pageNum, int pageSize, String sortField, Sort.Direction sortType)
    {
        log.info("query task[{}] defect list by {}", taskId, queryWarningReq);
        //获取任务信息
        CodeCCResult<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskInfoResult.getData();

        String toolName = queryWarningReq.getToolName();
        String buildId = queryWarningReq.getBuildId();

        // 获取相同包id下的规则集合
        Set<String> pkgChecker = multitoolCheckerService.queryPkgRealCheckers(queryWarningReq.getPkgId(), toolName, taskDetailVO);

        // 获取某个构建id下的告警id
        Set<String> defectIdSet = getDefectIdsByBuildId(taskId, toolName, buildId);

        // 获取新旧告警判断时间
        long newDefectJudgeTime = getNewDefectJudgeTime(taskId, toolName, queryWarningReq.getDefectType(), taskDetailVO);

        Set<String> condStatusList = queryWarningReq.getStatus();
        if (CollectionUtils.isEmpty(condStatusList))
        {
            condStatusList = new HashSet<>(1);
            condStatusList.add(String.valueOf(ComConstants.DefectStatus.NEW.value()));
            queryWarningReq.setStatus(condStatusList);
        }

        LintDefectQueryRspVO lintDefectQueryRsp = new LintDefectQueryRspVO();
        lintDefectQueryRsp.setNewDefectJudgeTime(newDefectJudgeTime);

        // 按文件聚类
        String clusterType = queryWarningReq.getClusterType();
        if (StringUtils.isNotEmpty(clusterType) && ComConstants.ClusterType.file.name().equalsIgnoreCase(clusterType))
        {
            Page<LintFileVO> pageResult = lintDefectV2Dao.findDefectFilePageByCondition(taskId, queryWarningReq, defectIdSet, pkgChecker,
                    newDefectJudgeTime, pageNum, pageSize, sortField, sortType);
            lintDefectQueryRsp.setFileList(pageResult);
        }
        // 按问题聚类
        else
        {
            Map<String, Boolean> filedMap = getDefectBaseFieldMap();

            Page<LintDefectV2Entity> result = lintDefectV2Dao.findDefectPageByCondition(taskId, queryWarningReq, defectIdSet, pkgChecker,
                    newDefectJudgeTime, filedMap, pageNum, pageSize, sortField, sortType);

            List<LintDefectVO> defectVOList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(result.getRecords()))
            {
                StatisticEntity statisticEntity = lintStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);
                defectVOList = result.getRecords().stream().map(defectV2Entity ->
                {
                    LintDefectVO defectVO = new LintDefectVO();
                    BeanUtils.copyProperties(defectV2Entity, defectVO);
                    defectVO.setMark(convertMarkStatus(defectV2Entity.getMark(), defectV2Entity.getMarkTime(), statisticEntity));
                    defectVO.setSeverity(defectVO.getSeverity() == ComConstants.PROMPT_IN_DB ? ComConstants.PROMPT : defectVO.getSeverity());
                    return defectVO;
                }).collect(Collectors.toList());
            }
            Page<LintDefectVO> pageResult = new Page<>(result.getCount(), result.getPage(), result.getPageSize(), result.getTotalPages(), defectVOList);
            lintDefectQueryRsp.setDefectList(pageResult);
        }

        return lintDefectQueryRsp;
    }

    @Override
    public CommonDefectDetailQueryRspVO processQueryWarningDetailRequest(long taskId, String userId, CommonDefectDetailQueryReqVO queryWarningDetailReq,
                                                                         String sortField, Sort.Direction sortType)
    {
        log.info("query task{} defects by {}", taskId, GsonUtils.toJson(queryWarningDetailReq));
        if (!(queryWarningDetailReq instanceof LintDefectDetailQueryReqVO))
        {
            log.error("input param class type incorrect!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"queryWarningDetailReq"}, null);
        }

        //获取任务信息
        CodeCCResult<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskInfoResult.getData();

        LintDefectDetailQueryReqVO lintDefectQueryReqVO = (LintDefectDetailQueryReqVO) queryWarningDetailReq;
        String toolName = lintDefectQueryReqVO.getToolName();

        List<LintDefectV2Entity> defectList = new ArrayList<>();

        // 按问题聚类
        String clusterType = lintDefectQueryReqVO.getClusterType();
        if (StringUtils.isNotEmpty(clusterType) && ComConstants.ClusterType.defect.name().equalsIgnoreCase(clusterType))
        {
            log.info("query defects by entity: {}", lintDefectQueryReqVO.getEntityId());
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
            Set<String> pkgChecker = multitoolCheckerService.queryPkgRealCheckers(lintDefectQueryReqVO.getPkgId(), toolName, taskDetailVO);

            // 获取某个构建id下的告警id
            Set<String> defectIdSet = getDefectIdsByBuildId(taskId, toolName, lintDefectQueryReqVO.getBuildId());

            // 获取新旧告警判断时间
            long newDefectJudgeTime = getNewDefectJudgeTime(taskId, toolName, lintDefectQueryReqVO.getDefectType(), taskDetailVO);

            Map<String, Boolean> filedMap = getDefectBaseFieldMap();
            filedMap.put("rel_path", true);
            filedMap.put("url", true);
            filedMap.put("repo_id", true);
            filedMap.put("revision", true);
            filedMap.put("branch", true);
            filedMap.put("sub_module", true);
            filedMap.put("code_comment", true);

            DefectQueryReqVO queryWarningReq = new DefectQueryReqVO();
            BeanUtils.copyProperties(lintDefectQueryReqVO, queryWarningReq);
            defectList = lintDefectV2Dao.findDefectByCondition(taskId, queryWarningReq, defectIdSet, pkgChecker, newDefectJudgeTime, filedMap);
        }

        LintDefectDetailQueryRspVO lintDefectQueryRspVO = new LintDefectDetailQueryRspVO();
        if (CollectionUtils.isEmpty(defectList))
        {
            log.info("defect not found by condition: {}", lintDefectQueryReqVO);
            return lintDefectQueryRspVO;
        }
        StatisticEntity statisticEntity = lintStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);

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
            defectVO.setMark(convertMarkStatus(defectV2Entity.getMark(), defectV2Entity.getMarkTime(), statisticEntity));
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
        String content = getFileContent(taskId, userId, defectEntity.getUrl(), defectEntity.getRepoId(),
            relPath, defectEntity.getRevision(), defectEntity.getBranch(), defectEntity.getSubModule());
        if (StringUtils.isBlank(content))
        {
            content = EMPTY_FILE_CONTENT_TIPS;
        }
        lintDefectQueryRspVO.setFileContent(content);

        //4.获取文件相对路径
        String relativePath = PathUtils.getRelativePath(defectEntity.getUrl(), defectEntity.getRelPath());
        lintDefectQueryRspVO.setRelativePath(relativePath);

        //5.获取告警规则详情和规则类型
        getCheckerDetailAndType(defectVOList, toolName);

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

    @Nullable
    protected Set<String> getDefectIdsByBuildId(long taskId, String toolName, String buildId)
    {
        Set<String> defectIdSet = null;
        if (StringUtils.isNotEmpty(buildId) && defectCommitSuccess(taskId, toolName, buildId))
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
        return filedMap;
    }

    @Override
    public QueryWarningPageInitRspVO processQueryWarningPageInitRequest(Long taskId, String toolName, Set<String> statusSet)
    {
        long beginTime = System.currentTimeMillis();

        QueryWarningPageInitRspVO rspVO = new QueryWarningPageInitRspVO();

        // 根据状态过滤后获取规则，处理人、文件路径
        List<LintFileVO> fileInfos = lintDefectV2Dao.getCheckerAuthorPathForPageInit(taskId, toolName, statusSet);
        log.info("get file info size is: {}, task id: {}, tool name: {}", fileInfos.size(), taskId, toolName);
        Set<String> authors = new HashSet<>();
        Set<String> checkerList = new HashSet<>();
        Set<String> defectPaths = new TreeSet<>();
        if (CollectionUtils.isNotEmpty(fileInfos))
        {
            fileInfos.forEach(fileInfo ->
            {
                // 设置作者
                if (CollectionUtils.isNotEmpty(fileInfo.getAuthorList()))
                {
                    Set<String> authorSet = fileInfo.getAuthorList().stream().filter(author -> StringUtils.isNotEmpty(author)).collect(Collectors.toSet());
                    authors.addAll(authorSet);
                }

                // 设置规则
                if (CollectionUtils.isNotEmpty(fileInfo.getCheckerList()))
                {
                    checkerList.addAll(fileInfo.getCheckerList());
                }

                // 获取所有警告文件的相对路径
                String relativePath = PathUtils.getRelativePath(fileInfo.getUrl(), fileInfo.getRelPath());
                if (StringUtils.isNotBlank(relativePath))
                {
                    defectPaths.add(relativePath);
                }
                else
                {
                    defectPaths.add(fileInfo.getFilePath());
                }
            });
        }

        // 处理文件树
        TreeService treeService = treeServiceBizServiceFactory.createBizService(toolName, ComConstants.BusinessType.TREE_SERVICE.value(), TreeService.class);
        TreeNodeVO treeNode = treeService.getTreeNode(taskId, defectPaths);
        rspVO.setFilePathTree(treeNode);

        rspVO.setAuthorList(authors);
        rspVO.setCheckerList(handleCheckerList(toolName, checkerList));

        log.info("======================getCheckerAuthorPathForPageInit cost: {}", System.currentTimeMillis() - beginTime);
        return rspVO;
    }

    @Override
    public QueryWarningPageInitRspVO pageInit(long taskId, DefectQueryReqVO defectQueryReqVO)
    {
        log.info("======================begin pageInit {}, taskId: {}, reqVo: {}", defectQueryReqVO.getStatisticType(), taskId, GsonUtils.toJson(defectQueryReqVO));
        QueryWarningPageInitRspVO rspVO = new QueryWarningPageInitRspVO();
        String toolName = defectQueryReqVO.getToolName();

        // 获取任务信息
        CodeCCResult<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskInfoResult.getData();
        long newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, toolName, taskDetailVO);
        rspVO.setNewDefectJudgeTime(newDefectJudgeTime);

        // 获取相同包id下的规则集合
        Set<String> pkgChecker = multitoolCheckerService.queryPkgRealCheckers(defectQueryReqVO.getPkgId(), toolName, taskDetailVO);

        Set<String> defectIdSet = getDefectIdsByBuildId(taskId, toolName, defectQueryReqVO.getBuildId());

        defectQueryReqVO.setSeverity(null);
        defectQueryReqVO.setDefectType(null);

        // 默认查询待修复的告警
        Set<String> condStatusList = defectQueryReqVO.getStatus();
        if (CollectionUtils.isEmpty(condStatusList))
        {
            condStatusList = new HashSet<>(1);
            condStatusList.add(String.valueOf(ComConstants.DefectStatus.NEW.value()));
            defectQueryReqVO.setStatus(condStatusList);
        }

        long beginTime = System.currentTimeMillis();
        // 1.根据规则、处理人、快照、路径、日期过滤后计算各状态告警数
        if (ComConstants.StatisticType.STATUS.name().equalsIgnoreCase(defectQueryReqVO.getStatisticType()))
        {
            statisticByStatus(taskId, defectQueryReqVO, pkgChecker, defectIdSet, rspVO);
        }
        // 2.根据规则、处理人、快照、路径、日期、状态过滤后计算: 各严重级别告警数
        else if (ComConstants.StatisticType.SEVERITY.name().equalsIgnoreCase(defectQueryReqVO.getStatisticType()))
        {
            statisticBySeverity(taskId, defectQueryReqVO, pkgChecker, defectIdSet, rspVO);
        }
        // 3.根据规则、处理人、快照、路径、日期、状态过滤后计算: 新老告警数
        else if (ComConstants.StatisticType.DEFECT_TYPE.name().equalsIgnoreCase(defectQueryReqVO.getStatisticType()))
        {
            statisticByDefectType(taskId, defectQueryReqVO, pkgChecker, defectIdSet, newDefectJudgeTime, rspVO);
        }
        else
        {
            log.error("StatisticType is invalid. {}", GsonUtils.toJson(defectQueryReqVO));
        }
        log.info("======================pageInit {} cost: {}", defectQueryReqVO.getStatisticType(), System.currentTimeMillis() - beginTime);
        return rspVO;
    }

    protected void statisticBySeverity(long taskId, DefectQueryReqVO defectQueryReqVO, Set<String> pkgChecker, Set<String> defectIdSet, QueryWarningPageInitRspVO rspVO)
    {
        List<LintDefectGroupStatisticVO> groups = lintDefectV2Dao.statisticBySeverity(taskId, defectQueryReqVO, defectIdSet, pkgChecker);
        groups.forEach(it ->
        {
            if (ComConstants.SERIOUS == it.getSeverity())
            {
                rspVO.setSeriousCount(it.getDefectCount());
            }
            else if (ComConstants.NORMAL == it.getSeverity())
            {
                rspVO.setNormalCount(it.getDefectCount());
            }
            else if (ComConstants.PROMPT_IN_DB == it.getSeverity())
            {
                rspVO.setPromptCount(it.getDefectCount());
            }
        });
    }

    protected void statisticByDefectType(long taskId, DefectQueryReqVO defectQueryReqVO, Set<String> pkgChecker, Set<String> defectIdSet, long newDefectJudgeTime, QueryWarningPageInitRspVO rspVO)
    {
        int newDefectCount = 0;
        int historyDefectCount = 0;

        List<LintDefectGroupStatisticVO> groups = lintDefectV2Dao.statisticByDefectType(taskId, defectQueryReqVO, defectIdSet, pkgChecker);

        for (LintDefectGroupStatisticVO group : groups)
        {
            // 按新告警、历史告警统计
            long defectCreateTime = DateTimeUtils.getThirteenTimestamp(group.getLineUpdateTime());
            if (defectCreateTime < newDefectJudgeTime)
            {
                historyDefectCount += group.getDefectCount();
            }
            else
            {
                newDefectCount += group.getDefectCount();
            }
        }
        rspVO.setNewCount(newDefectCount);
        rspVO.setHistoryCount(historyDefectCount);
    }

    protected void statisticByStatus(long taskId, DefectQueryReqVO defectQueryReqVO, Set<String> pkgChecker, Set<String> defectIdSet, QueryWarningPageInitRspVO rspVO)
    {
        Set<String> condStatusSet = defectQueryReqVO.getStatus();

        // 只需要查状态为待修复，已修复，已忽略的告警
        Set<String> needQueryStatusSet = Sets.newHashSet(
                String.valueOf(DefectStatus.NEW.value()),
                String.valueOf(DefectStatus.NEW.value() | DefectStatus.FIXED.value()),
                String.valueOf(DefectStatus.NEW.value() | DefectStatus.IGNORE.value()));
        defectQueryReqVO.setStatus(needQueryStatusSet);
        List<LintDefectGroupStatisticVO> groups = lintDefectV2Dao.statisticByStatus(taskId, defectQueryReqVO, defectIdSet, pkgChecker);
        groups.forEach(it ->
        {
            if (it.getStatus() == DefectStatus.NEW.value())
            {
                rspVO.setExistCount(it.getDefectCount());
            }
            else if (it.getStatus() == (DefectStatus.NEW.value() | DefectStatus.FIXED.value()))
            {
                rspVO.setFixCount(it.getDefectCount());
            }
            else if (it.getStatus() == (DefectStatus.NEW.value() | DefectStatus.IGNORE.value()))
            {
                rspVO.setIgnoreCount(it.getDefectCount());
            }
        });
        defectQueryReqVO.setStatus(condStatusSet);
    }

    /**
     * 获取规则类型
     *
     * @param toolName
     * @return
     */
    private List<CheckerCustomVO> handleCheckerList(String toolName, Set<String> checkerList)
    {
        if (CollectionUtils.isEmpty(checkerList))
        {
            return new ArrayList<>();
        }
        // 获取工具对应的所有警告类型 [初始化新增时一定要检查规则名称是否重复]
        Map<String, CheckerDetailVO> checkerDetailVOMap = multitoolCheckerService.queryAllChecker(toolName);

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


    private void getCheckerDetailAndType(List<LintDefectVO> lintDefectVOList, String toolName)
    {
        if (CollectionUtils.isNotEmpty(lintDefectVOList))
        {
            String pattern = toolMetaCache.getToolPattern(toolName);
            Map<String, CheckerDetailVO> checkers = multitoolCheckerService.queryAllChecker(toolName);

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


