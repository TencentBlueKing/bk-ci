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
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.model.BuildDefectEntity;
import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.StatisticEntity;
import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.CCNDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.CCNDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.CCNDefectVO;
import com.tencent.bk.codecc.defect.vo.CodeCommentVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectRspVO;
import com.tencent.bk.codecc.defect.vo.TreeNodeVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.PathUtils;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * 圈复杂度告警管理服务实现
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Slf4j
@Service("CCNQueryWarningBizService")
public class CCNQueryWarningBizServiceImpl extends AbstractQueryWarningBizService
{
    private static Logger logger = LoggerFactory.getLogger(CCNQueryWarningBizServiceImpl.class);

    @Autowired
    private Client client;

    @Autowired
    CheckerService checkerService;

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Autowired
    private BizServiceFactory<TreeService> treeServiceBizServiceFactory;

    @Autowired
    private NewDefectJudgeService newDefectJudgeService;

    @Autowired
    private CCNDefectDao ccnDefectDao;

    @Autowired
    private BuildDefectRepository buildDefectRepository;

    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;

    @Override
    public CommonDefectQueryRspVO processQueryWarningRequest(long taskId, DefectQueryReqVO queryWarningReq, int pageNum, int pageSize, String sortField, Sort.Direction sortType)
    {
        logger.info("query task[{}] defect list by {}", taskId, queryWarningReq);

        Set<String> fileList = queryWarningReq.getFileList();
        String author = queryWarningReq.getAuthor();
        List<CCNDefectEntity> originalCCNDefectList = ccnDefectDao.findByTaskIdAndAuthorAndRelPaths(taskId, author, fileList);

        CCNDefectQueryRspVO ccnFileQueryRspVO = new CCNDefectQueryRspVO();
        // 从Task服务获取任务信息
        Result<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        if (taskInfoResult.isNotOk() || null == taskInfoResult.getData()) {
            log.error("get task info fail! stream name is: {}, msg: {}", taskId, taskInfoResult.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        // 从任务信息中获取CCN配置信息
        ToolConfigInfoVO toolConfigInfoVO = taskInfoResult.getData()
                .getToolConfigInfoList()
                .stream()
                .filter(toolConfig -> toolConfig.getToolName().equalsIgnoreCase(ComConstants.Tool.CCN.name()))
                .findAny()
                .orElseGet(ToolConfigInfoVO::new);
        // 查询ccn圈复杂度阀值
        int ccnThreshold = checkerService.getCcnThreshold(toolConfigInfoVO);
        ccnFileQueryRspVO.setCcnThreshold(ccnThreshold);

        // 根据根据前端传入的条件过滤告警，并分类统计
        Set<String> defectPaths =
            filterDefectByCondition(taskId, originalCCNDefectList, null, queryWarningReq, ccnFileQueryRspVO, null);

        StatisticEntity statisticEntity =
            ccnStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, ComConstants.Tool.CCN.name());
        List<CCNDefectVO> ccnDefectVOS = originalCCNDefectList.stream().map(ccnDefectEntity ->
        {
            CCNDefectVO ccnDefectVO = new CCNDefectVO();
            BeanUtils.copyProperties(ccnDefectEntity, ccnDefectVO);
            ccnDefectVO.setMark(convertMarkStatus(ccnDefectVO.getMark(), ccnDefectVO.getMarkTime(), statisticEntity.getTime()));
            return ccnDefectVO;
        }).collect(Collectors.toList());

        Page<CCNDefectVO> defectVOPage = sortAndPage(pageNum, pageSize, sortField, sortType, ccnDefectVOS);
        ccnFileQueryRspVO.setDefectList(defectVOPage);

        return ccnFileQueryRspVO;
    }

    @Override
    public CommonDefectDetailQueryRspVO processQueryWarningDetailRequest(long taskId, String userId, CommonDefectDetailQueryReqVO queryWarningDetailReq, String sortField, Sort.Direction sortType)
    {
        CCNDefectDetailQueryRspVO ccnDefectQueryRspVO = new CCNDefectDetailQueryRspVO();

        //查询告警信息
        CCNDefectEntity ccnDefectEntity = ccnDefectRepository.findFirstByEntityId(queryWarningDetailReq.getEntityId());

        CCNDefectVO ccnDefectVO = new CCNDefectVO();

        BeanUtils.copyProperties(ccnDefectEntity, ccnDefectVO);
        BeanUtils.copyProperties(ccnDefectEntity, ccnDefectQueryRspVO); // todo delete
        ccnDefectQueryRspVO.setDefectVO(ccnDefectVO);

        // 校验传入的路径是否合法（路径是否是告警对应的文件）
        verifyFilePathIsValid(queryWarningDetailReq.getFilePath(), ccnDefectEntity.getFilePath());

        //获取任务信息
        Result<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskInfoResult.getData();

        //根据文件路径从分析集群获取文件内容
        String content = getFileContent(taskId, taskDetailVO == null ? null : taskDetailVO.getProjectId(), userId,
                ccnDefectEntity.getUrl(), ccnDefectEntity.getRepoId(),
                ccnDefectEntity.getRelPath(), ccnDefectEntity.getRevision(), ccnDefectEntity.getBranch(),
                ccnDefectEntity.getSubModule());
        content = trimCodeSegment(content, ccnDefectEntity.getStartLines(), ccnDefectEntity.getEndLines(), ccnDefectQueryRspVO);

        //设置代码评论
        if (null != ccnDefectEntity.getCodeComment() &&
                CollectionUtils.isNotEmpty(ccnDefectEntity.getCodeComment().getCommentList()))
        {
            CodeCommentVO codeCommentVO = convertCodeComment(ccnDefectEntity.getCodeComment());
            ccnDefectVO.setCodeComment(codeCommentVO);
            ccnDefectQueryRspVO.setCodeComment(codeCommentVO); // todo delete
        }

        //获取文件的相对路径
        String relativePath = PathUtils.getRelativePath(ccnDefectEntity.getUrl(), ccnDefectEntity.getRelPath());
        ccnDefectQueryRspVO.setRelativePath(relativePath);

        String filePath = ccnDefectEntity.getFilePath();

        //获取文件的url
        String url = PathUtils.getFileUrl(ccnDefectEntity.getUrl(), ccnDefectEntity.getBranch(), ccnDefectEntity.getRelPath());
        ccnDefectQueryRspVO.setFilePath(StringUtils.isEmpty(url) ? filePath : url);
        int fileNameIndex = filePath.lastIndexOf("/");
        if (fileNameIndex == -1)
        {
            fileNameIndex = filePath.lastIndexOf("\\");
        }
        ccnDefectQueryRspVO.setFileName(filePath.substring(fileNameIndex + 1));
        ccnDefectQueryRspVO.setFileContent(content);

        ccnDefectQueryRspVO.setRevision(ccnDefectEntity.getRevision());

        return ccnDefectQueryRspVO;
    }

    @Override
    public QueryWarningPageInitRspVO processQueryWarningPageInitRequest(Long taskId, String toolName, String dimension, Set<String> statusSet, String checkerSet)
    {
        List<CCNDefectEntity> ccnDefectEntityList = ccnDefectRepository.findByTaskIdAndStatus(
                taskId, DefectStatus.NEW.value());

        Set<String> authorSet = new TreeSet<>();
        Set<String> defectPaths = new TreeSet<>();
        if (CollectionUtils.isNotEmpty(ccnDefectEntityList))
        {
            ccnDefectEntityList.forEach(defect ->
            {
                // 设置作者
                if (defect.getAuthor() != null)
                {
                    authorSet.add(defect.getAuthor());
                }

                // 获取所有警告文件的相对路径
                String relativePath = PathUtils.getRelativePath(defect.getUrl(), defect.getRelPath());
                if (StringUtils.isNotBlank(relativePath))
                {
                    defectPaths.add(relativePath);
                }
                else
                {
                    defectPaths.add(defect.getFilePath());
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
    public ToolDefectRspVO processToolWarningRequest(Long taskId, DefectQueryReqVO queryWarningReq, Integer pageNum,
            Integer pageSize, String sortField, Sort.Direction sortType)
    {
        ToolDefectRspVO toolDefectRspVO = new ToolDefectRspVO();
        logger.info("processToolWarningRequest task[{}] defect list by {}", taskId, queryWarningReq);

        // 排序分页
        pageNum = pageNum == null ? 0 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        sortField = sortField == null ? "ccn" : sortField;
        sortType = sortType == null ? Sort.Direction.DESC : sortType;

        CCNDefectQueryRspVO ccnDefectQueryRspVO = (CCNDefectQueryRspVO) this
                .processQueryWarningRequest(taskId, queryWarningReq, pageNum, pageSize, sortField, sortType);

        BeanUtils.copyProperties(ccnDefectQueryRspVO, toolDefectRspVO, "defectList");
        Page<CCNDefectVO> defectList = ccnDefectQueryRspVO.getDefectList();

        com.tencent.devops.common.api.pojo.Page<CCNDefectVO> page =
                new com.tencent.devops.common.api.pojo.Page<>(defectList.getTotalElements(), defectList.getNumber() + 1,
                        defectList.getSize(), defectList.getTotalPages(), defectList.getContent());

        toolDefectRspVO.setCcnDefectList(page);
        toolDefectRspVO.setTaskId(taskId);
        toolDefectRspVO.setToolName(queryWarningReq.getToolName());
        return toolDefectRspVO;
    }

    @Override
    public Set<String> filterDefectByCondition(long taskId, List<?> defectList,
                                               Set<String> allChecker,
                                               DefectQueryReqVO queryWarningReq,
                                               CommonDefectQueryRspVO defectQueryRspVO,
                                               List<String> toolNameSet) {
        Set<String> severity = queryWarningReq.getSeverity();
        Set<String> conditionDefectType = queryWarningReq.getDefectType();
        String buildId = queryWarningReq.getBuildId();

        Set<String> condStatusList = queryWarningReq.getStatus();
        if (CollectionUtils.isEmpty(condStatusList))
        {
            condStatusList = new HashSet<>(1);
            condStatusList.add(String.valueOf(DefectStatus.NEW.value()));
        }
        if (condStatusList.contains(String.valueOf(DefectStatus.PATH_MASK.value()))) {
            condStatusList.add(String.valueOf(DefectStatus.CHECKER_MASK.value()));
        }

        // 获取风险系数值
        Map<String, String> riskFactorConfMap = thirdPartySystemCaller.getRiskFactorConfig(ComConstants.Tool.CCN.name());

        // 查询新老告警判定时间
        long newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, queryWarningReq.getToolName(), null);

        // 是否需要构建号过滤
        boolean needBuildIdFilter = false;
        Set<String> currentBuildEntityIds = Sets.newHashSet();
        if (StringUtils.isNotEmpty(buildId))
        {
            if (taskLogService.defectCommitSuccess(taskId, Lists.newArrayList(ComConstants.Tool.CCN.name()), buildId, getSubmitStepNum()).get(ComConstants.Tool.CCN.name()))
            {
                List<BuildDefectEntity> buildFiles = buildDefectRepository.findByTaskIdAndToolNameAndBuildId(taskId, ComConstants.Tool.CCN.name(), buildId);
                if (CollectionUtils.isNotEmpty(buildFiles))
                {
                    for (BuildDefectEntity buildDefectEntity : buildFiles)
                    {
                        currentBuildEntityIds.add(buildDefectEntity.getDefectId());
                    }
                }
            }
            needBuildIdFilter = true;
        }

        // 需要统计的数据
        int superHighCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;
        int totalCheckerCount = 0;
        int newDefectCount = 0;
        int historyDefectCount = 0;
        int existCount = 0;
        int fixCount = 0;
        int ignoreCount = 0;
        int maskCount = 0;

        // 过滤计数
        Iterator<CCNDefectEntity> it = (Iterator<CCNDefectEntity>) defectList.iterator();
        while (it.hasNext())
        {
            CCNDefectEntity ccnDefectEntity = it.next();

            // 判断是否匹配告警状态，先收集统计匹配告警状态的规则、处理人、文件路径
            boolean notMatchStatus = isNotMatchStatus(condStatusList, ccnDefectEntity.getStatus());

            // 按构建号筛选
            if (needBuildIdFilter && !currentBuildEntityIds.contains(ccnDefectEntity.getEntityId()))
            {
                it.remove();
                continue;
            }

            // 根据状态过滤，注：已修复和其他状态（忽略，路径屏蔽，规则屏蔽）不共存，已忽略状态优先于屏蔽
            int status = ccnDefectEntity.getStatus();
            if (DefectStatus.NEW.value() == status)
            {
                existCount++;
            }
            else if ((DefectStatus.FIXED.value() & status) > 0)
            {
                fixCount++;
            }
            else if ((DefectStatus.IGNORE.value() & status) > 0)
            {
                ignoreCount++;
            }
            else if ((DefectStatus.PATH_MASK.value() & status) > 0
                    || (DefectStatus.CHECKER_MASK.value() & status) > 0) {
                maskCount++;
            }
            // 严重程度条件不为空且与当前数据的严重程度不匹配时，判断为true移除，否则false不移除
            if (notMatchStatus)
            {
                it.remove();
                continue;
            }

            //5.按照严重程度统计缺陷数量并过滤
            fillingRiskFactor(ccnDefectEntity, riskFactorConfMap);
            int riskFactor = ccnDefectEntity.getRiskFactor();
            if (ComConstants.RiskFactor.SH.value() == riskFactor)
            {
                superHighCount++;
            }
            else if (ComConstants.RiskFactor.H.value() == riskFactor)
            {
                highCount++;
            }
            else if (ComConstants.RiskFactor.M.value() == riskFactor)
            {
                mediumCount++;
            }
            else if (ComConstants.RiskFactor.L.value() == riskFactor)
            {
                lowCount++;
            }
            boolean meetSeverity = CollectionUtils.isNotEmpty(severity) &&
                    !severity.contains(String.valueOf(riskFactor));
            if (meetSeverity)
            {
                it.remove();
                continue;
            }

            // 统计历史告警数和新告警数并过滤
            long defectLastUpdateTime = DateTimeUtils.getThirteenTimestamp(ccnDefectEntity.getLatestDateTime() == null ? 0 : ccnDefectEntity.getLatestDateTime());
            if (defectLastUpdateTime < newDefectJudgeTime)
            {
                historyDefectCount++;
            }
            else
            {
                newDefectCount++;
            }

            // 按新告警和历史告警筛选
            String defectType = defectLastUpdateTime > newDefectJudgeTime ? ComConstants.DefectType.NEW.stringValue() : ComConstants.DefectType.HISTORY.stringValue();
            boolean notMatchDefectType = CollectionUtils.isNotEmpty(conditionDefectType) && !conditionDefectType.contains(defectType);
            if (notMatchDefectType)
            {
                it.remove();
                continue;
            }

            totalCheckerCount++;

//            String relativePath = PathUtils.getRelativePath(ccnDefectEntity.getUrl(), ccnDefectEntity.getRelPath());
//            ccnDefectEntity.setRelPath(relativePath);
        }

        CCNDefectQueryRspVO ccnFileQueryRspVO = (CCNDefectQueryRspVO) defectQueryRspVO;
        ccnFileQueryRspVO.setSuperHighCount(superHighCount);
        ccnFileQueryRspVO.setHighCount(highCount);
        ccnFileQueryRspVO.setMediumCount(mediumCount);
        ccnFileQueryRspVO.setLowCount(lowCount);
        ccnFileQueryRspVO.setExistCount(existCount);
        ccnFileQueryRspVO.setFixCount(fixCount);
        ccnFileQueryRspVO.setIgnoreCount(ignoreCount);
        ccnFileQueryRspVO.setMaskCount(maskCount);
        ccnFileQueryRspVO.setNewDefectCount(newDefectCount);
        ccnFileQueryRspVO.setHistoryDefectCount(historyDefectCount);
        ccnFileQueryRspVO.setTotalCount(totalCheckerCount);
        ccnFileQueryRspVO.setNewDefectJudgeTime(newDefectJudgeTime);

        return null;
    }


    /**
     * 校验传入的路径是否合法（路径是否是告警对应的文件）
     * 修改原因：修复安全组检测出的通过告警详情可以获取codecc工具分析服务器的任意文件的安全漏洞：
     *
     * @param filePath
     * @param defectFilePath
     * @return
     * @date 2019/1/15
     * @version V3.4.1
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
     * 根据风险系数配置给告警方法赋值风险系数
     *
     * @param riskFactorConfMap
     */
    private void fillingRiskFactor(CCNDefectEntity ccnDefectEntity, Map<String, String> riskFactorConfMap)
    {
        if (riskFactorConfMap == null)
        {
            logger.error("Has not init risk factor config!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"风险系数"}, null);
        }
        int sh = Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.SH.name()));
        int h = Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.H.name()));
        int m = Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.M.name()));

        int ccn = ccnDefectEntity.getCcn();
        if (ccn >= m && ccn < h)
        {
            ccnDefectEntity.setRiskFactor(ComConstants.RiskFactor.M.value());
        }
        else if (ccn >= h && ccn < sh)
        {
            ccnDefectEntity.setRiskFactor(ComConstants.RiskFactor.H.value());
        }
        else if (ccn >= sh)
        {
            ccnDefectEntity.setRiskFactor(ComConstants.RiskFactor.SH.value());
        }
        else if (ccn < m)
        {
            ccnDefectEntity.setRiskFactor(ComConstants.RiskFactor.L.value());
        }
    }

    @Override
    public int getSubmitStepNum()
    {
        return ComConstants.Step4MutliTool.COMMIT.value();
    }
}
