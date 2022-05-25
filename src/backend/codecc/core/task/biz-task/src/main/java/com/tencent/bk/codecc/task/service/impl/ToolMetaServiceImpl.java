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
import com.google.gson.reflect.TypeToken;
import com.tencent.bk.codecc.task.constant.TaskConstants.ToolPattern;
import com.tencent.bk.codecc.task.dao.ToolMetaCacheServiceImpl;
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolMetaRepository;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import com.tencent.bk.codecc.task.model.ToolVersionEntity;
import com.tencent.bk.codecc.task.service.GrayToolProjectService;
import com.tencent.bk.codecc.task.service.ToolMetaService;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.devops.common.api.RefreshDockerImageHashReqVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.AESUtil;
import com.tencent.devops.common.util.GsonUtils;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.web.mq.ConstantsKt;
import com.tencent.devops.image.api.ServiceDockerImageResource;
import com.tencent.devops.image.pojo.CheckDockerImageRequest;
import com.tencent.devops.image.pojo.CheckDockerImageResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.STRING_SPLIT;

/**
 * 工具元数据注册接口实现
 *
 * @version V1.0
 * @date 2020/4/8
 */
@Service
@Slf4j
public class ToolMetaServiceImpl implements ToolMetaService {

    private static final String TOOL_TYPE = "TOOL_TYPE";
    private static final String LANG = "LANG";
    private static final String DOCKER_IMAGE_DEFAULT_ACCOUNT = "DOCKER_IMAGE_DEFAULT_ACCOUNT";
    @Value("${aes.encryptor.key:#{null}}")
    private String encryptorKey;
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
    @Autowired
    private GrayToolProjectService grayToolProjectService;

    @Override
    public ToolMetaDetailVO register(String userName, ToolMetaDetailVO toolMetaDetailVO) {
        log.info("begin register tool: {}", toolMetaDetailVO);

        if (StringUtils.isNotBlank(toolMetaDetailVO.getPattern()) && toolMetaDetailVO.getPattern()
                .equalsIgnoreCase(ToolPattern.STAT.name())) {
            if (toolMetaDetailVO.getCustomToolInfo() == null) {
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                        new String[]{"STAT 工具必须有自定义参数信息"});
            }

            if (toolMetaDetailVO.getCustomToolInfo().getCustomToolDimension() == null
                    || toolMetaDetailVO.getCustomToolInfo().getCustomToolDimension().isEmpty()) {
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                        new String[]{"STAT 工具必须有自定义参数信息"});
            }

