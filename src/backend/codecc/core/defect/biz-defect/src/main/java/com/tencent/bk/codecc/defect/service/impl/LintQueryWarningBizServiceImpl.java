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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.ToolMetaCacheServiceImpl;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.IgnoreCheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.IgnoreCheckerDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectDao;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.PipelineService;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.utils.ConvertUtil;
import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectExtReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import com.tencent.bk.codecc.defect.vo.openapi.CheckerPkgDefectRespVO;
import com.tencent.bk.codecc.defect.vo.openapi.CheckerPkgDefectVO;
import com.tencent.bk.codecc.defect.vo.openapi.TaskDefectVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.GongfengPublicProjVO;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskListVO;
import com.tencent.bk.codecc.task.vo.gongfeng.ProjectStatVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
public class LintQueryWarningBizServiceImpl extends AbstractQueryWarningBizService {
    @Autowired
    private Client client;

    @Autowired
    private CheckerService multitoolCheckerService;

    @Autowired
    private LintDefectDao lintDefectDao;

    @Autowired
    private BizServiceFactory<TreeService> treeServiceBizServiceFactory;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private IgnoreCheckerDao ignoreCheckerDao;

    @Autowired
    private IgnoreCheckerRepository ignoreCheckerRepository;

    @Autowired
    private CheckerRepository checkerRepository;

    @Autowired
    private LintDefectRepository lintDefectRepository;

    @Autowired
    private ToolMetaCacheServiceImpl toolMetaCache;

    @Autowired
    private NewDefectJudgeService newDefectJudgeService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${codecc.gateway.host}")
    private String codeccGateWay;

    @Autowired
    private BuildDefectRepository buildDefectRepository;

    @Autowired
    private LintStatisticRepository lintStatisticRepository;


