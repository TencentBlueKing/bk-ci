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

package com.tencent.bk.codecc.task.dao;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolMetaRepository;
import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import com.tencent.bk.codecc.task.vo.ToolMetaBaseVO;
import com.tencent.bk.codecc.task.vo.ToolMetaDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.GlobalMessage;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.utils.GlobalMessageUtil;
import com.tencent.devops.common.util.CompressionUtils;
import com.tencent.devops.common.util.JsonUtil;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.tencent.devops.common.constant.RedisKeyConstants.*;

/**
 * 工具缓存
 *
 * @version V1.0
 * @date 2019/3/7
 */
@Component
public class ToolMetaCache
{
    private static Logger logger = LoggerFactory.getLogger(ToolMetaCache.class);

    @Autowired
    private ToolMetaRepository toolMetaRepository;

    @Autowired
    private GlobalMessageUtil globalMessageUtil;

    private Cache<String, ToolMetaEntity> toolCache = CacheBuilder.newBuilder()
            .maximumSize(20)
            .build();

    /**
     * 加载工具缓存
     */
    private void loadToolCache()
    {
        List<ToolMetaEntity> toolMetaList = toolMetaRepository.findAll();

        for (ToolMetaEntity tool : toolMetaList)
        {
            // 解压图标和图文详情
            String logo = tool.getLogo();
            if (StringUtils.isNotEmpty(logo))
            {
                byte[] compressLogoBytes = logo.getBytes(StandardCharsets.ISO_8859_1);
                byte[] afterDecompress = CompressionUtils.decompress(compressLogoBytes);
                tool.setLogo(new String(afterDecompress, StandardCharsets.UTF_8));
            }

            String graphicDetails = tool.getGraphicDetails();
            if (StringUtils.isNotEmpty(logo))
            {
                byte[] compressGraphicDetailsBytes = graphicDetails.getBytes(StandardCharsets.ISO_8859_1);
                byte[] afterDecompress = CompressionUtils.decompress(compressGraphicDetailsBytes);
                tool.setGraphicDetails(new String(afterDecompress, StandardCharsets.UTF_8));
            }
        }

        toolCache.cleanUp();
        for (ToolMetaEntity tool : toolMetaList)
        {
            toolCache.put(tool.getName(), tool);
        }

        logger.info("load tool cache success");
    }

    /**
     * 根据工具名获取工具模型
     *
     * @param toolName
     * @return
     */
    public String getToolPattern(String toolName)
    {
        if (toolCache.size() == 0)
        {
            loadToolCache();
        }

        String pattern = null;
        ToolMetaEntity tool = toolCache.getIfPresent(toolName);
        if (tool != null)
        {
            pattern = tool.getPattern();
        }

        if (StringUtils.isEmpty(pattern))
        {
            logger.error("tool[{}] is invalid.", toolName);
            throw new CodeCCException(CommonMessageCode.INVALID_TOOL_NAME, new String[]{toolName}, null);
        }

        return pattern;
    }


    /**
     * 根据工具名从缓存中获取工具基本信息
     *
     * @param toolName
     * @return
     */
    public ToolMetaEntity getToolFromCache(String toolName)
    {
        if (toolCache.size() == 0)
        {
            loadToolCache();
        }

        ToolMetaEntity toolMetaEntity = toolCache.getIfPresent(toolName);
        if (Objects.isNull(toolMetaEntity))
        {
            logger.error("tool[{}] is invalid.", toolName);
            throw new CodeCCException(CommonMessageCode.INVALID_TOOL_NAME, new String[]{toolName}, null);
        }

        Map<String, GlobalMessage> globalMessageMap = globalMessageUtil.getGlobalMessageMap(GLOBAL_TOOL_DESCRIPTION);
        // 工具描述国际化
        if (MapUtils.isNotEmpty(globalMessageMap))
        {
            GlobalMessage globalMessage = globalMessageMap.get(toolName);
            String description = globalMessageUtil.getMessageByLocale(globalMessage);
            toolMetaEntity.setDescription(description);
            toolMetaEntity.setBriefIntroduction(description);
        }

        return toolMetaEntity;
    }

