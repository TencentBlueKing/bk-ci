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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetProjectRelationshipRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerPropsEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetProjectRelationshipEntity;
import com.tencent.bk.codecc.defect.service.CheckerImportService;
import com.tencent.bk.codecc.defect.service.IV3CheckerSetBizService;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.CheckerImportVO;
import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerRecommendType;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetCategory;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.checkerset.CreateCheckerSetReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CheckerConstants;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.GsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class CheckerImportServiceImpl implements CheckerImportService {
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
    @Autowired
    private CheckerSetProjectRelationshipRepository projectRelationshipRepository;

    @Override
    public Map<String, List<CheckerPropVO>> checkerImport(String userName, String projectId,
                                                          CheckerImportVO checkerImportVO) {
        log.info("do checker import, userName:{}, projectId:{}, request:{}",
                userName, projectId, GsonUtils.toJson(checkerImportVO));

        // 查询语言参数列表
        Result<List<BaseDataVO>> paramsResult =
                client.get(ServiceBaseDataResource.class).getParamsByType(KEY_CODE_LANG);
        if (paramsResult.isNotOk() || CollectionUtils.isEmpty(paramsResult.getData())) {
            log.error("param list is empty! param type: {}", KEY_CODE_LANG);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        List<BaseDataVO> codeLangParams = paramsResult.getData();

        // 1.校验入参
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

        // 本次集成是否有增加新规则
        List<CheckerDetailEntity> newCheckerDetailEntityList = checkerDetailVOList.stream().map(checkerDetailVO -> {
            // 初始化规则详情对象
            CheckerDetailEntity checkerDetailEntity = getCheckerDetailEntity(userName, toolName, oldCheckerEntityMap,
                    checkerDetailVO);
            checkerDetailEntity.setLanguage(convertLang(checkerDetailVO.getCheckerLanguage(), codeLangParams));

            // 初始化规则集
            initCheckerSet(toolName, toolDisplayName, checkerSetPropsMap, createCheckerSetMap, checkerDetailEntity,
                    codeLangParams);
            return checkerDetailEntity;
        }).collect(Collectors.toList());

        /* 如果规则不在本次导入规则列表中，则抛异常，不允许删除规则
         * 对于测试状态的规则，则不需要报错，因为存在一种场景：
         * 同时有多个开发同学在开发，其中有A同学在分支A中增加一条规则1，B同学在分支B开发，他们都把代码合入test分支调试，
         * 这时规则1已经导入到数据库中，并且状态为-1（测试），后来B同学测试验证ok，先合并分支B到master发布到生产，
         * 这时问题就来了，分支B中没有规则1，那么集成到时候就会报错"不允许删除规则"。
         * 为了兼容这种场景，对于测试状态的规则，不被当成删除来报错
         */
        Map<String, CheckerDetailEntity> closeCheckerEntityMap = oldCheckerEntityMap.values().stream()
                .filter(checker -> checker.getCheckerVersion() != ComConstants.ToolIntegratedStatus.T.value())
                .collect(Collectors.toMap(CheckerDetailEntity::getCheckerKey, Function.identity(), (k, v) -> v));
        if (closeCheckerEntityMap.size() > 0) {
            // 如果规则不在本次导入规则列表中，则将规则状态变更为已关闭
            //            closeCheckerEntityMap.forEach((checkerName, checkerDetailEntity) ->
            //            {
            //                checkerDetailEntity.setStatus(CheckerConstants.CheckerOpenStatus.CLOSE.value());
            //                checkerDetailEntity.setUpdatedBy(userName);
            //                checkerDetailEntity.setUpdatedDate(System.currentTimeMillis());
            //            });
            //            newCheckerDetailEntityList.addAll(oldCheckerEntityMap.values());
            String errMsg = String.format("not allow to close checker, please contact the codecc administrator: %s",
                    closeCheckerEntityMap.keySet());
            throw new CodeCCException(errMsg);
        }

        // 3.规则数据入库
        Map<String, String> codeLangParamsMap =
                codeLangParams.stream().collect(Collectors.toMap(BaseDataVO::getLangFullKey, BaseDataVO::getParamName));
        newCheckerDetailEntityList.forEach(it -> {
            Set<String> checkerLanguage =
                    it.getCheckerLanguage().stream().map(codeLangParamsMap::get).collect(Collectors.toSet());
            it.setCheckerLanguage(checkerLanguage);
        });
        checkerRepository.saveAll(newCheckerDetailEntityList);

        // 4.创建或更新全量规则集
        Set<String> checkerSetIds = createCheckerSetMap.keySet();
        List<CheckerSetEntity> checkerSetList = checkerSetRepository.findByCheckerSetIdIn(checkerSetIds);
        Map<String, CheckerSetEntity> checkerSetMap = checkerSetList.stream()
                .collect(Collectors.toMap(CheckerSetEntity::getCheckerSetId, Function.identity(), (k, v) -> k));
        checkerSetPropsMap.forEach((checkerSetId, checkerProps) ->
        {
            // 如果规则集没有创建过，先创建规则集
            if (checkerSetMap.get(checkerSetId) == null) {
                CreateCheckerSetReqVO createCheckerSetReqVO = createCheckerSetMap.get(checkerSetId);
                createCheckerSetReqVO.setVersion(ComConstants.ToolIntegratedStatus.T.value());
                checkerSetBizService.createCheckerSet(userName, projectId, createCheckerSetReqVO);
            }

            // 更新规则集中的规则
            checkerSetBizService.updateCheckersOfSet(checkerSetId, userName, checkerProps,
                    ComConstants.ToolIntegratedStatus.T.value());
        });

        // 5.更新开源规则集
        List<CheckerSetVO> standardCheckerSetVOList = checkerImportVO.getStandardCheckerSetList();
        if (CollectionUtils.isNotEmpty(standardCheckerSetVOList)) {
            standardCheckerSetVOList.forEach(standardCheckerSetVO -> {
                Pair<String, List<CheckerPropVO>> standardCheckerSetPair = updateStandardCheckerSet(userName, projectId,
                        checkerImportVO, codeLangParams, standardCheckerSetVO);
                checkerSetPropsMap.put(standardCheckerSetPair.getKey(), standardCheckerSetPair.getValue());
            });
        }

        return checkerSetPropsMap;
    }

    /**
     * 更新规范规则集
     *
     * @param userName
     * @param projectId
     * @param checkerImportVO
     * @param codeLangParams
     * @param standardCheckerSetVO
     */
    private Pair<String, List<CheckerPropVO>> updateStandardCheckerSet(
            String userName, String projectId, CheckerImportVO checkerImportVO,
            List<BaseDataVO> codeLangParams, CheckerSetVO standardCheckerSetVO) {
        String standardCheckerSetId = standardCheckerSetVO.getCheckerSetId();
        List<CheckerSetEntity> standardCheckerSetList = checkerSetRepository.findByCheckerSetId(standardCheckerSetId);

        // 转换规则集语言
        long newCodeLang = convertLang(Sets.newHashSet(standardCheckerSetVO.getCheckerSetLang()), codeLangParams);

        if (CollectionUtils.isEmpty(standardCheckerSetList)) {
            CreateCheckerSetReqVO createCheckerSetReqVO = new CreateCheckerSetReqVO();
            BeanUtils.copyProperties(standardCheckerSetVO, createCheckerSetReqVO);
            createCheckerSetReqVO.setVersion(ComConstants.ToolIntegratedStatus.T.value());
            createCheckerSetReqVO.setCodeLang(newCodeLang);
            createCheckerSetReqVO.setCatagories(Lists.newArrayList(CheckerSetCategory.FORMAT.name()));
            checkerSetBizService.createCheckerSet(userName, projectId, createCheckerSetReqVO);

            // 更新规则集中的规则
            checkerSetBizService.updateCheckersOfSet(standardCheckerSetId, userName,
                    standardCheckerSetVO.getCheckerProps(), ComConstants.ToolIntegratedStatus.T.value());
        } else {
            CheckerSetEntity latestCheckerSet = standardCheckerSetList.stream()
                    .max(Comparator.comparing(CheckerSetEntity::getVersion)).get();
            CheckerSetEntity testCheckerSet = standardCheckerSetList.stream()
                    .filter(it -> ComConstants.ToolIntegratedStatus.T.value() == it.getVersion())
                    .findFirst().orElse(null);
            if (testCheckerSet == null) {
                testCheckerSet = latestCheckerSet;
                testCheckerSet.setEntityId(null);
                testCheckerSet.setVersion(ComConstants.ToolIntegratedStatus.T.value());
            }

            // 校验规则集语言是否改变（规则集语言不允许改变）
            if (newCodeLang != latestCheckerSet.getCodeLang()) {
                log.error("can not change standardCheckerSet Lang! new id:{}, old id:{}", newCodeLang,
                        latestCheckerSet.getCodeLang(), checkerImportVO.getToolName());
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                        new String[]{"standardCheckerSetLang"}, null);
            }
            // 判断规则集是否有变化，包括基本信息(checker_set_name、description)，如果有变化则更新规则集的基本信息
            String newCheckerSetName = standardCheckerSetVO.getCheckerSetName();
            String newDescription = standardCheckerSetVO.getDescription();
            if (newCheckerSetName != null && !newCheckerSetName.equals(latestCheckerSet.getCheckerSetName())) {
                testCheckerSet.setCheckerSetName(newCheckerSetName);
            }
            if (newDescription != null && !newDescription.equals(latestCheckerSet.getDescription())) {
                testCheckerSet.setDescription(newDescription);
            }

            List<CheckerPropsEntity> checkerPropsEntities = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(standardCheckerSetVO.getCheckerProps())) {
                for (CheckerPropVO checkerPropVO : standardCheckerSetVO.getCheckerProps()) {
                    CheckerPropsEntity checkerPropsEntity = new CheckerPropsEntity();
                    BeanUtils.copyProperties(checkerPropVO, checkerPropsEntity);
                    checkerPropsEntities.add(checkerPropsEntity);
                }
            }
            List<CheckerPropsEntity> oldCheckerProps = testCheckerSet.getCheckerProps();
            testCheckerSet.setCheckerProps(checkerPropsEntities);
            testCheckerSet.setLastUpdateTime(System.currentTimeMillis());
            testCheckerSet.setUpdatedBy(userName);
            checkerSetRepository.save(testCheckerSet);

            // 查询已关联此规则集，且选择了latest版本自动更新的项目数据
            List<CheckerSetProjectRelationshipEntity> projectRelationships =
                    projectRelationshipRepository.findByCheckerSetIdAndUselatestVersion(standardCheckerSetId, true);
            if (CollectionUtils.isNotEmpty(projectRelationships)) {
                // 如果是测试或灰度规则集，且项目是测试，则设置测试或灰度的项目为强制全量，且更新工具
                CheckerSetEntity fromCheckerSet = new CheckerSetEntity();
                fromCheckerSet.setCheckerProps(oldCheckerProps);
                checkerSetBizService.updateTaskAfterChangeCheckerSet(testCheckerSet, fromCheckerSet,
                        projectRelationships, userName);
            }
        }
        return ImmutablePair.of(standardCheckerSetId, standardCheckerSetVO.getCheckerProps());
    }

    /**
     * 校验入参
     *
     * @param checkerImportVO
     * @param codeLangParams
     */
    private void validateParam(CheckerImportVO checkerImportVO, List<BaseDataVO> codeLangParams) {
        Map<String, BaseDataVO> langMap = new HashMap<>();
        codeLangParams.forEach(baseDataEntity -> {
            langMap.put(baseDataEntity.getLangFullKey(), baseDataEntity);
        });

        List<String> languages = new ArrayList<>();
        checkerImportVO.getCheckerDetailVOList().forEach(item -> languages.addAll(item.getCheckerLanguage()));

        if (CollectionUtils.isNotEmpty(languages) && !langMap.keySet().containsAll(languages)) {
            String errMsg = String.format("输入的工具支持语言: %s, 不在取值范围内: %s", languages, langMap.keySet());
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }

        Set<String> checkerCategorySet =
                Arrays.stream(CheckerCategory.values()).map(CheckerCategory::name).collect(Collectors.toSet());
        Set<String> checkerRecommendSet = Arrays.stream(CheckerRecommendType.values())
                .map(CheckerRecommendType::name).collect(Collectors.toSet());
        checkerImportVO.getCheckerDetailVOList().forEach(it -> {
            // 检查 checkerCategory
            if (!checkerCategorySet.contains(it.getCheckerCategory())) {
                String errMsg = String.format("输入的规则类型: %s, 不在取值范围内: %s",
                        it.getCheckerCategory(), checkerCategorySet);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
            // 检查 checkerRecommend
            if (!checkerRecommendSet.contains(it.getCheckerRecommend())) {
                String errMsg = String.format("输入的规则推荐类型: %s, 不在取值范围内: %s", it.getCheckerRecommend(),
                        checkerRecommendSet);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
        });

        // 检验开源规则集
        List<CheckerSetVO> standardCheckerSetList = checkerImportVO.getStandardCheckerSetList();
        if (CollectionUtils.isNotEmpty(standardCheckerSetList)) {
            // 查询工具对应的规范规则集ID的配置
            Result<List<BaseDataVO>> result =
                    client.get(ServiceBaseDataResource.class).getInfoByTypeAndCode(ComConstants.STANDARD_CHECKER_SET_ID,
                            checkerImportVO.getToolName());
            if (result.isNotOk() || CollectionUtils.isEmpty(result.getData())
                    || StringUtils.isEmpty(result.getData().get(0).getParamValue())) {
                log.error("param list is empty! paramType: {}, paramCode: {}", ComConstants.STANDARD_CHECKER_SET_ID,
                        checkerImportVO.getToolName());
                throw new CodeCCException(CommonMessageCode.JSON_PARAM_IS_INVALID, String.format(
                        "后台未给工具[%s]配置工具对应的 '规范规则集'，请联系CodeCC配置", checkerImportVO.getToolName()));
            }

            String standardCheckerSetIds = result.getData().get(0).getParamValue();
            List<String> standardCheckerSetIdList =
                    Lists.newArrayList(standardCheckerSetIds.split(ComConstants.STRING_SPLIT));

            Set<String> checkers = checkerImportVO.getCheckerDetailVOList().stream()
                    .map(CheckerDetailVO::getCheckerName).collect(Collectors.toSet());

            Map<String, List<String>> invalidCheckerMap = Maps.newTreeMap();
            checkerImportVO.getStandardCheckerSetList().forEach(standardCheckerSetVO -> {
                String standardCheckerSetId = standardCheckerSetVO.getCheckerSetId();

                // 校验规则集ID是否改变（ID不允许改变）
                if (!standardCheckerSetIdList.contains(standardCheckerSetId)) {
                    log.error("can not change standard checkerSetId! new id:{}, old ids:{}", standardCheckerSetId,
                            standardCheckerSetIdList, checkerImportVO.getToolName());
                    throw new CodeCCException(CommonMessageCode.JSON_PARAM_IS_INVALID, String.format(
                            "checker_set.json中配置的规则集Id与工具[%s]对应的 '规范规则集Id'不匹配，请联系CodeCC确认正确配置",
                            checkerImportVO.getToolName()));
                }

                // 校验是否开源规则集里面的规则是否属于工具的规则
                List<String> invalidCheckers = Lists.newArrayList();
                standardCheckerSetVO.getCheckerProps().forEach(checkerPropVO -> {
                    if (!checkers.contains(checkerPropVO.getCheckerKey())) {
                        invalidCheckers.add(checkerPropVO.getCheckerKey());
                    }
                });
                if (CollectionUtils.isNotEmpty(invalidCheckers)) {
                    invalidCheckerMap.put(standardCheckerSetId, invalidCheckers);
                }
            });
            if (invalidCheckerMap.size() > 0) {
                StringBuffer errMsg = new StringBuffer();
                invalidCheckerMap.forEach((standardCheckerSetId, invalidCheckers) -> errMsg.append(
                        String.format("规则集%s中的规则[%s]不是工具的合法规则; ", standardCheckerSetId, invalidCheckers)));
                log.error(errMsg.toString());
                throw new CodeCCException(CommonMessageCode.JSON_PARAM_IS_INVALID, errMsg.toString());
            }
        }
    }

    /**
     * 初始化规则集，每种语言创建一个规则集
     *
     * @param toolName
     * @param toolDisplayName
     * @param checkerSetPropsMap
     * @param createCheckerSetMap
     * @param checkerDetailEntity
     * @param codeLangParams
     */
    protected void initCheckerSet(String toolName, String toolDisplayName,
                                  Map<String, List<CheckerPropVO>> checkerSetPropsMap,
                                  Map<String, CreateCheckerSetReqVO> createCheckerSetMap,
                                  CheckerDetailEntity checkerDetailEntity, List<BaseDataVO> codeLangParams) {
        // 按语言归类规则，每种语言创建一个规则集
        CheckerPropVO checkerPropVO = new CheckerPropVO();
        BeanUtils.copyProperties(checkerDetailEntity, checkerPropVO);
        Set<String> checkerLanguageSet = checkerDetailEntity.getCheckerLanguage();
        checkerLanguageSet.forEach(lang ->
        {
            // 默认规则集ID的命名格式：工具名小写_语言_all_checkers, 比如：occheck_oc_all_checkers
            String checkerSetId = String.format("%s_%s_all_checkers", toolName.toLowerCase(), lang.toLowerCase());
            List<CheckerPropVO> checkerPropVOList = checkerSetPropsMap.get(checkerSetId);
            if (checkerPropVOList == null) {
                checkerPropVOList = new ArrayList<>();
                checkerSetPropsMap.put(checkerSetId, checkerPropVOList);

                // 初始化创建规则集请求对象
                String langDisplay = getLangDisplay(lang, codeLangParams);
                String checkerSetName = String.format("%s所有规则(%s)", toolDisplayName, langDisplay);
                CreateCheckerSetReqVO createCheckerSetReqVO = new CreateCheckerSetReqVO();
                createCheckerSetReqVO.setCheckerSetId(checkerSetId);
                createCheckerSetReqVO.setCheckerSetName(checkerSetName);
                createCheckerSetReqVO.setCodeLang(convertLang(Sets.newHashSet(lang), codeLangParams));
                createCheckerSetReqVO.setDescription(String.format("注册工具时系统自动创建的规则集，包含%s语言的所有规则",
                        langDisplay));
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
    protected CheckerDetailEntity getCheckerDetailEntity(String userName, String toolName, Map<String,
            CheckerDetailEntity> oldCheckerEntityMap, CheckerDetailVO checkerDetailVO) {
        String checkerName = checkerDetailVO.getCheckerName();
        CheckerDetailEntity checkerDetailEntity = oldCheckerEntityMap.get(checkerName);
        if (checkerDetailEntity == null) {
            checkerDetailEntity = new CheckerDetailEntity();
            checkerDetailEntity.setCreatedBy(userName);
            checkerDetailEntity.setCreatedDate(System.currentTimeMillis());
            checkerDetailEntity.setCheckerVersion(ComConstants.ToolIntegratedStatus.T.value());
            checkerDetailEntity.setStatus(CheckerConstants.CheckerOpenStatus.OPEN.value());
        } else {
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
        checkerDetailEntity.setProps(CollectionUtils.isEmpty(checkerDetailVO.getCheckerProps()) ? null :
                GsonUtils.toJson(checkerDetailVO.getCheckerProps()));
        checkerDetailEntity.setNativeChecker(true);

        if (checkerDetailVO.getEditable() != null) {
            checkerDetailEntity.setEditable(checkerDetailVO.getEditable());
        } else {
            checkerDetailEntity.setEditable(!CollectionUtils.isEmpty(checkerDetailVO.getCheckerProps()));
        }
        return checkerDetailEntity;
    }

    private long convertLang(Set<String> supportedLanguages, List<BaseDataVO> codeLangParams) {
        Map<String, BaseDataVO> langMap = codeLangParams.stream().collect(Collectors.toMap(BaseDataVO::getLangFullKey,
                Function.identity()));

        long lang = 0;
        if (CollectionUtils.isNotEmpty(supportedLanguages)) {
            for (String langStr : supportedLanguages) {
                lang += Long.valueOf(langMap.get(langStr).getParamCode());
            }
        }

        return lang;
    }

    private String getLangDisplay(String language, List<BaseDataVO> codeLangParams) {
        Map<String, BaseDataVO> langMap = codeLangParams.stream().collect(Collectors.toMap(BaseDataVO::getLangFullKey,
                Function.identity()));

        BaseDataVO langVO = langMap.get(language);
        if (langVO != null) {
            language = langVO.getParamName();
        }
        return language;
    }
}
