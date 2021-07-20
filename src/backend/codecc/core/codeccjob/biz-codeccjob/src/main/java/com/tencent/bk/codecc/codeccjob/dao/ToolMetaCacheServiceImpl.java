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

package com.tencent.bk.codecc.codeccjob.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.ToolMetaCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工具缓存
 *
 * @version V1.0
 * @date 2019/3/7
 */
@Slf4j
@Component
public class ToolMetaCacheServiceImpl implements ToolMetaCacheService
{
    @Autowired
    private Client client;

    /**
     * 工具基础信息缓存
     */
    private Map<String, ToolMetaBaseVO> toolMetaBasicMap = Maps.newConcurrentMap();

    /**
     * 工具维度基础信息缓存
     */
    private Map<String, Set<ToolMetaBaseVO>> toolMetaBasicDimensionMap = Maps.newConcurrentMap();

    /**
     * 加载工具缓存
     */
    @Override
    public List<ToolMetaBaseVO> loadToolBaseCache()
    {
        Result<Map<String, ToolMetaBaseVO>> taskResult = client.get(ServiceTaskRestResource.class).getToolMetaListFromCache();
        if (taskResult.isNotOk() || null == taskResult.getData() || MapUtils.isEmpty(taskResult.getData()))
        {
            log.error("all tool metadata is empty!");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        Map<String, ToolMetaBaseVO> toolMetaBaseVOMap = taskResult.getData();

        toolMetaBasicMap.clear();
        toolMetaBasicDimensionMap.clear();
        List<ToolMetaBaseVO> toolMetaBaseVOS = Lists.newArrayList();
        for (Map.Entry<String, ToolMetaBaseVO> entry : toolMetaBaseVOMap.entrySet())
        {
            // 缓存基础信息
            ToolMetaBaseVO tool = entry.getValue();
            toolMetaBasicMap.put(tool.getName(), tool);
            toolMetaBaseVOS.add(tool);

            // 缓存维度基础信息
            // DEFECT类型的工具特殊处理下
            String dimensionMapKey = tool.getType();
            if (dimensionMapKey.equals(ComConstants.ToolType.DEFECT.name()) && tool.getPattern().equals(ComConstants.ToolPattern.LINT.name())) {
                dimensionMapKey = ComConstants.ToolPattern.LINT.name();
            }
            Set<ToolMetaBaseVO> toolDimensionSet = toolMetaBasicDimensionMap.get(dimensionMapKey);
            if (toolDimensionSet == null) {
                toolDimensionSet = Sets.newHashSet();
            }
            toolDimensionSet.add(tool);
            toolMetaBasicDimensionMap.put(dimensionMapKey, toolDimensionSet);
        }

        log.info("load tool dimension cache success: {}", toolMetaBasicDimensionMap);

        log.info("load tool cache success: {}", toolMetaBasicMap);

        return toolMetaBaseVOS;
    }

    /**
     * 加载工具缓存
     */
    @Override
    public List<ToolMetaDetailVO> loadToolDetailCache()
    {
        return null;
    }

    /**
     * 根据工具名获取工具模型
     *
     * @param toolName
     * @return
     */
    @Override
    public String getToolPattern(String toolName)
    {
        String pattern;
        if (toolMetaBasicMap.get(toolName) != null && StringUtils.isNotEmpty(toolMetaBasicMap.get(toolName).getPattern()))
        {
            pattern = toolMetaBasicMap.get(toolName).getPattern();
        }
        else
        {
            ToolMetaBaseVO toolMetaBaseVO = getToolFromCache(toolName);
            pattern = toolMetaBaseVO.getPattern();
        }
        return pattern;
    }

    /**
     * 根据工具名获取工具模型
     *
     * @param toolName
     * @return
     */
    public String getToolParams(String toolName)
    {
        String params;
        if (toolMetaBasicMap.get(toolName) != null && StringUtils.isNotEmpty(toolMetaBasicMap.get(toolName).getParams()))
        {
            params = toolMetaBasicMap.get(toolName).getParams();
        }
        else
        {
            ToolMetaBaseVO toolMetaBaseVO = getToolFromCache(toolName);
            params = toolMetaBaseVO.getParams();
        }
        return params;
    }

    /**
     * 获取工具基础信息缓存
     *
     * @param toolName
     * @return
     */
    @Override
    public ToolMetaBaseVO getToolBaseMetaCache(String toolName)
    {
        if (toolMetaBasicMap.get(toolName) != null)
        {
            return toolMetaBasicMap.get(toolName);
        }
        else
        {
            ToolMetaBaseVO toolMetaBaseVO = getToolFromCache(toolName);
            return toolMetaBaseVO;
        }
    }

    /**
     * 获取工具显示名称
     *
     * @param toolName
     * @return
     */
    @Override
    public String getToolDisplayName(String toolName)
    {
        String displayName;
        if (toolMetaBasicMap.get(toolName) != null && StringUtils.isNotEmpty(toolMetaBasicMap.get(toolName).getDisplayName()))
        {
            displayName = toolMetaBasicMap.get(toolName).getDisplayName();
        }
        else
        {
            ToolMetaBaseVO toolMetaBaseVO = getToolFromCache(toolName);
            displayName = toolMetaBaseVO.getDisplayName();
        }
        return displayName;
    }

    /**
     * 从缓存中获取所有工具
     *
     * @param isDetail
     * @param isAdmin
     * @return
     */
    @Override
    public Map<String, ToolMetaBaseVO> getToolMetaListFromCache(boolean isDetail, boolean isAdmin)
    {
        // TODO 查询工具元数据列表
        return null;
    }

    /**
     * 根据工具名从缓存中获取工具完整信息
     *
     * @param toolName
     * @return
     */
    @Override
    public ToolMetaDetailVO getToolDetailFromCache(String toolName)
    {
        return null;
    }

    @Override
    public List<String> getToolDetailByDimension(String dimension) {
        if (StringUtils.isBlank(dimension)) {
            return null;
        }
        if (toolMetaBasicDimensionMap.get(dimension) == null || CollectionUtils.isEmpty(toolMetaBasicDimensionMap.get(dimension))) {
            loadToolBaseCache();
        }
        return toolMetaBasicDimensionMap.get(dimension).stream().map(ToolMetaBaseVO::getName).collect(Collectors.toList());
    }

    /**
     * 根据工具名从缓存中获取工具完整信息
     *
     * @param toolName
     * @return
     */
    private ToolMetaBaseVO getToolFromCache(String toolName)
    {
        ToolMetaBaseVO toolMetaBaseVOResult = null;
        List<ToolMetaBaseVO> toolMetaBaseVOS = loadToolBaseCache();
        if (CollectionUtils.isNotEmpty(toolMetaBaseVOS))
        {
            for (ToolMetaBaseVO toolMetaBaseVO : toolMetaBaseVOS)
            {
                if (toolName.equals(toolMetaBaseVO.getName()))
                {
                    toolMetaBaseVOResult = toolMetaBaseVO;
                    break;
                }
            }
        }
        if (Objects.isNull(toolMetaBaseVOResult))
        {
            log.error("tool[{}] is invalid.", toolName);
            throw new CodeCCException(CommonMessageCode.INVALID_TOOL_NAME, new String[]{toolName}, null);
        }

        return toolMetaBaseVOResult;
    }
}