            if (toolMetaDetailVO.getCustomToolInfo().getCustomToolParam() == null
                    || toolMetaDetailVO.getCustomToolInfo().getCustomToolParam().isEmpty()) {
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                        new String[]{"STAT 工具必须有自定义参数信息"});
            }
        }

        List<BaseDataEntity> baseDataEntityList =
                baseDataRepository.findByParamTypeIn(Lists.newArrayList(TOOL_TYPE, LANG, DOCKER_IMAGE_DEFAULT_ACCOUNT));

        // 参数校验
        validateParam(toolMetaDetailVO, baseDataEntityList);

        // 获取默认账号密码信息
        BaseDataEntity dockerImageAccount = baseDataEntityList.stream()
                .filter(baseDataEntity -> DOCKER_IMAGE_DEFAULT_ACCOUNT.equals(baseDataEntity.getParamType()))
                .findFirst().get();

        String toolName = toolMetaDetailVO.getName();
        // 根据调试流水线id查询工具是否存在，不存在表示是注册新工具，否则是更新已有工具
        ToolMetaEntity toolMetaEntity = toolMetaRepository.findByDebugPipelineId(toolMetaDetailVO.getDebugPipelineId());
        if (toolMetaEntity == null) {
            // 注册新工具时，需要校验工具名是否已经存在，存在则不能注册
            if (toolMetaRepository.existsByName(toolName)) {
                log.error("tool has register: {}", toolName);
                throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{toolName}, null);
            }

            if (StringUtils.isBlank(toolMetaDetailVO.getPattern())) {
                toolMetaDetailVO.setPattern(ToolPattern.LINT.name());
            }
            toolMetaEntity = new ToolMetaEntity();
            BeanUtils.copyProperties(toolMetaDetailVO, toolMetaEntity);
            toolMetaEntity.setPattern(toolMetaDetailVO.getPattern());
            toolMetaEntity.setDockerImageAccount(dockerImageAccount.getParamCode());
            toolMetaEntity.setDockerImagePasswd(dockerImageAccount.getParamValue());
            toolMetaEntity.setCreatedBy(userName);
            toolMetaEntity.setCreatedDate(System.currentTimeMillis());

            // 给工具排序
            resetToolOrder(toolMetaEntity, baseDataEntityList);
        } else {
            // 更新已有工具时，工具名（name）不能修改
            if (!toolMetaEntity.getName().equals(toolName)) {
                log.error("can not change tool name: {} -> {}", toolMetaEntity.getName(), toolName);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                        new String[]{toolMetaEntity.getName() + " -> " + toolName}, null);
            }

            // 更新已有工具时，工具模式（pattern）不能修改
            String pattern = toolMetaDetailVO.getPattern();
            if (StringUtils.isNotEmpty(pattern) && !toolMetaEntity.getPattern().equals(pattern)) {
                log.error("can not change tool pattern: {} -> {}", toolMetaEntity.getPattern(), pattern);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                        new String[]{toolMetaEntity.getPattern() + " -> " + pattern}, null);
            }

            String oldType = toolMetaEntity.getType();
            toolMetaEntity.setName(toolName);
            toolMetaEntity.setDisplayName(toolMetaDetailVO.getDisplayName());
            toolMetaEntity.setType(toolMetaDetailVO.getType());
            toolMetaEntity.setBriefIntroduction(toolMetaDetailVO.getBriefIntroduction());
            toolMetaEntity.setDockerTriggerShell(toolMetaDetailVO.getDockerTriggerShell());
            toolMetaEntity.setToolScanCommand(toolMetaDetailVO.getToolScanCommand());
            toolMetaEntity.setDockerImageURL(toolMetaDetailVO.getDockerImageURL());
            toolMetaEntity.setLogo(toolMetaDetailVO.getLogo());
            toolMetaEntity.setUpdatedBy(userName);
            toolMetaEntity.setUpdatedDate(System.currentTimeMillis());
            toolMetaEntity.setDockerImageAccount(dockerImageAccount.getParamCode());
            toolMetaEntity.setDockerImagePasswd(dockerImageAccount.getParamValue());

            // 如果工具类型变更了，需要重新对工具排序
            if (oldType.equals(toolMetaDetailVO.getType())) {
                resetToolOrder(toolMetaEntity, baseDataEntityList);
            }
        }

        // 转换语言
        List<String> supportedLanguages = toolMetaDetailVO.getSupportedLanguages();
        long lang = convertLang(supportedLanguages, baseDataEntityList);
        toolMetaEntity.setLang(lang);

        // 转换个性化参数
        toolMetaEntity.setParams(CollectionUtils.isEmpty(toolMetaDetailVO.getToolOptions())
                ? null : GsonUtils.toJson(toolMetaDetailVO.getToolOptions()));

        List<ToolVersionEntity> toolVersionSet = toolMetaEntity.getToolVersions() == null
                ? new ArrayList<>() : toolMetaEntity.getToolVersions();
        ToolVersionEntity toolVersionEntity = toolVersionSet.stream()
                .filter(it -> it.getVersionType().equals(ToolIntegratedStatus.T.name()))
                .findFirst().orElse(null);
        long curTime = System.currentTimeMillis();
        if (toolVersionEntity == null) {
            toolVersionEntity = new ToolVersionEntity(
                    ToolIntegratedStatus.T.name(),
                    toolMetaDetailVO.getDockerTriggerShell(),
                    toolMetaDetailVO.getDockerImageURL(),
                    toolMetaDetailVO.getDockerImageVersion(),
                    toolMetaDetailVO.getForeignDockerImageVersion(),
                    null, curTime, userName, curTime, userName);
            toolVersionSet.add(toolVersionEntity);
            toolMetaEntity.setToolVersions(toolVersionSet);
        } else {
            toolVersionEntity.setDockerTriggerShell(toolMetaDetailVO.getDockerTriggerShell());
            toolVersionEntity.setDockerImageURL(toolMetaDetailVO.getDockerImageURL());
            toolVersionEntity.setDockerImageVersion(toolMetaDetailVO.getDockerImageVersion());
            toolVersionEntity.setForeignDockerImageVersion(toolMetaDetailVO.getForeignDockerImageVersion());
            toolVersionEntity.setCreatedDate(curTime);
            toolVersionEntity.setCreatedBy(userName);
            toolVersionEntity.setUpdatedDate(curTime);
            toolVersionEntity.setUpdatedBy(userName);
        }

        String newDockerImageHash = getDockerImageHash(toolMetaEntity, toolMetaDetailVO.getDockerImageVersion());
        if (StringUtils.isNotEmpty(newDockerImageHash)) {
            log.info("set docker image hash! toolName: {}, newDockerImageHash: {}", toolName, newDockerImageHash);
            toolVersionEntity.setDockerImageHash(newDockerImageHash);
        }

        if (toolMetaDetailVO.getPattern().equals(ToolPattern.STAT.name())) {
            toolMetaEntity.setCustomToolInfo(JsonUtil.INSTANCE.toJson(toolMetaDetailVO.getCustomToolInfo()));
        }

        // 更新工具元数据
        updateToolMeta(toolMetaEntity);

        BeanUtils.copyProperties(toolMetaEntity, toolMetaDetailVO);
        toolMetaDetailVO.setSupportedLanguages(supportedLanguages);

        return toolMetaDetailVO;
    }

    @Override
    public List<ToolMetaDetailVO> queryToolMetaDataList(String projectId) {
        Map<String, ToolMetaBaseVO> toolMetaDetailVOMap =
                toolMetaCacheService.getToolMetaListFromCache(Boolean.TRUE, Boolean.TRUE);


        //工具版本，T-测试版本，G-灰度版本，P-正式发布版本
        String toolV = ToolIntegratedStatus.P.name();

        //查询是否灰度项目，并获取灰度状态
        GrayToolProjectVO grayPro = grayToolProjectService.findGrayInfoByProjectId(projectId);
        if (grayPro != null) {
            if (grayPro.getStatus() == ToolIntegratedStatus.G.value()) {
                toolV = ToolIntegratedStatus.G.name();
            } else if (grayPro.getStatus() == ToolIntegratedStatus.T.value()) {
                toolV = ToolIntegratedStatus.T.name();
            }
        }

        List<ToolMetaDetailVO> toolMetaDetailVOList = new ArrayList<>(toolMetaDetailVOMap.size());
        String finalToolV = toolV;
        toolMetaDetailVOMap.forEach((toolName, tool) -> {
            ToolMetaDetailVO toolMetaDetailVO = (ToolMetaDetailVO) tool;
            toolMetaDetailVO.setGraphicDetails(null);
            toolMetaDetailVO.setLogo(null);
            //通过灰度状态toolV，获取相应版本，并赋值到toolMetaDetailVO的dockerImageVersion
            toolMetaDetailVO.getToolVersions().forEach(toolversion -> {
                if (finalToolV.equals(toolversion.getVersionType())) {
                    toolMetaDetailVO.setDockerTriggerShell(toolversion.getDockerTriggerShell());
                    toolMetaDetailVO.setDockerImageURL(toolversion.getDockerImageURL());
                    toolMetaDetailVO.setDockerImageVersion(toolversion.getDockerImageVersion());
                    toolMetaDetailVO.setDockerImageVersionType(finalToolV);
                }
            });

            toolMetaDetailVOList.add(toolMetaDetailVO);
        });

        return toolMetaDetailVOList;
    }

    /**
     * 刷新工具镜像版本
     *
     * @param refreshDockerImageHashReqVO
     * @return
     */
    @Override
    public Boolean refreshDockerImageHash(RefreshDockerImageHashReqVO refreshDockerImageHashReqVO) {
        log.info("refresh dockerImageHash: {}", GsonUtils.toJson(refreshDockerImageHashReqVO));
        String toolName = refreshDockerImageHashReqVO.getToolName();
        String versionType = refreshDockerImageHashReqVO.getVersionType();
        ToolMetaEntity toolMetaEntity = toolMetaRepository.findFirstByName(toolName);

        if (toolMetaEntity == null) {
            log.error("not found tool by toolName: {}", toolName);
            throw new CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL,
                    String.format("not found tool by toolName: %s", toolName));
        }

        // coverity,klocwork,pinpoint工具没有镜像，通过工具版本号来判断是否有变化
        if (Tool.COVERITY.name().equals(toolName)
                || Tool.KLOCWORK.name().equals(toolName)
                || Tool.PINPOINT.name().equals(toolName)) {
            String newToolVersion = refreshDockerImageHashReqVO.getToolVersion();
            toolMetaEntity.setToolVersion(newToolVersion);
            updateToolMeta(toolMetaEntity);
        } else {
            ToolVersionEntity toolVersion = toolMetaEntity.getToolVersions().stream()
                    .filter(it -> it.getVersionType().equals(versionType)).findFirst().get();
            if (toolVersion == null) {
                log.error("tool[{}] not exist such version type[{}]! ", toolName, versionType);
                return false;
            }
            String newDockerImageHash = getDockerImageHash(toolMetaEntity, toolVersion.getDockerImageVersion());
            if (StringUtils.isNotEmpty(newDockerImageHash)
                    && !newDockerImageHash.equals(toolVersion.getDockerImageHash())) {
                log.info("update docker image hash! toolName: {}, dockerImageHash: {}", toolName, newDockerImageHash);
                toolVersion.setDockerImageHash(newDockerImageHash);
                updateToolMeta(toolMetaEntity);
            }
        }

        log.info("refresh dockerImageHash finish. {}", toolName);
        return true;
    }

    private void updateToolMeta(ToolMetaEntity toolMetaEntity) {
        toolMetaEntity = toolMetaRepository.save(toolMetaEntity);

        // 刷新工具缓存
        // toolMetaRepository.save可能不立即生效
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        toolMetaCacheService.loadToolDetailCache();

        // 刷新Redis缓存后，需要同步刷新task的其他节点以及defect和codeccjob的缓存，确保每个节点的缓存都是最新的工具信息
//        rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_REFRESH_TOOLMETA_CACHE,
//                "", toolMetaEntity.getName());
        redisTemplate.convertAndSend(ConstantsKt.EXCHANGE_REFRESH_TOOLMETA_CACHE, toolMetaEntity.getName());
    }

    @Nullable
    private String getDockerImageHash(ToolMetaEntity toolMetaEntity, String imageVersion) {
        String toolName = toolMetaEntity.getName();
        String userId = toolMetaEntity.getDockerImageAccount();
        String imageUrl = toolMetaEntity.getDockerImageURL();
        if (StringUtils.isNotEmpty(imageVersion)) {
            imageUrl = String.format("%s:%s", imageUrl, imageVersion);
        }
        String passwd = toolMetaEntity.getDockerImagePasswd();
        if (StringUtils.isNotEmpty(passwd)) {
            passwd = AESUtil.INSTANCE.decrypt(encryptorKey, passwd);
        }
        String registryHost = imageUrl.split("/")[0];
        List<CheckDockerImageRequest> requestList =
                Lists.newArrayList(new CheckDockerImageRequest(imageUrl, registryHost, userId, passwd));
        try {
            Result<List<CheckDockerImageResponse>> imageResult =
                    client.getDevopsService(ServiceDockerImageResource.class).checkDockerImage(userId, requestList);
            if (imageResult.isNotOk() || null == imageResult.getData()
                    || null == imageResult.getData().get(0) || imageResult.getData().get(0).getErrorCode() != 0) {
                String errMsg = String.format("get image list fail! toolName: %s, imageUrl: %s, imageResult: %s",
                        toolName, imageUrl, imageResult);
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL, errMsg);
            }
            String dockerImageHash = imageResult.getData().get(0).getId();
            log.info("getDockerImageHash success. toolName: {}, dockerImageHash: {}", toolName, dockerImageHash);
            return dockerImageHash;
        } catch (Throwable throwable) {
            String errMsg = String.format("get image list fail! toolName: %s, imageUrl: %s", toolName, imageUrl);
            log.error(errMsg, throwable);
            throw new CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL, errMsg, throwable);
        }
    }

    private long convertLang(List<String> supportedLanguages, List<BaseDataEntity> baseDataEntityList) {
        Map<String, BaseDataEntity> langMap = new HashMap<>();
        baseDataEntityList.forEach(baseDataEntity -> {
            if (LANG.equals(baseDataEntity.getParamType())) {
                langMap.put(baseDataEntity.getLangFullKey(), baseDataEntity);
            }
        });

        long lang = 0;
        if (CollectionUtils.isNotEmpty(supportedLanguages)) {
            for (int i = 0; i < supportedLanguages.size(); i++) {
                String langStr = supportedLanguages.get(i);
                lang += Long.valueOf(langMap.get(langStr).getParamCode());
            }
        }

        return lang;
    }

    private void validateParam(ToolMetaDetailVO toolMetaDetailVO, List<BaseDataEntity> baseDataEntityList) {
        // 检查工具类型
        validateToolType(toolMetaDetailVO.getType());

        // 校验语言
        validateLanguage(toolMetaDetailVO.getSupportedLanguages());
    }

    @Override
    public Boolean validateToolType(String toolType) {
        List<BaseDataEntity> baseDataEntityList = baseDataRepository.findByParamTypeIn(Lists.newArrayList(TOOL_TYPE));
        Set<String> toolTypeSet = new HashSet<>();
        baseDataEntityList.forEach(baseDataEntity -> toolTypeSet.add(baseDataEntity.getParamCode()));

        if (!toolTypeSet.contains(toolType)) {
            String errMsg = String.format("输入的工具类型type:[%s]", toolType);
            log.error("{}不在取值范围内: {}", errMsg, toolTypeSet);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }
        return true;
    }

    @Override
    public Boolean validateLanguage(List<String> languages) {
        List<BaseDataEntity> baseDataEntityList = baseDataRepository.findByParamTypeIn(Lists.newArrayList(LANG));
        Map<String, BaseDataEntity> langMap = new HashMap<>();
        baseDataEntityList.forEach(baseDataEntity -> {
            langMap.put(baseDataEntity.getLangFullKey(), baseDataEntity);
        });

        if (CollectionUtils.isNotEmpty(languages) && !langMap.keySet().containsAll(languages)) {
            String errMsg = String.format("输入的工具支持语言: %s, 不在取值范围内: %s", languages, langMap.keySet());
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }
        return true;
    }

    /**
     * 获取工具元数据信息
     *
     * @param toolName
     */
    @Override
    public ToolMetaDetailVO obtainToolMetaData(String toolName) {
        ToolMetaEntity toolMetaEntity = toolMetaRepository.findFirstByName(toolName);
        ToolMetaDetailVO toolMetaDetailVO = new ToolMetaDetailVO();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(toolMetaEntity.getCustomToolInfo())) {
            ToolMetaDetailVO.CustomToolInfo customToolInfo;
            customToolInfo = GsonUtils.fromJson(toolMetaEntity.getCustomToolInfo(),
                    new TypeToken<ToolMetaDetailVO.CustomToolInfo>() {
                    }.getType());
            toolMetaDetailVO.setCustomToolInfo(customToolInfo);
        }

        BeanUtils.copyProperties(toolMetaEntity, toolMetaDetailVO);
        log.info("obtain tool meta info from task, toolName: {}", toolName);
        return toolMetaDetailVO;
    }


    /**
     * 首次添加是给工具默认排序，排在同类型工具的最后一个
     *
     * @param toolMetaEntity
     * @param baseDataEntityList
     */
    private void resetToolOrder(ToolMetaEntity toolMetaEntity, List<BaseDataEntity> baseDataEntityList) {
        String toolID = toolMetaEntity.getName();
        String type = toolMetaEntity.getType();
        List<ToolMetaEntity> allTools = toolMetaRepository.findAllByEntityIdIsNotNull();
        Map<String, ToolMetaEntity> toolMap = allTools.stream()
                .collect(Collectors.toMap(ToolMetaEntity::getName, Function.identity()));

        List<BaseDataEntity> toolTypeList = baseDataEntityList.stream()
                .filter(baseDataEntity -> TOOL_TYPE.equals(baseDataEntity.getParamType()))
                .sorted(Comparator.comparing(BaseDataEntity::getParamExtend3))
                .collect(Collectors.toList());

        BaseDataEntity toolOederEntity = baseDataRepository.findFirstByParamType(RedisKeyConstants.KEY_TOOL_ORDER);
        String toolOrder = toolOederEntity.getParamValue();
        String[] toolOrderArr = toolOrder.split(STRING_SPLIT);

        // 1.分组
        Map<String, List<String>> groupToolByTypeMap = new HashMap<>();
        for (int i = 0; i < toolOrderArr.length; i++) {
            String name = toolOrderArr[i];
            ToolMetaEntity tool = toolMap.get(name);
            if (tool == null) {
                continue;
            }
            String tmpType = tool.getType();

            if (!toolID.equalsIgnoreCase(name)) {
                groupToolByType(groupToolByTypeMap, name, tmpType);
            }
        }
        groupToolByType(groupToolByTypeMap, toolID, type);

        // 2.按组的顺序叠加工具
        StringBuffer newToolOrder = new StringBuffer();
        for (BaseDataEntity toolType : toolTypeList) {
            List<String> toolList = groupToolByTypeMap.get(toolType.getParamCode());
            if (CollectionUtils.isNotEmpty(toolList)) {
                for (String toolId : toolList) {
                    newToolOrder.append(toolId).append(",");
                }
            }
        }

        // 去掉最后一个逗号
        if (newToolOrder.length() > 0) {
            newToolOrder.deleteCharAt(newToolOrder.length() - 1);
        }

        toolOederEntity.setParamValue(newToolOrder.toString());
        baseDataRepository.save(toolOederEntity);

        // 同步刷新redis缓存中工具的顺序
        redisTemplate.opsForValue().set(RedisKeyConstants.KEY_TOOL_ORDER, toolOrder);
    }

    private void groupToolByType(Map<String, List<String>> groupToolByTypeMap, String name, String tmpType) {
        List<String> toolList = groupToolByTypeMap.get(tmpType);
        if (CollectionUtils.isEmpty(toolList)) {
            toolList = new ArrayList<>();
            groupToolByTypeMap.put(tmpType, toolList);
        }
        toolList.add(name);
    }

    @Override
    public String updateToolMetaToStatus(String toolName, ToolIntegratedStatus status, String username) {
        ToolMetaEntity toolEntity = toolMetaRepository.findFirstByName(toolName);

        if (toolEntity == null) {
            throw new CodeCCException(CommonMessageCode.INVALID_TOOL_NAME, new String[]{toolName}, null);
        }

        if (status == ToolIntegratedStatus.T) {
            // do nothing
            return "Test integrated status update successfully";
        }

        if (status == ToolIntegratedStatus.G) {
            return changeToolStatus(toolEntity, ToolIntegratedStatus.T, ToolIntegratedStatus.G, username);
        }

        if (status == ToolIntegratedStatus.P) {
            return changeToolStatus(toolEntity, ToolIntegratedStatus.G, ToolIntegratedStatus.P, username);
        }

        return String.format("integrated status %s do not thing", status);
    }

    @Override
    public String revertToolMetaStatus(String toolName, ToolIntegratedStatus status, String username) {
        ToolMetaEntity toolEntity = toolMetaRepository.findFirstByName(toolName);

        if (toolEntity == null) {
            throw new CodeCCException(CommonMessageCode.INVALID_TOOL_NAME, new String[]{toolName}, null);
        }

        List<ToolVersionEntity> lastToolVersions = toolEntity.getLastToolVersions();
        if (CollectionUtils.isEmpty(lastToolVersions)) {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                    new String[]{"lastToolVersions is null, can not be revert"}, null);
        }

        if (status == ToolIntegratedStatus.T) {
            return "do nothing";
        }

        List<ToolVersionEntity> toolVersions = toolEntity.getToolVersions();

        ToolVersionEntity toolVersionEntity = toolVersions.stream().filter(it ->
                it.getVersionType().equals(status.name())).findFirst().orElse(new ToolVersionEntity());
        ToolVersionEntity lastToolVersionEntity = lastToolVersions.stream().filter(it ->
                it.getVersionType().equals(status.name())).findFirst().orElse(new ToolVersionEntity());

        String resultMsg = String.format("from: %s \n to: %s", lastToolVersionEntity, toolVersionEntity);
        BeanUtils.copyProperties(lastToolVersionEntity, toolVersionEntity);

        updateToolMeta(toolEntity);

        return String.format("revert tool status successfully: %s, %s\n%s", toolName, status, resultMsg);
    }

    private String changeToolStatus(ToolMetaEntity toolEntity,
                                    ToolIntegratedStatus fromStatus,
                                    ToolIntegratedStatus toStatus,
                                    String username) {
        log.info("change tool status: {}, {}, {}", toolEntity.getName(), fromStatus, toStatus);


        List<ToolVersionEntity> toolVersions =
                toolEntity.getToolVersions() != null ? toolEntity.getToolVersions() : new ArrayList<>();
        List<ToolVersionEntity> lastToolVersions =
                toolEntity.getLastToolVersions() != null ? toolEntity.getLastToolVersions() : new ArrayList<>();

        Map<String, ToolVersionEntity> toolVersionsMap = new HashMap<>();
        Map<String, ToolVersionEntity> lastToolVersionsMap = new HashMap<>();

        toolVersions.forEach(it -> toolVersionsMap.put(it.getVersionType(), it));

        lastToolVersions.forEach(it -> lastToolVersionsMap.put(it.getVersionType(), it));

        ToolVersionEntity fromEntity = toolVersionsMap.getOrDefault(fromStatus.name(), new ToolVersionEntity());
        ToolVersionEntity toEntity = toolVersionsMap.getOrDefault(toStatus.name(), null);
        ToolVersionEntity lastToolVersion = lastToolVersionsMap.getOrDefault(toStatus.name(), new ToolVersionEntity());

        log.info("backup test data: {}", toolEntity.getName());
        if (toEntity != null) {
            BeanUtils.copyProperties(toEntity, lastToolVersion);
            lastToolVersion.setUpdatedBy(username);
            lastToolVersion.setUpdatedDate(System.currentTimeMillis());
            lastToolVersionsMap.put(toStatus.name(), lastToolVersion);
        } else {
            toEntity = new ToolVersionEntity();
        }


        log.info("copy data: {}", toolEntity.getName());

        BeanUtils.copyProperties(fromEntity, toEntity);
        toEntity.setVersionType(toStatus.name());
        toEntity.setUpdatedBy(username);
        toEntity.setUpdatedDate(System.currentTimeMillis());
        toolVersionsMap.put(toStatus.name(), toEntity);

        // update toolEntity
        String resultMsg = "";
        if (CollectionUtils.isNotEmpty(toolVersionsMap.values())) {
            List<ToolVersionEntity> newToolVersions = new ArrayList<>(toolVersionsMap.values());
            resultMsg = String.format("from : %s ===> %s", toolEntity.getToolVersions(), newToolVersions);
            toolEntity.setToolVersions(newToolVersions);
        }
        if (CollectionUtils.isNotEmpty(lastToolVersionsMap.values())) {
            toolEntity.setLastToolVersions(new ArrayList<>(lastToolVersionsMap.values()));
        }

        updateToolMeta(toolEntity);
        return String.format("%s integrated status update to %s status successfully: %s", fromStatus, toStatus,
                resultMsg);
    }
}