    /**
     * 获取规则类型
     *
     * @param toolName
     * @return
     */
    private List<CheckerCustomVO> handleCheckerList(String toolName, Set<String> checkerList) {
        if (CollectionUtils.isEmpty(checkerList)) {
            return new ArrayList<>();
        }
        // 获取工具对应的所有警告类型 [初始化新增时一定要检查规则名称是否重复]
        Map<String, CheckerDetailVO> checkerDetailVOMap = multitoolCheckerService.queryAllChecker(toolName);

        if (MapUtils.isNotEmpty(checkerDetailVOMap)) {
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

                boolean anyMatchType = checkerCustomList.stream()
                        .anyMatch(e -> e.getTypeName().equals(checkerType));
                // 构建一种规则类型有多个告警规则
                if (anyMatchType) {
                    checkerCustomList.stream()
                            .filter(e -> e.getTypeName().equals(checkerType))
                            .findFirst()
                            .get()
                            .getCheckers()
                            .add(checker);
                } else {
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


    @Override
    public CommonDefectQueryRspVO processQueryWarningRequest(long taskId, DefectQueryReqVO queryWarningReq, int pageNum, int pageSize, String sortField, Sort.Direction sortType) {
        log.info("query task[{}] defect list by {}", taskId, queryWarningReq);
        //获取任务信息
        CodeCCResult<TaskDetailVO> taskInfoCodeCCResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskInfoCodeCCResult.getData();

        //1.获取相同包id下的规则集合
        Set<String> pkgChecker = multitoolCheckerService.queryPkgRealCheckers(queryWarningReq.getPkgId(), queryWarningReq.getToolName(), taskDetailVO);

        //查询数值
        LintDefectQueryRspVO lintFileQueryRspVO = findLintFileByParam(taskId, queryWarningReq, pkgChecker, false,
                pageNum, pageSize, sortField, sortType);

        return lintFileQueryRspVO;
    }

    @Override
    public CommonDefectDetailQueryRspVO processQueryWarningDetailRequest(long taskId, CommonDefectDetailQueryReqVO queryWarningDetailReq,
                                                                         String sortField, Sort.Direction sortType) {
        if (!(queryWarningDetailReq instanceof LintDefectDetailQueryReqVO)) {
            log.error("input param class type incorrect!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"queryWarningDetailReq"}, null);
        }
        LintDefectDetailQueryReqVO lintDefectQueryReqVO = (LintDefectDetailQueryReqVO) queryWarningDetailReq;

        LintDefectDetailQueryRspVO lintDefectQueryRspVO = new LintDefectDetailQueryRspVO();
        String clusterType = lintDefectQueryReqVO.getClusterType();

        LintFileEntity lintFileEntity;
        List<LintDefectVO> lintDefectVOList;
        // 按问题聚类
        if (StringUtils.isNotEmpty(clusterType) && ComConstants.ClusterType.defect.name().equalsIgnoreCase(clusterType)) {
            lintFileEntity = lintDefectRepository.findByEntityIdAndDefectId(lintDefectQueryReqVO.getEntityId(), lintDefectQueryReqVO.getDefectId());

            if (null == lintFileEntity || CollectionUtils.isEmpty(lintFileEntity.getDefectList())) {
                log.info("empty lint file or defect found!");
                return lintDefectQueryRspVO;
            }
            StatisticEntity statisticEntity = lintStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, queryWarningDetailReq.getToolName());
            List<LintDefectEntity> lintDefectEntityList = lintFileEntity.getDefectList();
            lintDefectVOList = lintDefectEntityList.stream()
                    .filter(lintDefectEntity -> StringUtils.isNotBlank(lintDefectEntity.getDefectId()) && lintDefectEntity.getDefectId().equals(lintDefectQueryReqVO.getDefectId()))
                    .map(lintDefectEntity ->
                    {
                        LintDefectVO lintDefectVO = new LintDefectVO();
                        BeanUtils.copyProperties(lintDefectEntity, lintDefectVO);
                        lintDefectVO.setMark(convertMarkStatus(lintDefectVO.getMark(), lintDefectVO.getMarkTime(), statisticEntity));
                        //设置告警评论
                        if (null != lintDefectEntity.getCodeComment() &&
                                CollectionUtils.isNotEmpty(lintDefectEntity.getCodeComment().getCommentList())) {
                            CodeCommentVO codeCommentVO = convertCodeComment(lintDefectEntity.getCodeComment());
                            lintDefectVO.setCodeComment(codeCommentVO);
                        }
                        return lintDefectVO;
                    })
                    .collect(Collectors.toList());

            // 如果是已修复的告警，优先取告警修复时的代码库版本号，分支等信息
            if (CollectionUtils.isNotEmpty(lintDefectVOList)) {
                LintDefectVO defectVO = lintDefectVOList.get(0);
                if ((defectVO.getStatus() & DefectStatus.FIXED.value()) > 0 && StringUtils.isNotEmpty(defectVO.getFixedRepoId())) {
                    lintFileEntity.setRepoId(defectVO.getFixedRepoId());
                    lintFileEntity.setRevision(defectVO.getFixedRevision());
                    lintFileEntity.setBranch(defectVO.getFixedBranch());
                }
            }
        }
        // 按文件聚类
        else {
            //获取任务信息
            CodeCCResult<TaskDetailVO> taskInfoCodeCCResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
            TaskDetailVO taskDetailVO = taskInfoCodeCCResult.getData();

            //1.获取相同包id下的规则集合
            Set<String> pkgChecker = multitoolCheckerService.queryPkgRealCheckers(lintDefectQueryReqVO.getPkgId(), queryWarningDetailReq.getToolName(), taskDetailVO);

            //2. lint类文件查询结果
            lintFileEntity = lintDefectDao.findDefectByParam(lintDefectQueryReqVO.getEntityId(), lintDefectQueryReqVO.getChecker(), lintDefectQueryReqVO.getAuthor());

            if (null == lintFileEntity) {
                log.info("empty lint file found!");
                return lintDefectQueryRspVO;
            }

            StatisticEntity statisticEntity = lintStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, queryWarningDetailReq.getToolName());

            // 按过滤条件过滤告警
            List<LintFileEntity> fileInfoEntityList = Lists.newArrayList(lintFileEntity);
            DefectQueryReqVO queryWarningReq = new DefectQueryReqVO();
            BeanUtils.copyProperties(lintDefectQueryReqVO, queryWarningReq);
            queryWarningReq.setPkgChecker(pkgChecker);
            filterDefectByCondition(taskId, fileInfoEntityList, queryWarningReq, new LintDefectQueryRspVO());

            if (CollectionUtils.isEmpty(fileInfoEntityList)) {
                log.info("empty lint file found after filterDefectByCondition!");
                return lintDefectQueryRspVO;
            }

            //根据字段进行排序
            lintDefectVOList = fileInfoEntityList.get(0).getDefectList().stream()
                    .sorted((o1, o2) -> sortDefectByField(o1, o2, sortField, null == sortType ? "ASC" : sortType.name()))
                    .map(lintDefectEntity ->
                    {
                        LintDefectVO lintDefectVO = new LintDefectVO();
                        BeanUtils.copyProperties(lintDefectEntity, lintDefectVO);
                        lintDefectVO.setMark(convertMarkStatus(lintDefectVO.getMark(), lintDefectVO.getMarkTime(), statisticEntity));
                        //设置告警评论
                        if (null != lintDefectEntity.getCodeComment() &&
                                CollectionUtils.isNotEmpty(lintDefectEntity.getCodeComment().getCommentList())) {
                            CodeCommentVO codeCommentVO = convertCodeComment(lintDefectEntity.getCodeComment());
                            lintDefectVO.setCodeComment(codeCommentVO);
                        }
                        return lintDefectVO;
                    })
                    .collect(Collectors.toList());
        }

        String content;
        if (StringUtils.isNotBlank(lintFileEntity.getRelPath())) {
            content = pipelineService.getFileContent(taskId, lintFileEntity.getRepoId(), lintFileEntity.getRelPath(),
                    lintFileEntity.getRevision(), lintFileEntity.getBranch(), lintFileEntity.getSubModule());
        } else if (StringUtils.isNotBlank(lintFileEntity.getFilePath())) {
            content = pipelineService.getFileContent(taskId, lintFileEntity.getRepoId(), lintFileEntity.getFilePath().substring(22),
                    lintFileEntity.getRevision(), lintFileEntity.getBranch(), lintFileEntity.getSubModule());
        } else {
            log.info("rel path and file path is empty! task id: {}, tool name: {}", taskId, queryWarningDetailReq.getToolName());
            content = "无法获取代码信息";
        }

        lintDefectQueryRspVO.setFileContent(content);

        //4.获取文件相对路径
        String relativePath = PathUtils.getRelativePath(lintFileEntity.getUrl(), lintFileEntity.getRelPath());
        lintDefectQueryRspVO.setRelativePath(relativePath);

        //5.获取告警规则详情和规则类型
        getCheckerDetailAndType(lintDefectVOList, queryWarningDetailReq.getToolName());

        String filePath = lintFileEntity.getFilePath();

        //获取文件的url
        String url = PathUtils.getFileUrl(lintFileEntity.getUrl(), lintFileEntity.getRelPath());
        lintDefectQueryRspVO.setFilePath(StringUtils.isEmpty(url) ? filePath : url);

        int fileNameIndex = filePath.lastIndexOf("/");
        if (fileNameIndex == -1) {
            fileNameIndex = filePath.lastIndexOf("\\");
        }
        lintDefectQueryRspVO.setFileName(filePath.substring(fileNameIndex + 1));
        lintDefectQueryRspVO.setLintDefectList(lintDefectVOList);
        return lintDefectQueryRspVO;
    }

    private int sortDefectByField(LintDefectEntity lintDefectEntity1, LintDefectEntity lintDefectEntity2, String sortField, String sortType) {
        try {
            PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor(LintDefectEntity.class, StringUtils.isEmpty(sortField) ? "lineNum" : sortField);
            Method readMethod = propertyDescriptor.getReadMethod();
            switch (sortType) {
                case "ASC":
                    return (int) readMethod.invoke(lintDefectEntity1) - (int) readMethod.invoke(lintDefectEntity2);
                case "DESC":
                    return (int) readMethod.invoke(lintDefectEntity2) - (int) readMethod.invoke(lintDefectEntity1);
                default:
                    return (int) readMethod.invoke(lintDefectEntity1) - (int) readMethod.invoke(lintDefectEntity2);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("invoke read method error! sort field: {}", sortField);
            return 1;
        }
    }

    @Override
    public QueryWarningPageInitRspVO processQueryWarningPageInitRequest(Long taskId, String toolName) {
        List<LintFileEntity> fileInfos = lintDefectDao.findFileInfoList(taskId, toolName);

        log.info("get file info size is: {}, task id: {}, tool name: {}", fileInfos.size(),
                taskId, toolName);
        Set<String> authors = new HashSet<>();
        Set<String> checkerList = new HashSet<>();
        Set<String> defectPaths = new TreeSet<>();
        if (CollectionUtils.isNotEmpty(fileInfos)) {
            fileInfos.forEach(fileInfo ->
            {
                if (ComConstants.FileType.NEW.value() == fileInfo.getStatus()) {
                    // 设置作者
                    if (CollectionUtils.isNotEmpty(fileInfo.getAuthorList())) {
                        Set<String> authorSet = fileInfo.getAuthorList().stream().filter(author -> StringUtils.isNotEmpty(author)).collect(Collectors.toSet());
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
                }
            });
        }

        QueryWarningPageInitRspVO checker = new QueryWarningPageInitRspVO();
        checker.setAuthorList(authors);
        checker.setCheckerList(handleCheckerList(toolName, checkerList));

        // 处理文件树
        TreeService treeService = treeServiceBizServiceFactory.createBizService(toolName, ComConstants.BusinessType.TREE_SERVICE.value(), TreeService.class);
        TreeNodeVO treeNode = treeService.getTreeNode(taskId, defectPaths);
        checker.setFilePathTree(treeNode);

        return checker;
    }

    @Override
    public CheckerPkgDefectVO getPkgDefectList(String toolName, String pkgId, Integer bgId, Long taskId,
                                               Integer pageNum, Integer pageSize, String sortField, Sort.Direction sortType) {
        Set<String> supportTools =
                Sets.newHashSet(Tool.HORUSPY.name(), Tool.WOODPECKER_SENSITIVE.name(), Tool.RIPS.name());
        if (!supportTools.contains(toolName)) {
            log.error("Currently unsupported tool: {}, taskId: {}", toolName, taskId);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"toolName"}, null);
        }
        if (bgId == null && taskId == null) {
            log.error("param is not given: [bgId] or [taskId]");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"bgId or taskId"}, null);
        }

        log.info("query defect tool[{}] pkgId[{}] taskId[{}] bgId[{}]", toolName, pkgId, taskId, bgId);
        // 默认条件：任务为启用状态
        int taskStatus = ComConstants.Status.ENABLE.value();

        CheckerPkgDefectVO checkerPkgDefectVO = new CheckerPkgDefectVO();

        List<CheckerDetailEntity> checkerList = checkerRepository.findByToolName(toolName);
        Set<String> checkerNameSet =
                checkerList.stream().map(CheckerDetailEntity::getCheckerKey).collect(Collectors.toSet());

        List<IgnoreCheckerEntity> ignoreDefaultCheckerEntityList =
                ignoreCheckerDao.queryCloseDefaultCheckers(toolName, checkerNameSet);

        // 3.统计各任务开启默认的规则数
        Set<Long> closeAllDefaultTaskIds = Sets.newHashSet();
        if (CollectionUtils.isNotEmpty(ignoreDefaultCheckerEntityList)) {
            closeAllDefaultTaskIds = ignoreDefaultCheckerEntityList.stream().map(IgnoreCheckerEntity::getTaskId)
                    .collect(Collectors.toSet());
        }

        QueryTaskListReqVO queryTaskListReqVO = new QueryTaskListReqVO();
        // 经沟通啄木鸟接口只允许查询工蜂开源扫描的任务
        queryTaskListReqVO.setCreateFrom(Lists.newArrayList(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()));
        queryTaskListReqVO.setToolName(toolName);
        queryTaskListReqVO.setStatus(taskStatus);
        if (bgId != null && bgId != 0) {
            // 设置排除标识
            queryTaskListReqVO.setIsExcludeTaskIds(Boolean.toString(true));
            queryTaskListReqVO.setBgId(bgId);
            queryTaskListReqVO.setTaskIds(closeAllDefaultTaskIds);
        } else {
            checkerPkgDefectVO.setTaskId(taskId);
            queryTaskListReqVO.setTaskIds(Sets.newHashSet(taskId));
        }
        CodeCCResult<TaskListVO> taskCodeCCResult = client.get(ServiceTaskRestResource.class).getTaskDetailList(queryTaskListReqVO);
        if (taskCodeCCResult.isNotOk() || taskCodeCCResult.getData() == null) {
            log.error("task list is empty! status {}, toolName {}", taskStatus, toolName);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL,
                    new String[]{queryTaskListReqVO.toString()}, null);
        }
        List<TaskDetailVO> enableTasks = taskCodeCCResult.getData().getEnableTasks();
        Set<Long> taskIdSet = Sets.newHashSet();
        Map<Long, String> taskBkNameMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(enableTasks)) {
            enableTasks.forEach(task ->
            {
                long id = task.getTaskId();
                taskIdSet.add(id);
                taskBkNameMap.put(id, task.getProjectId());
            });
        }

        // 排序分页
        sortField = sortField == null ? "taskId" : sortField;
        sortType = sortType == null ? Sort.Direction.ASC : sortType;
        Pageable pageable = getPageable(pageNum, pageSize, sortField, sortType);

        long totalLintFile = 0;
        int totalPages = 0;

        List<LintFileVO> lintFileDetailList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(taskIdSet)) {
            Page<LintFileEntity> lintFilesList =
                    lintDefectRepository.findByTaskIdInAndToolNameIs(taskIdSet, toolName, pageable);

            List<LintFileEntity> lintFiles = lintFilesList.getContent();
            if (CollectionUtils.isNotEmpty(lintFiles)) {
                lintFiles.forEach(lintFileEntity ->
                {
                    if (judgeFilter(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value(), lintFileEntity.getFilePath())) {
                        return;
                    }
                    List<LintDefectEntity> lintDefectEntityList = lintFileEntity.getDefectList();
                    List<LintDefectVO> lintDefectVoList = Lists.newArrayList();
                    if (CollectionUtils.isNotEmpty(lintDefectEntityList)) {
                        long lintFileEntityTaskId = lintFileEntity.getTaskId();
                        String url = "http://" + codeccGateWay + "/codecc/" + taskBkNameMap.get(lintFileEntityTaskId) +
                                "/task/" + lintFileEntityTaskId + "/defect/lint/" + toolName + "/list";
                        lintDefectEntityList.forEach(defectEntity ->
                        {
                            LintDefectVO lintDefectVO = new LintDefectVO();
                            BeanUtils.copyProperties(defectEntity, lintDefectVO);
                            lintDefectVO.setDefectDetailUrl(url);
                            //临时过滤条件 todo 后续要去掉
                            if (("RIPS".equalsIgnoreCase(toolName) && "rips-php-security".equalsIgnoreCase(lintDefectVO.getChecker()))) {
                                return;
                            }
                            // 返回统一严重级别：提示(3 -> 4)
                            if (lintDefectVO.getSeverity() == ComConstants.PROMPT_IN_DB) {
                                lintDefectVO.setSeverity(ComConstants.PROMPT);
                            }
                            // 临时处理脏数据：修复优先
                            int status = lintDefectVO.getStatus();
                            if (status == 7 || status == 11) {
                                lintDefectVO.setStatus(DefectStatus.FIXED.value());
                            }

                            lintDefectVoList.add(lintDefectVO);
                        });
                    }
                    LintFileVO lintFileVO = new LintFileVO();
                    BeanUtils.copyProperties(lintFileEntity, lintFileVO, "defectList");
                    lintFileVO.setDefectList(lintDefectVoList);
                    // 赋值兼容接口
                    if (lintFileVO.getCreateTime() == 0) {
                        lintFileVO.setCreateTime(lintFileEntity.getFileUpdateTime());
                    }
                    // 临时处理脏数据：修复优先
                    if (lintFileVO.getStatus() == 7 || lintFileVO.getStatus() == 11) {
                        lintFileVO.setStatus(DefectStatus.FIXED.value());
                    }

                    String filePath = lintFileEntity.getFilePath();
                    String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
                    lintFileVO.setFileName(fileName);
                    lintFileDetailList.add(lintFileVO);
                });
                totalLintFile = lintFilesList.getTotalElements();
                totalPages = lintFilesList.getTotalPages();
            }
        }
        com.tencent.devops.common.api.pojo.Page<LintFileVO> lintFileList =
                new com.tencent.devops.common.api.pojo.Page<>(totalLintFile, pageable.getPageNumber() + 1,
                        pageable.getPageSize(), totalPages, lintFileDetailList);
        checkerPkgDefectVO.setLintDefectList(lintFileList);
        checkerPkgDefectVO.setToolName(toolName);

        return checkerPkgDefectVO;
    }

    @Override
    public CheckerPkgDefectRespVO processCheckerPkgDefectRequest(String toolName, String pkgId, Integer bgId,
                                                                 Integer deptId, Integer pageNum, Integer pageSize, Sort.Direction sortType) {
        Set<String> supportTools =
                Sets.newHashSet(Tool.HORUSPY.name(), Tool.WOODPECKER_SENSITIVE.name(), Tool.RIPS.name());
        if (!supportTools.contains(toolName)) {
            log.error("Currently unsupported tool: {}", toolName);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"toolName"}, null);
        }
        // 默认条件：任务为启用状态
        int taskStatus = ComConstants.Status.ENABLE.value();

        // 1.获取工具的规则列表
        List<CheckerDetailEntity> checkerList = checkerRepository.findByToolName(toolName);
        Set<String> checkerNameSet = Sets.newHashSet();
        if (CollectionUtils.isNotEmpty(checkerList)) {
            checkerNameSet = checkerList.stream().map(CheckerDetailEntity::getCheckerKey).collect(Collectors.toSet());
        }

        // 分页查询任务列表
        QueryTaskListReqVO queryTaskListReqVO =
                getQueryTaskListReqVO(toolName, bgId, deptId, taskStatus, pageNum, pageSize, sortType);
        queryTaskListReqVO.setSortField("task_id");
        // 经沟通啄木鸟接口只允许查询工蜂开源扫描的任务
        queryTaskListReqVO.setCreateFrom(Lists.newArrayList(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()));

        // 4.调用task Service获取符合条件的任务列表
        CodeCCResult<com.tencent.devops.common.api.pojo.Page<TaskDetailVO>> taskCodeCCResult =
                client.get(ServiceTaskRestResource.class).getTaskDetailPage(queryTaskListReqVO);
        if (taskCodeCCResult.isNotOk() || taskCodeCCResult.getData() == null) {
            log.error("task list is empty! status {}, toolName {}", taskStatus, toolName);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        List<Long> taskIdList = Lists.newArrayList();
        Set<Integer> gfProjectIds = Sets.newHashSet();
        com.tencent.devops.common.api.pojo.Page<TaskDetailVO> resultData = taskCodeCCResult.getData();
        List<TaskDetailVO> enableTasks = resultData.getRecords();
        if (CollectionUtils.isNotEmpty(enableTasks)) {
            for (TaskDetailVO elem : enableTasks) {
                taskIdList.add(elem.getTaskId());
                Integer projectId = elem.getGongfengProjectId();
                if (projectId != null) {
                    gfProjectIds.add(projectId);
                }
            }
        }
        log.info("lintCheckerPkgDefectReq tool:{},taskId: {}, pageNum:{}, pageSize:{}, totalPage{}, count:{}", toolName,
                taskIdList.size(), resultData.getPage(), resultData.getPageSize(), resultData.getTotalPages(),
                resultData.getCount());

        // 5.获取指定任务的工具告警列表
        List<LintFileEntity> defectEntityList = lintDefectRepository.findByTaskIdInAndToolNameIs(taskIdList, toolName);

        // 组装成Map映射
        Map<Long, List<LintDefectEntity>> defectMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(defectEntityList)) {
            defectEntityList.forEach(fileEntity ->
            {
                if (judgeFilter(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value(), fileEntity.getFilePath())) {
                    return;
                }

                long taskId = fileEntity.getTaskId();
                List<LintDefectEntity> defectEntities = defectMap.computeIfAbsent(taskId, lint -> Lists.newArrayList());
                List<LintDefectEntity> fileEntityDefectList = fileEntity.getDefectList();
                if (CollectionUtils.isNotEmpty(fileEntityDefectList)) {
                    //rips要额外做过滤
                    if ("RIPS".equalsIgnoreCase(toolName)) {
                        fileEntityDefectList = fileEntityDefectList.stream().filter(lintDefectEntity ->
                                !"rips-php-security".equalsIgnoreCase(lintDefectEntity.getChecker())
                        ).collect(Collectors.toList());
                    }
                    defectEntities.addAll(fileEntityDefectList);
                }
            });
        }

        // 获取代码语言元数据
        List<MetadataVO> metadataVoList = getCodeLangMetadataVoList();

        Map<String, String> deptInfo =
                (Map<String, String>) redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);

        // 从蓝盾获取代码库
        Map<Integer, GongfengPublicProjVO> gongfengPublicProjVoMap = getGongfengPublicProjVoMap(gfProjectIds);
        // 获取代码库度量数据
        Map<Integer, ProjectStatVO> gongfengStatProjVoMap = getGongfengStatProjVoMap(bgId, gfProjectIds);

        // 获取代码行数
        Map<Long, Long> codeLineCountMap = getCodeLineNumMap(taskIdList, null);

        List<TaskDefectVO> statisticsTask = Lists.newArrayList();
        for (TaskDetailVO taskDetailVO : enableTasks) {
            TaskDefectVO taskDefectVO = new TaskDefectVO();
            BeanUtils.copyProperties(taskDetailVO, taskDefectVO);
            taskDefectVO.setBgName(deptInfo.get(String.valueOf(taskDetailVO.getBgId())));
            taskDefectVO.setDeptName(deptInfo.get(String.valueOf(taskDetailVO.getDeptId())));
            taskDefectVO.setCenterName(deptInfo.get(String.valueOf(taskDetailVO.getCenterId())));
            taskDefectVO.setCodeLang(ConvertUtil.convertCodeLang(taskDetailVO.getCodeLang(), metadataVoList));
            taskDefectVO.setAnalyzeDate("");

            long taskId = taskDetailVO.getTaskId();
            Long codeLineCount = codeLineCountMap.get(taskId);
            taskDefectVO.setCodeLineNum(Integer.valueOf(String.valueOf(codeLineCount == null ? 0 : codeLineCount)));

            // 设置代码库地址
            setGongfengInfo(gongfengPublicProjVoMap, gongfengStatProjVoMap, taskDetailVO, taskDefectVO);

            List<LintDefectEntity> defectList = defectMap.get(taskId);
            if (CollectionUtils.isEmpty(defectList)) {
                statisticsTask.add(taskDefectVO);
                continue;
            }

            // 按严重级别统计告警数
            for (LintDefectEntity defect : defectList) {
                // 过滤不是啄木鸟规则的告警
                if (!checkerNameSet.contains(defect.getChecker())) {
                    continue;
                }

                int defectStatus = defect.getStatus();
                int severity = defect.getSeverity();
                if (DefectStatus.NEW.value() == defectStatus) {
                    taskDefectVO.getExistCount().count(severity);
                } else if ((DefectStatus.FIXED.value() & defectStatus) > 0) {
                    taskDefectVO.getFixedCount().count(severity);
                } else if ((ComConstants.DefectStatus.IGNORE.value() & defectStatus) > 0) {
                    taskDefectVO.getIgnoreCount().count(defect.getIgnoreReasonType());
                } else if ((DefectStatus.CHECKER_MASK.value() & defectStatus) > 0 ||
                        (DefectStatus.PATH_MASK.value() & defectStatus) > 0) {
                    taskDefectVO.getExcludedCount().count(severity);
                }
            }

            statisticsTask.add(taskDefectVO);
        }

        // 数据组装
        com.tencent.devops.common.api.pojo.Page<TaskDefectVO> taskDetailVoPage =
                new com.tencent.devops.common.api.pojo.Page<>(resultData.getCount(), resultData.getPage(),
                        resultData.getPageSize(), resultData.getTotalPages(), statisticsTask);

        CheckerPkgDefectRespVO pkgDefectRespVO = new CheckerPkgDefectRespVO();
        pkgDefectRespVO.setCheckerCount(checkerNameSet.size());
        pkgDefectRespVO.setTaskCount(statisticsTask.size());
        pkgDefectRespVO.setStatisticsTask(taskDetailVoPage);

        return pkgDefectRespVO;
    }

    @Override
    public ToolDefectRspVO processToolWarningRequest(Long taskId, DefectQueryReqVO queryWarningReq, Integer pageNum,
                                                     Integer pageSize, String sortField, Sort.Direction sortType) {
        log.info("query task[{}] defect list by {}", taskId, queryWarningReq);
        String toolName = queryWarningReq.getToolName();

        //获取工具信息
        CodeCCResult<TaskDetailVO> taskInfoCodeCCResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskInfoCodeCCResult.getData();

        // 排序分页
        pageNum = pageNum == null ? 0 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        sortField = sortField == null ? "taskId" : sortField;
        Pageable pageable = getPageable(pageNum, pageSize, sortField, sortType);
        // 1.获取相同包id下的规则集合
        Set<String> pkgChecker =
                multitoolCheckerService.queryPkgRealCheckers(queryWarningReq.getPkgId(), toolName, taskDetailVO);
        log.info("pkg id [{}] checkers count: [{}]", queryWarningReq.getPkgId(), pkgChecker.size());

        // 修复与界面逻辑的区别：不传默认查询所有
        if (CollectionUtils.isEmpty(queryWarningReq.getDefectType())) {
            Set<String> defectTypes = Sets.newHashSet();
            defectTypes.add(String.valueOf(ComConstants.DefectType.NEW.value()));
            defectTypes.add(String.valueOf(ComConstants.DefectType.HISTORY.value()));
            queryWarningReq.setDefectType(defectTypes);
        }
        if (CollectionUtils.isEmpty(queryWarningReq.getSeverity())) {
            Set<String> severity = Sets.newHashSet();
            severity.add(String.valueOf(ComConstants.SERIOUS));
            severity.add(String.valueOf(ComConstants.NORMAL));
            severity.add(String.valueOf(ComConstants.PROMPT));
            queryWarningReq.setSeverity(severity);
        }

        // 查询数值
        LintDefectQueryRspVO lintFileQueryRspVO = findLintFileByParam(taskId, queryWarningReq, pkgChecker, false,
                pageNum, pageSize, sortField, sortType);

        // 转换为视图类
        Page<LintFileVO> lintFileListPage = lintFileQueryRspVO.getFileList();
        long totalElements = lintFileListPage.getTotalElements();
        log.info("tool [{}] lint file entity count: [{}]", toolName, totalElements);

        // 分页
        com.tencent.devops.common.api.pojo.Page<LintFileVO> lintFileVoPage =
                new com.tencent.devops.common.api.pojo.Page<>(totalElements, pageable.getPageNumber() + 1,
                        pageable.getPageSize(), lintFileListPage.getTotalPages(), lintFileListPage.getContent());

        ToolDefectRspVO lintFileRspVO = new ToolDefectRspVO();
        BeanUtils.copyProperties(lintFileQueryRspVO, lintFileRspVO, "lintFileList");
        lintFileRspVO.setTaskId(taskId);
        lintFileRspVO.setToolName(toolName);
        lintFileRspVO.setLintFileList(lintFileVoPage);
        lintFileRspVO.setFirstAnalysisSuccessTime(lintFileQueryRspVO.getNewDefectJudgeTime());
        return lintFileRspVO;
    }


    @NotNull
    private Pageable getPageable(Integer pageNum, Integer pageSize, String sortField, Sort.Direction sortType) {
        Sort pageSort;
        if (StringUtils.isEmpty(sortField) || null == sortType) {
            pageSort = new Sort(Sort.Direction.ASC, "defect_count");
        } else {
            pageSort = new Sort(sortType, sortField);
        }
        //封装分页类
        return new PageRequest(pageNum == null || pageNum - 1 < 0 ? 0 : pageNum - 1,
                pageSize == null || pageSize <= 0 ? 10 : pageSize, pageSort);
    }

    private void getCheckerDetailAndType(List<LintDefectVO> lintDefectVOList, String toolName) {
        if (CollectionUtils.isNotEmpty(lintDefectVOList)) {
            String pattern = toolMetaCache.getToolPattern(toolName);
            Map<String, CheckerDetailVO> checkers = multitoolCheckerService.queryAllChecker(toolName);

            lintDefectVOList.forEach(lintDefectVO ->
            {
                String checker = lintDefectVO.getChecker();
                if (checkers.size() > 0) {
                    CheckerDetailVO checkerDetail;
                    String checkerDesc;
                    if (ComConstants.ToolPattern.LINT.name().equals(pattern)) {
                        checkerDetail = checkers.get(checker);
                        if (null != checkerDetail) {
                            lintDefectVO.setCheckerType(checkerDetail.getCheckerType());
                            checkerDesc = checkerDetail.getCheckerDesc();
                        } else {
                            lintDefectVO.setCheckerType("自定义");
                            checkerDesc = "该规则为自定义规则，暂无规则描述";
                        }
                    } else {
                        checkerDetail = checkers.get(checker);
                        checkerDesc = checkerDetail.getCheckerDesc();
                    }
                    lintDefectVO.setCheckerDetail(checkerDesc);
                }

            });
        }
    }

    /**
     * 根据参数查询lint文件
     *
     * @param taskId
     * @param queryWarningReq
     * @param pkgChecker
     * @param needDefectList
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortType
     * @return
     */
    public LintDefectQueryRspVO findLintFileByParam(long taskId, DefectQueryReqVO queryWarningReq, Set<String> pkgChecker, Boolean needDefectList,
                                                    Integer pageNum, Integer pageSize, String sortField, Sort.Direction sortType) {
        LintDefectQueryRspVO lintFileQueryRsp = new LintDefectQueryRspVO();

        String toolName = queryWarningReq.getToolName();
        Set<String> fileList = queryWarningReq.getFileList();
        String checker = queryWarningReq.getChecker();
        String author = queryWarningReq.getAuthor();

        //查询总的数量，并且过滤计数
        List<LintFileEntity> originalFileInfoEntityList =
                lintDefectDao.findFileListByParams(taskId, toolName, fileList, checker, author);

        queryWarningReq.setPkgChecker(pkgChecker);

        // 按过滤条件过滤告警
        filterDefectByCondition(taskId, originalFileInfoEntityList, queryWarningReq, lintFileQueryRsp);

        // 按问题聚类
        String clusterType = queryWarningReq.getClusterType();
        if (StringUtils.isNotEmpty(clusterType) && ComConstants.ClusterType.defect.name().equalsIgnoreCase(clusterType)) {
            StatisticEntity statisticEntity = lintStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);
            List<LintDefectVO> lintDefectVoList = Lists.newArrayList();
            originalFileInfoEntityList.forEach(lintFileEntity ->
            {
                String filePath = lintFileEntity.getFilePath();
                String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
                List<LintDefectVO> singleFileDefectVOList = lintFileEntity.getDefectList().stream().map(lintDefectEntity ->
                {
                    LintDefectVO lintDefectVO = new LintDefectVO();
                    BeanUtils.copyProperties(lintDefectEntity, lintDefectVO);
                    lintDefectVO.setEntityId(lintFileEntity.getEntityId());
                    lintDefectVO.setFileName(fileName);
                    lintDefectVO.setFilePath(filePath);
                    lintDefectVO.setMark(convertMarkStatus(lintDefectVO.getMark(), lintDefectVO.getMarkTime(), statisticEntity));
                    return lintDefectVO;
                }).collect(Collectors.toList());
                lintDefectVoList.addAll(singleFileDefectVOList);
            });

            lintFileQueryRsp.setTotalCount(lintDefectVoList.size());
            Page<LintDefectVO> defectVOPage;
            if ("fileName".equals(sortField)) {
                defectVOPage = sortByDefectLocationAndPage(pageNum, pageSize, sortField, sortType, lintDefectVoList);
            } else {
                defectVOPage = sortAndPage(pageNum, pageSize, sortField, sortType, lintDefectVoList);
            }

            lintFileQueryRsp.setDefectList(defectVOPage);
        }
        // 按文件聚类
        else {
            List<LintFileVO> lintFilesVOList = originalFileInfoEntityList.stream().map(lintFileEntity ->
            {
                LintFileVO lintFileVO = new LintFileVO();
                BeanUtils.copyProperties(lintFileEntity, lintFileVO);
                String filePath = lintFileEntity.getFilePath();
                String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
                lintFileVO.setFileName(fileName);
                if (!needDefectList) {
                    List<LintDefectVO> singleFileDefectVOList = lintFileEntity.getDefectList().stream().map(lintDefectEntity ->
                    {
                        LintDefectVO lintDefectVO = new LintDefectVO();
                        BeanUtils.copyProperties(lintDefectEntity, lintDefectVO);
                        return lintDefectVO;
                    }).collect(Collectors.toList());
                    lintFileVO.setDefectList(singleFileDefectVOList);
                }
                return lintFileVO;
            }).collect(Collectors.toList());

            lintFileQueryRsp.setTotalCount(lintFilesVOList.size());
            Page<LintFileVO> fileVOPage = sortAndPage(pageNum, pageSize, sortField, sortType, lintFilesVOList);
            lintFileQueryRsp.setFileList(fileVOPage);
        }


        return lintFileQueryRsp;
    }

    /**
     * 根据根据前端传入的条件过滤告警，并分类统计
     *
     * @param taskId
     * @param defectList
     * @param defectQueryReqVO
     * @param defectQueryRspVO
     * @return
     */
    @Override
    public Set<String> filterDefectByCondition(long taskId, List<?> defectList, DefectQueryReqVO defectQueryReqVO, CommonDefectQueryRspVO defectQueryRspVO) {
        String toolName = defectQueryReqVO.getToolName();
        String checker = defectQueryReqVO.getChecker();
        String author = defectQueryReqVO.getAuthor();
        Set<String> conditionDefectType = defectQueryReqVO.getDefectType();
        Set<String> conditionSeverity = defectQueryReqVO.getSeverity();
        String startTime = defectQueryReqVO.getStartCreateTime();
        String endTime = defectQueryReqVO.getEndCreateTime();
        String buildId = defectQueryReqVO.getBuildId();
        Set<String> pkgChecker = defectQueryReqVO.getPkgChecker();
        Set<String> condStatusList = defectQueryReqVO.getStatus();
        if (CollectionUtils.isEmpty(condStatusList)) {
            condStatusList = new HashSet<>(1);
            condStatusList.add(String.valueOf(ComConstants.DefectStatus.NEW.value()));
        }

        //获取任务信息
        CodeCCResult<TaskDetailVO> taskInfoCodeCCResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskInfoCodeCCResult.getData();

        // 查询新老告警判定时间
        long newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, toolName, taskDetailVO);

        // 按构建号筛选
        boolean needBuildIdFilter = false;
        Map<String, Set<String>> buildFileDefectMap = Maps.newHashMap();
        if (StringUtils.isNotEmpty(buildId)) {
            if (defectCommitSuccess(taskId, toolName, buildId)) {
                List<BuildDefectEntity> buildFiles = buildDefectRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
                if (CollectionUtils.isNotEmpty(buildFiles)) {
                    for (BuildDefectEntity buildDefectEntity : buildFiles) {
                        if (buildFileDefectMap.get(buildDefectEntity.getFileRelPath()) == null) {
                            buildFileDefectMap.put(buildDefectEntity.getFileRelPath(), Sets.newHashSet());
                        }
                        if (CollectionUtils.isNotEmpty(buildDefectEntity.getFileDefectIds())) {
                            for (String defectId : buildDefectEntity.getFileDefectIds()) {
                                buildFileDefectMap.get(buildDefectEntity.getFileRelPath()).add(defectId);
                            }
                        }
                    }
                }
            }
            needBuildIdFilter = true;
        }

        //对查询到的原始数据进行过滤，并统计数量
        int existCount = 0;
        int fixCount = 0;
        int ignoreCount = 0;
        int newDefectCount = 0;
        int historyDefectCount = 0;
        int seriousCount = 0;
        int normalCount = 0;
        int promptCount = 0;
        if (CollectionUtils.isNotEmpty(defectList)) {
            Iterator<LintFileEntity> it = (Iterator<LintFileEntity>) defectList.iterator();
            while (it.hasNext()) {
                LintFileEntity lintFileEntity = it.next();

                if (taskDetailVO!=null&&judgeFilter(taskDetailVO.getCreateFrom(), lintFileEntity.getFilePath())) {
                    it.remove();
                    continue;
                }

                Set<Integer> severityList = new TreeSet<>();
                Set<String> authorList = new TreeSet<>();
                Set<String> checkerList = new TreeSet<>();
                int seriousCount4SingleFile = 0;
                int normalCount4SingleFile = 0;
                int promptCount4SingleFile = 0;
                List<LintDefectEntity> lintDefectEntityList = lintFileEntity.getDefectList();
                if (CollectionUtils.isNotEmpty(lintDefectEntityList)) {
                    Set<String> buildFileDefects = buildFileDefectMap.get(lintFileEntity.getRelPath());
                    Iterator<LintDefectEntity> lintDefectIt = lintDefectEntityList.iterator();
                    while (lintDefectIt.hasNext()) {
                        LintDefectEntity lintDefectEntity = lintDefectIt.next();

                        // 按构建号筛选
                        if (needBuildIdFilter
                                && (buildFileDefects == null
                                || !buildFileDefects.contains(lintDefectEntity.getDefectId()))) {
                            lintDefectIt.remove();
                            continue;
                        }

                        // 判断是否匹配告警状态，先收集统计匹配告警状态的规则、处理人、文件路径
                        boolean notMatchStatus = isNotMatchStatus(condStatusList, lintDefectEntity.getStatus());

                        //缺陷类型过滤
                        boolean meetChecker = StringUtils.isNotEmpty(checker) && !checker.equals(lintDefectEntity.getChecker());

                        //判断是否属于查询的规则包中的规则
                        if (CollectionUtils.isNotEmpty(pkgChecker)) {
                            meetChecker = !pkgChecker.contains(lintDefectEntity.getChecker());
                        }

                        // 处理人条件不为空且文件的处理人不包含处理人条件时，判断为true移除，否则false不移除
                        boolean meetAuthor = StringUtils.isNotEmpty(author) && !author.equals(lintDefectEntity.getAuthor());

                        // 不满足缺陷类型、处理人条件的先移除掉
                        if (meetChecker || meetAuthor) {
                            lintDefectIt.remove();
                            continue;
                        }

                        //创建时间过滤
                        Long lineUpdateTime = lintDefectEntity.getLineUpdateTime();
                        if (lineUpdateTime == null) {
                            lineUpdateTime = lintDefectEntity.getCreateTime();
                        }
                        boolean notMatchDateTime = DateTimeUtils.filterDate(startTime, endTime, lineUpdateTime);
                        if (notMatchDateTime) {
                            lintDefectIt.remove();
                            continue;
                        }

                        // 根据状态过滤，注：已修复和其他状态（忽略，路径屏蔽，规则屏蔽）不共存，已忽略状态优先于屏蔽
                        int status = lintDefectEntity.getStatus();
                        if (ComConstants.DefectStatus.NEW.value() == status) {
                            existCount++;
                        } else if ((ComConstants.DefectStatus.FIXED.value() & status) > 0) {
                            fixCount++;
                        } else if ((ComConstants.DefectStatus.IGNORE.value() & status) > 0) {
                            ignoreCount++;
                        }
                        // 严重程度条件不为空且与当前数据的严重程度不匹配时，判断为true移除，否则false不移除
                        if (notMatchStatus) {
                            lintDefectIt.remove();
                            continue;
                        }

                        // 按严重级别过滤，当从数据库中查询来的缺陷严重程度为提示（3）时，需要转换成前端表示提示的数值4
                        int severity = lintDefectEntity.getSeverity();
                        if (severity == ComConstants.PROMPT_IN_DB) {
                            severity = ComConstants.PROMPT;
                            lintDefectEntity.setSeverity(severity);
                        }
                        if (ComConstants.SERIOUS == severity) {
                            seriousCount++;
                        } else if (ComConstants.NORMAL == severity) {
                            normalCount++;
                        } else if (ComConstants.PROMPT == severity) {
                            promptCount++;
                        }

                        // 严重程度条件不为空且与当前数据的严重程度不匹配时移除
                        if (!conditionSeverity.contains(String.valueOf(severity))) {
                            lintDefectIt.remove();
                            continue;
                        }


                        // 按新告警、历史告警筛选
                        long defectCreateTime = DateTimeUtils.getThirteenTimestamp(lineUpdateTime);
                        if (defectCreateTime < newDefectJudgeTime) {
                            historyDefectCount++;
                        } else {
                            newDefectCount++;
                        }

                        // 按新告警、历史告警筛选
                        String defectType = defectCreateTime > newDefectJudgeTime ? ComConstants.DefectType.NEW.stringValue() : ComConstants.DefectType.HISTORY.stringValue();
                        boolean notMatchDefectType = CollectionUtils.isNotEmpty(conditionDefectType) && !conditionDefectType.contains(defectType);
                        if (notMatchDefectType) {
                            lintDefectIt.remove();
                            continue;
                        }

                        // 统计单个文件中经过过滤后的告警
                        if (ComConstants.SERIOUS == severity) {
                            severityList.add(ComConstants.SERIOUS);
                            seriousCount4SingleFile++;
                        } else if (ComConstants.NORMAL == severity) {
                            severityList.add(ComConstants.NORMAL);
                            normalCount4SingleFile++;
                        } else if (ComConstants.PROMPT == severity) {
                            severityList.add(ComConstants.PROMPT);
                            promptCount4SingleFile++;
                        }

                        if (StringUtils.isNotEmpty(lintDefectEntity.getAuthor())) {
                            authorList.add(lintDefectEntity.getAuthor());
                        }

                        if (StringUtils.isNotEmpty(lintDefectEntity.getChecker())) {
                            checkerList.add(lintDefectEntity.getChecker());
                        }
                    }
                }

                // 过滤筛选后告警为0的文件
                if (CollectionUtils.isEmpty(lintDefectEntityList)) {
                    it.remove();
                    continue;
                }

                // 获取相对路径
//                String relativePath = PathUtils.getRelativePath(lintFileEntity.getUrl(), lintFileEntity.getRelPath());
//                lintFileEntity.setRelPath(relativePath);

                lintFileEntity.setSeverityList(severityList);
                lintFileEntity.setAuthorList(authorList);
                lintFileEntity.setCheckerList(checkerList);
                lintFileEntity.setDefectCount(lintDefectEntityList.size());
                // 为了使严重告警多的文件排在前面，所以这里严重、一般、提示的权重设置4、2、1，按数字之和来排序，默认大的数字在上面
                lintFileEntity.setSeverity(seriousCount4SingleFile * 4 + normalCount4SingleFile * 2 + promptCount4SingleFile * 1);
            }
        }

        LintDefectQueryRspVO lintFileQueryRsp = (LintDefectQueryRspVO) defectQueryRspVO;
        lintFileQueryRsp.setNewCount(newDefectCount);
        lintFileQueryRsp.setHistoryCount(historyDefectCount);
        lintFileQueryRsp.setSeriousCount(seriousCount);
        lintFileQueryRsp.setNormalCount(normalCount);
        lintFileQueryRsp.setPromptCount(promptCount);
        lintFileQueryRsp.setExistCount(existCount);
        lintFileQueryRsp.setFixCount(fixCount);
        lintFileQueryRsp.setIgnoreCount(ignoreCount);
        lintFileQueryRsp.setNewDefectJudgeTime(newDefectJudgeTime);
        return null;
    }

    /**
     * 排序并分页
     *
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortType
     * @param defectVOs
     * @param <T>
     * @return
     */
    protected <T> org.springframework.data.domain.Page<LintDefectVO> sortByDefectLocationAndPage(int pageNum, int pageSize, String sortField,
                                                                                                 Sort.Direction sortType, List<LintDefectVO> defectVOs) {
        defectVOs.sort((o1, o2) ->
        {
            String fileName1 = o1.getFileName();
            String fileName2 = o2.getFileName();
            int compareRes = fileName1.compareTo(fileName2);
            if (compareRes == 0) {
                int line1 = o1.getLineNum();
                int line2 = o2.getLineNum();
                compareRes = line1 - line2;
            }

            if (null != sortType && Sort.Direction.DESC.name().equalsIgnoreCase(sortType.name())) {
                return -compareRes;
            } else {
                return compareRes;
            }
        });
        int total = defectVOs.size();
        pageNum = pageNum - 1 < 0 ? 0 : pageNum - 1;
        pageSize = pageSize <= 0 ? 10 : pageSize;
        int subListBeginIdx = pageNum * pageSize;
        int subListEndIdx = subListBeginIdx + pageSize;
        if (subListBeginIdx > total) {
            subListBeginIdx = 0;
        }
        defectVOs = defectVOs.subList(subListBeginIdx, subListEndIdx > total ? total : subListEndIdx);

        //封装分页类
        Pageable pageable = new PageRequest(pageNum, pageSize, new Sort(sortType, sortField));
        return new PageImpl<>(defectVOs, pageable, total);
    }

    @Override
    public int getSubmitStepNum() {
        return ComConstants.Step4MutliTool.COMMIT.value();
    }

    @Override
    public CheckerPkgDefectRespVO processOverallDefectRequest(String toolName, DeptTaskDefectExtReqVO reqVO,
                                                              Integer pageNum, Integer pageSize, Sort.Direction sortType) {
        log.info("processOverallDefectRequest query by: {}", reqVO);
        // 默认条件：任务为启用状态
        int taskStatus = ComConstants.Status.ENABLE.value();
        QueryTaskListReqVO queryTaskListReqVO = new QueryTaskListReqVO();
        BeanUtils.copyProperties(reqVO, queryTaskListReqVO);
        queryTaskListReqVO.setStatus(taskStatus);

        // 是否需要按指定规则列表筛选告警
        List<String> checkerKeyListReq = reqVO.getCheckerKeyList();
        boolean needFilterCheckerKey = CollectionUtils.isNotEmpty(checkerKeyListReq);

        // 4.调用task Service获取符合条件的任务列表
        CodeCCResult<List<TaskDetailVO>> taskCodeCCResult =
                client.get(ServiceTaskRestResource.class).batchGetTaskList(queryTaskListReqVO);
        if (taskCodeCCResult.isNotOk() || taskCodeCCResult.getData() == null) {
            log.error("task list is empty! status {}, toolName {}", taskStatus, toolName);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        Set<Integer> gfProjectIds = Sets.newHashSet();
        List<Long> taskIdList = Lists.newArrayList();
        List<TaskDetailVO> enableTasks = taskCodeCCResult.getData();

        int totalPageNum = 0;
        int total = 0;
        if (CollectionUtils.isNotEmpty(enableTasks)) {
            // 如果传工具，需筛选掉未接入该工具的任务
            if (StringUtils.isNotEmpty(toolName)) {
                enableTasks = enableTasks.stream().filter(taskDetailVO ->
                {
                    String toolNames = taskDetailVO.getToolNames();
                    return StringUtils.isNotBlank(toolNames) && toolNames.contains(toolName);
                }).collect(Collectors.toList());
            }

            if (Sort.Direction.ASC.equals(sortType)) {
                enableTasks.sort((Comparator.comparingLong(TaskDetailVO::getTaskId)));
            } else {
                enableTasks.sort((obj1, obj2) -> Long.compare(obj2.getTaskId(), obj1.getTaskId()));
            }

            totalPageNum = 0;
            total = enableTasks.size();
            pageNum = pageNum - 1 < 0 ? 0 : pageNum - 1;
            pageSize = pageSize <= 0 ? 10 : pageSize;
            if (total > 0) {
                totalPageNum = (total + pageSize - 1) / pageSize;
            }

            int subListBeginIdx = pageNum * pageSize;
            int subListEndIdx = subListBeginIdx + pageSize;
            if (subListBeginIdx > total) {
                subListBeginIdx = 0;
            }
            // 分页任务
            enableTasks = enableTasks.subList(subListBeginIdx, subListEndIdx > total ? total : subListEndIdx);

            for (TaskDetailVO elem : enableTasks) {
                taskIdList.add(elem.getTaskId());
                Integer projectId = elem.getGongfengProjectId();
                if (projectId != null) {
                    gfProjectIds.add(projectId);
                }
            }
        }
        log.info("the number of tasks id satisfying the conditions: {}", taskIdList.size());

        // 5.获取指定任务的工具告警列表
        List<LintFileEntity> defectEntityList = lintDefectDao.findByTaskIdInAndToolNameIs(taskIdList, toolName, null);

        // 组装成Map映射
        Map<Long, List<LintDefectEntity>> defectMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(defectEntityList)) {
            defectEntityList.forEach(fileEntity ->
            {
                if (judgeFilter(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value(),fileEntity.getFilePath())) {
                    return;
                }

                long taskId = fileEntity.getTaskId();
                List<LintDefectEntity> defectEntities = defectMap.computeIfAbsent(taskId, lint -> Lists.newArrayList());
                List<LintDefectEntity> fileEntityDefectList = fileEntity.getDefectList();
                if (CollectionUtils.isNotEmpty(fileEntityDefectList)) {
                    // 按规则列表来筛选告警再统计
                    if (needFilterCheckerKey) {
                        fileEntityDefectList = fileEntityDefectList.stream()
                                .filter(entity -> checkerKeyListReq.contains(entity.getChecker()))
                                .collect(Collectors.toList());
                    }
                    defectEntities.addAll(fileEntityDefectList);
                }
            });
        }

        // 获取代码语言元数据
        List<MetadataVO> metadataVoList = getCodeLangMetadataVoList();

        Map<String, String> deptInfo =
                (Map<String, String>) redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);

        // 从蓝盾获取代码库
        Map<Integer, GongfengPublicProjVO> gongfengPublicProjVoMap = getGongfengPublicProjVoMap(gfProjectIds);

        // 按工具初始化代码语言类型
        List<String> languageList = Lists.newArrayList();
        if (Tool.CHECKSTYLE.name().equals(toolName)) {
            languageList.add("Java");
        } else if (Tool.CPPLINT.name().equals(toolName)) {
            languageList.add("C++");
            languageList.add("C/C++ Header");
        } else if (Tool.OCCHECK.name().equals(toolName)) {
            languageList.add("Objective C");
            languageList.add("Objective C++");
        } else if (Tool.STYLECOP.name().equals(toolName)) {
            languageList.add("C#");
        } else if (Tool.PYLINT.name().equals(toolName)) {
            languageList.add("Python");
        } else if (Tool.ESLINT.name().equals(toolName)) {
            languageList.add("JavaScript");
        } else if (Tool.GOML.name().equals(toolName)) {
            languageList.add("Go");
        }

        // 获取代码行数
        Map<Long, Long> codeLineCountMap = getCodeLineNumMap(taskIdList, languageList);

        List<TaskDefectVO> statisticsTask = Lists.newArrayList();
        for (TaskDetailVO taskDetailVO : enableTasks) {
            long taskId = taskDetailVO.getTaskId();
            TaskDefectVO taskDefectVO = new TaskDefectVO();
            BeanUtils.copyProperties(taskDetailVO, taskDefectVO);
            taskDefectVO.setBgName(deptInfo.get(String.valueOf(taskDetailVO.getBgId())));
            taskDefectVO.setDeptName(deptInfo.get(String.valueOf(taskDetailVO.getDeptId())));
            taskDefectVO.setCenterName(deptInfo.get(String.valueOf(taskDetailVO.getCenterId())));
            taskDefectVO.setCodeLang(ConvertUtil.convertCodeLang(taskDetailVO.getCodeLang(), metadataVoList));
            taskDefectVO.setProjectId(taskDetailVO.getProjectId());
            taskDefectVO.setAnalyzeDate("http://" + codeccGateWay + "/codecc/" + taskDetailVO.getProjectId() + "/task/" +
                    taskId + "/detail");
            Long codeLineCount = codeLineCountMap.get(taskId);
            taskDefectVO.setCodeLineNum(Integer.valueOf(String.valueOf(codeLineCount == null ? 0 : codeLineCount)));

            // 设置代码库地址
            String repoUrl = "";
            Integer gongfengProjectId = taskDetailVO.getGongfengProjectId();
            GongfengPublicProjVO publicProjVO = gongfengPublicProjVoMap.get(gongfengProjectId);
            if (publicProjVO != null) {
                repoUrl = publicProjVO.getHttpUrlToRepo();
            }
            taskDefectVO.setRepoUrl(repoUrl);

            List<LintDefectEntity> defectList = defectMap.get(taskId);
            if (CollectionUtils.isEmpty(defectList)) {
                statisticsTask.add(taskDefectVO);
                continue;
            }

            for (LintDefectEntity defect : defectList) {
                int defectStatus = defect.getStatus();
                int severity = defect.getSeverity();
                if ((DefectStatus.CHECKER_MASK.value() & defectStatus) > 0 ||
                        (DefectStatus.PATH_MASK.value() & defectStatus) > 0) {
                    taskDefectVO.getExcludedCount().count(severity);
                } else if ((DefectStatus.FIXED.value() & defectStatus) > 0) {
                    taskDefectVO.getFixedCount().count(severity);
                } else if ((DefectStatus.NEW.value() & defectStatus) > 0) {
                    taskDefectVO.getExistCount().count(severity);
                }
            }

            statisticsTask.add(taskDefectVO);
        }

        // 数据列表分页
        com.tencent.devops.common.api.pojo.Page<TaskDefectVO> taskDetailVoPage =
                new com.tencent.devops.common.api.pojo.Page<>(total, pageNum == 0 ? 1 : pageNum + 1, pageSize,
                        totalPageNum, statisticsTask);

        CheckerPkgDefectRespVO pkgDefectRespVO = new CheckerPkgDefectRespVO();
        pkgDefectRespVO.setTaskCount(total);
        pkgDefectRespVO.setStatisticsTask(taskDetailVoPage);

        return pkgDefectRespVO;
    }

    @Override
    public DeptTaskDefectRspVO processDeptTaskDefectReq(DeptTaskDefectReqVO deptTaskDefectReqVO) {
        return null;
    }

}
