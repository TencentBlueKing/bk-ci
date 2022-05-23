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

package com.tencent.bk.codecc.task.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolMetaRepository;
import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import com.tencent.bk.codecc.task.model.ToolVersionEntity;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.ToolVersionVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.GlobalMessage;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.service.utils.GlobalMessageUtil;
import com.tencent.devops.common.util.CompressionUtils;
import com.tencent.devops.common.util.JsonUtil;

import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.tencent.devops.common.constant.RedisKeyConstants.GLOBAL_TOOL_DESCRIPTION;

/**
 * 工具缓存
 *
 * @version V1.0
 * @date 2019/3/7
 */
@Slf4j
@Component
public class ToolMetaCacheServiceImpl implements ToolMetaCacheService {
    private static final String TOOL_CACHE_KEY = "TOOL_METADATA";

    @Autowired
    private ToolMetaRepository toolMetaRepository;
    @Autowired
    private GlobalMessageUtil globalMessageUtil;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 工具基础信息缓存
     */
    private Map<String, ToolMetaBaseVO> toolMetaBasicMap = Maps.newConcurrentMap();

    /**
     * 加载工具缓存
     */
    @Override
    public List<ToolMetaDetailVO> loadToolDetailCache() {
        List<ToolMetaDetailVO> toolMetaDetailVOS = Lists.newArrayList();
        List<ToolMetaEntity> toolMetaList = toolMetaRepository.findAll();
        Map<String, GlobalMessage> globalMessageMap = globalMessageUtil.getGlobalMessageMap(GLOBAL_TOOL_DESCRIPTION);

        toolMetaBasicMap.clear();
        for (ToolMetaEntity tool : toolMetaList) {
            // 缓存基础信息
            cacheToolBaseMeta(tool);

            // 解压图标和图文详情
            String logo = tool.getLogo();
            if (StringUtils.isNotEmpty(logo)) {
                byte[] compressLogoBytes = logo.getBytes(StandardCharsets.ISO_8859_1);
                byte[] afterDecompress = CompressionUtils.decompress(compressLogoBytes);
                tool.setLogo(new String(afterDecompress, StandardCharsets.UTF_8));
            }

            String graphicDetails = tool.getGraphicDetails();
            if (StringUtils.isNotBlank(graphicDetails)) {
                byte[] compressGraphicDetailsBytes = graphicDetails.getBytes(StandardCharsets.ISO_8859_1);
                byte[] afterDecompress = CompressionUtils.decompress(compressGraphicDetailsBytes);
                tool.setGraphicDetails(new String(afterDecompress, StandardCharsets.UTF_8));
            }

            // 工具描述国际化
            if (MapUtils.isNotEmpty(globalMessageMap)) {
                GlobalMessage globalMessage = globalMessageMap.get(tool.getName());
                if (null != globalMessage) {
                    String description = globalMessageUtil.getMessageByLocale(globalMessage);
                    tool.setDescription(description);
                    tool.setBriefIntroduction(description);
                }
            }

            ToolMetaDetailVO toolMetaDetailVO = new ToolMetaDetailVO();
            BeanUtils.copyProperties(tool, toolMetaDetailVO);

            List<ToolVersionVO> versionVOList = getToolVersionVOs(tool);

            toolMetaDetailVO.setToolVersions(versionVOList);
            toolMetaDetailVOS.add(toolMetaDetailVO);
        }

        redisTemplate.opsForValue().set(TOOL_CACHE_KEY, JsonUtil.INSTANCE.toJson(toolMetaDetailVOS));

        log.info("load tool cache success");
        return toolMetaDetailVOS;
    }

    @NotNull
    private List<ToolVersionVO> getToolVersionVOs(ToolMetaEntity tool) {
        List<ToolVersionEntity> versionList = tool.getToolVersions();
        List<ToolVersionVO> versionVOList = new ArrayList<>();
        if (CollectionUtils.isEmpty(versionList)) {
            ToolVersionVO toolVersionVO = new ToolVersionVO();
            toolVersionVO.setVersionType(ToolIntegratedStatus.P.name());
            versionVOList.add(toolVersionVO);
        } else {
            versionList.forEach(toolVersionEntity -> {
                ToolVersionVO toolVersionVO = new ToolVersionVO();
                BeanUtils.copyProperties(toolVersionEntity, toolVersionVO);
                versionVOList.add(toolVersionVO);
            });
        }
        return versionVOList;
    }

