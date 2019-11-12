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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.codecc.defect.dao.mongorepository.FirstAnalysisSuccessTimeRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectDao;
import com.tencent.bk.codecc.defect.model.FirstAnalysisSuccessEntity;
import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.model.LintFileQueryRspEntity;
import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.MultitoolCheckerService;
import com.tencent.bk.codecc.defect.service.PipelineService;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.defect.vo.common.*;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.exception.StreamException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.PathUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.beans.PropertyDescriptor;
import java.io.IOException;
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
public class LintQueryWarningBizServiceImpl extends AbstractQueryWarningBizService
{
    private static Logger logger = LoggerFactory.getLogger(LintQueryWarningBizServiceImpl.class);

    @Autowired
    private Client client;

    @Autowired
    private MultitoolCheckerService multitoolCheckerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LintDefectDao lintDefectDao;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private BizServiceFactory<TreeService> treeServiceBizServiceFactory;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private FirstAnalysisSuccessTimeRepository firstAnalysisSuccessTimeRepository;


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

                boolean anyMatchType = checkerCustomList.stream()
                        .anyMatch(e -> e.getTypeName().equals(checkerType));
                // 构建一种规则类型有多个告警规则
                if (anyMatchType)
                {
                    checkerCustomList.stream()
                            .filter(e -> e.getTypeName().equals(checkerType))
                            .findFirst()
                            .get()
                            .getCheckers()
                            .add(checker);
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


    @Override
    public CommonFileQueryRspVO processQueryWarningRequest(long taskId, CommonFileQueryReqVO queryWarningReq, int pageNum, int pageSize, String sortField, Sort.Direction sortType)
    {
        //获取任务信息
        //TaskDetailVO taskDetailVO = getTaskInfo(taskId);
        //获取工具信息
        ToolConfigInfoVO toolConfigInfoVO = getToolInfo(taskId, queryWarningReq.getToolName());

        Sort pageSort;
        if (StringUtils.isEmpty(sortField) || null == sortType)
        {
            pageSort = new Sort(Sort.Direction.ASC, "defect_count");
        }
        else
        {
            pageSort = new Sort(sortType, sortField);
        }
        //封装分页类
        Pageable pageable = new PageRequest(pageNum - 1 < 0 ? 0 : pageNum - 1, pageSize <= 0 ? 10 : pageSize, pageSort);
        //1.获取相同包id下的规则集合
        Set<String> pkgChecker = multitoolCheckerService.queryPkgRealCheckers(queryWarningReq.getPkgId(), toolConfigInfoVO);
        //查询数值
        LintFileQueryRspEntity lintFileInfoEntityPage = lintDefectDao.findLintFileByParam(taskId, queryWarningReq.getToolName(), queryWarningReq.getFileList(),
                queryWarningReq.getChecker(), queryWarningReq.getAuthor(), queryWarningReq.getFileType(), queryWarningReq.getSeverity(), pkgChecker,
                pageable);
        FirstAnalysisSuccessEntity firstAnalysisSuccessEntity = firstAnalysisSuccessTimeRepository.findFirstByTaskIdAndToolName(taskId, queryWarningReq.getToolName());
        if(null != firstAnalysisSuccessEntity)
        {
            lintFileInfoEntityPage.setFirstAnalysisSuccessTime(firstAnalysisSuccessEntity.getFirstAnalysisSuccessTime());
        }
        //转换为视图类
        List<LintFileEntity> lintFileInfoEntityList = lintFileInfoEntityPage.getLintFileList().getContent();
        List<LintFileVO> lintFileVOs = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(lintFileInfoEntityList))
        {
            // 排除规则为空的值
//            lintFileInfoEntityList = lintFileInfoEntityList.stream().filter(lint -> CollectionUtils.isNotEmpty(lint.getCheckerList()))
//                    .collect(Collectors.toList());
            try
            {
                //获取相对路径
                batchGetRelativePath(lintFileInfoEntityList);
                String lintFileStr = objectMapper.writeValueAsString(lintFileInfoEntityList);
                lintFileVOs = objectMapper.readValue(lintFileStr, new TypeReference<List<LintFileVO>>()
                {
                });
            }
            catch (IOException e)
            {
                String message = "string conversion LintFileVO error";
                logger.error(message);
                throw new StreamException(message);
            }
        }
        Page<LintFileVO> lintFileVOPage = new PageImpl<>(lintFileVOs, pageable, lintFileInfoEntityPage.getLintFileList().getTotalElements());
        LintFileQueryRspVO lintFileQueryRspVO = new LintFileQueryRspVO();
        BeanUtils.copyProperties(lintFileInfoEntityPage, lintFileQueryRspVO, "lintFileList");
        lintFileQueryRspVO.setLintFileList(lintFileVOPage);
        return lintFileQueryRspVO;
    }

