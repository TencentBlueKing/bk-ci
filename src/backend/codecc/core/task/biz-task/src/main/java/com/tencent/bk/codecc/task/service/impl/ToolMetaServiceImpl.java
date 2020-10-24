/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.task.service.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.dao.ToolMetaCacheServiceImpl;
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolMetaRepository;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import com.tencent.bk.codecc.task.service.ToolMetaService;
import com.tencent.devops.common.api.RefreshToolImageRevisionReqVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.AESUtil;
import com.tencent.devops.common.util.GsonUtils;
import com.tencent.devops.common.web.mq.ConstantsKt;
import com.tencent.devops.image.api.ServiceDockerImageResource;
import com.tencent.devops.image.pojo.CheckDockerImageRequest;
import com.tencent.devops.image.pojo.CheckDockerImageResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工具元数据注册接口实现
 *
 * @version V1.0
 * @date 2020/4/8
 */
@Service
@Slf4j
public class ToolMetaServiceImpl implements ToolMetaService
{
    @Value("${aes.encryptor.key:#{null}}")
    private String encryptorKey;

    private static final String TOOL_TYPE = "TOOL_TYPE";

    private static final String LANG = "LANG";

    private static final String DOCKER_IMAGE_DEFAULT_ACCOUNT = "DOCKER_IMAGE_DEFAULT_ACCOUNT";

    @Autowired
    private ToolMetaRepository toolMetaRepository;
    @Autowired
    private BaseDataRepository baseDataRepository;
    @Autowired
    private ToolMetaCacheServiceImpl toolMetaCacheService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private Client client;

    @Override
    public ToolMetaDetailVO register(String userName, ToolMetaDetailVO toolMetaDetailVO)
    {
        log.info("begin register tool: {}", toolMetaDetailVO);

        List<BaseDataEntity> baseDataEntityList = baseDataRepository.findByParamTypeIn(Lists.newArrayList(TOOL_TYPE, LANG, DOCKER_IMAGE_DEFAULT_ACCOUNT));

        // 参数校验
        validateParam(toolMetaDetailVO, baseDataEntityList);

        // 获取默认账号密码信息
        BaseDataEntity dockerImageAccount = baseDataEntityList.stream()
                .filter(baseDataEntity -> DOCKER_IMAGE_DEFAULT_ACCOUNT.equals(baseDataEntity.getParamType()))
                .findFirst().get();

        // 根据调试流水线id查询工具是否存在，不存在表示是注册新工具，否则是更新已有工具
        ToolMetaEntity toolMetaEntity = toolMetaRepository.findByDebugPipelineId(toolMetaDetailVO.getDebugPipelineId());
        if (toolMetaEntity == null)
        {
            // 注册新工具时，需要校验工具名是否已经存在，存在则不能注册
            if (toolMetaRepository.existsByName(toolMetaDetailVO.getName()))
            {
                log.error("tool has register: {}", toolMetaDetailVO.getName());
                throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{toolMetaDetailVO.getName()}, null);
            }

            toolMetaEntity = new ToolMetaEntity();
            BeanUtils.copyProperties(toolMetaDetailVO, toolMetaEntity);
            toolMetaEntity.setPattern(TaskConstants.ToolPattern.LINT.name());
            toolMetaEntity.setDockerImageAccount(dockerImageAccount.getParamCode());
            toolMetaEntity.setDockerImagePasswd(dockerImageAccount.getParamValue());
            toolMetaEntity.setCreatedBy(userName);
            toolMetaEntity.setStatus(TaskConstants.ToolIntegratedStatus.P.name());
            toolMetaEntity.setCreatedDate(System.currentTimeMillis());

            // 给工具排序
            resetToolOrder(toolMetaEntity, baseDataEntityList);
        }
        else
        {
            // 更新已有工具时，工具名（name）不能修改
            if (!toolMetaEntity.getName().equals(toolMetaDetailVO.getName()))
            {
                log.error("can not change tool name: {} -> {}", toolMetaEntity.getName(), toolMetaDetailVO.getName());
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                        new String[]{toolMetaEntity.getName() + " -> " + toolMetaDetailVO.getName()}, null);
            }
            String oldType = toolMetaEntity.getType();
            toolMetaEntity.setName(toolMetaDetailVO.getName());
            toolMetaEntity.setDisplayName(toolMetaDetailVO.getDisplayName());
            toolMetaEntity.setType(toolMetaDetailVO.getType());
            toolMetaEntity.setBriefIntroduction(toolMetaDetailVO.getBriefIntroduction());
            toolMetaEntity.setDockerTriggerShell(toolMetaDetailVO.getDockerTriggerShell());
            toolMetaEntity.setToolScanCommand(toolMetaDetailVO.getToolScanCommand());
            toolMetaEntity.setDockerImageURL(toolMetaDetailVO.getDockerImageURL());
            toolMetaEntity.setDockerImageVersion(toolMetaDetailVO.getDockerImageVersion());
            toolMetaEntity.setLogo(toolMetaDetailVO.getLogo());
            toolMetaEntity.setUpdatedBy(userName);
            toolMetaEntity.setUpdatedDate(System.currentTimeMillis());
            toolMetaEntity.setDockerImageAccount(dockerImageAccount.getParamCode());
            toolMetaEntity.setDockerImagePasswd(dockerImageAccount.getParamValue());


