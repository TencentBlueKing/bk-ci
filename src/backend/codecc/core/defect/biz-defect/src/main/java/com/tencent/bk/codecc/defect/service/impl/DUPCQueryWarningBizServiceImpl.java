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
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.DUPCDefectDao;
import com.tencent.bk.codecc.defect.model.BuildDefectEntity;
import com.tencent.bk.codecc.defect.model.CodeBlockEntity;
import com.tencent.bk.codecc.defect.model.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.CodeBlockVO;
import com.tencent.bk.codecc.defect.vo.DUPCDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.DUPCDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.DUPCDefectVO;
import com.tencent.bk.codecc.defect.vo.GetFileContentSegmentReqVO;
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
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.PathUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 圈复杂度告警管理服务实现
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Service("DUPCQueryWarningBizService")
public class DUPCQueryWarningBizServiceImpl extends AbstractQueryWarningBizService
{
    private static Logger logger = LoggerFactory.getLogger(DUPCQueryWarningBizServiceImpl.class);

    @Autowired
    private DUPCDefectDao dupcDefectDao;

    @Autowired
    private DUPCDefectRepository dupcDefectRepository;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Autowired
    private BizServiceFactory<TreeService> treeServiceBizServiceFactory;

    @Autowired
    private BuildDefectRepository buildDefectRepository;

    @Autowired
    private NewDefectJudgeService newDefectJudgeService;

    @Override
    public QueryWarningPageInitRspVO processQueryWarningPageInitRequest(Long taskId, String toolName, String dimension,
                                                                        Set<String> statusSet, String checkerSet)
    {
        List<DUPCDefectEntity> dupcDefectEntityList = dupcDefectRepository.findByTaskIdAndStatus(
                taskId, ComConstants.DefectStatus.NEW.value());

        Set<String> authorSet = new HashSet<>();
        Set<String> defectPaths = new TreeSet<>();
        if (CollectionUtils.isNotEmpty(dupcDefectEntityList))
        {
            dupcDefectEntityList.forEach(defect ->
            {
                // 设置作者
                String authorList = defect.getAuthorList();
                if (StringUtils.isNotEmpty(authorList))
                {
                    String[] authorArray = authorList.split(";");
                    for (String author : authorArray)
                    {
                        if (null != author && author.trim().length() != 0)
                        {
                            authorSet.add(author);
                        }
                    }
                }

                // 获取所有警告文件的相对路径
                String relativePath = PathUtils.getRelativePath(defect.getUrl(), defect.getRelPath());
                if (org.apache.commons.lang3.StringUtils.isNotBlank(relativePath))
                {
                    defectPaths.add(relativePath);
                }
                else
                {
                    defectPaths.add(defect.getFilePath() == null ? "" : defect.getFilePath());
                }
            });
        }
        QueryWarningPageInitRspVO queryWarningPageInitRspVO = new QueryWarningPageInitRspVO();
        queryWarningPageInitRspVO.setAuthorList(authorSet);

        // 处理文件树
        TreeService treeService = treeServiceBizServiceFactory.createBizService(toolName, ComConstants.BusinessType.TREE_SERVICE.value(), TreeService.class);
        TreeNodeVO treeNode = treeService.getTreeNode(taskId, defectPaths);
        queryWarningPageInitRspVO.setFilePathTree(treeNode);
        return queryWarningPageInitRspVO;
    }

    @Override
    public CommonDefectQueryRspVO processQueryWarningRequest(long taskId, DefectQueryReqVO queryWarningReq, int pageNum, int pageSize, String sortField,
            Sort.Direction sortType)
    {
        logger.info("query task[{}] defect list by {}", taskId, queryWarningReq);

        //获取风险系数值
        Map<String, String> riskConfigMap = thirdPartySystemCaller.getRiskFactorConfig(ComConstants.Tool.DUPC.name());

        //查询数值
        DUPCDefectQueryRspVO dupcFileQueryRspVO = findDUPCFileByParam(taskId, queryWarningReq.getFileList(),
                queryWarningReq.getAuthor(), queryWarningReq.getSeverity(), riskConfigMap, queryWarningReq.getBuildId(),
                pageNum, pageSize, sortField, sortType, queryWarningReq.getDefectType(), queryWarningReq.getToolName());

        return dupcFileQueryRspVO;
    }

