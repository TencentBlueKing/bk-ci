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

package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.service.CheckerImportService;
import com.tencent.bk.codecc.defect.service.IV3CheckerSetBizService;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.CheckerImportVO;
import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerRecommendType;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetCategory;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.bk.codecc.task.vo.BaseDataVO;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.checkerset.CreateCheckerSetReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CheckerConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.GsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.KEY_CODE_LANG;

/**
 * 规则导入逻辑实现
 *
 * @version V1.0
 * @date 2020/4/10
 */
@Service
@Slf4j
@Validated
public class CheckerImportServiceImpl implements CheckerImportService
{
    @Autowired
    private CheckerRepository checkerRepository;

    @Autowired
    private CheckerSetRepository checkerSetRepository;

    @Autowired
    private IV3CheckerSetBizService checkerSetBizService;

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Autowired
    private Client client;

    @Override
    public Map<String, List<CheckerPropVO>> checkerImport(String userName, String projectId, CheckerImportVO checkerImportVO)
    {
        log.info("do checker import: " + userName + " " + projectId + " " + checkerImportVO);

        // 查询语言参数列表
        CodeCCResult<List<BaseDataVO>> paramsCodeCCResult = client.get(ServiceBaseDataResource.class).getParamsByType(KEY_CODE_LANG);
        if (paramsCodeCCResult.isNotOk() || CollectionUtils.isEmpty(paramsCodeCCResult.getData()))
        {
            log.error("param list is empty! param type: {}", KEY_CODE_LANG);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        List<BaseDataVO> codeLangParams = paramsCodeCCResult.getData();

        // 1.初始化
        validateParam(checkerImportVO, codeLangParams);

        String toolName = checkerImportVO.getToolName();
        List<CheckerDetailEntity> oldCheckerDetailEntityList = checkerRepository.findByToolName(toolName);
        Map<String, CheckerDetailEntity> oldCheckerEntityMap = oldCheckerDetailEntityList.stream()
                .collect(Collectors.toMap(CheckerDetailEntity::getCheckerName, Function.identity()));

        // 2.整理数据
        String toolDisplayName = toolMetaCacheService.getToolDisplayName(toolName);
        Map<String, List<CheckerPropVO>> checkerSetPropsMap = new HashMap<>();
        Map<String, CreateCheckerSetReqVO> createCheckerSetMap = new HashMap<>();
        List<CheckerDetailVO> checkerDetailVOList = checkerImportVO.getCheckerDetailVOList();
        List<CheckerDetailEntity> newCheckerDetailEntityList = checkerDetailVOList.stream().map(checkerDetailVO ->
        {
            // 初始化规则详情对象
            CheckerDetailEntity checkerDetailEntity = getCheckerDetailEntity(userName, toolName, oldCheckerEntityMap, checkerDetailVO);
            checkerDetailEntity.setLanguage(convertLang(checkerDetailVO.getCheckerLanguage(), codeLangParams));

            // 初始化规则集
            initCheckerSet(toolName, toolDisplayName, checkerSetPropsMap, createCheckerSetMap, checkerDetailEntity, codeLangParams);

            return checkerDetailEntity;
        }).collect(Collectors.toList());

        // 如果规则不在本次导入规则列表中，则将规则状态变更为已关闭
        if (oldCheckerEntityMap.size() > 0)
        {
            oldCheckerEntityMap.forEach((checkerName, checkerDetailEntity) ->
            {
                checkerDetailEntity.setStatus(CheckerConstants.CheckerOpenStatus.CLOSE.value());
                checkerDetailEntity.setUpdatedBy(userName);
                checkerDetailEntity.setUpdatedDate(System.currentTimeMillis());
            });
            newCheckerDetailEntityList.addAll(oldCheckerEntityMap.values());
        }

        // 3.规则数据入库
        Map<String, String> codeLangParamsMap = codeLangParams.stream().collect(Collectors.toMap(BaseDataVO::getLangFullKey, BaseDataVO::getParamName));
        newCheckerDetailEntityList.forEach(it -> {
            it.setCheckerLanguage(it.getCheckerLanguage().stream().map(codeLangParamsMap::get).collect(Collectors.toSet()));
        });
        checkerRepository.save(newCheckerDetailEntityList);

        // 4.创建或更新规则集
        Set<String> checkerSetIds = createCheckerSetMap.keySet();
        List<CheckerSetEntity> checkerSetList = checkerSetRepository.findByCheckerSetIdIn(checkerSetIds);
        Map<String, CheckerSetEntity> checkerSetMap = checkerSetList.stream()
                .collect(Collectors.toMap(CheckerSetEntity::getCheckerSetId, Function.identity(), (k, v) -> k));
        checkerSetPropsMap.forEach((checkerSetId, checkerProps) ->
        {
            // 如果规则集没有创建过，先创建规则集
            if (checkerSetMap.get(checkerSetId) == null)
            {
                checkerSetBizService.createCheckerSet(userName, projectId, createCheckerSetMap.get(checkerSetId));
            }

            // 更新规则集中的规则
            checkerSetBizService.updateCheckersOfSet(checkerSetId, projectId, checkerProps);
        });

        return checkerSetPropsMap;
    }

    private void validateParam(CheckerImportVO checkerImportVO, List<BaseDataVO> codeLangParams)
    {
        Map<String, BaseDataVO> langMap = new HashMap<>();
        codeLangParams.forEach(baseDataEntity -> {
            langMap.put(baseDataEntity.getLangFullKey(), baseDataEntity);
        });

        List<String> languages = new ArrayList<>();
        checkerImportVO.getCheckerDetailVOList().forEach(item -> languages.addAll(item.getCheckerLanguage()));

        if (CollectionUtils.isNotEmpty(languages) && !langMap.keySet().containsAll(languages))
        {
            String errMsg = String.format("输入的工具支持语言: %s, 不在取值范围内: %s", languages, langMap.keySet());
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }

        Set<String> checkerCategorySet = Arrays.stream(CheckerCategory.values()).map(CheckerCategory::name).collect(Collectors.toSet());
        Set<String> checkerRecommendSet = Arrays.stream(CheckerRecommendType.values()).map(CheckerRecommendType::name).collect(Collectors.toSet());
        checkerImportVO.getCheckerDetailVOList().forEach(it -> {
            // 检查 checkerCategory
            if (!checkerCategorySet.contains(it.getCheckerCategory())) {
                String errMsg = String.format("输入的规则类型: %s, 不在取值范围内: %s", it.getCheckerCategory(), checkerCategorySet);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
            // 检查 checkerRecommend
            if (!checkerRecommendSet.contains(it.getCheckerRecommend())) {
                String errMsg = String.format("输入的规则推荐类型: %s, 不在取值范围内: %s", it.getCheckerRecommend(), checkerRecommendSet);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
        });
    }

    /**
     * 初始化规则集
     *
     * @param toolName
     * @param toolDisplayName
     * @param checkerSetPropsMap
     * @param createCheckerSetMap
     * @param checkerDetailEntity
     * @param codeLangParams
     */
    protected void initCheckerSet(String toolName, String toolDisplayName, Map<String, List<CheckerPropVO>> checkerSetPropsMap,
                                  Map<String, CreateCheckerSetReqVO> createCheckerSetMap, CheckerDetailEntity checkerDetailEntity, List<BaseDataVO> codeLangParams)
    {
        // 按语言归类规则，每种语言创建一个规则集
        CheckerPropVO checkerPropVO = new CheckerPropVO();
        BeanUtils.copyProperties(checkerDetailEntity, checkerPropVO);
        Set<String> checkerLanguageSet = checkerDetailEntity.getCheckerLanguage();
        checkerLanguageSet.forEach(lang ->
        {
            // 默认规则集ID的命名格式：工具名小写_语言_all_checkers, 比如：occheck_oc_all_checkers
            String checkerSetId = String.format("%s_%s_all_checkers", toolName.toLowerCase(), lang.toLowerCase());
            List<CheckerPropVO> checkerPropVOList = checkerSetPropsMap.get(checkerSetId);
            if (checkerPropVOList == null)
            {
                checkerPropVOList = new ArrayList<>();
                checkerSetPropsMap.put(checkerSetId, checkerPropVOList);

                // 初始化创建规则集请求对象
                String langDisplay = getLangDisplay(lang, codeLangParams);
                String checkerSetName = String.format("%s所有规则(%s)", toolDisplayName, langDisplay);
                CreateCheckerSetReqVO createCheckerSetReqVO = new CreateCheckerSetReqVO();
                createCheckerSetReqVO.setCheckerSetId(checkerSetId);
                createCheckerSetReqVO.setCheckerSetName(checkerSetName);
                createCheckerSetReqVO.setCodeLang(convertLang(Sets.newHashSet(lang), codeLangParams));
                createCheckerSetReqVO.setDescription(String.format("注册工具时系统自动创建的规则集，包含%s语言的所有规则", langDisplay));
                createCheckerSetReqVO.setCatagories(Lists.newArrayList(CheckerSetCategory.DEFECT.name()));
                createCheckerSetMap.put(checkerSetId, createCheckerSetReqVO);
            }
            checkerPropVOList.add(checkerPropVO);
        });
    }

    /**
     * 初始化规则详情对象
     *
     * @param userName
     * @param toolName
     * @param oldCheckerEntityMap
     * @param checkerDetailVO
     * @return
     */
    @NotNull
    protected CheckerDetailEntity getCheckerDetailEntity(String userName, String toolName, Map<String, CheckerDetailEntity> oldCheckerEntityMap,
                                                         CheckerDetailVO checkerDetailVO)
    {
        String checkerName = checkerDetailVO.getCheckerName();
        CheckerDetailEntity checkerDetailEntity = oldCheckerEntityMap.get(checkerName);
        if (checkerDetailEntity == null)
        {
            checkerDetailEntity = new CheckerDetailEntity();
            checkerDetailEntity.setCreatedBy(userName);
            checkerDetailEntity.setCreatedDate(System.currentTimeMillis());
        }
        else
        {
            checkerDetailEntity.setUpdatedBy(userName);
            checkerDetailEntity.setUpdatedDate(System.currentTimeMillis());
            oldCheckerEntityMap.remove(checkerName);
        }

        checkerDetailEntity.setToolName(toolName);
        checkerDetailEntity.setCheckerKey(checkerName);
        checkerDetailEntity.setCheckerName(checkerName);
        checkerDetailEntity.setSeverity(checkerDetailVO.getSeverity());
        checkerDetailEntity.setCheckerCategory(checkerDetailVO.getCheckerCategory());
        checkerDetailEntity.setCheckerDesc(checkerDetailVO.getCheckerDesc());
        checkerDetailEntity.setCheckerDescModel(checkerDetailVO.getCheckerDescModel());
        checkerDetailEntity.setCheckerLanguage(checkerDetailVO.getCheckerLanguage());
        checkerDetailEntity.setErrExample(checkerDetailVO.getErrExample());
        checkerDetailEntity.setRightExample(checkerDetailVO.getRightExample());
        checkerDetailEntity.setCheckerRecommend(checkerDetailVO.getCheckerRecommend());
        checkerDetailEntity.setCheckerTag(checkerDetailVO.getCheckerTag());
        checkerDetailEntity.setProps(CollectionUtils.isEmpty(checkerDetailVO.getCheckerProps()) ? null : GsonUtils.toJson(checkerDetailVO.getCheckerProps()));
        checkerDetailEntity.setStatus(CheckerConstants.CheckerOpenStatus.OPEN.value());
        checkerDetailEntity.setNativeChecker(true);
        checkerDetailEntity.setEditable(!CollectionUtils.isEmpty(checkerDetailVO.getCheckerProps()));
        return checkerDetailEntity;
    }

    private long convertLang(Set<String> supportedLanguages, List<BaseDataVO> codeLangParams)
    {
        Map<String, BaseDataVO> langMap = codeLangParams.stream().collect(Collectors.toMap(BaseDataVO::getLangFullKey, Function.identity()));

        long lang = 0;
        if (CollectionUtils.isNotEmpty(supportedLanguages))
        {
            for (String langStr : supportedLanguages)
            {
                lang += Long.valueOf(langMap.get(langStr).getParamCode());
            }
        }

        return lang;
    }

    private String getLangDisplay(String language, List<BaseDataVO> codeLangParams)
    {
        Map<String, BaseDataVO> langMap = codeLangParams.stream().collect(Collectors.toMap(BaseDataVO::getLangFullKey, Function.identity()));

        BaseDataVO langVO = langMap.get(language);
        if (langVO != null)
        {
            language = langVO.getParamName();
        }
        return language;
    }
}
