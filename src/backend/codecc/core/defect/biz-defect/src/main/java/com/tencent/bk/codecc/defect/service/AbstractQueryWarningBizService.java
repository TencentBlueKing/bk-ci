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

package com.tencent.bk.codecc.defect.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CLOCStatisticsDao;
import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.model.CodeCommentEntity;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.model.LintStatisticEntity;
import com.tencent.bk.codecc.defect.utils.ConvertUtil;
import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import com.tencent.bk.codecc.defect.vo.openapi.PkgDefectDetailVO;
import com.tencent.bk.codecc.defect.vo.openapi.TaskDefectVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.UserMetaRestResource;
import com.tencent.bk.codecc.task.vo.GongfengPublicProjVO;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.gongfeng.ForkProjVO;
import com.tencent.bk.codecc.task.vo.gongfeng.ProjectStatVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.pojo.external.AuthExConstantsKt;
import com.tencent.devops.common.auth.api.util.AuthApiUtils;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.GitUtil;
import com.tencent.devops.common.util.ListSortUtil;
import com.tencent.devops.common.util.MD5Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.devops.common.auth.api.pojo.external.AuthExConstantsKt.KEY_CREATE_FROM;

/**
 * 告警管理抽象类
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Slf4j
public abstract class AbstractQueryWarningBizService implements IQueryWarningBizService
{
    private static Logger logger = LoggerFactory.getLogger(AbstractQueryWarningBizService.class);

    protected static String EMPTY_FILE_CONTENT_TIPS = "无法获取代码片段。请确保你对代码库拥有权限，且该文件未从代码库中删除。";

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected RedisTemplate<String, String> redisTemplate;

    @Autowired
    protected Client client;

    @Autowired
    protected TaskLogService taskLogService;

    @Autowired
    private CLOCStatisticsDao clocStatisticsDao;

    @Autowired
    protected PipelineScmService pipelineScmService;

    @Autowired
    private LintStatisticRepository lintStatisticRepository;

    @Value("${git.host:}")
    private String gitHost;

    @Override
    public CommonDefectDetailQueryRspVO processGetFileContentSegmentRequest(long taskId, String userId, GetFileContentSegmentReqVO reqModel)
    {
        return new CommonDefectDetailQueryRspVO();
    }

    /**
     * 根据告警的开始行和结束行截取文件片段
     *
     * @param fileContent
     * @param beginLine
     * @param endLine
     * @param defectQueryRspVO
     * @return
     */
    protected String trimCodeSegment(String fileContent, int beginLine, int endLine, CommonDefectDetailQueryRspVO defectQueryRspVO)
    {
        if (StringUtils.isBlank(fileContent))
        {
            return EMPTY_FILE_CONTENT_TIPS;
        }

        String[] lines = fileContent.split("\n");
        if (lines.length <= 2000)
        {
            defectQueryRspVO.setTrimBeginLine(1);
            return fileContent;
        }

        int trimBeginLine = 1;
        int trimEndLine = lines.length;
        int limitLines = 500;
        if (beginLine - limitLines > 0)
        {
            trimBeginLine = beginLine - limitLines;
        }

        if (endLine + limitLines < lines.length)
        {
            trimEndLine = endLine + limitLines;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = trimBeginLine - 1; i < trimEndLine - 1; i++)
        {
            builder.append(lines[i] + "\n");
        }
        defectQueryRspVO.setTrimBeginLine(trimBeginLine);
        return builder.toString();
    }


    @Override
    public DeptTaskDefectRspVO processDeptTaskDefectReq(DeptTaskDefectReqVO deptTaskDefectReqVO)
    {
        return null;
    }


    @Override
    public Set<String> filterDefectByCondition(long taskId, List<?> defectList,
                                               Set<String> allChecker,
                                               DefectQueryReqVO queryCondObj,
                                               CommonDefectQueryRspVO defectQueryRspVO,
                                               List<String> toolNameSet) {
        return null;
    }

    @Override
    public ToolDefectRspVO processDeptDefectList(DeptTaskDefectReqVO defectQueryReq, Integer pageNum, Integer pageSize,
            String sortField, Sort.Direction sortType)
    {
        return null;
    }

    @Override
    public QueryWarningPageInitRspVO pageInit(long taskId, DefectQueryReqVO defectQueryReqVO)
    {
        return null;
    }

    protected List<MetadataVO> getCodeLangMetadataVoList()
    {
        Result<Map<String, List<MetadataVO>>> metaDataResult =
                client.get(UserMetaRestResource.class).metadatas(ComConstants.KEY_CODE_LANG);
        if (metaDataResult.isNotOk() || metaDataResult.getData() == null)
        {
            logger.error("meta data result is empty! meta data type {}", ComConstants.KEY_CODE_LANG);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return metaDataResult.getData().get(ComConstants.KEY_CODE_LANG);
    }

    @Override
    public ToolDefectRspVO processToolWarningRequest(Long taskId, DefectQueryReqVO queryWarningReq, Integer pageNum,
                                                     Integer pageSize, String sortField, Sort.Direction sortType)
    {
        return null;
    }

    protected Map<Integer, GongfengPublicProjVO> getGongfengPublicProjVoMap(Set<Integer> gfProjectIds)
    {
        Result<Map<Integer, GongfengPublicProjVO>> gongfengProjResult =
                client.get(ServiceTaskRestResource.class).getGongfengProjInfo(gfProjectIds);
        if (gongfengProjResult.isNotOk() || gongfengProjResult.getData() == null)
        {
            logger.error("gong feng project result is empty! gong feng id: {}", gfProjectIds.toString());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{"gongfengProj empty"}, null);
        }
        return gongfengProjResult.getData();
    }


    protected void setGongfengInfo(Map<Integer, GongfengPublicProjVO> gongfengPublicProjVoMap,
            Map<Integer, ProjectStatVO> gongfengStatProjVoMap, TaskDetailVO taskDetailVO, TaskDefectVO taskDefectVO)
    {
        int forkedFromId = 0;
        String repoUrl = "";
        Integer gongfengProjectId = taskDetailVO.getGongfengProjectId();
        GongfengPublicProjVO publicProjVO = gongfengPublicProjVoMap.get(gongfengProjectId);
        if (publicProjVO != null)
        {
            repoUrl = publicProjVO.getHttpUrlToRepo();

            // 当工蜂ID与forked来源ID不一致时表示该代码库为fork 需赋值给forkedFromId
            ForkProjVO forkedFromProject = publicProjVO.getForkedFromProject();
            Integer forkedFromProjectId = forkedFromProject.getId();
            if (forkedFromProjectId != 0 && !gongfengProjectId.equals(forkedFromProjectId))
            {
                forkedFromId = forkedFromProjectId;
            }
        }
        taskDefectVO.setRepoUrl(repoUrl);
        taskDefectVO.setForkedFromId(forkedFromId);

        // 设置项目是否开源,归属,所有成员
        String repoBelong = "";
        String owners = "";
        Integer repoVisibilityLevel = null;
        ProjectStatVO projectStatVO = gongfengStatProjVoMap.get(gongfengProjectId);
        if (projectStatVO != null)
        {
            repoBelong = projectStatVO.getBelong();
            repoVisibilityLevel = projectStatVO.getVisibilityLevel();
            owners = projectStatVO.getOwners();
        }
        taskDefectVO.setRepoBelong(repoBelong);
        taskDefectVO.setRepoOwners(owners);
        taskDefectVO.setRepoVisibilityLevel(repoVisibilityLevel);
    }


    protected Map<Integer, ProjectStatVO> getGongfengStatProjVoMap(Integer bgId, Set<Integer> gfProjectIds)
    {
        Result<Map<Integer, ProjectStatVO>> gongfengStatResult =
                client.get(ServiceTaskRestResource.class).getGongfengStatProjInfo(bgId, gfProjectIds);
        if (gongfengStatResult.isNotOk() || gongfengStatResult.getData() == null)
        {
            logger.error("gong feng project result is empty! gong feng id: {}", gfProjectIds.toString());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{"gongfengStat empty"}, null);
        }
        return gongfengStatResult.getData();
    }


    protected QueryTaskListReqVO getQueryTaskListReqVO(String toolName, Integer bgId, Integer deptId, int taskStatus,
            Integer pageNum, Integer pageSize, Sort.Direction sortType)
    {
        QueryTaskListReqVO queryTaskListReqVO = new QueryTaskListReqVO();
        queryTaskListReqVO.setBgId(bgId);
        queryTaskListReqVO.setStatus(taskStatus);
        queryTaskListReqVO.setToolName(toolName);

        if (deptId != null)
        {
            queryTaskListReqVO.setDeptIds(Lists.newArrayList(deptId));
        }

        queryTaskListReqVO.setSortType(sortType == null ? Sort.Direction.ASC.name() : sortType.name());
        queryTaskListReqVO.setPageNum(pageNum);
        queryTaskListReqVO.setPageSize(pageSize);
        return queryTaskListReqVO;
    }


    /**
     * 数据列表分页
     *
     * @param pageNum        页码
     * @param pageSize       页数
     * @param statisticsTask 任务列表
     * @return page obj
     */
    protected Page<TaskDefectVO> getTaskDefectVoPage(Integer pageNum, Integer pageSize, List<TaskDefectVO> statisticsTask)
    {
        if (CollectionUtils.isNotEmpty(statisticsTask))
        {
            statisticsTask.sort((o1, o2) -> Long.compare(o2.getTaskId(), o1.getTaskId()));
        }

        int totalSize = statisticsTask.size();
        int pageSizeNum = 10;
        int totalPageNum = 0;
        if (pageSize != null && pageSize >= 0)
        {
            pageSizeNum = pageSize;
        }
        if (totalSize > 0)
        {
            totalPageNum = (totalSize + pageSizeNum - 1) / pageSizeNum;
        }

        pageNum = pageNum == null || pageNum - 1 < 0 ? 0 : pageNum - 1;
        int subListBeginIdx = pageNum * pageSizeNum;
        int subListEndIdx = subListBeginIdx + pageSizeNum;
        if (subListBeginIdx > totalSize)
        {
            subListBeginIdx = 0;
        }
        List<TaskDefectVO> taskDefectVoList =
                statisticsTask.subList(subListBeginIdx, subListEndIdx > totalSize ? totalSize : subListEndIdx);

        return new Page<>(totalSize, pageNum == 0 ? 1 : pageNum + 1, pageSizeNum, totalPageNum, taskDefectVoList);
    }

    /**
     * 统计使用各规则的任务数
     *
     * @param statisticsChecker 规则统计的响应结构
     * @param taskCheckersMap   各任务开启的规则情况
     * @param taskIdSet         符合条件的任务集合
     */
    protected void statisticsCheckerTaskCount(Map<String, PkgDefectDetailVO> statisticsChecker,
            Map<Long, List<String>> taskCheckersMap, Collection<Long> taskIdSet)
    {
        for (Map.Entry<Long, List<String>> entry : taskCheckersMap.entrySet())
        {
            // 过滤不符合条件的任务ID
            if (!taskIdSet.contains(entry.getKey()))
            {
                continue;
            }

            List<String> taskCheckers = entry.getValue();
            for (String checker : taskCheckers)
            {
                statisticsChecker.get(checker).addTaskCount();
            }
        }
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
    protected <T> org.springframework.data.domain.Page<T> sortAndPage(int pageNum, int pageSize, String sortField,
                                                                      Sort.Direction sortType, List<T> defectVOs)
    {
        if (StringUtils.isEmpty(sortField))
        {
            sortField = "severity";
        }
        if (null == sortType)
        {
            sortType = Sort.Direction.ASC;
        }

        // 严重程度要跟前端传入的排序类型相反
        if ("severity".equals(sortField))
        {
            if (sortType.isAscending())
            {
                sortType = Sort.Direction.DESC;
            }
            else
            {
                sortType = Sort.Direction.ASC;
            }
        }
        ListSortUtil.sort(defectVOs, sortField, sortType.name());
        int total = defectVOs.size();
        pageNum = pageNum - 1 < 0 ? 0 : pageNum - 1;
        pageSize = pageSize <= 0 ? 10 : pageSize;
        int subListBeginIdx = pageNum * pageSize;
        int subListEndIdx = subListBeginIdx + pageSize;
        if (subListBeginIdx > total)
        {
            subListBeginIdx = 0;
        }
        defectVOs = defectVOs.subList(subListBeginIdx, subListEndIdx > total ? total : subListEndIdx);

        //封装分页类
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(sortType, sortField));
        return new PageImpl<>(defectVOs, pageable, total);
    }

    /**
     * 判断是否匹配前端传入的状态条件，不匹配返回true,匹配返回false
     *
     * @param condStatusList
     * @param status
     * @return
     */
    protected boolean isNotMatchStatus(Set<String> condStatusList, int status)
    {
        boolean notMatchStatus = true;
        for (String condStatus : condStatusList)
        {
            // 查询条件是待修复，且告警状态是NEW
            if (ComConstants.DefectStatus.NEW.value() == Integer.parseInt(condStatus)
                    && ComConstants.DefectStatus.NEW.value() == status)
            {
                notMatchStatus = false;
                break;
            }
            // 查询条件是已修复或已忽略，且告警状态是匹配
            else if (ComConstants.DefectStatus.NEW.value() < Integer.parseInt(condStatus)
                    && (Integer.parseInt(condStatus) & status) > 0)
            {
                notMatchStatus = false;
                break;
            }
        }

        return notMatchStatus;
    }

    protected DefectDetailVO getDefectDetailVO(long taskId, DefectEntity defectEntity)
    {
        DefectDetailVO defectDetailVO = new DefectDetailVO();
        BeanUtils.copyProperties(defectEntity, defectDetailVO);
        List<DefectEntity.DefectInstance> defectInstanceList = defectEntity.getDefectInstances();
        if (CollectionUtils.isNotEmpty(defectInstanceList))
        {
            for (DefectEntity.DefectInstance defectInstance : defectInstanceList)
            {
                List<DefectEntity.Trace> traces = defectInstance.getTraces();
                for (DefectEntity.Trace trace : traces)
                {
                    parseTrace(defectDetailVO, trace);
                }
            }
            if (defectDetailVO.getFileInfoMap().size() < 1)
            {
                String md5 = defectDetailVO.getFileMD5();
                if (StringUtils.isEmpty(md5))
                {
                    md5 = MD5Utils.getMD5(defectDetailVO.getFilePathname());
                }
                DefectDetailVO.FileInfo fileInfo = new DefectDetailVO.FileInfo();
                fileInfo.setFilePathname(defectDetailVO.getFilePathname());
                fileInfo.setFileMD5(md5);
                fileInfo.setMinDefectLineNum(defectDetailVO.getLineNumber());
                fileInfo.setMaxDefectLineNum(defectDetailVO.getLineNumber());
                defectDetailVO.getFileInfoMap().put(md5, fileInfo);
            }
        }
        defectDetailVO = getFilesContent(defectDetailVO);
        return defectDetailVO;
    }

    protected DefectDetailVO getFilesContent(DefectDetailVO defectDetailVO)
    {
        return defectDetailVO;
    }

    /**
     * 解析告警实例的跟踪事件，转换为Trace对象，并获取文件信息
     *
     * @param defectDetailVO
     * @param trace
     */
    private void parseTrace(DefectDetailVO defectDetailVO, DefectEntity.Trace trace)
    {
        if (trace.getLinkTrace() != null)
        {
            for (DefectEntity.Trace linkTrace : trace.getLinkTrace())
            {
                parseTrace(defectDetailVO, linkTrace);
            }
        }

        String fileName = trace.getFilePathname();
        int lineNumber = trace.getLineNumber();

        String md5 = trace.getFileMD5();
        if (StringUtils.isEmpty(md5))
        {
            md5 = MD5Utils.getMD5(fileName);
            trace.setFileMD5(md5);
        }

        DefectDetailVO.FileInfo fileInfo = defectDetailVO.getFileInfoMap().get(md5);
        if (fileInfo == null)
        {
            fileInfo = new DefectDetailVO.FileInfo();
            fileInfo.setFilePathname(fileName);
            fileInfo.setFileMD5(md5);
            fileInfo.setMinDefectLineNum(lineNumber);
            fileInfo.setMaxDefectLineNum(lineNumber);
            defectDetailVO.getFileInfoMap().put(md5, fileInfo);
        }
        else
        {
            if (lineNumber < fileInfo.getMinDefectLineNum())
            {
                fileInfo.setMinDefectLineNum(lineNumber);
            }
            else if (lineNumber > fileInfo.getMaxDefectLineNum())
            {
                fileInfo.setMaxDefectLineNum(lineNumber);
            }
        }
    }

    /**
     * 获取提单步骤的值，子类必须实现这个方法
     * 普通工具有4个分析步骤：1：代码下载，2、代码下载；3：代码扫描，4：代码缺陷提交
     * Klocwork/Coverity有5个分析步骤：1：上传，2：排队状态，3、分析中；4：缺陷提交，5：提单
     *
     * @return
     */
    public abstract int getSubmitStepNum();


    protected CodeCommentVO convertCodeComment(CodeCommentEntity codeCommentEntity)
    {
        //设置告警评论
        CodeCommentVO codeCommentVO = new CodeCommentVO();
        BeanUtils.copyProperties(codeCommentEntity, codeCommentVO);
        codeCommentVO.setCommentList(codeCommentEntity.getCommentList().stream().
                map(singleCommentEntity -> {
                    SingleCommentVO singleCommentVO = new SingleCommentVO();
                    BeanUtils.copyProperties(singleCommentEntity, singleCommentVO);
                    return singleCommentVO;
                }).collect(Collectors.toList())
        );
        return codeCommentVO;
    }

    /**
     * 根据标志修改时间与最近一次分析时间比较来判断告警是否是被标记后仍未被修改
     *
     * @param mark
     * @param markTime
     * @param lastAnalyzeTime
     * @return
     */
    public Integer convertMarkStatus(Integer mark, Long markTime, Long lastAnalyzeTime)
    {
        if (mark != null && mark == ComConstants.MarkStatus.MARKED.value() && markTime != null)
        {
            if (lastAnalyzeTime != null && markTime < lastAnalyzeTime)
            {
                mark = ComConstants.MarkStatus.NOT_FIXED.value();
            }
        }
        return mark;
    }

    /**
     *  批量获取最近分析日志
     *
     * @param taskIdSet 任务ID集合
     * @param toolName  指定工具名
     * @return map
     */
    protected Map<Long, TaskLogVO> getTaskLogVoMap(Set<Long> taskIdSet, String toolName)
    {
        Map<Long, TaskLogVO> taskLogVoMap = Maps.newHashMap();
        if (CollectionUtils.isEmpty(taskIdSet))
        {
            return taskLogVoMap;
        }

        List<TaskLogVO> taskLogVoList = taskLogService.batchTaskLogList(taskIdSet, toolName);
        if (CollectionUtils.isNotEmpty(taskLogVoList))
        {
            taskLogVoList.forEach(taskLogVO -> taskLogVoMap.put(taskLogVO.getTaskId(), taskLogVO));
        }

        return taskLogVoMap;
    }

    /**
     * 赋值任务最近分析状态
     *
     * @param taskId 任务ID
     * @param taskLogVoMap 最新分析日志
     * @param taskDefectVO 告警统计对象
     */
    protected void setAnalyzeDateStatus(long taskId, Map<Long, TaskLogVO> taskLogVoMap, TaskDefectVO taskDefectVO)
    {
        TaskLogVO taskLogVO = taskLogVoMap.get(taskId);
        String analyzeDateStr = "";
        if (taskLogVO != null)
        {
            int currStep = taskLogVO.getCurrStep();
            int flag = taskLogVO.getFlag();
            long analyzeStartTime = taskLogVO.getStartTime();
            String currStepStr = ConvertUtil.convertStep4Cov(currStep);
            String stepFlag = ConvertUtil.getStepFlag(flag);
            // 2019-10-11 分析成功
            analyzeDateStr = DateTimeUtils.second2DateString(analyzeStartTime) + " " + currStepStr + stepFlag;
        }
        taskDefectVO.setAnalyzeDate(analyzeDateStr);
    }

    /**
     * 多条件批量获取任务详情列表
     *
     * @param deptTaskDefectReqVO reqObj
     * @return list
     */
    protected List<TaskDetailVO> getTaskDetailVoList(DeptTaskDefectReqVO deptTaskDefectReqVO)
    {
        QueryTaskListReqVO queryTaskListReqVO = new QueryTaskListReqVO();
        queryTaskListReqVO.setTaskIds(deptTaskDefectReqVO.getTaskIds());
        queryTaskListReqVO.setBgId(deptTaskDefectReqVO.getBgId());
        queryTaskListReqVO.setDeptIds(deptTaskDefectReqVO.getDeptIds());
        queryTaskListReqVO.setCreateFrom(deptTaskDefectReqVO.getCreateFrom());
        queryTaskListReqVO.setStatus(ComConstants.Status.ENABLE.value());

        Result<List<TaskDetailVO>> batchGetTaskListResult =
                client.get(ServiceTaskRestResource.class).batchGetTaskList(queryTaskListReqVO);
        return batchGetTaskListResult.getData();
    }

    /**
     * 按任务ID批量获取有效代码行
     *
     * @param taskIdList 任务ID集合
     * @param languages  语言类型列表
     * @return map
     */
    protected Map<Long, Long> getCodeLineNumMap(List<Long> taskIdList, List<String> languages)
    {
        if (CollectionUtils.isEmpty(languages))
        {
            languages = Lists.newArrayList("C#", "C++", "C/C++ Header", "Java", "PHP", "Objective C", "Objective C++",
                    "Python", "JavaScript", "Ruby", "Go", "Swift", "TypeScript");
        }
        // 分组汇总各任务的代码行数
        List<CLOCStatisticEntity> entityList = clocStatisticsDao.batchQueryCodeLineSum(taskIdList, languages);

        Map<Long, Long> codeLineCountMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(entityList))
        {
            codeLineCountMap = entityList.stream()
                    .collect(Collectors.toMap(CLOCStatisticEntity::getTaskId, CLOCStatisticEntity::getSumCode));
        }
        return codeLineCountMap;
    }

    protected String getFileContent(long taskId, String projectId, String userId, String url, String repoId,
                                    String relPath, String revision, String branch, String subModule)
    {
        // check if is oauth
        boolean isOauth = false;

        Pair<String, String> pair = getTaskCreateFrom(projectId, taskId);
        String createFrom = pair.getKey();
        String realProjectId = pair.getValue();

        if (StringUtils.isNotBlank(url) && !gitHost.isEmpty() && url.contains(URI.create(gitHost).getHost()) && !branch.equals("devops-virtual-branch")) {
            if (!ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(createFrom)
                || realProjectId.startsWith("CUSTOMPROJ_"))
            {
                isOauth = true;
            }
        }

        // get file content
        String content;
        if (isOauth)
        {
            String oauthUserId = userId;
            if (!realProjectId.startsWith("CUSTOMPROJ_")
                && AuthApiUtils.INSTANCE.isAdminMember(redisTemplate, userId)) {
                Result<TaskDetailVO> result = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
                oauthUserId = result.getData().getUpdatedBy();
            }

            content = pipelineScmService.getFileContentOauth(oauthUserId, GitUtil.INSTANCE.getProjectName(url),
                relPath, (revision != null ? revision : branch));
        } else if (projectId != null && (projectId.startsWith("git_") || projectId.startsWith("github_"))) {
            content = pipelineScmService.getStreamFileContent(projectId, userId, url, relPath, revision, branch);
        }else {
            content = pipelineScmService.getFileContent(taskId, repoId, relPath, revision, branch, subModule, createFrom);
        }

        return content;
    }

    private Pair<String, String> getTaskCreateFrom(String projectId, long taskId) {
        String createFrom = "";
        String realProjectId = projectId;
        // get from redis first
        Object value = redisTemplate.opsForHash().get(AuthExConstantsKt.PREFIX_TASK_INFO + taskId, KEY_CREATE_FROM);
        if (value instanceof String) {
            createFrom = (String) value;
        }

        // get from remote
        if (StringUtils.isBlank(createFrom) || StringUtils.isBlank(projectId)) {
            Result<TaskDetailVO> result = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
            if (result.isNotOk() || result.getData() == null) {
                logger.error("fail to get task info by id, taskId: {} | err: {}", taskId, result.getMessage());
                throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
            }
            createFrom = result.getData().getCreateFrom();
            realProjectId = result.getData().getProjectId();
        }

        return ImmutablePair.of(createFrom, realProjectId);
    }

    protected Map<String, Long> getLastAnalyzeTimeMap(long taskId, List<String> toolNameSet) {
        log.info("start to find defect and group by toolName and sort by time desc for task: {}", taskId);

        Map<String, Long> lastAnalyzeTimeMap = new HashMap<>();
        toolNameSet.forEach(it -> lastAnalyzeTimeMap.put(it, 0L));

        List<LintStatisticEntity> statisticEntityList =
                lintStatisticRepository.findByTaskIdAndToolNameIn(taskId, toolNameSet);
        statisticEntityList.forEach(it -> {
            Long lastAnalyzeTime = lastAnalyzeTimeMap.get(it.getToolName());
            if (lastAnalyzeTime == null ||lastAnalyzeTime < it.getTime()) {
                lastAnalyzeTimeMap.put(it.getToolName(), it.getTime());
            }
        });

        log.info("finish to find defect and group by toolName and sort by time desc for task: {}", taskId);

        return lastAnalyzeTimeMap;
    }
}