    @Override
    public CommonDefectQueryRspVO processQueryWarningDetailRequest(long taskId, CommonDefectQueryReqVO queryWarningDetailReq,
                                                                   String sortField, Sort.Direction sortType)
    {
        if (!(queryWarningDetailReq instanceof LintDefectQueryReqVO))
        {
            logger.error("input param class type incorrect!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"queryWarningDetailReq"}, null);
        }
        LintDefectQueryReqVO lintDefectQueryReqVO = (LintDefectQueryReqVO) queryWarningDetailReq;
        //获取任务信息
//        TaskDetailVO taskDetailVO = getTaskInfo(taskId);
        //获取工具信息
        ToolConfigInfoVO toolConfigInfoVO = getToolInfo(taskId, lintDefectQueryReqVO.getToolName());

        LintDefectQueryRspVO lintDefectQueryRspVO = new LintDefectQueryRspVO();
        //1.获取相同包id下的规则集合
        Set<String> pkgChecker = multitoolCheckerService.queryPkgRealCheckers(lintDefectQueryReqVO.getPkgId(), toolConfigInfoVO);
        //2. lint类文件查询结果
        LintFileEntity lintFileEntity = lintDefectDao.findDefectByParam(lintDefectQueryReqVO.getEntityId(),
                lintDefectQueryReqVO.getChecker(), lintDefectQueryReqVO.getAuthor());
        if (null == lintFileEntity)
        {
            logger.info("empty lint file found!");
            return lintDefectQueryRspVO;
        }
        List<LintDefectEntity> lintDefectEntityList = lintFileEntity.getDefectList();

        //对告警再进行过滤
        lintDefectEntityList = filterDefectByParam(lintDefectEntityList, lintDefectQueryReqVO.getChecker(), lintDefectQueryReqVO.getAuthor(),
                lintDefectQueryReqVO.getSeverity(), pkgChecker);
        //根据字段进行排序
        lintDefectEntityList.sort((o1, o2) -> sortDefectByField(o1, o2, sortField, null == sortType ? "ASC" : sortType.name()));



        lintFileEntity.setDefectList(lintDefectEntityList);

        List<LintDefectVO> lintDefectVOList;
        if (CollectionUtils.isEmpty(lintDefectEntityList))
        {
            lintDefectVOList = new ArrayList<>();
        }
        else
        {
            lintDefectVOList = lintDefectEntityList.stream()
                    .map(lintDefectEntity ->
                    {
                        LintDefectVO lintDefectVO = new LintDefectVO();
                        BeanUtils.copyProperties(lintDefectEntity, lintDefectVO);
                        return lintDefectVO;
                    })
                    .collect(Collectors.toList());
        }

        String content = pipelineService.getFileContent(taskId, lintFileEntity.getRepoId(), lintFileEntity.getRelPath(),
                lintFileEntity.getRevision(), lintFileEntity.getBranch(), lintFileEntity.getSubModule());
        lintDefectQueryRspVO.setFileContent(content);

        //4.获取文件相对路径
        String relativePath = PathUtils.getRelativePath(lintFileEntity.getUrl(), lintFileEntity.getRelPath());
        lintDefectQueryRspVO.setRelativePath(relativePath);

        //5.获取告警规则详情和规则类型
        getCheckerDetailAndType(lintDefectVOList, toolConfigInfoVO, toolConfigInfoVO.getToolName());

        //6.组装告警详情model
        String filePath = lintFileEntity.getFilePath();
        lintDefectQueryRspVO.setFilePath(filePath);
        int fileNameIndex = filePath.lastIndexOf("/");
        if (fileNameIndex == -1)
        {
            fileNameIndex = filePath.lastIndexOf("\\");
        }
        lintDefectQueryRspVO.setFileName(filePath.substring(fileNameIndex + 1));
        lintDefectQueryRspVO.setLintDefectList(lintDefectVOList);
        return lintDefectQueryRspVO;
    }


    /**
     * 获取任务信息
     *
     * @param taskId
     * @return
     */
    private TaskDetailVO getTaskInfo(long taskId)
    {
        Result<TaskDetailVO> taskDetailVOResult;
        try
        {
            taskDetailVOResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        }
        catch (Exception e)
        {
            logger.error("get task detail info fail! task id: {}", taskId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        if (taskDetailVOResult.isNotOk() || null == taskDetailVOResult.getData())
        {
            logger.error("get task detail info fail! task id: {}", taskId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return taskDetailVOResult.getData();
    }

    /**
     * 获取工具信息
     *
     * @param taskId
     * @param toolName
     * @return
     */
    private ToolConfigInfoVO getToolInfo(long taskId, String toolName)
    {
        Result<ToolConfigInfoVO> toolConfigInfoVOResult = client.get(ServiceToolRestResource.class).getToolByTaskIdAndName(taskId, toolName);
        if (toolConfigInfoVOResult.isNotOk() || null == toolConfigInfoVOResult.getData())
        {
            logger.error("get tool info fail! task id: {}, tool name: {}", taskId, toolName);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return toolConfigInfoVOResult.getData();
    }

    private int sortDefectByField(LintDefectEntity lintDefectEntity1, LintDefectEntity lintDefectEntity2, String sortField, String sortType)
    {
        try
        {
            PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor(LintDefectEntity.class, StringUtils.isEmpty(sortField) ? "lineNum" : sortField);
            Method readMethod = propertyDescriptor.getReadMethod();
            switch (sortType)
            {
                case "ASC":
                    return (int) readMethod.invoke(lintDefectEntity1) - (int) readMethod.invoke(lintDefectEntity2);
                case "DESC":
                    return (int) readMethod.invoke(lintDefectEntity2) - (int) readMethod.invoke(lintDefectEntity1);
                default:
                    return (int) readMethod.invoke(lintDefectEntity1) - (int) readMethod.invoke(lintDefectEntity2);
            }
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            logger.error("invoke read method error! sort field: {}", sortField);
            return 1;
        }
    }

    @Override
    public QueryWarningPageInitRspVO processQueryWarningPageInitRequest(Long taskId, String toolName)
    {
        List<LintFileEntity> fileInfos = lintDefectDao.findFileInfoList(taskId, toolName);

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
                    authors.addAll(fileInfo.getAuthorList());
                }

                // 设置规则
                if (CollectionUtils.isNotEmpty(fileInfo.getCheckerList()))
                {
                    checkerList.addAll(fileInfo.getCheckerList());
                }

                // 获取所有警告文件的相对路径
                String relativePath = PathUtils.getRelativePath(fileInfo.getUrl(), fileInfo.getRelPath());
                if (org.apache.commons.lang3.StringUtils.isNotBlank(relativePath))
                {
                    defectPaths.add(relativePath);
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

    /**
     * 获取相对路径
     *
     * @param lintFileInfoList
     * @return
     */
    private void batchGetRelativePath(List<LintFileEntity> lintFileInfoList)
    {
        if (CollectionUtils.isNotEmpty(lintFileInfoList))
        {
            //加入相对路径
            for (LintFileEntity defect : lintFileInfoList)
            {
                String relativePath = PathUtils.getRelativePath(defect.getUrl(), defect.getRelPath());
                defect.setRelPath(relativePath);
            }
        }
    }


    private void getCheckerDetailAndType(List<LintDefectVO> lintDefectVOList,
                                         ToolConfigInfoVO toolConfigInfoVO, String toolName)
    {
        if (CollectionUtils.isNotEmpty(lintDefectVOList))
        {
            //            List<CheckerDetailVO> allCheckerList = new ArrayList<>();
            String pattern = (String) redisTemplate.opsForHash().get(String.format("%s%s", RedisKeyConstants.PREFIX_TOOL, toolName), RedisKeyConstants.FILED_PATTERN);
            Map<String, CheckerDetailVO> checkers = multitoolCheckerService.queryAllChecker(toolConfigInfoVO.getToolName());

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


    /**
     * 根据参数过滤告警
     *
     * @param defectEntityList
     * @param checker
     * @param author
     * @param severity
     * @param pkgChecker
     * @return
     */
    private List<LintDefectEntity> filterDefectByParam(List<LintDefectEntity> defectEntityList, String checker, String author,
                                                       List<String> severity, Set<String> pkgChecker)
    {
        return defectEntityList.stream().filter(lintDefectEntity ->
        {
            if (StringUtils.isNotEmpty(checker) && !checker.equals(lintDefectEntity.getChecker()))
            {
                return false;
            }
            if (StringUtils.isNotEmpty(author) && !author.equals(lintDefectEntity.getAuthor()))
            {
                return false;
            }
            if (CollectionUtils.isNotEmpty(severity))
            {
                List<String> finalSeverity = severity.stream().map(s ->
                {
                    if (Integer.valueOf(s) == ComConstants.PROMPT_IN_DB)
                    {
                        return String.valueOf(ComConstants.PROMPT);
                    }
                    else
                    {
                        return s;
                    }
                }).
                        collect(Collectors.toList());
                if (!finalSeverity.contains(String.valueOf(lintDefectEntity.getSeverity())))
                {
                    return false;
                }
            }
            if (CollectionUtils.isNotEmpty(pkgChecker) && !pkgChecker.contains(lintDefectEntity.getChecker()))
            {
                return false;
            }
            if (ComConstants.DefectStatus.NEW.value() != lintDefectEntity.getStatus())
            {
                return false;
            }
            return true;
        })
                .collect(Collectors.toList());

    }


//    @Override
//    public Result processQueryWarningDetailRequest(ToolConfigBaseVO queryWarningDetailReq)
//    {
//        System.out.println("processQueryWarningDetailRequest");
//        return null;
//    }
//
//    @Override
//    public Result processQueryWarningPageInitRequest(ToolConfigBaseVO queryCheckerListReq)
//    {
//        return null;
//    }
//
//    @Override
//    public Result processChangeBugAuthor(ToolConfigBaseVO changeBugAuthorReq)
//    {
//        return null;
//    }
//
//    @Override
//    public Result processGetFileContentSegmentRequest(ToolConfigBaseVO reqModel)
//    {
//        return null;
//    }
}
