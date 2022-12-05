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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.constant.DefectMessageCode;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerConfigRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.IgnoreCheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CheckerDetailDao;
import com.tencent.bk.codecc.defect.model.CheckerConfigEntity;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.IgnoreCheckerEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerPropsEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.ICheckerSetBizService;
import com.tencent.bk.codecc.defect.service.IV3CheckerSetBizService;
import com.tencent.bk.codecc.defect.service.ToolBuildInfoService;
import com.tencent.bk.codecc.defect.vo.CheckerCommonCountVO;
import com.tencent.bk.codecc.defect.vo.CheckerCountListVO;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.CheckerListQueryReq;
import com.tencent.bk.codecc.defect.vo.CheckerProps;
import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerListSortType;
import com.tencent.bk.codecc.defect.vo.enums.CheckerRecommendType;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetSource;
import com.tencent.bk.codecc.task.api.ServiceGrayToolProjectResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.UserMetaRestResource;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.CovSubcategoryVO;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.OpenCheckerVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.ToolVersionVO;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CheckerConstants;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.util.StringCompress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 多工具规则服务层实现
 *
 * @version V1.0
 * @date 2019/4/26
 */
@Service
@Slf4j
public class CheckerServiceImpl implements CheckerService
{

    // 因为Swift语言的一些规则是默认打开，而其他语言默认不打开，所以针对swift语言，
    // 当用户不打开下面这些规则时，需要将以下规则通过closeDefaultCheckers字段返回给工具侧，让工具侧disable掉
    private static final List<String> SWIFT_DEFAULT_CHECKERS = Lists.newArrayList("HARDCODED_CREDENTIALS", "RISKY_CRYPTO");

    @Autowired
    private CheckerRepository checkerRepository;

    @Autowired
    private CheckerConfigRepository checkerConfigRepository;

    @Autowired
    private IgnoreCheckerRepository ignoreCheckerRepository;

    @Autowired
    private ICheckerSetBizService checkerSetBizService;

    @Autowired
    private ToolBuildInfoService toolBuildInfoService;

    @Autowired
    private CheckerService checkerService;

    @Autowired
    private CheckerDetailDao checkerDetailDao;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CheckerSetRepository checkerSetRepository;

    @Autowired
    private IV3CheckerSetBizService iv3CheckerSetBizService;

    @Autowired
    private Client client;

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Override
    public Map<String, CheckerDetailVO> queryAllChecker(String toolName) {
        return queryAllChecker(Lists.newArrayList(toolName), null);
    }

    @Override
    public Map<String, CheckerDetailVO> queryAllChecker(List<String> toolNameSet, String checkerSet)
    {
        List<CheckerDetailEntity> checkerDetailEntityList = checkerRepository.findByToolNameIn(toolNameSet);
        Set<String> checkerDetailSet = new HashSet<>();
        if (StringUtils.isNotEmpty(checkerSet)) {
            checkerSetRepository.findByCheckerSetId(checkerSet).forEach(checkerSetEntity -> {
                checkerDetailSet.addAll(checkerSetEntity.getCheckerProps().stream().map(CheckerPropsEntity::getCheckerKey).collect(Collectors.toList()));
            });
        }
        List<CheckerDetailVO> checkerDetailList = new ArrayList<>();
        checkerDetailEntityList.forEach(checkerDetailEntity ->
                {
                    if (StringUtils.isNotBlank(checkerSet)) {
                        if (!checkerDetailSet.contains(checkerDetailEntity.getCheckerKey())) {
                            return;
                        }
                    }
                    CheckerDetailVO checkerDetailVO = new CheckerDetailVO();
                    BeanUtils.copyProperties(checkerDetailEntity, checkerDetailVO);
                    checkerDetailList.add(checkerDetailVO);
                });
        return checkerDetailList.stream()
                .collect(Collectors.toMap(CheckerDetailVO::getCheckerKey, Function.identity(), (k, v) -> v));
    }

    @Override
    public Map<String, CheckerDetailVO> queryOpenCheckers(long taskId, String toolName, String paramJson, long codeLang)
    {
        List<CheckerDetailVO> checkerDetailEntityList = queryAllChecker(taskId, toolName, paramJson, codeLang);
        IgnoreCheckerEntity ignoreChecker = ignoreCheckerRepository.findFirstByTaskIdAndToolName(taskId, toolName);

        Map<String, CheckerDetailVO> opencheckersMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(checkerDetailEntityList))
        {
            for (CheckerDetailVO checkerDetail : checkerDetailEntityList)
            {
                boolean isOpen;

                // 默认规则要判断是否在关闭的默认规则列表中
                if (ComConstants.CheckerPkgKind.DEFAULT.value().equals(checkerDetail.getPkgKind()))
                {
                    isOpen = true;
                    if (ignoreChecker != null && CollectionUtils.isNotEmpty(ignoreChecker.getCloseDefaultCheckers())
                            && ignoreChecker.getCloseDefaultCheckers().contains(checkerDetail.getCheckerKey()))
                    {
                        isOpen = false;
                    }
                }

                // 非默认规则要判断是否在打开的非默认规则中
                else
                {
                    isOpen = false;
                    if (ignoreChecker != null && CollectionUtils.isNotEmpty(ignoreChecker.getOpenNonDefaultCheckers())
                            && ignoreChecker.getOpenNonDefaultCheckers().contains(checkerDetail.getCheckerKey()))
                    {
                        isOpen = true;
                    }
                }
                if (isOpen)
                {
                    opencheckersMap.put(checkerDetail.getCheckerKey(), checkerDetail);
                }
            }
        }