            // 如果工具类型变更了，需要重新对工具排序
            if (oldType.equals(toolMetaDetailVO.getType()))
            {
                resetToolOrder(toolMetaEntity, baseDataEntityList);
            }
        }

        // 转换语言
        List<String> supportedLanguages = toolMetaDetailVO.getSupportedLanguages();
        long lang = convertLang(supportedLanguages, baseDataEntityList);
        toolMetaEntity.setLang(lang);

        // 转换个性化参数
        toolMetaEntity.setParams(CollectionUtils.isEmpty(toolMetaDetailVO.getToolOptions()) ? null : GsonUtils.toJson(toolMetaDetailVO.getToolOptions()));

        String newToolImageRevision = getToolImageRevision(toolMetaEntity);
        if (StringUtils.isNotEmpty(newToolImageRevision) && !newToolImageRevision.equals(toolMetaEntity.getToolImageRevision()))
        {
            log.info("set tool image! toolName: {}, toolImageRevision: {}", toolMetaEntity.getName(), newToolImageRevision);
            toolMetaEntity.setToolImageRevision(newToolImageRevision);
        }
        toolMetaEntity = toolMetaRepository.save(toolMetaEntity);
        BeanUtils.copyProperties(toolMetaEntity, toolMetaDetailVO);
        toolMetaDetailVO.setSupportedLanguages(supportedLanguages);

        // 刷新工具缓存
        toolMetaCacheService.loadToolDetailCache();

        // 刷新Redis缓存后，需要同步刷新task的其他节点以及defect和codeccjob的缓存，确保每个节点的缓存都是最新的工具信息
        rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_REFRESH_TOOLMETA_CACHE, "", toolMetaEntity.getName());

        return toolMetaDetailVO;
    }

    @Override
    public List<ToolMetaDetailVO> queryToolMetaDataList()
    {
        Map<String, ToolMetaBaseVO> toolMetaDetailVOMap = toolMetaCacheService.getToolMetaListFromCache(Boolean.TRUE, Boolean.TRUE);

        List<ToolMetaDetailVO> toolMetaDetailVOList = new ArrayList<>(toolMetaDetailVOMap.size());
        toolMetaDetailVOMap.forEach((toolName, tool) ->
        {
            ToolMetaDetailVO toolMetaDetailVO = (ToolMetaDetailVO) tool;
            toolMetaDetailVO.setGraphicDetails(null);
            toolMetaDetailVO.setLogo(null);
            toolMetaDetailVOList.add(toolMetaDetailVO);
        });

        return toolMetaDetailVOList;
    }

    /**
     * 刷新工具镜像版本
     * @param refreshToolImageRevisionReqVO
     * @return
     */
    @Override
    public Boolean refreshToolImageRevision(RefreshToolImageRevisionReqVO refreshToolImageRevisionReqVO)
    {
        log.info("refresh toolImageRevision: {}", GsonUtils.toJson(refreshToolImageRevisionReqVO));
        String toolName = refreshToolImageRevisionReqVO.getToolName();
        ToolMetaEntity toolMetaEntity = toolMetaRepository.findByName(toolName);

        if (toolMetaEntity == null)
        {
            log.error("not found tool by toolName: {}", toolName);
            throw new CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL, String.format("not found tool by toolName: %s", toolName));
        }

        // coverity,klocwork,pinpoint工具没有镜像，通过工具版本号来判断是否有变化
        if (ComConstants.Tool.COVERITY.name().equals(toolName) || ComConstants.Tool.KLOCWORK.name().equals(toolName) || ComConstants.Tool.PINPOINT.name().equals(toolName))
        {
            String newToolVersion = refreshToolImageRevisionReqVO.getToolVersion();
            toolMetaEntity.setToolVersion(newToolVersion);
        }
        else
        {
            String newToolImageRevision = getToolImageRevision(toolMetaEntity);
            if (StringUtils.isNotEmpty(newToolImageRevision) && !newToolImageRevision.equals(toolMetaEntity.getToolImageRevision()))
            {
                log.info("need to update tool image! toolName: {}, toolImageRevision: {}", toolName, newToolImageRevision);
                toolMetaEntity.setToolImageRevision(newToolImageRevision);
            }
        }

        toolMetaEntity = toolMetaRepository.save(toolMetaEntity);

        // 刷新工具缓存
        toolMetaCacheService.loadToolDetailCache();

        // 刷新Redis缓存后，需要同步刷新task的其他节点以及defect和codeccjob的缓存，确保每个节点的缓存都是最新的工具信息
        rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_REFRESH_TOOLMETA_CACHE, "", toolMetaEntity.getName());
        log.info("refresh toolImageRevision finish. {}", refreshToolImageRevisionReqVO.getToolName());

        return true;
    }

    @Nullable
    private String getToolImageRevision(ToolMetaEntity toolMetaEntity)
    {
        String toolName = toolMetaEntity.getName();
        String userId = toolMetaEntity.getDockerImageAccount();
        String imageUrl = toolMetaEntity.getDockerImageURL();
        String imageVersion = toolMetaEntity.getDockerImageVersion();
        if (StringUtils.isNotEmpty(imageVersion))
        {
            imageUrl = String.format("%s:%s", imageUrl, imageVersion);
        }
        String passwd = toolMetaEntity.getDockerImagePasswd();
        if (StringUtils.isNotEmpty(passwd))
        {
            passwd = AESUtil.INSTANCE.decrypt(encryptorKey, passwd);
        }
        String registryHost = imageUrl.split("/")[0];
        List<CheckDockerImageRequest> requestList = Lists.newArrayList(new CheckDockerImageRequest(imageUrl, registryHost, userId, passwd));
        try
        {
            Result<List<CheckDockerImageResponse>> imageResult = client.getDevopsService(ServiceDockerImageResource.class).checkDockerImage(userId, requestList);
            if (imageResult.isNotOk() || null == imageResult.getData() || null == imageResult.getData().get(0) || imageResult.getData().get(0).getErrorCode() != 0)
            {
                String errMsg = String.format("get image list fail! toolName: %s, imageUrl: %s, imageResult: %s", toolName, imageUrl, imageResult);
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL, errMsg);
            }
            String toolImageRevision = imageResult.getData().get(0).getId();
            log.info("getToolImageRevision success. toolName: {}, toolImageRevision: {}", toolName, toolImageRevision);
            return toolImageRevision;
        }
        catch (Throwable throwable)
        {
            String errMsg = String.format("get image list fail! toolName: %s, imageUrl: %s", toolName, imageUrl);
            log.error(errMsg, throwable);
            throw new CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL, errMsg, throwable);
        }
    }

    private long convertLang(List<String> supportedLanguages, List<BaseDataEntity> baseDataEntityList)
    {
        Map<String, BaseDataEntity> langMap = new HashMap<>();
        baseDataEntityList.forEach(baseDataEntity ->
        {
            if (LANG.equals(baseDataEntity.getParamType()))
            {
                langMap.put(baseDataEntity.getLangFullKey(), baseDataEntity);
            }
        });

        long lang = 0;
        if (CollectionUtils.isNotEmpty(supportedLanguages))
        {
            for (int i = 0; i < supportedLanguages.size(); i++)
            {
                String langStr = supportedLanguages.get(i);
                lang += Long.valueOf(langMap.get(langStr).getParamCode());
            }
        }

        return lang;
    }

    private void validateParam(ToolMetaDetailVO toolMetaDetailVO, List<BaseDataEntity> baseDataEntityList)
    {
        // 检查工具类型
        validateToolType(toolMetaDetailVO.getType());

        // 校验语言
        validateLanguage(toolMetaDetailVO.getSupportedLanguages());
    }

    @Override
    public Boolean validateToolType(String toolType)
    {
        List<BaseDataEntity> baseDataEntityList = baseDataRepository.findByParamTypeIn(Lists.newArrayList(TOOL_TYPE));
        Set<String> toolTypeSet = new HashSet<>();
        baseDataEntityList.forEach(baseDataEntity -> toolTypeSet.add(baseDataEntity.getParamCode()));

        if (!toolTypeSet.contains(toolType))
        {
            String errMsg = String.format("输入的工具类型type:[%s]", toolType);
            log.error("{}不在取值范围内: {}", errMsg, toolTypeSet);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }
        return true;
    }

    @Override
    public Boolean validateLanguage(List<String> languages)
    {
        List<BaseDataEntity> baseDataEntityList = baseDataRepository.findByParamTypeIn(Lists.newArrayList(LANG));
        Map<String, BaseDataEntity> langMap = new HashMap<>();
        baseDataEntityList.forEach(baseDataEntity ->
        {
            langMap.put(baseDataEntity.getLangFullKey(), baseDataEntity);
        });

        if (CollectionUtils.isNotEmpty(languages) && !langMap.keySet().containsAll(languages))
        {
            String errMsg = String.format("输入的工具支持语言: %s, 不在取值范围内: %s", languages, langMap.keySet());
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }
        return true;
    }

    /**
     * 首次添加是给工具默认排序，排在同类型工具的最后一个
     *
     * @param toolMetaEntity
     * @param baseDataEntityList
     */
    private void resetToolOrder(ToolMetaEntity toolMetaEntity, List<BaseDataEntity> baseDataEntityList)
    {
        String toolID = toolMetaEntity.getName();
        String type = toolMetaEntity.getType();
        List<ToolMetaEntity> allTools = toolMetaRepository.findAllByEntityIdIsNotNull();
        Map<String, ToolMetaEntity> toolMap = allTools.stream().collect(Collectors.toMap(ToolMetaEntity::getName, Function.identity()));

        List<BaseDataEntity> toolTypeList = baseDataEntityList.stream()
                .filter(baseDataEntity -> TOOL_TYPE.equals(baseDataEntity.getParamType()))
                .sorted(Comparator.comparing(BaseDataEntity::getParamExtend3))
                .collect(Collectors.toList());

        BaseDataEntity toolOederEntity = baseDataRepository.findFirstByParamType(RedisKeyConstants.KEY_TOOL_ORDER);
        String toolOrder = toolOederEntity.getParamValue();
        String[] toolOrderArr = toolOrder.split(ComConstants.STRING_SPLIT);

        // 1.分组
        Map<String, List<String>> groupToolByTypeMap = new HashMap<>();
        for (int i = 0; i < toolOrderArr.length; i++)
        {
            String name = toolOrderArr[i];
            ToolMetaEntity tool = toolMap.get(name);
            if (tool == null)
            {
                continue;
            }
            String tmpType = tool.getType();

            if (!toolID.equalsIgnoreCase(name))
            {
                groupToolByType(groupToolByTypeMap, name, tmpType);
            }
        }
        groupToolByType(groupToolByTypeMap, toolID, type);

        // 2.按组的顺序叠加工具
        StringBuffer newToolOrder = new StringBuffer();
        for (BaseDataEntity toolType : toolTypeList)
        {
            List<String> toolList = groupToolByTypeMap.get(toolType.getParamCode());
            if (CollectionUtils.isNotEmpty(toolList))
            {
                for (String toolId : toolList)
                {
                    newToolOrder.append(toolId).append(",");
                }
            }
        }

        // 去掉最后一个逗号
        if (newToolOrder.length() > 0)
        {
            newToolOrder.deleteCharAt(newToolOrder.length() - 1);
        }

        toolOederEntity.setParamValue(newToolOrder.toString());
        baseDataRepository.save(toolOederEntity);

        // 同步刷新redis缓存中工具的顺序
        redisTemplate.opsForValue().set(RedisKeyConstants.KEY_TOOL_ORDER, toolOrder);
    }

    private void groupToolByType(Map<String, List<String>> groupToolByTypeMap, String name, String tmpType)
    {
        List<String> toolList = groupToolByTypeMap.get(tmpType);
        if (CollectionUtils.isEmpty(toolList))
        {
            toolList = new ArrayList<>();
            groupToolByTypeMap.put(tmpType, toolList);
        }
        toolList.add(name);
    }
}