    /**
     * 根据工具名获取工具模型
     *
     * @param toolName
     * @return
     */
    @Override
    public String getToolPattern(String toolName) {
        String pattern;
        if (toolMetaBasicMap.get(toolName) != null
                && StringUtils.isNotEmpty(toolMetaBasicMap.get(toolName).getPattern())) {
            pattern = toolMetaBasicMap.get(toolName).getPattern();
        } else {
            ToolMetaBaseVO toolMetaDetailVO = getToolBaseMetaCache(toolName);
            pattern = toolMetaDetailVO.getPattern();
        }
        return pattern;
    }

    /**
     * 加载工具缓存
     */
    @Override
    public List<ToolMetaBaseVO> loadToolBaseCache() {
        List<ToolMetaBaseVO> toolMetaBaseVOS = Lists.newArrayList();
        List<ToolMetaEntity> toolMetaList = toolMetaRepository.findAll();
        for (ToolMetaEntity tool : toolMetaList) {
            // 缓存基础信息
            cacheToolBaseMeta(tool);
        }
        return toolMetaBaseVOS;
    }

    /**
     * 根据工具名获取工具模型
     *
     * @param toolName
     * @return
     */
    public String getToolParams(String toolName) {
        String params;
        if (toolMetaBasicMap.get(toolName) != null
                && StringUtils.isNotEmpty(toolMetaBasicMap.get(toolName).getParams())) {
            params = toolMetaBasicMap.get(toolName).getParams();
        } else {
            ToolMetaBaseVO toolMetaBaseVO = getToolBaseMetaCache(toolName);
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
    public ToolMetaBaseVO getToolBaseMetaCache(String toolName) {
        if (toolMetaBasicMap.get(toolName) != null) {
            return toolMetaBasicMap.get(toolName);
        } else {
            ToolMetaDetailVO toolMetaDetailVO = getToolDetailFromCache(toolName);
            ToolMetaBaseVO toolMetaBaseVO = new ToolMetaBaseVO();
            BeanUtils.copyProperties(toolMetaDetailVO, toolMetaBaseVO);
            toolMetaBasicMap.put(toolMetaBaseVO.getName(), toolMetaBaseVO);
            return toolMetaBaseVO;
        }
    }


    /**
     * 根据工具名从缓存中获取工具完整信息
     *
     * @param toolName
     * @return
     */
    @Override
    public ToolMetaDetailVO getToolDetailFromCache(String toolName) {
        List<ToolMetaDetailVO> toolMetaDetails;
        Object toolMetadataObj = redisTemplate.opsForValue().get(TOOL_CACHE_KEY);
        if (toolMetadataObj == null) {
            toolMetaDetails = loadToolDetailCache();
        } else {
            toolMetaDetails = JsonUtil.INSTANCE.to((String) toolMetadataObj, new TypeReference<List<ToolMetaDetailVO>>() {
            });
        }

        ToolMetaDetailVO toolMetaDetailVOResult = null;
        if (CollectionUtils.isNotEmpty(toolMetaDetails)) {
            for (ToolMetaDetailVO toolMetaDetail : toolMetaDetails) {
                if (toolName.equals(toolMetaDetail.getName())) {
                    toolMetaDetailVOResult = toolMetaDetail;
                    break;
                }
            }
        }

        if (Objects.isNull(toolMetaDetailVOResult)) {
            log.error("tool[{}] is invalid.", toolName);
            throw new CodeCCException(CommonMessageCode.INVALID_TOOL_NAME, new String[]{toolName}, null);
        }

        return toolMetaDetailVOResult;
    }

    @Override
    public List<String> getToolDetailByDimension(String dimension) {
        return null;
    }

    /**
     * 从缓存中获取所有工具
     *
     * @param isDetail
     * @return
     */
    @Override
    public Map<String, ToolMetaBaseVO> getToolMetaListFromCache(boolean isDetail, boolean isAdmin) {
        List<ToolMetaDetailVO> toolMetaDetails;

        // 如果不需要详情，并且基础信息缓存不为空，就直接使用基础信息缓存，否则就查询Redis缓存中的工具信息
        if (Boolean.FALSE.equals(isDetail) && MapUtils.isNotEmpty(toolMetaBasicMap)) {
            toolMetaDetails = Lists.newArrayList();
            for (Map.Entry<String, ToolMetaBaseVO> entry : toolMetaBasicMap.entrySet()) {
                ToolMetaDetailVO toolMetaDetailVO = new ToolMetaDetailVO();
                BeanUtils.copyProperties(entry.getValue(), toolMetaDetailVO);
                toolMetaDetails.add(toolMetaDetailVO);
            }
        } else {
            Object toolMetadataObj = redisTemplate.opsForValue().get(TOOL_CACHE_KEY);
            if (toolMetadataObj == null) {
                toolMetaDetails = loadToolDetailCache();
            } else {
                toolMetaDetails = JsonUtil.INSTANCE.to((String) toolMetadataObj,
                        new TypeReference<List<ToolMetaDetailVO>>() {
                        });
            }
        }

        Map<String, ToolMetaBaseVO> toolCacheCopy = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(toolMetaDetails)) {
            for (ToolMetaDetailVO toolMetaDetailVO : toolMetaDetails) {
//                if (Boolean.FALSE.equals(isAdmin)
//                && !ComConstants.ToolIntegratedStatus.P.name().equals(toolMetaDetailVO.getStatus()))
//                {
//                    continue;
//                }

                ToolMetaBaseVO toolMetaVO;
                if (Boolean.TRUE.equals(isDetail)) {
                    toolMetaVO = toolMetaDetailVO;
                } else {
                    toolMetaVO = new ToolMetaBaseVO();
                    BeanUtils.copyProperties(toolMetaDetailVO, toolMetaVO);
                }
                toolCacheCopy.put(toolMetaVO.getName(), toolMetaVO);
            }
        }

        return toolCacheCopy;
    }

    /**
     * 获取工具显示名称
     *
     * @param toolName
     * @return
     */
    @Override
    public String getToolDisplayName(String toolName) {
        String displayName;
        if (toolMetaBasicMap.get(toolName) != null
                && StringUtils.isNotEmpty(toolMetaBasicMap.get(toolName).getDisplayName())) {
            displayName = toolMetaBasicMap.get(toolName).getDisplayName();
        } else {
            ToolMetaBaseVO toolMetaBaseVO = getToolBaseMetaCache(toolName);
            displayName = toolMetaBaseVO.getDisplayName();
        }
        return displayName;
    }

    /**
     * 缓存工具基础信息
     *
     * @param toolMetaEntity
     */
    private ToolMetaBaseVO cacheToolBaseMeta(ToolMetaEntity toolMetaEntity) {
        ToolMetaBaseVO newToolMetaBaseVO = new ToolMetaBaseVO();
        BeanUtils.copyProperties(toolMetaEntity, newToolMetaBaseVO);
        List<ToolVersionVO> versionVOList = getToolVersionVOs(toolMetaEntity);
        newToolMetaBaseVO.setToolVersions(versionVOList);
        toolMetaBasicMap.put(newToolMetaBaseVO.getName(), newToolMetaBaseVO);
        return newToolMetaBaseVO;
    }

    /**
     * 缓存工具基础信息
     *
     * @param toolName
     */
    public void cacheToolBaseMeta(String toolName) {
        ToolMetaEntity toolMetaEntity = toolMetaRepository.findFirstByName(toolName);
        if (toolMetaEntity != null) {
            ToolMetaBaseVO newToolMetaBaseVO = new ToolMetaBaseVO();
            BeanUtils.copyProperties(toolMetaEntity, newToolMetaBaseVO);
            List<ToolVersionVO> versionVOList = getToolVersionVOs(toolMetaEntity);
            newToolMetaBaseVO.setToolVersions(versionVOList);
            toolMetaBasicMap.put(newToolMetaBaseVO.getName(), newToolMetaBaseVO);
            log.info("cache tool success. {}", toolName);
        }
    }

}