        return opencheckersMap;
    }

    @Override
    public Set<String> queryPkgRealCheckers(String pkgId, String toolName, TaskDetailVO taskDetailVO)
    {
        return queryPkgRealCheckers(pkgId, Lists.newArrayList(toolName), taskDetailVO);
    }

    @Override
    public Set<String> queryPkgRealCheckers(String pkgId, List<String> toolNameSet, TaskDetailVO taskDetailVO) {
        Set<String> conditionPkgCheckers = Sets.newHashSet();
        if (StringUtils.isNotEmpty(pkgId))
        {
            Set<String> upperToolNameSet = toolNameSet.stream().map(String::toUpperCase).collect(Collectors.toSet());
            ToolConfigInfoVO toolConfigInfoVO = taskDetailVO.getToolConfigInfoList().stream().filter(
                toolConfigInfoVO1 ->
                    upperToolNameSet.contains(toolConfigInfoVO1.getToolName().toUpperCase())).
                findFirst().orElseThrow(() -> new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL));
            Map<String, CheckerDetailVO> allCheckers = queryOpenCheckers(toolConfigInfoVO.getTaskId(), toolConfigInfoVO.getToolName(),
                toolConfigInfoVO.getParamJson(), taskDetailVO.getCodeLang());
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
    public List<CheckerDetailVO> queryAllChecker(long taskId, String toolName, String paramJson, long codeLang)
    {
        if (StringUtils.isNotEmpty(paramJson))
        {
            paramJson.trim();
        }
        List<CheckerDetailEntity> checkerDetailEntities = checkerRepository.findByToolName(toolName);

        List<CheckerDetailVO> checkerDetailVOList = filterCheckerDetailByCodeLangAndParamJson(toolName, paramJson, codeLang, checkerDetailEntities);

        return getCheckerConfigParams(taskId, toolName, checkerDetailVOList);
    }

    @Override
    public List<CheckerDetailVO> queryCheckerByTool(String toolName)
    {
        List<CheckerDetailEntity> checkerDetailEntities = checkerRepository.findByToolName(toolName);

        return generateCheckerDetailVo(checkerDetailEntities);
    }

    @NotNull
    protected List<CheckerDetailVO> filterCheckerDetailByCodeLangAndParamJson(String toolName, String paramJson, Long codeLang, List<CheckerDetailEntity> checkerDetailEntities)
    {
        List<CheckerDetailEntity> list = checkerDetailEntities.stream().
            filter(checkerDetailEntity ->
            {
                if (checkerDetailEntity.getStatus() == 1) {
                    return false;
                }
                JSONObject parseObj = StringUtils.isBlank(paramJson) ? new JSONObject() : JSONObject.fromObject(paramJson);
                if (ComConstants.Tool.ESLINT.name().equals(toolName)) {
                    if (parseObj.containsKey("eslint_rc")) {
                        String eslintRc = parseObj.getString("eslint_rc");
                        if (ComConstants.EslintFrameworkType.standard.name().equals(eslintRc)) {
                            return ComConstants.EslintFrameworkType.standard.name().equals(checkerDetailEntity.getFrameworkType());
                        } else if (ComConstants.EslintFrameworkType.vue.name().equals(eslintRc)) {
                            return !ComConstants.EslintFrameworkType.react.name().equals(checkerDetailEntity.getFrameworkType());
                        } else if (ComConstants.EslintFrameworkType.react.name().equals(eslintRc)) {
                            return !ComConstants.EslintFrameworkType.vue.name().equals(checkerDetailEntity.getFrameworkType());
                        }
                    }
                } else if (ComConstants.Tool.PHPCS.name().equals(toolName)) {
                    if (parseObj.containsKey("phpcs_standard")) {
                        String phpcsStandard = parseObj.getString("phpcs_standard");
                        if (StringUtils.isNotBlank(phpcsStandard)) {
                            int standCode = ComConstants.PHPCSStandardCode.valueOf(phpcsStandard).code();
                            return (standCode & checkerDetailEntity.getStandard()) != 0;
                        }
                    }
                } else if (ComConstants.Tool.COVERITY.name().equals(toolName) || ComConstants.Tool.KLOCWORK.name().equals(toolName)) {
                    return ((null == codeLang ? 0L : codeLang) & checkerDetailEntity.getLanguage()) != 0;
                }
                return true;
            }).collect(Collectors.toList());
        return generateCheckerDetailVo(list);
    }

    private List<CheckerDetailVO> generateCheckerDetailVo(List<CheckerDetailEntity> entityList) {
        return entityList.stream().map(checkerDetailEntity ->
        {
            CheckerDetailVO checkerDetailVO = new CheckerDetailVO();
            BeanUtils.copyProperties(checkerDetailEntity, checkerDetailVO);
            if (CollectionUtils.isNotEmpty(checkerDetailEntity.getCovSubcategory()))
            {
                checkerDetailVO.setCovSubcategory(checkerDetailEntity.getCovSubcategory().stream().map(covSubcategoryEntity ->
                {
                    CovSubcategoryVO covSubcategoryVO = new CovSubcategoryVO();
                    BeanUtils.copyProperties(covSubcategoryEntity, covSubcategoryVO);
                    return covSubcategoryVO;
                }).collect(Collectors.toList()));
            }
            return checkerDetailVO;
        }).collect(Collectors.toList());
    }

    /**
     * 获取规则配置参数
     *
     * @param taskId
     * @param toolName
     * @param checkerDetailVOList
     */
    private List<CheckerDetailVO> getCheckerConfigParams(long taskId, String toolName, List<CheckerDetailVO> checkerDetailVOList)
    {
        // 规则参数配置
        List<CheckerConfigEntity> configList = checkerConfigRepository.findByTaskIdAndToolName(taskId, toolName);
        Map<String, CheckerConfigEntity> checkerConfigEntityMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(configList))
        {
            for (CheckerConfigEntity checkerConfigEntity : configList)
            {
                checkerConfigEntityMap.put(checkerConfigEntity.getCheckerKey(), checkerConfigEntity);
            }
        }

        for (CheckerDetailVO checkerVo : checkerDetailVOList)
        {
            // 规则是否支持配置
            CheckerConfigEntity config = checkerConfigEntityMap.get(checkerVo.getCheckerKey());
            if (Objects.isNull(checkerVo.getEditable()) || !checkerVo.getEditable()
                    || config == null)
            {
                continue;
            }

            // 设置配置参数
            checkerVo.setProps(config.getProps());
            checkerVo.setParamValue(config.getParamValue());
            checkerVo.setCheckerDesc(config.getCheckerDesc());
        }
        return checkerDetailVOList;
    }


    /**
     * 更新规则配置参数
     *
     * @param taskId
     * @param toolName
     * @param checkerKey
     * @param paramValue
     * @return
     */
    @Override
    public boolean updateCheckerConfigParam(Long taskId, String toolName, String checkerKey, String paramValue, String user)
    {
        CheckerDetailEntity checkerDetailEntity = checkerRepository.findFirstByToolNameAndCheckerKey(toolName, checkerKey);
        if (Objects.isNull(checkerDetailEntity))
        {
            log.info("the checker detail is null, taskId: {}, checkerKey: {}", taskId, checkerKey);
            throw new CodeCCException(DefectMessageCode.NOT_FIND_CHECKER, new String[]{checkerKey}, null);
        }
        if (!checkerDetailEntity.getEditable())
        {
            log.info("This checker has no configuration parameter function, taskId: {}, checkerKey: {}", taskId, checkerKey);
            throw new CodeCCException(DefectMessageCode.NOT_FIND_CHECKER, new String[]{checkerKey}, null);
        }

        CheckerConfigEntity checkerConfigEntity = checkerConfigRepository.findFirstByTaskIdAndToolNameAndCheckerKey(taskId, toolName, checkerKey);
        if (Objects.nonNull(checkerConfigEntity) && (paramValue.equals(checkerConfigEntity.getParamValue())))
        {
            log.info("The checker parameter configuration is the same and cannot be modified repeatedly, taskId: {}, checkerKey: {}", taskId, checkerKey);
            return true;
        }
        if (Objects.isNull(checkerConfigEntity))
        {
            checkerConfigEntity = new CheckerConfigEntity();
            checkerConfigEntity.setTaskId(taskId);
            checkerConfigEntity.setToolName(toolName);
            checkerConfigEntity.setCheckerKey(checkerKey);
            checkerConfigEntity.setCreatedBy("system_admin");
            checkerConfigEntity.setCreatedDate(System.currentTimeMillis());
        }
        else
        {
            checkerConfigEntity.setUpdatedBy("system_admin");
            checkerConfigEntity.setUpdatedDate(System.currentTimeMillis());
        }

        String checkerDescModel = checkerDetailEntity.getCheckerDescModel();
        String checkerDesc = StringUtils.isNotBlank(checkerDescModel) ? String.format(checkerDescModel, paramValue) : checkerDetailEntity.getCheckerDesc();
        //CheckerProps checkerProps = JsonUtil.INSTANCE.to(checkerDetailEntity.getProps(), CheckerProps.class);
        //checkerProps.setPropValue(paramValue);
        //checkerProps.setDisplayValue(paramValue);
        // TODO 这里是List
        List<CheckerProps> checkerPropsList = JsonUtil.INSTANCE.to(checkerDetailEntity.getProps(), new TypeReference<List<CheckerProps>>()
        {
        });
        for (CheckerProps checkerProps : checkerPropsList)
        {
            checkerProps.setPropValue(paramValue);
            checkerProps.setDisplayValue(paramValue);
        }
        checkerConfigEntity.setParamValue(paramValue);
        checkerConfigEntity.setCheckerDesc(checkerDesc);
        checkerConfigEntity.setCheckerDetail(checkerDetailEntity);
        checkerConfigEntity.setProps(JsonUtil.INSTANCE.toJson(checkerPropsList));
        checkerConfigRepository.save(checkerConfigEntity);

        Result<TaskDetailVO> taskResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        if (taskResult.isNotOk() || null == taskResult.getData())
        {
            log.error("task information is empty! task id: {}", taskId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        TaskDetailVO taskDetailVO = taskResult.getData();
        String paramJson = null;
        if (CollectionUtils.isNotEmpty(taskDetailVO.getToolConfigInfoList()))
        {
            for (ToolConfigInfoVO toolConfigInfoVO : taskDetailVO.getToolConfigInfoList())
            {
                if (toolName.equals(toolConfigInfoVO.getToolName()))
                {
                    paramJson = toolConfigInfoVO.getParamJson();
                    break;
                }
            }
        }

        // 查询已打开规则
        Map<String, CheckerDetailVO> openCheckerMap = checkerService.queryOpenCheckers(taskId, toolName, paramJson, taskDetailVO.getCodeLang());

        // 如果修改的是已打开的规则参数，则需要清除规则集并设置强制全量扫描标志
        if (MapUtils.isNotEmpty(openCheckerMap) && openCheckerMap.get(checkerKey) != null)
        {
            // 清除已绑定的规则集
            checkerSetBizService.clearTaskCheckerSets(taskDetailVO, Lists.newArrayList(toolName), user, true);

            // 设置强制全量扫描标志
            toolBuildInfoService.setForceFullScan(taskId, Lists.newArrayList(toolName));
        }

        return true;
    }

    @Override
    public CheckerDetailVO queryCheckerDetail(String toolName, String checkerKey)
    {
        CheckerDetailEntity checkerDetailEntity = checkerRepository.findFirstByToolNameAndCheckerKey(toolName, checkerKey);
        CheckerDetailVO checkerDetailVO = new CheckerDetailVO();
        if (checkerDetailEntity != null)
        {
            BeanUtils.copyProperties(checkerDetailEntity, checkerDetailVO);
            String codeExample = checkerDetailVO.getCodeExample();
            if (StringUtils.isNotEmpty(codeExample))
            {
                try
                {
                    checkerDetailVO.setCodeExample(StringCompress.uncompress(codeExample));
                }
                catch (Exception e)
                {
                    checkerDetailVO.setCodeExample(codeExample);
                }
            }
        }
        else
        {
            checkerDetailVO.setToolName(toolName);
            checkerDetailVO.setCheckerKey(checkerKey);
        }
        return checkerDetailVO;
    }

    @Override
    public List<CheckerDetailVO> queryCheckerDetailList(CheckerListQueryReq checkerListQueryReq, String projectId, Integer pageNum,
                                                        Integer pageSize, Sort.Direction sortType, CheckerListSortType sortField)
    {
        //处理规则集的选中问题
        Set<String> checkerKeySet;
        //单个规则集关联
        if (StringUtils.isNotEmpty(checkerListQueryReq.getCheckerSetId()))
        {
            checkerKeySet = getCheckerSetKeys(checkerListQueryReq.getCheckerSetId(), checkerListQueryReq.getVersion());
        }
        //项目下的所有规则集关联
        else
        {
            checkerKeySet = getCheckerSetKeys(projectId);
        }

        Result<GrayToolProjectVO> result = client.get(ServiceGrayToolProjectResource.class)
                .getGrayToolProjectInfoByProjrctId(projectId);

        if (result.isNotOk() || result.getData() == null) {
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }
        int toolIntegratedStatus = result.getData().getStatus();

        //封装分页类
        List<CheckerDetailEntity> checkerDetailEntityList = checkerDetailDao.findByComplexCheckerCondition(checkerListQueryReq.getKeyWord(), checkerListQueryReq.getCheckerLanguage(),
                checkerListQueryReq.getCheckerCategory(), checkerListQueryReq.getToolName(), checkerListQueryReq.getTag(),
                checkerListQueryReq.getSeverity(), checkerListQueryReq.getEditable(), checkerListQueryReq.getCheckerRecommend(),
                checkerKeySet, checkerListQueryReq.getCheckerSetSelected(), pageNum, pageSize,
                sortType, sortField, ToolIntegratedStatus.getInstance(toolIntegratedStatus));


        return checkerDetailEntityList.stream().map(checkerDetailEntity ->
        {
            CheckerDetailVO checkerDetailVO = new CheckerDetailVO();
            BeanUtils.copyProperties(checkerDetailEntity, checkerDetailVO);
            checkerDetailVO.setCheckerCategoryName(CheckerCategory.valueOf(checkerDetailEntity.getCheckerCategory()).getName());
            checkerDetailVO.setCheckerKeyAndToolName(String.format("%s_%s", checkerDetailEntity.getCheckerKey(),
                    checkerDetailEntity.getToolName()));
            if (ComConstants.PROMPT_IN_DB == checkerDetailVO.getSeverity())
            {
                checkerDetailVO.setSeverity(ComConstants.PROMPT);
            }
            return checkerDetailVO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CheckerCommonCountVO> queryCheckerCountListNew(CheckerListQueryReq checkerListQueryReq, String projectId)
    {
        Result<GrayToolProjectVO> result = client.get(ServiceGrayToolProjectResource.class)
                .getGrayToolProjectInfoByProjrctId(projectId);

        if (result.isNotOk() || result.getData() == null) {
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }

        int toolIntegratedStatus = result.getData().getStatus();

        List<CheckerDetailEntity> checkerDetailEntityList = checkerDetailDao.findByComplexCheckerCondition(checkerListQueryReq.getKeyWord(), null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, ToolIntegratedStatus.getInstance(toolIntegratedStatus));
        List<CheckerCommonCountVO> checkerCommonCountVOList = new ArrayList<>();
        //1. 语言数量map
        Map<String, Integer> langMap = new HashMap<>();
        List<String> langOrder = Arrays.asList(redisTemplate.opsForValue().get(RedisKeyConstants.KEY_LANG_ORDER).split(","));
        for (String codeLang : langOrder)
        {
            langMap.put(codeLang, 0);
        }
        //2. 规则数量map(数量不变，初始化就把所有规则放上去)
        CheckerCategory[] checkerCategories = CheckerCategory.values();
        Map<String, Integer> categoryMap = new LinkedHashMap<>();
        for (CheckerCategory checkerCategory : checkerCategories)
        {
            categoryMap.put(checkerCategory.name(), 0);
        }
        //3. 规则工具数量map
        Map<String, Integer> toolMap = new HashMap<>();
        List<String> toolOrder = Arrays.asList(redisTemplate.opsForValue().get(RedisKeyConstants.KEY_TOOL_ORDER).split(","));
        for (String tool : toolOrder)
        {
            ToolMetaBaseVO toolMeta = toolMetaCacheService.getToolBaseMetaCache(tool);
            List<ToolVersionVO> toolVersionList = toolMeta.getToolVersions();
            List<String> versionList = Collections.singletonList(
                    String.valueOf(ToolIntegratedStatus.P.value()));
            if (CollectionUtils.isNotEmpty(toolVersionList)) {
                versionList = toolVersionList
                        .stream()
                        .map(ToolVersionVO::getVersionType)
                        .collect(Collectors.toList());
            }

            if (versionList.contains(String.valueOf(ToolIntegratedStatus.P.value()))) {
                toolMap.put(tool, 0);
                continue;
            }

            if (toolIntegratedStatus != ToolIntegratedStatus.P.value()
                    && versionList.contains(String.valueOf(toolIntegratedStatus))) {
                toolMap.put(tool, 0);
            }
        }
        //4. 规则标签
        Map<String, Integer> tagMap = new HashMap<>();
        //5. 规则严重级别数量(数量不变，初始化就把所有规则放上去)
        Map<Integer, Integer> severityMap = new HashMap<Integer, Integer>()
        {{
            put(ComConstants.PROMPT, 0);
            put(ComConstants.NORMAL, 0);
            put(ComConstants.SERIOUS, 0);
        }};
        //6. 可修改参数数量
        Map<Boolean, Integer> editableMap = new HashMap<Boolean, Integer>()
        {{
            put(Boolean.TRUE, 0);
            put(Boolean.FALSE, 0);
        }};
        //7. 系统推荐数量
        Map<String, Integer> recommendMap = new HashMap<>();
        CheckerRecommendType[] checkerRecommendTypes = CheckerRecommendType.values();
        for (CheckerRecommendType checkerRecommendType : checkerRecommendTypes)
        {
            recommendMap.put(checkerRecommendType.name(), 0);
        }
        //8.选中未选中数量
        Map<Boolean, Integer> selectMap = new LinkedHashMap<Boolean, Integer>()
        {{
            put(Boolean.TRUE, 0);
            put(Boolean.FALSE, 0);
        }};
        //处理规则集的选中问题
        Set<String> checkerKeySet;
        //单个规则集关联
        if (StringUtils.isNotEmpty(checkerListQueryReq.getCheckerSetId()))
        {
            checkerKeySet = getCheckerSetKeys(checkerListQueryReq.getCheckerSetId(), checkerListQueryReq.getVersion());
        }
        //项目下的所有规则集关联
        else
        {
            checkerKeySet = getCheckerSetKeys(projectId);
        }

        //9. 总数
        List<CheckerDetailEntity> totalList = new ArrayList<>();

        checkerDetailEntityList.forEach(checkerDetailEntity ->
        {
            //1. 计算语言数量
            if (judgeQualifiedChecker(null, checkerListQueryReq.getCheckerCategory(), checkerListQueryReq.getToolName(),
                    checkerListQueryReq.getTag(), checkerListQueryReq.getSeverity(), checkerListQueryReq.getEditable(),
                    checkerListQueryReq.getCheckerRecommend(), checkerListQueryReq.getCheckerSetSelected(), checkerKeySet,
                    checkerDetailEntity) && CollectionUtils.isNotEmpty(checkerDetailEntity.getCheckerLanguage()))
            {
                checkerDetailEntity.getCheckerLanguage()
                        .stream()
                        .filter(Objects::nonNull)
                        .forEach(checkerLanguage ->
                                langMap.compute(checkerLanguage, (k, v) -> {
                                    if (null == v) {
                                        return 1;
                                    } else {
                                        v++;
                                        return v;
                                    }
                                })
                );
            }

            //2. 计算规则数量
            if (judgeQualifiedChecker(checkerListQueryReq.getCheckerLanguage(), null, checkerListQueryReq.getToolName(),
                    checkerListQueryReq.getTag(), checkerListQueryReq.getSeverity(), checkerListQueryReq.getEditable(),
                    checkerListQueryReq.getCheckerRecommend(), checkerListQueryReq.getCheckerSetSelected(), checkerKeySet, checkerDetailEntity) && StringUtils.isNotEmpty(checkerDetailEntity.getCheckerCategory()))
            {
                categoryMap.compute(checkerDetailEntity.getCheckerCategory(), (k, v) ->
                {
                    if (null == v)
                    {
                        return 1;
                    }
                    else
                    {
                        v++;
                        return v;
                    }
                });
            }

            //3. 计算工具数量
            if (judgeQualifiedChecker(checkerListQueryReq.getCheckerLanguage(), checkerListQueryReq.getCheckerCategory(), null,
                    checkerListQueryReq.getTag(), checkerListQueryReq.getSeverity(), checkerListQueryReq.getEditable(),
                    checkerListQueryReq.getCheckerRecommend(), checkerListQueryReq.getCheckerSetSelected(), checkerKeySet, checkerDetailEntity) && StringUtils.isNotEmpty(checkerDetailEntity.getToolName()))
            {
                toolMap.compute(checkerDetailEntity.getToolName(), (k, v) ->
                {
                    if (null == v)
                    {
                        return 1;
                    }
                    else
                    {
                        v++;
                        return v;
                    }
                });
            }

            //4. 计算规则标签数量
            if (judgeQualifiedChecker(checkerListQueryReq.getCheckerLanguage(), checkerListQueryReq.getCheckerCategory(), checkerListQueryReq.getToolName(),
                    null, checkerListQueryReq.getSeverity(), checkerListQueryReq.getEditable(),
                    checkerListQueryReq.getCheckerRecommend(), checkerListQueryReq.getCheckerSetSelected(), checkerKeySet, checkerDetailEntity) && CollectionUtils.isNotEmpty(checkerDetailEntity.getCheckerTag()))
            {
                checkerDetailEntity.getCheckerTag().forEach(checkerTag ->
                        tagMap.compute(checkerTag, (k, v) ->
                        {
                            if (null == v)
                            {
                                return 1;
                            }
                            else
                            {
                                v++;
                                return v;
                            }
                        })
                );
            }

            //5. 计算严重级别数量
            if (judgeQualifiedChecker(checkerListQueryReq.getCheckerLanguage(), checkerListQueryReq.getCheckerCategory(), checkerListQueryReq.getToolName(),
                    checkerListQueryReq.getTag(), null, checkerListQueryReq.getEditable(),
                    checkerListQueryReq.getCheckerRecommend(), checkerListQueryReq.getCheckerSetSelected(), checkerKeySet, checkerDetailEntity) && 0 != checkerDetailEntity.getSeverity())
            {
                severityMap.compute(ComConstants.PROMPT_IN_DB == checkerDetailEntity.getSeverity() ? ComConstants.PROMPT : checkerDetailEntity.getSeverity()
                        , (k, v) ->
                        {
                            if (null == v)
                            {
                                return 1;
                            }
                            else
                            {
                                v++;
                                return v;
                            }
                        });
            }

            //6. 计算可修改参数数量
            if (judgeQualifiedChecker(checkerListQueryReq.getCheckerLanguage(), checkerListQueryReq.getCheckerCategory(), checkerListQueryReq.getToolName(),
                    checkerListQueryReq.getTag(), checkerListQueryReq.getSeverity(), null,
                    checkerListQueryReq.getCheckerRecommend(), checkerListQueryReq.getCheckerSetSelected(), checkerKeySet, checkerDetailEntity))
            {
                editableMap.compute(null == checkerDetailEntity.getEditable() ? Boolean.FALSE : checkerDetailEntity.getEditable(), (k, v) ->
                {
                    if (null == v)
                    {
                        return 1;
                    }
                    else
                    {
                        v++;
                        return v;
                    }
                });
            }

            //7. 计算推荐类型数量
            if (judgeQualifiedChecker(checkerListQueryReq.getCheckerLanguage(), checkerListQueryReq.getCheckerCategory(), checkerListQueryReq.getToolName(),
                    checkerListQueryReq.getTag(), checkerListQueryReq.getSeverity(), checkerListQueryReq.getEditable(),
                    null, checkerListQueryReq.getCheckerSetSelected(), checkerKeySet, checkerDetailEntity) && StringUtils.isNotEmpty(checkerDetailEntity.getCheckerRecommend()))
            {
                recommendMap.compute(checkerDetailEntity.getCheckerRecommend(), (k, v) ->
                {
                    if (null == v)
                    {
                        return 1;
                    }
                    else
                    {
                        v++;
                        return v;
                    }
                });
            }

            //8. 计算选中未选中数量
            if (judgeQualifiedChecker(checkerListQueryReq.getCheckerLanguage(), checkerListQueryReq.getCheckerCategory(), checkerListQueryReq.getToolName(),
                    checkerListQueryReq.getTag(), checkerListQueryReq.getSeverity(), checkerListQueryReq.getEditable(),
                    checkerListQueryReq.getCheckerRecommend(), null, null, checkerDetailEntity))
            {
                selectMap.compute(checkerKeySet.contains(checkerDetailEntity.getCheckerKey()), (k, v) ->
                {
                    if (null == v)
                    {
                        return 1;
                    }
                    else
                    {
                        v++;
                        return v;
                    }
                });
            }


            if (judgeQualifiedChecker(checkerListQueryReq.getCheckerLanguage(), checkerListQueryReq.getCheckerCategory(), checkerListQueryReq.getToolName(),
                    checkerListQueryReq.getTag(), checkerListQueryReq.getSeverity(), checkerListQueryReq.getEditable(),
                    checkerListQueryReq.getCheckerRecommend(), checkerListQueryReq.getCheckerSetSelected(), checkerKeySet, checkerDetailEntity))
            {
                totalList.add(checkerDetailEntity);
            }
        });

        //按照语言顺序
        List<CheckerCountListVO> checkerLangCountVOList = langMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(entry.getKey(), null, entry.getValue())
        ).sorted(Comparator.comparingInt(o -> langOrder.indexOf(o.getKey()))).collect(Collectors.toList());
        //按照枚举中的排序
        List<CheckerCategory> categoryOrder = Arrays.asList(CheckerCategory.values());
        List<CheckerCountListVO> checkerCateCountVOList = categoryMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(CheckerCategory.valueOf(entry.getKey()).name(),
                        CheckerCategory.valueOf(entry.getKey()).getName(), entry.getValue())
        ).sorted(Comparator.comparingInt(o -> categoryOrder.indexOf(CheckerCategory.valueOf(o.getKey())))).collect(Collectors.toList());

        //按照工具的排序
        List<CheckerCountListVO> checkerToolCountVOList = toolMap.entrySet().stream().filter(stringIntegerEntry -> stringIntegerEntry.getValue() > 0).
                map(entry ->
                        new CheckerCountListVO(entry.getKey(), null, entry.getValue())
                ).sorted(Comparator.comparingInt(o -> toolOrder.indexOf(o.getKey()))).collect(Collectors.toList());
        List<CheckerCountListVO> checkerTagCountVOList = tagMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(entry.getKey(), null, entry.getValue())
        ).sorted(Comparator.comparing(CheckerCountListVO::getCount).reversed()).collect(Collectors.toList());
        List<CheckerCountListVO> checkerSeverityCountVOList = severityMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(String.valueOf(entry.getKey()), null, entry.getValue())
        ).collect(Collectors.toList());
        List<CheckerCountListVO> checkerEditableCountVOList = editableMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(String.valueOf(entry.getKey()), null, entry.getValue())
        ).collect(Collectors.toList());

        //按照枚举中排序
        List<CheckerRecommendType> recommendOrder = Arrays.asList(CheckerRecommendType.values());
        List<CheckerCountListVO> checkerRecommendCountVOList = recommendMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(CheckerRecommendType.valueOf(entry.getKey()).name(), CheckerRecommendType.valueOf(entry.getKey()).getName(),
                        entry.getValue())
        ).sorted(Comparator.comparingInt(o -> recommendOrder.indexOf(CheckerRecommendType.valueOf(o.getKey())))).collect(Collectors.toList());

        List<CheckerCountListVO> checkerSetSelectCountVOList = selectMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(String.valueOf(entry.getKey()), entry.getKey() ? "启用中" : "未启用", entry.getValue())
        ).collect(Collectors.toList());
        List<CheckerCountListVO> checkerTotalCountVOList = Collections.singletonList(new CheckerCountListVO("total", null, totalList.size()));


        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerLanguage", checkerLangCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerCategory", checkerCateCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("toolName", checkerToolCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("tag", checkerTagCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("severity", checkerSeverityCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("editable", checkerEditableCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerRecommend", checkerRecommendCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerSetSelected", checkerSetSelectCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("total", checkerTotalCountVOList));

        return checkerCommonCountVOList;

    }

    @Override
    public AnalyzeConfigInfoVO getTaskCheckerConfig(AnalyzeConfigInfoVO analyzeConfigInfoVO)
    {
        long beginTime = System.currentTimeMillis();
        log.info("begin getTaskCheckerConfig: taskId={}, toolName={}", analyzeConfigInfoVO.getTaskId(), analyzeConfigInfoVO.getMultiToolType());
        long taskId = analyzeConfigInfoVO.getTaskId();
        String toolName = analyzeConfigInfoVO.getMultiToolType();
        List<CheckerSetVO> checkerSetVOList = iv3CheckerSetBizService.getCheckerSetsByTaskId(taskId);

        if (CollectionUtils.isEmpty(checkerSetVOList))
        {
            log.info("task {} checker set is empty.", taskId);
            return analyzeConfigInfoVO;
        }

        List<CheckerDetailEntity> checkerDetailEntities = checkerRepository.findByToolName(toolName);

        List<CheckerDetailVO> checkerDetailVOList = filterCheckerDetailByCodeLangAndParamJson(
                toolName, analyzeConfigInfoVO.getParamJson(), analyzeConfigInfoVO.getLanguage(), checkerDetailEntities);
        if (CollectionUtils.isEmpty(checkerDetailVOList))
        {
            log.info("tool {} checker detail list is empty.", toolName);
            return analyzeConfigInfoVO;
        }

        // 合并任务关联的多个规则集中的规则
        Map<String, CheckerPropVO> mergeTaskCheckers = mergeTaskCheckerSets(toolName, checkerSetVOList, checkerDetailVOList);

        Set<String> skipCheckers = Sets.newHashSet();
        Set<String> covPWCheckers = Sets.newHashSet();
        Set<String> covOptions = Sets.newHashSet();
        List<OpenCheckerVO> openCheckerList = new ArrayList<>();
        Set<String> tosaCheckers = Sets.newHashSet();
        // 遍历工具所有规则，提取出需要屏蔽的规则和打开的规则
        for (CheckerDetailVO checkerDetail : checkerDetailVOList)
        {
            // 如果规则集中配置了该规则，表示规则是打开状态true，否是是关闭状态false
            boolean isOpen = mergeTaskCheckers.containsKey(checkerDetail.getCheckerKey());
            CheckerPropVO checkerProp = mergeTaskCheckers.get(checkerDetail.getCheckerKey());
            String checkerName = getRealChecker(checkerDetail);
            if (ComConstants.CheckerPkgKind.DEFAULT.value().equals(checkerDetail.getPkgKind()))
            {
                if (isOpen)
                {
                    // COVERITY不需要记录打开的默认规则，其他工具记录到打开规则列表
                    if (!ComConstants.Tool.COVERITY.name().equals(toolName))
                    {
                        openCheckerList.add(getOpenChecker(checkerName, checkerProp, checkerDetail, analyzeConfigInfoVO));
                        if (checkerName.endsWith("-tosa"))
                        {
                            tosaCheckers.add(checkerName);
                        }
                    }
                }
                // 默认包中关闭的规则记录到关闭列表
                else
                {
                    skipCheckers.add(checkerName);
                }
            }
            else
            {
                // 不在默认包的默认规则如果被关闭，则要记录到skipCheckers
                if (SWIFT_DEFAULT_CHECKERS.contains(checkerDetail.getCheckerKey()))
                {
                    if (!isOpen)
                    {
                        skipCheckers.add(checkerName);
                    }
                }
                else
                {
                    // 其他打开的非默认规则记录到打开规则列表
                    if (isOpen)
                    {
                        // PW规则不加入到OpenCheckers里
                        if (ComConstants.Tool.COVERITY.name().equals(toolName) && ComConstants.CheckerPkgKind.COMPILE.value().equals(checkerDetail.getPkgKind()))
                        {
                            covPWCheckers.add(checkerName);
                        }
                        else
                        {
                            openCheckerList.add(getOpenChecker(checkerName, checkerProp, checkerDetail, analyzeConfigInfoVO));
                        }
                        if (checkerName.endsWith("-tosa"))
                        {
                            tosaCheckers.add(checkerName);
                        }
                    }
                }
            }

            // 记录Coverity规则子选项
            if (isOpen && ComConstants.Tool.COVERITY.name().equals(toolName)
                    && CheckerConstants.CheckerProperty.ADVANCED.value() == checkerDetail.getCovProperty()
                    && CollectionUtils.isNotEmpty(checkerDetail.getCovSubcategory()))
            {
                for (CovSubcategoryVO covSubcategoryVO : checkerDetail.getCovSubcategory())
                {
                    covOptions.add(covSubcategoryVO.getCheckerName() + ":" + covSubcategoryVO.getCheckerSubcategoryDetail());
                }
            }
        }

        // 如果有tosa规则，则去掉原有规则，并去掉tosa规则的-tosa字段
        if (CollectionUtils.isNotEmpty(openCheckerList))
        {
            Iterator<OpenCheckerVO> it = openCheckerList.iterator();
            while (it.hasNext())
            {
                OpenCheckerVO openChecker = it.next();
                if (openChecker.getCheckerName().contains("-tosa"))
                {
                    openChecker.setCheckerName(openChecker.getCheckerName().replaceAll("-tosa", ""));
                }
                else if (tosaCheckers.contains(openChecker.getCheckerName() + "-tosa"))
                {
                    it.remove();
                }
            }
        }
        if(CollectionUtils.isEmpty(openCheckerList))
        {
            openCheckerList = new ArrayList<>();
        }

        // 转为分号分隔字符串
        analyzeConfigInfoVO.setSkipCheckers(String.join(";", skipCheckers));
        analyzeConfigInfoVO.setOpenCheckers(openCheckerList);
        analyzeConfigInfoVO.setCovOptions(Lists.newArrayList(covOptions));
        analyzeConfigInfoVO.setCovPWCheckers(String.join(";", covPWCheckers));
        log.info("end getTaskCheckerConfig.. cost:{}", System.currentTimeMillis() - beginTime);
        return analyzeConfigInfoVO;
    }

    @Override
    public int getCcnThreshold(ToolConfigInfoVO toolConfigInfoVO) {
        int ccnThreshold = ComConstants.DEFAULT_CCN_THRESHOLD;
        AnalyzeConfigInfoVO analyzeConfigInfoVO = new AnalyzeConfigInfoVO();
        analyzeConfigInfoVO.setTaskId(toolConfigInfoVO.getTaskId());
        analyzeConfigInfoVO.setMultiToolType(ComConstants.Tool.CCN.name());
        // 查询任务规则配置，从配置中获取CCN阀值
        analyzeConfigInfoVO = getTaskCheckerConfig(analyzeConfigInfoVO);
        List<OpenCheckerVO> openCheckers = analyzeConfigInfoVO.getOpenCheckers();
        if (CollectionUtils.isNotEmpty(openCheckers) && CollectionUtils.isNotEmpty(openCheckers.get(0).getCheckerOptions()))
        {
            String ccnThresholdStr = openCheckers.get(0).getCheckerOptions().get(0).getCheckerOptionValue();
            ccnThreshold = org.apache.commons.lang3.StringUtils.isEmpty(ccnThresholdStr)
                    ? ComConstants.DEFAULT_CCN_THRESHOLD : Integer.valueOf(ccnThresholdStr.trim());
        } else {
            // 任务规则配置中获取不到，从个性化参数中获取CCN阀值
            if (StringUtils.isNotEmpty(toolConfigInfoVO.getParamJson())) {
                org.json.JSONObject paramJson = new org.json.JSONObject(toolConfigInfoVO.getParamJson());
                if (paramJson.has(ComConstants.KEY_CCN_THRESHOLD)) {
                    String ccnThresholdStr = paramJson.getString(ComConstants.KEY_CCN_THRESHOLD);
                    ccnThreshold = org.apache.commons.lang3.StringUtils.isEmpty(ccnThresholdStr)
                            ? ComConstants.DEFAULT_CCN_THRESHOLD : Integer.valueOf(ccnThresholdStr.trim());
                }
            }
        }
        return ccnThreshold;
    }

    @NotNull
    protected Map<String, CheckerPropVO> mergeTaskCheckerSets(String toolName,
            List<CheckerSetVO> checkerSetVOList,
            List<CheckerDetailVO> checkerDetailVOList) {
        Map<String, CheckerDetailVO> checkerDetailMap = checkerDetailVOList.stream()
                .collect(Collectors.toMap(CheckerDetailVO::getCheckerKey, Function.identity(), (k, v) -> v));

        /*
         * 将所有规则集中的所有规则去重合并成一个当前任务工具打开的规则列表，取各个规则集中的规则的并集，对于重复并且有规则参数配置的规则，合并优先级如下：
         * 1.默认规则集中的规则的参数配置
         * 2.推荐规则集中的规则的参数配置
         * 3.使用率高的规则集中的规则的参数配置
         */
        // 初步合并，保留重复规则的规则集，为下一步判断规则集的优先级做准备
        Map<String, Map<CheckerSetVO, CheckerPropVO>> preMergeTaskCheckers = new HashMap<>();
        checkerSetVOList.forEach(checkerSetVO ->
        {
            List<CheckerPropVO> checkerSetCheckerPropsList = checkerSetVO.getCheckerProps();
            if (CollectionUtils.isNotEmpty(checkerSetCheckerPropsList))
            {
                checkerSetCheckerPropsList.forEach(checkerPropVO ->
                {
                    if (toolName.equalsIgnoreCase(checkerPropVO.getToolName()))
                    {
                        Map<CheckerSetVO, CheckerPropVO> checkerSetVOCheckerPropVOMap = preMergeTaskCheckers.get(checkerPropVO.getCheckerKey());
                        if (checkerSetVOCheckerPropVOMap == null)
                        {
                            checkerSetVOCheckerPropVOMap = new LinkedHashMap<>();
                            preMergeTaskCheckers.put(checkerPropVO.getCheckerKey(), checkerSetVOCheckerPropVOMap);
                        }
                        CheckerDetailVO checkerDetailVO = checkerDetailMap.get(checkerPropVO.getCheckerKey());
                        if (StringUtils.isEmpty(checkerPropVO.getProps())
                                && checkerDetailVO != null && StringUtils.isNotEmpty(checkerDetailVO.getProps()))
                        {
                            checkerPropVO.setProps(checkerDetailVO.getProps());
                        }
                        checkerSetVOCheckerPropVOMap.put(checkerSetVO, checkerPropVO);
                    }
                });
            }
        });

        // 最终合并， 得到规则列表及其规则参数配置，通过一个Map保存
        Map<String, CheckerPropVO> mergeTaskCheckers = new HashMap<>();
        preMergeTaskCheckers.forEach((checkerKey, checkerSetVOCheckerPropVOMap) ->
        {
            CheckerPropVO chooseCheckerPropVO = null;
            boolean hasRecommended = false;
            for (Map.Entry<CheckerSetVO, CheckerPropVO> preMergeEntry : checkerSetVOCheckerPropVOMap.entrySet())
            {
                CheckerSetVO checkerSetVO = preMergeEntry.getKey();
                CheckerPropVO checkerPropVO = preMergeEntry.getValue();
                if (null != checkerSetVO.getDefaultCheckerSet() && checkerSetVO.getDefaultCheckerSet())
                {
                    chooseCheckerPropVO = checkerPropVO;
                    break;
                }
                else if (!hasRecommended
                        && (CheckerSetSource.DEFAULT.equals(checkerSetVO.getCheckerSetSource())
                        || CheckerSetSource.RECOMMEND.equals(checkerSetVO.getCheckerSetSource())))
                {
                    chooseCheckerPropVO = checkerPropVO;
                    hasRecommended = true;
                }
                else if (chooseCheckerPropVO == null)
                {
                    chooseCheckerPropVO = checkerPropVO;
                }
            }
            mergeTaskCheckers.put(checkerKey, chooseCheckerPropVO);
        });
        return mergeTaskCheckers;
    }

    private String getRealChecker(CheckerDetailVO checkerDetail)
    {
        if (ComConstants.Tool.COVERITY.name().equals(checkerDetail.getToolName()))
        {
            return checkerDetail.getCheckerName();
        }
        else
        {
            return checkerDetail.getCheckerKey();
        }
    }

    private OpenCheckerVO getOpenChecker(String checker, CheckerPropVO checkerProp, CheckerDetailVO checkerDetail, AnalyzeConfigInfoVO analyzeConfigInfoVO)
    {
        OpenCheckerVO openChecker = new OpenCheckerVO();
        openChecker.setCheckerName(checker);
        openChecker.setNativeChecker(Objects.isNull(checkerDetail.getNativeChecker()) ? false : checkerDetail.getNativeChecker());
        openChecker.setSeverity(checkerDetail.getSeverity());
        if (checkerProp != null && StringUtils.isNotBlank(checkerProp.getProps()))
        {
            List<CheckerProps> checkerPropsList = JsonUtil.INSTANCE.to(checkerProp.getProps(), new TypeReference<List<CheckerProps>>()
            {
            });
            List<OpenCheckerVO.CheckerOptions> list = checkerPropsList.stream().map(checkerProps ->
            {
                OpenCheckerVO.CheckerOptions checkerOptions = new OpenCheckerVO.CheckerOptions();
                checkerOptions.setCheckerOptionName(checkerProps.getPropName());
                checkerOptions.setCheckerOptionValue(checkerProps.getPropValue());
                return checkerOptions;
            }).collect(Collectors.toList());
            openChecker.setCheckerOptions(list);
        }

        // 如果是圈复杂度工具，因为他的规则是由后台封装实现的，但对于工具来说，还是根据工具个性化参数来扫描
        if (ComConstants.Tool.CCN.name().equalsIgnoreCase(analyzeConfigInfoVO.getMultiToolType()))
        {
            AnalyzeConfigInfoVO.ToolOptions toolOptions = new AnalyzeConfigInfoVO.ToolOptions();
            analyzeConfigInfoVO.setToolOptions(Lists.newArrayList(toolOptions));
            if (CollectionUtils.isNotEmpty(openChecker.getCheckerOptions()))
            {
                for (OpenCheckerVO.CheckerOptions checkerOptions: openChecker.getCheckerOptions())
                {
                    if (ComConstants.KEY_CCN_THRESHOLD.equals(checkerOptions.getCheckerOptionName()))
                    {
                        toolOptions.setOptionName(checkerOptions.getCheckerOptionName());
                        toolOptions.setOptionValue(checkerOptions.getCheckerOptionValue());
                        break;
                    }
                }
            }
            else
            {
                toolOptions.setOptionName(ComConstants.KEY_CCN_THRESHOLD);
                toolOptions.setOptionValue(String.valueOf(ComConstants.DEFAULT_CCN_THRESHOLD));
            }
        }
        return openChecker;
    }

    private Set<String> getCheckerSetKeys(String checkerSetId, Integer version)
    {
        if (StringUtils.isNotEmpty(checkerSetId) && null != version)
        {
            CheckerSetEntity checkerSetEntity = checkerSetRepository.findFirstByCheckerSetIdAndVersion(checkerSetId, version);
            if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
            {
                return checkerSetEntity.getCheckerProps().stream().map(CheckerPropsEntity::getCheckerKey).collect(Collectors.toSet());
            }
        }
        return new HashSet<>();
    }


    private Set<String> getCheckerSetKeys(String projectId)
    {
        List<CheckerSetEntity> checkerSetEntityList =
                iv3CheckerSetBizService.findAvailableCheckerSetsByProject(projectId,
                        Arrays.asList(true, false),
                        ToolIntegratedStatus.P.value());
        if(CollectionUtils.isNotEmpty(checkerSetEntityList))
        {
            return checkerSetEntityList.stream().filter(checkerSetEntity -> CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())).
                    map(CheckerSetEntity::getCheckerProps).flatMap(Collection::stream).filter(checkerPropsEntity -> null != checkerPropsEntity &&
                    StringUtils.isNotBlank(checkerPropsEntity.getCheckerKey())).
                    map(CheckerPropsEntity::getCheckerKey).distinct().collect(Collectors.toSet());
        }
        else
        {
            return new HashSet<>();
        }

    }

    private Boolean judgeQualifiedChecker(Set<String> checkerLanguage, Set<CheckerCategory> checkerCategory,
                                          Set<String> toolName, Set<String> tags, Set<String> severity, Set<Boolean> editable,
                                          Set<CheckerRecommendType> checkerRecommendType, Set<Boolean> checkerSelected,
                                          Set<String> checkerSelectedKey, CheckerDetailEntity checkerDetailEntity)
    {
        if (CollectionUtils.isNotEmpty(checkerLanguage) && checkerLanguage.stream().noneMatch(languange -> checkerDetailEntity.getCheckerLanguage().contains(languange)))
        {
            return false;
        }
        if (CollectionUtils.isNotEmpty(checkerCategory) && !checkerCategory.contains(CheckerCategory.valueOf(checkerDetailEntity.getCheckerCategory())))
        {
            return false;
        }
        if (CollectionUtils.isNotEmpty(toolName) && !toolName.contains(checkerDetailEntity.getToolName()))
        {
            return false;
        }
        if (CollectionUtils.isNotEmpty(tags) && tags.stream().noneMatch(tag -> checkerDetailEntity.getCheckerTag().contains(tag)))
        {
            return false;
        }
        if (CollectionUtils.isNotEmpty(severity))
        {
            Integer checkerSeverity = checkerDetailEntity.getSeverity();
            if (ComConstants.PROMPT_IN_DB == checkerSeverity)
            {
                checkerSeverity = ComConstants.PROMPT;
            }
            if (!severity.contains(String.valueOf(checkerSeverity)))
            {
                return false;
            }
        }
        if (CollectionUtils.isNotEmpty(editable) && !editable.contains(null == checkerDetailEntity.getEditable() ? false :
                checkerDetailEntity.getEditable()))
        {
            return false;
        }
        if (CollectionUtils.isNotEmpty(checkerRecommendType) && !checkerRecommendType.contains(
                CheckerRecommendType.valueOf(checkerDetailEntity.getCheckerRecommend())))
        {
            return false;
        }
        if (CollectionUtils.isNotEmpty(checkerSelected) && CollectionUtils.isNotEmpty(checkerSelectedKey) &&
                checkerSelected.stream().noneMatch(checkerSelect ->
                        checkerSelect.equals(checkerSelectedKey.contains(checkerDetailEntity.getCheckerKey()))
                ))
        {
            return false;
        }
        return true;
    }


    @Override
    public List<CheckerCommonCountVO> queryCheckerCountList(CheckerListQueryReq checkerListQueryReq)
    {
        List<CheckerDetailEntity> checkerDetailEntityList = checkerDetailDao.findByComplexCheckerCondition(checkerListQueryReq.getKeyWord(), checkerListQueryReq.getCheckerLanguage(),
                checkerListQueryReq.getCheckerCategory(), checkerListQueryReq.getToolName(), checkerListQueryReq.getTag(),
                checkerListQueryReq.getSeverity(), checkerListQueryReq.getEditable(), checkerListQueryReq.getCheckerRecommend(), null, null,
                null, null, null, null, ToolIntegratedStatus.P);
        List<CheckerCommonCountVO> checkerCommonCountVOList = new ArrayList<>();
        //1. 语言数量map
        Map<String, AtomicInteger> langMap = new ConcurrentHashMap<>();
        //2. 规则数量map(数量不变，初始化就把所有规则放上去)
        CheckerCategory[] checkerCategories = CheckerCategory.values();
        Map<String, AtomicInteger> categoryMap = new ConcurrentHashMap<>();
        for (CheckerCategory checkerCategory : checkerCategories)
        {
            categoryMap.put(checkerCategory.name(), new AtomicInteger(0));
        }
        //3. 规则工具数量map
        Map<String, AtomicInteger> toolMap = new ConcurrentHashMap<>();
        //4. 规则标签
        Map<String, AtomicInteger> tagMap = new ConcurrentHashMap<>();
        //5. 规则严重级别数量(数量不变，初始化就把所有规则放上去)
        Map<Integer, AtomicInteger> severityMap = new ConcurrentHashMap<Integer, AtomicInteger>()
        {{
            put(ComConstants.PROMPT_IN_DB, new AtomicInteger(0));
            put(ComConstants.NORMAL, new AtomicInteger(0));
            put(ComConstants.SERIOUS, new AtomicInteger(0));
        }};
        //6. 可修改参数数量
        Map<Boolean, AtomicInteger> editableMap = new ConcurrentHashMap<Boolean, AtomicInteger>()
        {{
            put(Boolean.TRUE, new AtomicInteger(0));
            put(Boolean.FALSE, new AtomicInteger(0));
        }};
        //7. 系统推荐数量
        Map<String, AtomicInteger> recommendMap = new ConcurrentHashMap<>();
        CheckerRecommendType[] checkerRecommendTypes = CheckerRecommendType.values();
        for (CheckerRecommendType checkerRecommendType : checkerRecommendTypes)
        {
            recommendMap.put(checkerRecommendType.name(), new AtomicInteger(0));
        }

        if (CollectionUtils.isNotEmpty(checkerDetailEntityList))
        {
            checkerDetailEntityList.forEach(checkerDetailEntity ->
            {
                //1. 计算语言数量
                if (CollectionUtils.isNotEmpty(checkerDetailEntity.getCheckerLanguage()))
                {
                    checkerDetailEntity.getCheckerLanguage().forEach(checkerLanguage ->
                    {

                        AtomicInteger langCount = langMap.putIfAbsent(checkerLanguage, new AtomicInteger(1));
                        if (null != langCount)
                        {
                            langCount.incrementAndGet();
                        }
                    });
                }

                //2.计算规则类型数量
                categoryMap.computeIfPresent(checkerDetailEntity.getCheckerCategory(), (k, v) ->
                {
                    v.incrementAndGet();
                    return v;
                });

                //3.规则工具数量
                AtomicInteger toolCount = toolMap.putIfAbsent(checkerDetailEntity.getToolName(), new AtomicInteger(1));
                if (null != toolCount)
                {
                    toolCount.incrementAndGet();
                }

                //4.规则标签数量
                if (CollectionUtils.isNotEmpty(checkerDetailEntity.getCheckerTag()))
                {
                    checkerDetailEntity.getCheckerTag().forEach(checkerTag ->
                    {
                        AtomicInteger tagCount = tagMap.putIfAbsent(checkerTag, new AtomicInteger(1));
                        if (null != tagCount)
                        {
                            tagCount.incrementAndGet();
                        }
                    });
                }

                //5.规则严重级别数量
                severityMap.computeIfPresent(checkerDetailEntity.getSeverity(), (k, v) ->
                {
                    v.incrementAndGet();
                    return v;
                });

                //6. 可修改参数数量
                editableMap.computeIfPresent(null == checkerDetailEntity.getEditable() ? false : checkerDetailEntity.getEditable(), (k, v) ->
                {
                    v.incrementAndGet();
                    return v;
                });

                //7. 系统推荐数量
                recommendMap.computeIfPresent(checkerDetailEntity.getCheckerRecommend(), (k, v) ->
                {
                    v.incrementAndGet();
                    return v;
                });
            });
        }

        //按照语言顺序
        String langOrderStr = redisTemplate.opsForValue().get(RedisKeyConstants.KEY_LANG_ORDER);
        List<String> langOrder = Arrays.asList(langOrderStr.split(","));
        List<CheckerCountListVO> checkerLangCountVOList = langMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(entry.getKey(), null, entry.getValue().intValue())
        ).sorted(Comparator.comparingInt(o -> langOrder.indexOf(o.getKey()))).collect(Collectors.toList());
        //按照枚举中的排序
        List<CheckerCategory> categoryOrder = Arrays.asList(CheckerCategory.values());
        List<CheckerCountListVO> checkerCateCountVOList = categoryMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(CheckerCategory.valueOf(entry.getKey()).name(),
                        CheckerCategory.valueOf(entry.getKey()).getName(), entry.getValue().intValue())
        ).sorted(Comparator.comparingInt(o -> categoryOrder.indexOf(CheckerCategory.valueOf(o.getKey())))).collect(Collectors.toList());

        //按照工具的排序
        String toolOrderStr = redisTemplate.opsForValue().get(RedisKeyConstants.KEY_TOOL_ORDER);
        List<String> toolOrder = Arrays.asList(toolOrderStr.split(","));
        List<CheckerCountListVO> checkerToolCountVOList = toolMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(entry.getKey(), null, entry.getValue().intValue())
        ).sorted(Comparator.comparingInt(o -> toolOrder.indexOf(o.getKey()))).collect(Collectors.toList());
        List<CheckerCountListVO> checkerTagCountVOList = tagMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(entry.getKey(), null, entry.getValue().intValue())
        ).collect(Collectors.toList());
        List<CheckerCountListVO> checkerSeverityCountVOList = severityMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(String.valueOf(entry.getKey()), null, entry.getValue().intValue())
        ).collect(Collectors.toList());
        List<CheckerCountListVO> checkerEditableCountVOList = editableMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(String.valueOf(entry.getKey()), null, entry.getValue().intValue())
        ).collect(Collectors.toList());

        //按照枚举中排序
        List<CheckerRecommendType> recommendOrder = Arrays.asList(CheckerRecommendType.values());
        List<CheckerCountListVO> checkerRecommendCountVOList = recommendMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(CheckerRecommendType.valueOf(entry.getKey()).name(), CheckerRecommendType.valueOf(entry.getKey()).getName(),
                        entry.getValue().intValue())
        ).sorted(Comparator.comparingInt(o -> recommendOrder.indexOf(CheckerRecommendType.valueOf(o.getKey())))).collect(Collectors.toList());
        List<CheckerCountListVO> checkerTotalCountVOList = Arrays.asList(new CheckerCountListVO("total", null, checkerDetailEntityList.size()));


        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerLanguage", checkerLangCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerCategory", checkerCateCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("toolName", checkerToolCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("tag", checkerTagCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("severity", checkerSeverityCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("editable", checkerEditableCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerRecommend", checkerRecommendCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("total", checkerTotalCountVOList));

        return checkerCommonCountVOList;
    }

    /**
     * 获取代码语言
     *
     * @return metaVO
     */
    private List<MetadataVO> getCodeLangMetadataVoList() {
        Result<Map<String, List<MetadataVO>>> metaDataResult =
                client.get(UserMetaRestResource.class).metadatas(ComConstants.KEY_CODE_LANG);
        if (metaDataResult.isNotOk() || metaDataResult.getData() == null) {
            log.error("meta data result is empty! meta data type {}", ComConstants.KEY_CODE_LANG);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return metaDataResult.getData().get(ComConstants.KEY_CODE_LANG);
    }

    /**
     * 根据checkerKey和ToolName更新规则详情
     *
     * @param checkerDetailVO
     * @return
     */
    @Override
    public boolean updateCheckerByCheckerKey(CheckerDetailVO checkerDetailVO) {
        // 根据ToolName和CheckerKey获取当前一条规则详情信息
        CheckerDetailEntity checkerDetailEntity = checkerRepository
                .findFirstByToolNameAndCheckerKey(checkerDetailVO.getToolName(), checkerDetailVO.getCheckerKey());
        if (checkerDetailEntity != null) {
            log.info("before: {}", checkerDetailEntity);

            Set<String> checkerLanguages = checkerDetailVO.getCheckerLanguage();
            List<MetadataVO> codeLangMetadataVoList = getCodeLangMetadataVoList();
            // 获取规则所属语言(数字)
            List<Integer> languageList = new ArrayList<>();
            for (String checkerOneLanguage : checkerLanguages) {
                for (MetadataVO metadataVO : codeLangMetadataVoList) {
                    String name = metadataVO.getName();
                    if (checkerOneLanguage.equals(name)) {
                        languageList.add(Integer.parseInt(metadataVO.getKey()));
                    }
                }
            }
            long language = 0;
            for (int i = 0; i < languageList.size(); i++) {
                language = languageList.get(i) + language;
            }
            // 编辑规则对应语言(文字)
            checkerDetailEntity.setCheckerLanguage(checkerLanguages);
            // 编辑规则所属语言(数字)
            checkerDetailEntity.setLanguage(language);
            // 编辑类别
            checkerDetailEntity.setCheckerCategory(checkerDetailVO.getCheckerCategory());
            // 编辑严重级别
            checkerDetailEntity.setSeverity(checkerDetailVO.getSeverity());
            // 编辑标签
            checkerDetailEntity.setCheckerTag(checkerDetailVO.getCheckerTag());
            // 编辑描述
            checkerDetailEntity.setCheckerDesc(checkerDetailVO.getCheckerDesc());
            // 编辑描述详情
            checkerDetailEntity.setCheckerDescModel(checkerDetailVO.getCheckerDescModel());
            // 编辑错误示例
            checkerDetailEntity.setErrExample(checkerDetailVO.getErrExample());
            // 编辑正确示例
            checkerDetailEntity.setRightExample(checkerDetailVO.getRightExample());
            checkerRepository.save(checkerDetailEntity);
            return true;
        } else {
            log.error("req checkerDetailEntity is null ：checker [{}]", checkerDetailVO.getCheckerKey());
        }
        return false;
    }
}