    @Override
    public CommonDefectDetailQueryRspVO processQueryWarningDetailRequest(long taskId, String userId, CommonDefectDetailQueryReqVO queryWarningDetailReq, String sortField, Sort.Direction sortType)
    {

        DUPCDefectDetailQueryRspVO dupcDefectQueryRspVO = new DUPCDefectDetailQueryRspVO();

        // 获取源重复块
        getSourceCodeBlockDetail(taskId, userId, queryWarningDetailReq, dupcDefectQueryRspVO);

        // 获取目标重复块
        getTargetCodeBlockDetail(taskId, dupcDefectQueryRspVO);

        return dupcDefectQueryRspVO;
    }

    @Override
    public CommonDefectDetailQueryRspVO processGetFileContentSegmentRequest(long taskId, String userId, GetFileContentSegmentReqVO reqModel)
    {
        String toolName = reqModel.getToolName();
        String filePath = reqModel.getFilePath();
        logger.debug("processGetFileContentSegmentRequest() ===BEGIN=== filePath: [{}]", filePath);
        DUPCDefectEntity dupcDefectEntity = dupcDefectRepository.findFirstByTaskIdAndFilePath(taskId, filePath);

        if (dupcDefectEntity == null)
        {
            logger.error("Can't find DUPC defect entity by taskId:{}, toolName:{}, filePath:{}", taskId, toolName, filePath);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{"重复率的缺陷实体"}, null);
        }

        //获取任务信息
        Result<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskInfoResult.getData();

        // 1. 根据文件路径从分析集群获取文件内容
        String content = getFileContent(taskId, taskDetailVO == null ? null : taskDetailVO.getProjectId(), userId,
                dupcDefectEntity.getUrl(), dupcDefectEntity.getRepoId(), dupcDefectEntity.getRelPath(),
                dupcDefectEntity.getRevision(), dupcDefectEntity.getBranch(), dupcDefectEntity.getSubModule());

        // 2. 根据告警的开始行和结束行截取文件片段
        CommonDefectDetailQueryRspVO dupcDefectQueryRspVO = new CommonDefectDetailQueryRspVO();
        if (reqModel.getEndLine() > 0)
        {
            content = trimCodeSegment(content, reqModel.getBeginLine(), reqModel.getEndLine(), dupcDefectQueryRspVO);
        }
        dupcDefectQueryRspVO.setFileContent(content);

        //获取文件的相对路径
        String relativePath = PathUtils.getRelativePath(dupcDefectEntity.getUrl(), dupcDefectEntity.getRelPath());
        dupcDefectQueryRspVO.setRelativePath(relativePath);
        dupcDefectQueryRspVO.setFilePath(filePath);
        int fileNameIndex = filePath.lastIndexOf("/");
        if (fileNameIndex == -1)
        {
            fileNameIndex = filePath.lastIndexOf("\\");
        }
        dupcDefectQueryRspVO.setFileName(filePath.substring(fileNameIndex + 1));

