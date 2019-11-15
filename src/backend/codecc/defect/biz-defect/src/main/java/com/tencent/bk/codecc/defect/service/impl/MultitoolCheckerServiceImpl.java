/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.IgnoreCheckerRepository;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.IgnoreCheckerEntity;
import com.tencent.bk.codecc.defect.service.MultitoolCheckerService;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.constant.ComConstants;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 多工具规则服务层实现
 *
 * @version V1.0
 * @date 2019/4/26
 */
@Service
public class MultitoolCheckerServiceImpl implements MultitoolCheckerService
{
    private static Logger logger = LoggerFactory.getLogger(MultitoolCheckerServiceImpl.class);

    @Autowired
    private CheckerRepository checkerRepository;

    @Autowired
    private IgnoreCheckerRepository ignoreCheckerRepository;

    @Override
    public Map<String, CheckerDetailVO> queryAllChecker(String toolName)
    {
        List<CheckerDetailEntity> checkerDetailEntityList = checkerRepository.findByToolName(toolName);
        return checkerDetailEntityList.stream().
                map(checkerDetailEntity ->
                {
                    CheckerDetailVO checkerDetailVO = new CheckerDetailVO();
                    BeanUtils.copyProperties(checkerDetailEntity, checkerDetailVO);
                    return checkerDetailVO;
                }).
                collect(Collectors.toMap(CheckerDetailVO::getCheckerKey, Function.identity(), (k, v) -> v));
    }

    @Override
    public List<CheckerDetailVO> queryAllChecker(ToolConfigInfoVO toolConfigInfoVO)
    {
        return queryAllChecker(toolConfigInfoVO.getToolName(), toolConfigInfoVO.getParamJson());
    }


    @Override
    public List<CheckerDetailVO> queryAllChecker(String toolName, String paramJson)
    {
        if (StringUtils.isNotEmpty(paramJson))
        {
            paramJson.trim();
        }
        List<CheckerDetailEntity> checkerDetailEntities = checkerRepository.findByToolName(toolName);
        return checkerDetailEntities.stream().
                filter(checkerDetailEntity ->
                {
                    if (checkerDetailEntity.getStatus() == 1)
                    {
                        return false;
                    }
                    JSONObject parseObj = StringUtils.isBlank(paramJson) ? new JSONObject() : JSONObject.fromObject(paramJson);
                    if (ComConstants.Tool.ESLINT.name().equals(toolName))
                    {
                        if (parseObj.containsKey("eslint_rc"))
                        {
                            String eslintRc = parseObj.getString("eslint_rc");
                            if (ComConstants.EslintFrameworkType.standard.name().equals(eslintRc))
                            {
                                return ComConstants.EslintFrameworkType.standard.name().equals(checkerDetailEntity.getFrameworkType());
                            }
                            else if (ComConstants.EslintFrameworkType.vue.name().equals(eslintRc))
                            {
                                return !ComConstants.EslintFrameworkType.react.name().equals(checkerDetailEntity.getFrameworkType());
                            }
                            else if (ComConstants.EslintFrameworkType.react.name().equals(eslintRc))
                            {
                                return !ComConstants.EslintFrameworkType.vue.name().equals(checkerDetailEntity.getFrameworkType());
                            }
                        }
                    }
                    else if (ComConstants.Tool.PHPCS.name().equals(toolName))
                    {
                        String phpcsStandard = parseObj.getString("phpcs_standard");
                        int standCode = ComConstants.PHPCSStandardCode.valueOf(phpcsStandard).code();
                        return (standCode & checkerDetailEntity.getStandard()) != 0;
                    }
                    return true;
                }).
                map(checkerDetailEntity ->
                {
                    CheckerDetailVO checkerDetailVO = new CheckerDetailVO();
                    BeanUtils.copyProperties(checkerDetailEntity, checkerDetailVO);
                    return checkerDetailVO;
                }).
                collect(Collectors.toList());
    }

    @Override
    public Map<String, CheckerDetailVO> queryOpenCheckers(ToolConfigInfoVO toolConfigInfoVO)
    {
        List<CheckerDetailVO> checkerDetailEntityList = queryAllChecker(toolConfigInfoVO.getToolName(), toolConfigInfoVO.getParamJson());
        IgnoreCheckerEntity ignoreCheckerEntity = ignoreCheckerRepository.findByTaskIdAndToolName(toolConfigInfoVO.getTaskId(), toolConfigInfoVO.getToolName());
        List<String> ignoreCheckerList = null == ignoreCheckerEntity ? new ArrayList<>() : ignoreCheckerEntity.getIgnoreList();
        return checkerDetailEntityList.stream().
                filter(checkerDetailEntity ->
                        !ignoreCheckerList.
                                contains(checkerDetailEntity.getCheckerKey())
                ).
                collect(Collectors.toMap(CheckerDetailVO::getCheckerKey, Function.identity(), (k, v) -> v));
    }

    @Override
    public Set<String> queryPkgRealCheckers(String pkgId, ToolConfigInfoVO toolConfigInfoVO)
    {
        Set<String> conditionPkgCheckers = Sets.newHashSet();
        if (StringUtils.isNotEmpty(pkgId))
        {
            Map<String, CheckerDetailVO> allCheckers = queryOpenCheckers(toolConfigInfoVO);
            if (MapUtils.isNotEmpty(allCheckers))
            {
                allCheckers.forEach(
                        (s, checkerDetailVO) ->
                        {
                            if (pkgId.equals(checkerDetailVO.getPkgKind()))
                            {
                                conditionPkgCheckers.add(s);
                            }
                        });
            }
        }
        return conditionPkgCheckers;

    }


    @Override
    public Boolean mergeIgnoreChecker(long taskId, String toolName, List<String> ignoreCheckers)
    {
        IgnoreCheckerEntity ignoreCheckerEntity = ignoreCheckerRepository.findByTaskIdAndToolName(taskId, toolName);
        if (null == ignoreCheckerEntity)
        {
            logger.error("no ignore checker found! task id: {}, tool name: {}", taskId, toolName);
        }
        List<String> ignoreCheckerList = ignoreCheckerEntity.getIgnoreList();
        if (CollectionUtils.isEmpty(ignoreCheckerList))
        {
            ignoreCheckerList = new ArrayList<>();
        }
        ignoreCheckerList.addAll(ignoreCheckers);
        ignoreCheckerEntity.setIgnoreList(ignoreCheckerList.stream().distinct().collect(Collectors.toList()));
        ignoreCheckerRepository.save(ignoreCheckerEntity);
        return true;
    }


}
