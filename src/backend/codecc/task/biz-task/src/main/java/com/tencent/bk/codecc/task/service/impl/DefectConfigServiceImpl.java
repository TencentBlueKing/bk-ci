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

package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.defect.api.ServiceCheckerRestResource;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.IgnoreCheckerVO;
import com.tencent.bk.codecc.task.dao.ToolMetaCache;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.service.DefectConfigService;
import com.tencent.bk.codecc.task.service.PipelineService;
import com.tencent.bk.codecc.task.vo.DefectConfigInfoVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.repository.pojo.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 获取配置服务类
 *
 * @version V1.0
 * @date 2019/5/20
 */
@Service
public class DefectConfigServiceImpl implements DefectConfigService
{
    private static Logger logger = LoggerFactory.getLogger(DefectConfigServiceImpl.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private ToolMetaCache toolMetaCache;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private Client client;

    @Override
    public DefectConfigInfoVO getDefectConfig(String streamName, String multiToolType)
    {
        DefectConfigInfoVO defectConfigInfoVO = new DefectConfigInfoVO();
        //获取任务详细信息
        TaskInfoEntity taskInfoEntity = taskRepository.findByNameEn(streamName);
        if (null == taskInfoEntity)
        {
            logger.info("empty task info found out! stream name: {}", streamName);
            return defectConfigInfoVO;
        }
        //获取工具详细信息
        ToolConfigInfoEntity toolConfigInfoEntity = toolRepository.findByTaskIdAndToolName(taskInfoEntity.getTaskId(), multiToolType);
        defectConfigInfoVO.setTaskId(taskInfoEntity.getTaskId());
        defectConfigInfoVO.setMultiToolType(multiToolType.toUpperCase());
        defectConfigInfoVO.setNameEn(streamName);
        //设置代码库信息
        if (ComConstants.BsTaskCreateFrom.BS_CODECC.value().equalsIgnoreCase(taskInfoEntity.getCreateFrom()))
        {
            setRepoInfo(taskInfoEntity, defectConfigInfoVO);
        }
        defectConfigInfoVO.setParamJson(toolConfigInfoEntity.getParamJson());


        //由于和流水线强相关，所以不需要加入认证信息
        defectConfigInfoVO.setProjOwner(CollectionUtils.isNotEmpty(taskInfoEntity.getTaskOwner()) ? taskInfoEntity.getTaskOwner().get(0) : "");
        //加入过滤路径
        defectConfigInfoVO.setSkipPaths(getFilterPath(taskInfoEntity));
        //加屏蔽规则
        addChecker(toolConfigInfoEntity, defectConfigInfoVO, taskInfoEntity, multiToolType);
        logger.info("get defect config finish!, task id: {}, tool type: {}", taskInfoEntity.getTaskId(),
                multiToolType);
        return defectConfigInfoVO;
    }


    /**
     * 获取过滤路径
     *
     * @param taskInfoEntity
     * @return
     */
    private String getFilterPath(TaskInfoEntity taskInfoEntity)
    {
        StringBuilder filterPathStr = new StringBuilder();
        if (CollectionUtils.isNotEmpty(taskInfoEntity.getDefaultFilterPath()))
        {
            taskInfoEntity.getDefaultFilterPath()
                    .forEach(filterPath -> filterPathStr.append(filterPath).append(";"));
        }
        if (CollectionUtils.isNotEmpty(taskInfoEntity.getFilterPath()))
        {
            taskInfoEntity.getFilterPath()
                    .forEach(filterPath -> filterPathStr.append(filterPath).append(";"));
        }
        return filterPathStr.toString();
    }


    /**
     * 设置代码库信息
     *
     * @param taskInfoEntity
     * @param defectConfigInfoVO
     */
    private void setRepoInfo(TaskInfoEntity taskInfoEntity, DefectConfigInfoVO defectConfigInfoVO)
    {
        logger.info("project id is {}, repo hash id: {}", taskInfoEntity.getProjectId(), taskInfoEntity.getRepoHashId());
        //由于现在代码库是任务维度的，则直接通过任务的repoId进行获取
        Repository repository = pipelineService.getRepoDetail(taskInfoEntity.getProjectId(), taskInfoEntity.getRepoHashId());
        //设置代码库信息
        if (repository instanceof CodeSvnRepository)
        {
            CodeSvnRepository codeSvnRepository = (CodeSvnRepository) repository;
            defectConfigInfoVO.setUrl(codeSvnRepository.getUrl());
            defectConfigInfoVO.setScmType(ComConstants.CodeHostingType.SVN.name());
        }
        else if (repository instanceof CodeGitRepository)
        {
            CodeGitRepository codeGitRepository = (CodeGitRepository) repository;
            defectConfigInfoVO.setUrl(codeGitRepository.getUrl());
            defectConfigInfoVO.setScmType(ComConstants.CodeHostingType.GIT.name());
            defectConfigInfoVO.setGitBranch(taskInfoEntity.getBranch());
        }
        else if (repository instanceof CodeGitlabRepository)
        {
            CodeGitlabRepository codeGitlabRepository = (CodeGitlabRepository) repository;
            defectConfigInfoVO.setUrl(codeGitlabRepository.getUrl());
            //确认gitlab也是git
            defectConfigInfoVO.setScmType(ComConstants.CodeHostingType.GIT.name());
            defectConfigInfoVO.setGitBranch(taskInfoEntity.getBranch());
        }
        else if (repository instanceof GithubRepository)
        {
            GithubRepository githubRepository = (GithubRepository) repository;
            defectConfigInfoVO.setUrl(githubRepository.getUrl());
            defectConfigInfoVO.setScmType(ComConstants.CodeHostingType.GITHUB.name());
            defectConfigInfoVO.setGitBranch(taskInfoEntity.getBranch());
        }
        else
        {
            if (null != repository)
            {
                CodeTGitRepository codeTGitRepository = (CodeTGitRepository) repository;
                defectConfigInfoVO.setUrl(codeTGitRepository.getUrl());
                defectConfigInfoVO.setScmType(ComConstants.CodeHostingType.GIT.name());
                defectConfigInfoVO.setGitBranch(taskInfoEntity.getBranch());
            }
        }
    }


    /**
     * 增加规则信息
     *
     * @param defectConfigInfoVO
     * @param taskInfoEntity
     * @param multiToolType
     */
    private void addChecker(ToolConfigInfoEntity toolConfigInfoEntity, DefectConfigInfoVO defectConfigInfoVO, TaskInfoEntity taskInfoEntity, String multiToolType)
    {

        if (null == toolConfigInfoEntity)
        {
            return;
        }
        List<String> checkerDetailEntityList = new ArrayList<>();
        Result<IgnoreCheckerVO> ignoreCheckerVOResult = client.get(ServiceCheckerRestResource.class).getIgnoreCheckerInfo(toolConfigInfoEntity.getTaskId(),
                                                toolConfigInfoEntity.getToolName());
        if(ignoreCheckerVOResult.isOk() || null != ignoreCheckerVOResult.getData())
        {
            checkerDetailEntityList = ignoreCheckerVOResult.getData().getIgnoreList();
        }
        Set<String> skipCheckers = new HashSet<>();
        if (CollectionUtils.isNotEmpty(checkerDetailEntityList))
        {
            skipCheckers = checkerDetailEntityList.stream()
                    .distinct()
                    .collect(Collectors.toSet());
        }

        defectConfigInfoVO.setSkipCheckers(String.join(";", skipCheckers));

        if (ComConstants.ToolPattern.LINT.name().equals(toolMetaCache.getToolPattern(multiToolType)))
        {
            ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
            BeanUtils.copyProperties(toolConfigInfoEntity, toolConfigInfoVO);
            toolConfigInfoVO.setIgnoreCheckers(checkerDetailEntityList);
            //加入已开启规则
            Result<Map<String, CheckerDetailVO>> openCheckerResult = client.get(ServiceCheckerRestResource.class).queryOpenChecker(toolConfigInfoVO);
            if (openCheckerResult.isNotOk() || null == openCheckerResult.getData())
            {
                logger.error("get open checker fail! message: {}", openCheckerResult.getMessage());
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
            Map<String, CheckerDetailVO> openCheckers = openCheckerResult.getData();
            Map<String, String> taskCheckerProps = toolConfigInfoEntity.getCheckerProps();
            JSONObject allPropsJson = new JSONObject();
            JSONObject checkerOptionsJson = new JSONObject();
            Set<String> checkerNameSet = new HashSet<>();
            if (MapUtils.isNotEmpty(openCheckers))
            {
                openCheckers.forEach((key, openChecker) ->
                {
                    if (MapUtils.isNotEmpty(taskCheckerProps) &&
                            taskCheckerProps.containsKey(openChecker.getCheckerKey()))
                    {
                        openChecker.setProps(taskCheckerProps.get(openChecker.getCheckerKey()));
                    }
                    checkerNameSet.add(openChecker.getCheckerKey());

                    //加入规则参数
                    if (StringUtils.isNotEmpty(openChecker.getProps()))
                    {
                        JSONObject propsJson = new JSONObject(openChecker.getProps());
                        JSONObject checkerPropsJson = new JSONObject();
                        for (String prop : propsJson.keySet())
                        {
                            JSONObject propJson = propsJson.getJSONObject(prop);
                            allPropsJson.put(propJson.getString("propName"), propJson.getString("propValue"));
                            checkerPropsJson.put(propJson.getString("propName"), propJson.getString("propValue"));
                        }
                        if (!ComConstants.Tool.PYLINT.name().equals(multiToolType))
                        {
                            checkerOptionsJson.put(openChecker.getCheckerKey(), checkerPropsJson.toString());
                        }
                    }
                });

                // PYLINT没有参数，都放到blank_rules里
                if (ComConstants.Tool.PYLINT.name().equals(multiToolType))
                {
                    checkerOptionsJson.put("blank_rules", allPropsJson.toString());
                }
            }
            defectConfigInfoVO.setOpenCheckers(String.join(";", checkerNameSet));
            defectConfigInfoVO.setCheckerOptions(checkerOptionsJson.toString());
        }

    }

}