        return dupcDefectQueryRspVO;
    }

    @Override
    public ToolDefectRspVO processToolWarningRequest(Long taskId, DefectQueryReqVO queryWarningReq, Integer pageNum,
            Integer pageSize, String sortField, Sort.Direction sortType)
    {
        logger.info("processToolWarningRequest task[{}] defect list by {}", taskId, queryWarningReq);

        ToolDefectRspVO toolDefectRspVO = new ToolDefectRspVO();
        // 排序分页
        pageNum = pageNum == null ? 0 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        sortField = sortField == null ? "dupRate" : sortField;
        sortType = sortType == null ? Sort.Direction.DESC : sortType;

        DUPCDefectQueryRspVO dupcDefectQueryRspVO =
                (DUPCDefectQueryRspVO) processQueryWarningRequest(taskId, queryWarningReq, pageNum, pageSize, sortField,
                        sortType);

        BeanUtils.copyProperties(dupcDefectQueryRspVO, toolDefectRspVO, "defectList");
        Page<DUPCDefectVO> defectList = dupcDefectQueryRspVO.getDefectList();

        com.tencent.devops.common.api.pojo.Page<DUPCDefectVO> page =
                new com.tencent.devops.common.api.pojo.Page<>(defectList.getTotalElements(), defectList.getNumber() + 1,
                        defectList.getSize(), defectList.getTotalPages(), defectList.getContent());

        toolDefectRspVO.setDupcDefectList(page);
        toolDefectRspVO.setTaskId(taskId);
        toolDefectRspVO.setToolName(queryWarningReq.getToolName());
        return toolDefectRspVO;
    }


    /**
     * 获取源重复代码块详情
     *
     * @param taskId
     * @param defectQueryReqVO
     * @param dupcDefectQueryRspVO
     * @return
     */
    @NotNull
    private CommonDefectDetailQueryRspVO getSourceCodeBlockDetail(long taskId, String userId, CommonDefectDetailQueryReqVO defectQueryReqVO, DUPCDefectDetailQueryRspVO dupcDefectQueryRspVO)
    {
        //查询告警信息
        DUPCDefectEntity dupcDefectEntity = dupcDefectRepository.findFirstByEntityId(defectQueryReqVO.getEntityId());

        if (dupcDefectEntity == null)
        {
            logger.error("Can't find DUPC defect entity by entityId:{}", defectQueryReqVO.getEntityId());
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{defectQueryReqVO.getEntityId()}, null);
        }


        // 校验传入的路径是否合法（路径是否是告警对应的文件）
        verifyFilePathIsValid(defectQueryReqVO.getFilePath(), dupcDefectEntity.getFilePath());
        //获取任务信息
        Result<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskInfoResult.getData();
        //根据文件路径从分析集群获取文件内容
        String content = "";
        if (StringUtils.isNotBlank(dupcDefectEntity.getRelPath()))
        {
            content = getFileContent(taskId, taskDetailVO == null ? null : taskDetailVO.getProjectId(), userId,
                    dupcDefectEntity.getUrl(), dupcDefectEntity.getRepoId(), dupcDefectEntity.getRelPath(),
                    dupcDefectEntity.getRevision(), dupcDefectEntity.getBranch(), dupcDefectEntity.getSubModule());
        }

        List<CodeBlockEntity> blockEntityList = dupcDefectEntity.getBlockList();
        if (blockEntityList != null && blockEntityList.size() >= 1)
        {
            //分别取最小的beginLine和最大的endLine
            Long beginLine = blockEntityList.stream().min(Comparator.comparingLong(CodeBlockEntity::getStartLines)).
                    orElse(new CodeBlockEntity()).getStartLines();
            Long endLine = blockEntityList.stream().max(Comparator.comparingLong(CodeBlockEntity::getEndLines)).
                    orElse(new CodeBlockEntity()).getEndLines();
            logger.info("begin line : {}, end line : {}", beginLine, endLine);
            content = trimCodeSegment(content, beginLine.intValue(), endLine.intValue(), dupcDefectQueryRspVO);
        }

        //获取文件的相对路径
        String relativePath = PathUtils.getRelativePath(dupcDefectEntity.getUrl(), dupcDefectEntity.getRelPath());
        dupcDefectQueryRspVO.setRelativePath(relativePath);
        //组装告警详情查询相应视图
        String filePath = dupcDefectEntity.getFilePath();
        dupcDefectQueryRspVO.setFilePath(filePath);
        int fileNameIndex = filePath.lastIndexOf("/");
        if (fileNameIndex == -1)
        {
            fileNameIndex = filePath.lastIndexOf("\\");
        }
        dupcDefectQueryRspVO.setFileName(filePath.substring(fileNameIndex + 1));
        dupcDefectQueryRspVO.setFileContent(content);
        List<CodeBlockEntity> blockList = dupcDefectEntity.getBlockList();
        if (CollectionUtils.isNotEmpty(blockList))
        {
            List<CodeBlockVO> blockVOList = new ArrayList<>();
            for (CodeBlockEntity codeBlockEntity : blockList)
            {
                CodeBlockVO codeBlockVO = new CodeBlockVO();
                BeanUtils.copyProperties(codeBlockEntity, codeBlockVO);
                blockVOList.add(codeBlockVO);
            }

            // 按其实行号对重复块列表排序
            sortBlockByStartLine(blockVOList);
            dupcDefectQueryRspVO.setBlockInfoList(blockVOList);
        }

        return dupcDefectQueryRspVO;
    }

    /**
     * 获取目标重复代码块详情
     *
     * @param taskId
     * @param dupcDefectQueryRspVO
     * @return
     */
    private CommonDefectDetailQueryRspVO getTargetCodeBlockDetail(long taskId, DUPCDefectDetailQueryRspVO dupcDefectQueryRspVO)
    {
        List<CodeBlockVO> sourceBlockList = dupcDefectQueryRspVO.getBlockInfoList();
        if (CollectionUtils.isNotEmpty(sourceBlockList))
        {
            List<DUPCDefectEntity> dupcDefectEntityList = dupcDefectDao.queryCodeBlocksByFingerPrint(taskId, sourceBlockList);

            if (CollectionUtils.isNotEmpty(dupcDefectEntityList))
            {
                Map<String, List<CodeBlockVO>> codeBlockVOListMap = new HashMap<>();
                sourceBlockList.forEach(sourceBlock ->
                {
                    List<CodeBlockVO> codeBlockVOList = getTargetCodeBlockVOS(sourceBlock, dupcDefectEntityList, codeBlockVOListMap);
                    sortBlockByFileName(codeBlockVOList);
                });
                dupcDefectQueryRspVO.setTargetBlockMap(codeBlockVOListMap);
            }


        }

        return dupcDefectQueryRspVO;
    }

    /**
     * 根据源代码块过去目标代码块列表
     *
     * @param dupcDefectEntityList
     * @param codeBlockVOListMap
     * @param sourceBlock
     * @return
     */
    @NotNull
    private List<CodeBlockVO> getTargetCodeBlockVOS(CodeBlockVO sourceBlock, List<DUPCDefectEntity> dupcDefectEntityList, Map<String, List<CodeBlockVO>> codeBlockVOListMap)
    {
        String fingerPrint = sourceBlock.getFingerPrint();
        String sourceFile = sourceBlock.getSourceFile();
        long startLines = sourceBlock.getStartLines();
        long endLines = sourceBlock.getEndLines();
        String blockKey = startLines + "_" + endLines;
        List<CodeBlockVO> codeBlockVOList = codeBlockVOListMap.computeIfAbsent(blockKey, k -> new ArrayList<>());
        for (DUPCDefectEntity defectEntity : dupcDefectEntityList)
        {
            List<CodeBlockEntity> blockList = defectEntity.getBlockList();
            for (CodeBlockEntity codeBlockEntity : blockList)
            {
                String tempFingerPrint = codeBlockEntity.getFingerPrint();
                String tempSourceFile = codeBlockEntity.getSourceFile();
                long tempStartLines = codeBlockEntity.getStartLines();
                long tempEndLines = codeBlockEntity.getEndLines();

                // 从重复文件里面过滤出fingerPrint相同的代码块，并且排除掉源代码块本身
                if (fingerPrint.equals(tempFingerPrint)
                        && !(sourceFile.equals(tempSourceFile) && startLines == tempStartLines && endLines == tempEndLines))
                {
                    CodeBlockVO codeBlockVO = new CodeBlockVO();
                    BeanUtils.copyProperties(codeBlockEntity, codeBlockVO);
                    String filePath = codeBlockVO.getSourceFile();
                    int fileNameIndex = filePath.lastIndexOf("/");
                    if (fileNameIndex == -1)
                    {
                        fileNameIndex = filePath.lastIndexOf("\\");
                    }
                    codeBlockVO.setFileName(filePath.substring(fileNameIndex + 1));
                    codeBlockVOList.add(codeBlockVO);
                }
            }
        }
        return codeBlockVOList;
    }

    /**
     * 获取DUPC相对路径
     *
     * @param dupcDefectModelList
     */
    private void batchGetRelativePath(List<DUPCDefectVO> dupcDefectModelList)
    {
        // 加入相对路径
        if (CollectionUtils.isNotEmpty(dupcDefectModelList))
        {
            for (DUPCDefectVO defect : dupcDefectModelList)
            {
                String relativePath = PathUtils.getRelativePath(defect.getUrl(), defect.getRelPath());
                defect.setRelPath(relativePath);
            }
        }
    }

    /**
     * 校验传入的路径是否合法（路径是否是告警对应的文件）
     * 修改原因：修复安全组检测出的通过告警详情可以获取codecc工具分析服务器的任意文件的安全漏洞：
     * 如下路径：http://xxx.xxx.com/backend/duplicatecode/targetFileContent?proj_id=12023&f=../../../../../../../../../../etc/passwd
     *
     * @param filePath
     * @param defectFilePath
     * @return
     */
    private void verifyFilePathIsValid(String filePath, String defectFilePath)
    {
        if (!filePath.equals(defectFilePath))
        {
            logger.error("传入参数错误：filePath是非法路径");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{filePath}, null);
        }
    }

    /**
     * 按其实行号对重复块列表排序
     *
     * @param blockList
     */
    private void sortBlockByStartLine(List<CodeBlockVO> blockList)
    {
        blockList.sort((o1, o2) ->
        {
            Long startLine1 = o1.getStartLines();
            Long startLine2 = o2.getStartLines();
            int compareRes = startLine1.compareTo(startLine2);
            if (compareRes == 0)
            {
                Long endLine1 = o1.getEndLines();
                Long endLine2 = o2.getEndLines();
                compareRes = endLine2.compareTo(endLine1);
            }
            return compareRes;
        });
    }

    /**
     * 按其实行号对重复块列表排序
     *
     * @param blockList
     */
    private void sortBlockByFileName(List<CodeBlockVO> blockList)
    {
        blockList.sort((o1, o2) ->
        {
            String file1 = o1.getSourceFile().substring(o1.getSourceFile().lastIndexOf("/") + 1);
            String file2 = o2.getSourceFile().substring(o2.getSourceFile().lastIndexOf("/") + 1);
            int compareRes = file1.compareToIgnoreCase(file2);
            if (compareRes == 0)
            {
                Long startLine1 = o1.getStartLines();
                Long startLine2 = o2.getStartLines();
                compareRes = startLine1.compareTo(startLine2);
                if (compareRes == 0)
                {
                    Long endLine1 = o1.getEndLines();
                    Long endLine2 = o2.getEndLines();
                    compareRes = endLine2.compareTo(endLine1);
                }
            }
            return compareRes;
        });
    }

    /**
     * 通过参数查询重复率文件信息
     *
     * @param taskId
     * @param fileList
     * @param author
     * @param severity
     * @param riskConfigMap
     * @param buildId
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortType
     * @return
     */
    public DUPCDefectQueryRspVO findDUPCFileByParam(long taskId, Set<String> fileList, String author,
                                                    Set<String> severity, Map<String, String> riskConfigMap,
                                                    String buildId, int pageNum, int pageSize, String sortField,
                                                    Sort.Direction sortType, Set<String> defectTypeSet, String toolName)
    {
        // 是否需要构建号过滤
        boolean needBuildIdFilter = false;
        Set<String> currentDupeFileRelPaths = Sets.newHashSet();
        if (StringUtils.isNotEmpty(buildId))
        {
            if (taskLogService.defectCommitSuccess(taskId, Lists.newArrayList(ComConstants.Tool.DUPC.name()), buildId, getSubmitStepNum()).get(ComConstants.Tool.DUPC.name()))
            {
                List<BuildDefectEntity> buildFiles = buildDefectRepository.findByTaskIdAndToolNameAndBuildId(taskId, ComConstants.Tool.DUPC.name(), buildId);
                if (CollectionUtils.isNotEmpty(buildFiles))
                {
                    for (BuildDefectEntity buildDefectEntity : buildFiles)
                    {
                        currentDupeFileRelPaths.add(buildDefectEntity.getFileRelPath());
                    }
                }
            }
            needBuildIdFilter = true;
        }


        TaskDetailVO taskVO = client.get(ServiceTaskRestResource.class)
                .getTaskInfoWithoutToolsByTaskId(taskId).getData();
        long newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, toolName, taskVO);
        boolean mustFilterByNewDefectJudge = !CollectionUtils.isEmpty(defectTypeSet)
                && !defectTypeSet.containsAll(Sets.newHashSet(ComConstants.DefectType.NEW.stringValue(),
                ComConstants.DefectType.HISTORY.stringValue()));



        // 需要统计的数据
        int seriousCheckerCount = 0;
        int normalCheckerCount = 0;
        int promptCheckerCount = 0;
        int totalCheckerCount = 0;
        int newDefectCount = 0;
        int historyDefectCount = 0;

        // 查询总的数量，并且过滤计数
        List<DUPCDefectVO> dupcDefectVOS = new ArrayList<>();
        List<DUPCDefectEntity> defectList = dupcDefectDao.findByTaskIdAndAuthorAndRelPaths(taskId, author, fileList);
        Iterator<DUPCDefectEntity> it = defectList.iterator();
        while (it.hasNext())
        {
            DUPCDefectEntity defectEntity = it.next();

            // 按照构建号过滤
            if (needBuildIdFilter && !currentDupeFileRelPaths.contains(defectEntity.getRelPath()))
            {
                continue;
            }

            // 根据当前处理人，文件过滤之后，需要按照严重程度统计缺陷数量
            fillingRiskFactor(defectEntity, riskConfigMap);
            int riskFactor = defectEntity.getRiskFactor();
            if (ComConstants.RiskFactor.SH.value() == riskFactor)
            {
                seriousCheckerCount++;
            }
            else if (ComConstants.RiskFactor.H.value() == riskFactor)
            {
                normalCheckerCount++;
            }
            else if (ComConstants.RiskFactor.M.value() == riskFactor)
            {
                promptCheckerCount++;
            }
            boolean meetSeverity = CollectionUtils.isNotEmpty(severity) &&
                    !severity.contains(String.valueOf(riskFactor));
            if (meetSeverity)
            {
                continue;
            }

            // scm文件修改时间有可能为空，当null时则认为是新告警处理
            boolean isNewDefectByJudgeTime = defectEntity.getFileChangeTime() == null
                    || defectEntity.getFileChangeTime() >= newDefectJudgeTime;

            // 新旧告警跟风险等级联动，不同风险等级里再区分新旧
            // 新旧告警统计
            if (isNewDefectByJudgeTime) {
                newDefectCount++;
            } else {
                historyDefectCount++;
            }

            // 新旧告警过滤
            if (mustFilterByNewDefectJudge) {
                if (defectTypeSet.contains(ComConstants.DefectType.NEW.stringValue())
                        && !isNewDefectByJudgeTime) {
                    continue;
                } else if (defectTypeSet.contains(ComConstants.DefectType.HISTORY.stringValue())
                        && isNewDefectByJudgeTime) {
                    continue;
                }
            }

            // 设置告警文件名
            setFileName(defectEntity);

            // 统计告警数
            totalCheckerCount++;

            // 构造视图
            DUPCDefectVO dupcDefectVO = new DUPCDefectVO();
            BeanUtils.copyProperties(defectEntity, dupcDefectVO);
            dupcDefectVOS.add(dupcDefectVO);
        }

        Page<DUPCDefectVO> defectVOPage = sortAndPage(pageNum, pageSize, sortField, sortType, dupcDefectVOS);

        DUPCDefectQueryRspVO dupcFileQueryRspVO = new DUPCDefectQueryRspVO();
        dupcFileQueryRspVO.setSuperHighCount(seriousCheckerCount);
        dupcFileQueryRspVO.setHighCount(normalCheckerCount);
        dupcFileQueryRspVO.setMediumCount(promptCheckerCount);
        dupcFileQueryRspVO.setTotalCount(totalCheckerCount);
        dupcFileQueryRspVO.setDefectList(defectVOPage);
        dupcFileQueryRspVO.setNewDefectJudgeTime(newDefectJudgeTime);
        dupcFileQueryRspVO.setNewCount(newDefectCount);
        dupcFileQueryRspVO.setHistoryCount(historyDefectCount);

        return dupcFileQueryRspVO;
    }

    /**
     * 获取原始查询条件
     *
     * @param taskId
     * @param author
     * @return
     */
    private Query getPremiumQuery(long taskId, String author)
    {
        Document fieldsObj = new Document();
        fieldsObj.put("blockList", false);
        Query query = new BasicQuery(new Document(), fieldsObj);
        query.addCriteria(Criteria.where("task_id").is(taskId).and("status").is(ComConstants.DefectStatus.NEW.value()));

        //作者过滤
        if (StringUtils.isNotEmpty(author))
        {
            query.addCriteria(Criteria.where("author_list").regex(String.format("%s%s%s", ".*", author, ".*")));
        }
        return query;
    }

    /**
     * 根据风险系数配置给告警方法赋值风险系数
     *
     * @param riskFactorConfMap
     */
    private void fillingRiskFactor(DUPCDefectEntity dupcDefectEntity, Map<String, String> riskFactorConfMap)
    {
        if (riskFactorConfMap == null)
        {
            logger.error("Has not init risk factor config!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"风险系数"}, null);
        }
        Float sh = Float.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.SH.name()));
        Float h = Float.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.H.name()));
        Float m = Float.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.M.name()));

        String dupRateStr = dupcDefectEntity.getDupRate();
        float dupRate = Float.valueOf(StringUtils.isEmpty(dupRateStr) ? "0" : dupRateStr.substring(0, dupRateStr.length() - 1));
        if (dupRate >= m && dupRate < h)
        {
            dupcDefectEntity.setRiskFactor(ComConstants.RiskFactor.M.value());
        }
        else if (dupRate >= h && dupRate < sh)
        {
            dupcDefectEntity.setRiskFactor(ComConstants.RiskFactor.H.value());
        }
        else if (dupRate >= sh)
        {
            dupcDefectEntity.setRiskFactor(ComConstants.RiskFactor.SH.value());
        }
    }

    /**
     * 设置告警文件名
     *
     * @param dupcDefectEntity
     */
    private void setFileName(DUPCDefectEntity dupcDefectEntity)
    {
        String filePath = dupcDefectEntity.getFilePath();
        if (filePath == null) return;
        int fileNameIndex = filePath.lastIndexOf("/");
        if (fileNameIndex == -1)
        {
            fileNameIndex = filePath.lastIndexOf("\\");
        }
        dupcDefectEntity.setFileName(filePath.substring(fileNameIndex + 1));
    }

    /**
     * 拼接严重等级参数
     *
     * @param severity
     * @param riskFactorConfMap
     * @param orCriteria
     */
    private void assembleSeverityParam(Set<String> severity, Map<String, String> riskFactorConfMap,
            List<Criteria> orCriteria)
    {
        if (CollectionUtils.isNotEmpty(severity))
        {
            List<Criteria> criteriaList = new ArrayList<>();
            Float sh = Float.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.SH.name()));
            Float h = Float.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.H.name()));
            Float m = Float.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.M.name()));
            severity.forEach(sev ->
            {
                if (Integer.valueOf(sev) == ComConstants.RiskFactor.SH.value())
                {
                    criteriaList.add(Criteria.where("dup_rate_value").gte(sh));
                }
                else if (Integer.valueOf(sev) == ComConstants.RiskFactor.H.value())
                {
                    criteriaList.add(Criteria.where("dup_rate_value").gte(h).lt(sh));
                }
                else if (Integer.valueOf(sev) == ComConstants.RiskFactor.M.value())
                {
                    criteriaList.add(Criteria.where("dup_rate_value").gte(m).lt(h));
                }
            });
            orCriteria.add(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
        }
        else
        {
            orCriteria.add(Criteria.where("dup_rate_value").gte(Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.M.name()))));
        }
    }

    @Override
    public int getSubmitStepNum()
    {
        return ComConstants.Step4MutliTool.COMMIT.value();
    }
}