    /**
     * 从缓存中获取所有工具
     *
     * @param isDetail
     * @return
     */
    public Map<String, ToolMetaBaseVO> getToolMetaListFromCache(boolean isDetail, boolean isAdmin)
    {
        if (toolCache.size() == 0)
        {
            loadToolCache();
        }

        Map<String, GlobalMessage> globalMessageMap = globalMessageUtil.getGlobalMessageMap(GLOBAL_TOOL_DESCRIPTION);
        Map<String, GlobalMessage> tipsMessage = globalMessageUtil.getGlobalMessageMap(GLOBAL_TOOL_PARAMS_TIPS);
        Map<String, GlobalMessage> labelNameMessage = globalMessageUtil.getGlobalMessageMap(GLOBAL_TOOL_PARAMS_LABEL_NAME);
        Map<String, ToolMetaBaseVO> toolCacheCopy = new HashMap<>((int) toolCache.size());
        for (Map.Entry<String, ToolMetaEntity> toolEntry : toolCache.asMap().entrySet())
        {
            ToolMetaEntity toolMetaEntity = toolEntry.getValue();

            if (!isAdmin && !TaskConstants.ToolIntegratedStatus.P.name().equals(toolMetaEntity.getStatus()))
            {
                continue;
            }

            ToolMetaBaseVO toolMetaVO;
            if (Boolean.TRUE.equals(isDetail))
            {
                toolMetaVO = new ToolMetaDetailVO();
                BeanUtils.copyProperties(toolMetaEntity, toolMetaVO);
                ((ToolMetaDetailVO) toolMetaVO).setGraphicDetails(null);
                // 工具描述国际化
                if (MapUtils.isNotEmpty(globalMessageMap))
                {
                    GlobalMessage globalMessage = globalMessageMap.get(toolMetaEntity.getName());
                    String description = globalMessageUtil.getMessageByLocale(globalMessage);
                    ((ToolMetaDetailVO) toolMetaVO).setDescription(description);
                    ((ToolMetaDetailVO) toolMetaVO).setBriefIntroduction(description);
                }
            }
            else
            {
                toolMetaVO = new ToolMetaBaseVO();
                BeanUtils.copyProperties(toolMetaEntity, toolMetaVO);
            }

            String params = toolMetaEntity.getParams();
            String toolName = toolMetaEntity.getName();
            if(StringUtils.isNotBlank(params) && !ComConstants.STRING_NULL_ARRAY.equals(params)){
                // 工具参数标签[ labelName ]国际化
                List<Map<String, Object>> paramArrays = JsonUtil.INSTANCE.to(params);
                for (Map<String, Object> paramMap : paramArrays)
                {
                    String varName = (String)paramMap.get("varName");
                    String varTips = (String)paramMap.get("varTips");
                    String labelName = (String)paramMap.get("labelName");
                    String varDefault = (String)paramMap.get("varDefault");

                    // 工具参数提示[ tips ]国际化
                    paramMap.put("varTips", getGlobalTip(toolName, varName, varTips, tipsMessage));
                    // 工具参数标签[ labelName ]国际化
                    paramMap.put("labelName", getGlobalLabel(toolName, varName, labelName, labelNameMessage));

                    // 设置SPOTBUGS的默认值
                    if(ComConstants.Tool.SPOTBUGS.name().equals(toolName))
                    {
                        paramMap.put("varDefault", getGlobalTip(toolName, varName, varDefault, tipsMessage));
                    }
                }
                params = JsonUtil.INSTANCE.toJson(paramArrays);
            }

            toolMetaVO.setParams(params);
            toolCacheCopy.put(toolMetaVO.getName(), toolMetaVO);
        }

        return toolCacheCopy;
    }


    /**
     * 工具参数标签[ labelName ]国际化
     * @param toolName
     * @param varName
     * @param labelName
     * @return
     */
    public String getGlobalLabel(String toolName, String varName, String labelName, Map<String, GlobalMessage> labelNameMessage){
        GlobalMessage labelGlobalMessage = labelNameMessage.get(String.format("%s:%s", toolName, varName));
        if (Objects.nonNull(labelGlobalMessage))
        {
            return globalMessageUtil.getMessageByLocale(labelGlobalMessage);
        }
        return labelName;
    }


    /**
     * 工具参数提示[ tips ]国际化
     * @param toolName
     * @param varName
     * @param tipName
     * @return
     */
    public String getGlobalTip(String toolName, String varName, String tipName, Map<String, GlobalMessage> tipsMessage){
        GlobalMessage tipGlobalMessage = tipsMessage.get(String.format("%s:%s", toolName, varName));
        if (Objects.nonNull(tipGlobalMessage))
        {
            return globalMessageUtil.getMessageByLocale(tipGlobalMessage);
        }
        return tipName;
    }


}
