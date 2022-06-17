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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerPackageRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.mongorepository.RedLineMetaRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.RedLineRepository;
import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.CheckerPackageEntity;
import com.tencent.bk.codecc.defect.model.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.model.DUPCStatisticEntity;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.RedLineMetaEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.RedLineEntity;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.FileDefectGatherService;
import com.tencent.bk.codecc.defect.service.IConfigCheckerPkgBizService;
import com.tencent.bk.codecc.defect.service.RedLineReportService;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.CheckerPkgRspVO;
import com.tencent.bk.codecc.defect.vo.FileDefectGatherVO;
import com.tencent.bk.codecc.defect.vo.redline.PipelineRedLineCallbackVO;
import com.tencent.bk.codecc.defect.vo.redline.RLCcnAndDupcDefectVO;
import com.tencent.bk.codecc.defect.vo.redline.RLCompileDefectVO;
import com.tencent.bk.codecc.defect.vo.redline.RLLintDefectVO;
import com.tencent.bk.codecc.defect.vo.redline.RedLineVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.bk.codecc.defect.constant.DefectConstants.FORBIDDEN_COUNT;
import static com.tencent.bk.codecc.defect.constant.DefectConstants.FORBIDDEN_COUNT_F;
import static com.tencent.bk.codecc.defect.constant.DefectConstants.LINUX_CODECC_SCRIPT;
import static com.tencent.bk.codecc.defect.constant.DefectConstants.LINUX_PAAS_CODECC_SCRIPT;
import static com.tencent.bk.codecc.defect.constant.DefectConstants.PASS_COUNT;

/**
 * 上报质量红线服务实现
 *
 * @version V1.0
 * @date 2019/7/4
 */
@Slf4j
@Service
public class RedLineReportServiceImpl implements RedLineReportService
{
    /**
     * 质量红线当前支持单独上报数量的规则
     */
    private static final Map<String, List<String>> RED_LINE_CHECKERS = ImmutableMap.of(
            "OCCHECK", Lists.newArrayList("MaxLinesPerFunction"),
            "CHECKSTYLE", Lists.newArrayList("MethodLength"),
            "GOML", Lists.newArrayList("golint/fnsize"));

    /**
     * 开源规范检查工具
     */
    private static final Set<String> TOSA_STANDARD_TOOLS = Sets.newHashSet(ComConstants.Tool.CPPLINT.name(), ComConstants.Tool.ESLINT.name(),
            ComConstants.Tool.STYLECOP.name(), ComConstants.Tool.CHECKSTYLE.name(), ComConstants.Tool.PYLINT.name(),
            ComConstants.Tool.GOML.name(), ComConstants.Tool.DETEKT.name(), ComConstants.Tool.OCCHECK.name());

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Autowired
    private DUPCDefectRepository dupcDefectRepository;

    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;

    @Autowired
    private RedLineMetaRepository redLineMetaRepository;

    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;

    @Autowired
    private DUPCStatisticRepository dupcStatisticRepository;

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Autowired
    private CheckerRepository checkerRepository;

    @Autowired
    private NewDefectJudgeService newDefectJudgeService;

    @Autowired
    private DefectRepository defectRepository;

    @Autowired
    private IConfigCheckerPkgBizService configCheckerPkgBizService;

    @Autowired
    private CheckerService checkerService;

    @Autowired
    private CheckerPackageRepository checkerPackageRepository;

    @Autowired
    private BuildDefectRepository buildDefectRepository;

    @Autowired
    private RedLineRepository redLineRepository;
    @Autowired
    private FileDefectGatherService fileDefectGatherService;

    /**
     * 查询质量红线指标数据
     *
     * @param taskDetailVO
     * @param effectiveTools
     * @return
     */
    private PipelineRedLineCallbackVO getRedLineIndicators(TaskDetailVO taskDetailVO, List<String> effectiveTools, String toolName)
    {
        // 拼装请求数据
        PipelineRedLineCallbackVO metadataCallback = new PipelineRedLineCallbackVO();
        String elementType = ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equals(taskDetailVO.getCreateFrom()) ? LINUX_PAAS_CODECC_SCRIPT : LINUX_CODECC_SCRIPT;
        metadataCallback.setElementType(elementType);
        metadataCallback.setData(Lists.newArrayList());

        // 如果当前任务工具存在收敛告警文件，那么相应质量红线指标为null
        FileDefectGatherVO fileDefectGatherVO = fileDefectGatherService.getFileDefectGather(taskDetailVO.getTaskId(), toolName, null);
        // 查询元数据模板
        Map<String, RedLineVO> metadataModel = loadRedLineMetaData(toolName);
        if (MapUtils.isEmpty(metadataModel) || fileDefectGatherVO != null)
        {
            return metadataCallback;
        }

        // 获取所有工具配置信息
        Map<String, ToolConfigInfoVO> toolConfigBaseMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(taskDetailVO.getToolConfigInfoList()))
        {
            for (ToolConfigInfoVO toolConfigInfoVO : taskDetailVO.getToolConfigInfoList())
            {
                toolConfigBaseMap.put(toolConfigInfoVO.getToolName(), toolConfigInfoVO);
            }
        }

        // 查询元数据
        ToolMetaBaseVO toolMeta = toolMetaCacheService.getToolBaseMetaCache(toolName);
        ToolConfigInfoVO toolConfig = toolConfigBaseMap.get(toolName);
        log.info("[ red line ] current reporting tool: {}", toolMeta);
        if (Objects.nonNull(toolMeta))
        {
            String pattern = toolMeta.getPattern();
            if (ComConstants.ToolPattern.LINT.name().equals(pattern))
            {
                getLintAnalysisResult(taskDetailVO, toolMeta, toolConfig, metadataModel, metadataCallback, effectiveTools);
            }
            else if (ComConstants.ToolPattern.COVERITY.name().equals(pattern))
            {
                getCoverityAnalysisResult(taskDetailVO, toolMeta, toolConfig, metadataModel, metadataCallback, effectiveTools);
            }
            else if (ComConstants.ToolPattern.KLOCWORK.name().equals(pattern))
            {
                getKwAnalysisResult(taskDetailVO, toolMeta, metadataModel, metadataCallback, effectiveTools);
            }
            else if (ComConstants.ToolPattern.CCN.name().equals(pattern)
                    || ComConstants.ToolPattern.DUPC.name().equals(pattern))
            {
                getCcnAndDupcResult(taskDetailVO, toolMeta, metadataModel, metadataCallback, effectiveTools, toolConfig);
            }
        }

        return metadataCallback;
    }


    /**
     * 查询Lint类工具分析结果
     *  @param taskDetailVO         任务信息
     * @param toolInfo         工具的基本信息[t_tool_meta]
     * @param metadataModel    元数据
     * @param metadataCallback 发送到蓝盾的元数据
     * @param effectiveTools   有效的工具
     */
    private void getLintAnalysisResult(TaskDetailVO taskDetailVO, ToolMetaBaseVO toolInfo, ToolConfigInfoVO toolConfig, Map<String, RedLineVO> metadataModel,
                                       PipelineRedLineCallbackVO metadataCallback, List<String> effectiveTools)
    {
        log.info("start to get lint analysis result for task: {}", taskDetailVO.getTaskId());

        long taskId = taskDetailVO.getTaskId();
        String toolName = toolInfo.getName();
        RLLintDefectVO lintRLModel = new RLLintDefectVO();
        lintRLModel.setHistoryCheckerPkgCounts(Maps.newHashMap());
        lintRLModel.setNewCheckerPkgCounts(Maps.newHashMap());
        lintRLModel.setNewCheckerCounts(Maps.newHashMap());
        lintRLModel.setHistoryCheckerCounts(Maps.newHashMap());

        // 查询所有规则详情
        Map<String, CheckerDetailVO> allCheckerMap = checkerService.queryAllChecker(toolName);

        // 初始化规则告警数和规则包告警数为0
        List<CheckerDetailEntity> checkerDetailList = checkerRepository.findByToolName(toolName);
        initRLLintChecker(toolName, lintRLModel, PASS_COUNT);
        initRLLintPkg(checkerDetailList, lintRLModel, PASS_COUNT);

        // 项目配置的编程语言是否与该工具支持的语言相符
        boolean toolMatchProjLang = (toolInfo.getLang() & taskDetailVO.getCodeLang()) != 0;

        // 工具与语言符合的，才需要计算告警数量，语言不符合就通过
        if (toolMatchProjLang)
        {
            // 语言符合时，如果未接入工具，数值全部为-1
            if (!effectiveTools.contains(toolName))
            {
                lintRLModel.setHistoryNormal(FORBIDDEN_COUNT);
                lintRLModel.setHistoryPrompt(FORBIDDEN_COUNT);
                lintRLModel.setHistorySerious(FORBIDDEN_COUNT);
                lintRLModel.setNewNormal(FORBIDDEN_COUNT);
                lintRLModel.setNewPrompt(FORBIDDEN_COUNT);
                lintRLModel.setNewSerious(FORBIDDEN_COUNT);
                initRLLintPkg(checkerDetailList, lintRLModel, FORBIDDEN_COUNT);
                initRLLintChecker(toolName, lintRLModel, FORBIDDEN_COUNT);
            }
            else
            {
                Set<String> tosaCheckers = Sets.newHashSet();
                boolean isTosaPkgOpened = false;

                // 查询规则包和规则打开情况
                List<CheckerPkgRspVO> checkerPkgList = configCheckerPkgBizService.getConfigCheckerPkg(taskId, toolName, toolInfo.getLang(), toolConfig).getCheckerPackages();

                Map<String, CheckerPkgRspVO> pkgMap = Maps.newHashMap();
                if (CollectionUtils.isNotEmpty(checkerPkgList))
                {
                    log.info("get lint analysis result for checkerPkgList: {}", checkerPkgList.size());

                    for (CheckerPkgRspVO checkerPkgRsp : checkerPkgList)
                    {
                        pkgMap.put(checkerPkgRsp.getPkgId(), checkerPkgRsp);
                    }
                }

                // 初始化规则包告警数
                if (MapUtils.isNotEmpty(pkgMap))
                {
                    log.info("get lint analysis result for pkgMap: {}", pkgMap.size());

                    // 初始化规则包告警数量，设置规则未全部打开的规则包告警数量为-1，全打开的设置为0
                    for (Map.Entry<String, CheckerPkgRspVO> checkerPkgEntry : pkgMap.entrySet())
                    {
                        if (isAllCheckerOpened(checkerPkgEntry.getValue()))
                        {
                            lintRLModel.getNewCheckerPkgCounts().put(checkerPkgEntry.getKey(), 0L);
                            lintRLModel.getHistoryCheckerPkgCounts().put(checkerPkgEntry.getKey(), 0L);

                            // 开源规范包的规则是否全部开启
                            if (ComConstants.CheckerPkgKind.TOSA.value().equals(checkerPkgEntry.getKey()))
                            {
                                isTosaPkgOpened = true;
                            }
                        }
                        else
                        {
                            lintRLModel.getNewCheckerPkgCounts().put(checkerPkgEntry.getKey(), FORBIDDEN_COUNT);
                            lintRLModel.getHistoryCheckerPkgCounts().put(checkerPkgEntry.getKey(), FORBIDDEN_COUNT);
                        }
                    }

                    // 获取开源规范包的规则
                    if (pkgMap.containsKey(ComConstants.CheckerPkgKind.TOSA.value()))
                    {
                        List<CheckerDetailVO> tosaCheckerModels = pkgMap.get(ComConstants.CheckerPkgKind.TOSA.value()).getCheckerList();
                        if (CollectionUtils.isNotEmpty(tosaCheckerModels))
                        {
                            for (CheckerDetailVO checkerDOModel : tosaCheckerModels)
                            {
                                String checkerKey = checkerDOModel.getCheckerKey();
                                if (checkerKey.endsWith("-tosa"))
                                {
                                    tosaCheckers.add(checkerKey.substring(0, checkerKey.indexOf("-tosa")));
                                }
                                else
                                {
                                    tosaCheckers.add(checkerKey);
                                }
                            }
                        }
                    }
                }

                List<LintDefectV2Entity> defectV2EntityList = lintDefectV2Repository.findFiledsByTaskIdAndToolNameAndStatus(taskId, toolName, ComConstants.DefectStatus.NEW.value());

                if (CollectionUtils.isNotEmpty(defectV2EntityList))
                {
                    log.info("get lint analysis result for defectV2EntityList: {}", defectV2EntityList.size());

                    // 查询新老告警判定时间
                    long newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, toolName, taskDetailVO);

                    // 查询接入前和接入后告警详情
                    for (LintDefectV2Entity defect : defectV2EntityList)
                    {
                        // 按新老告警判定时间获取新老告警列表
                        long defectLastUpdateTime = DateTimeUtils.getThirteenTimestamp(defect.getLineUpdateTime());

                        // 统计接入后告警数量
                        if (defectLastUpdateTime >= newDefectJudgeTime)
                        {
                            // 统计接入后各严重级别告警数
                            updateLintSeverityCount(defect.getSeverity(), ComConstants.FileType.NEW, lintRLModel);

                            // 统计各规则包告警数，工具与项目语言不符合的不做统计
                            updateLintCheckerPkgCount(lintRLModel.getNewCheckerPkgCounts(), defect.getChecker(), allCheckerMap, tosaCheckers, isTosaPkgOpened);

                            // 统计接入后各规则告警数量
                            updateLintCheckerCount(lintRLModel.getNewCheckerCounts(), defect.getChecker(), toolName);
                        }
                        // 统计接入前告警数量
                        else
                        {
                            // 统计接入前各严重级别告警数
                            updateLintSeverityCount(defect.getSeverity(), ComConstants.FileType.HISTORY, lintRLModel);

                            /**
                             * 统计各规则包告警数，工具与项目语言不符合的不做统计
                             * 由于目前腾讯开源包告警数量是统计接入前和接入后之和，所以此处把接入前的数据累加到接入后的数据中
                             */
                            updateLintCheckerPkgCount(lintRLModel.getNewCheckerPkgCounts(), defect.getChecker(), allCheckerMap, tosaCheckers, isTosaPkgOpened);

                            // 统计接入前各规则告警数量
                            updateLintCheckerCount(lintRLModel.getHistoryCheckerCounts(), defect.getChecker(), toolName);
                        }
                    }
                }
            }
        }

        // 更新元数据
        updateValue(toolName + "_NEW_SERIOUS", String.valueOf(lintRLModel.getNewSerious()), metadataModel, metadataCallback);
        updateValue(toolName + "_NEW_NORMAL", String.valueOf(lintRLModel.getNewNormal()), metadataModel, metadataCallback);
        updateValue(toolName + "_NEW_PROMPT", String.valueOf(lintRLModel.getNewPrompt()), metadataModel, metadataCallback);
        updateValue(toolName + "_HISTORY_SERIOUS", String.valueOf(lintRLModel.getHistorySerious()), metadataModel, metadataCallback);
        updateValue(toolName + "_HISTORY_NORMAL", String.valueOf(lintRLModel.getHistoryNormal()), metadataModel, metadataCallback);
        updateValue(toolName + "_HISTORY_PROMPT", String.valueOf(lintRLModel.getHistoryPrompt()), metadataModel, metadataCallback);

        // 规范类工具都需要上报开源规则包告警数
        if (TOSA_STANDARD_TOOLS.contains(toolName))
        {
            updateValue(toolName + "_NEW_TOSA",
                    String.valueOf(lintRLModel.getNewCheckerPkgCounts().get(ComConstants.CheckerPkgKind.TOSA.value())),
                    metadataModel, metadataCallback);
        }

        // 更新规则告警数
        for (Map.Entry<String, Long> entry : lintRLModel.getNewCheckerCounts().entrySet())
        {
            String checkerNameUpper = allCheckerMap.get(entry.getKey()).getCheckerName().toUpperCase();
            String metadataKey = toolName + ComConstants.KEY_UNDERLINE + checkerNameUpper + "_NEW";
            updateValue(metadataKey, String.valueOf(entry.getValue()), metadataModel,
                    metadataCallback);
        }
        for (Map.Entry<String, Long> entry : lintRLModel.getHistoryCheckerCounts().entrySet())
        {
            String checkerName = allCheckerMap.get(entry.getKey()).getCheckerName();
            String metadataKey = toolName + ComConstants.KEY_UNDERLINE + checkerName.toUpperCase() + "_HISTORY";
            updateValue(metadataKey, String.valueOf(entry.getValue()), metadataModel,
                    metadataCallback);
        }

        log.info("finish to get lint analysis result for task: {}", taskDetailVO.getTaskId());
    }
    /**
     * 查询Coverity分析结果
     *
     * @param taskInfo
     * @param toolMeta
     * @param toolConfig
     * @param metadataModel
     * @param metadataCallback
     * @param effectiveTools
     */
    public void getCoverityAnalysisResult(TaskBaseVO taskInfo, ToolMetaBaseVO toolMeta, ToolConfigInfoVO toolConfig,
            Map<String, RedLineVO> metadataModel, PipelineRedLineCallbackVO metadataCallback, List<String> effectiveTools)
    {
        long taskId = taskInfo.getTaskId();
        String toolName = ComConstants.Tool.COVERITY.name();
        RLCompileDefectVO coverityDefect = new RLCompileDefectVO();
        coverityDefect.setCheckerPkgCounts(Maps.newHashMap());

        // 查询规则包
        List<CheckerPackageEntity> allCheckerPkgList = checkerPackageRepository.findByToolName(toolName);
        long taskCodeLang = taskInfo.getCodeLang();


        // 项目配置的编程语言是否与该工具支持的语言相符
        boolean toolMatchProjLang = (toolMeta.getLang() & taskCodeLang) != 0;

        // 工具与语言不符合的，数值全部为0，不做拦截
        if (!toolMatchProjLang)
        {
            if (CollectionUtils.isNotEmpty(allCheckerPkgList))
            {
                for (CheckerPackageEntity checkerPkgRsp : allCheckerPkgList)
                {
                    coverityDefect.getCheckerPkgCounts().put(checkerPkgRsp.getPkgId(), PASS_COUNT);
                }
            }
        }
        else
        {
            // 语言符合但未接入工具时，数值全部为-1
            if (!effectiveTools.contains(toolName))
            {
                coverityDefect.setRemainPrompt(FORBIDDEN_COUNT);
                coverityDefect.setRemainNormal(FORBIDDEN_COUNT);
                coverityDefect.setRemainSerious(FORBIDDEN_COUNT);
                if (CollectionUtils.isNotEmpty(allCheckerPkgList))
                {
                    for (CheckerPackageEntity checkerPkgRsp : allCheckerPkgList)
                    {
                        coverityDefect.getCheckerPkgCounts().put(checkerPkgRsp.getPkgId(), FORBIDDEN_COUNT);
                    }
                }
            }
            else
            {
                // 先初始化全部规则包
                if (CollectionUtils.isNotEmpty(allCheckerPkgList))
                {
                    for (CheckerPackageEntity checkerPkgRsp : allCheckerPkgList)
                    {
                        coverityDefect.getCheckerPkgCounts().put(checkerPkgRsp.getPkgId(), 0L);
                    }
                }

                // 查询任务规则包配置情况
                List<CheckerPkgRspVO> checkerPkgList = configCheckerPkgBizService.getConfigCheckerPkg(taskId, toolName, taskCodeLang, toolConfig).getCheckerPackages();

                // 未打开的规则包设置为forbidden
                if (CollectionUtils.isNotEmpty(checkerPkgList))
                {
                    for (CheckerPkgRspVO checkerPkgRsp : checkerPkgList)
                    {
                        if (!checkerPkgRsp.getPkgStatus())
                        {
                            coverityDefect.getCheckerPkgCounts().put(checkerPkgRsp.getPkgId(), FORBIDDEN_COUNT);
                        }
                    }
                }

                // 查询规则详情
                Map<String, CheckerDetailVO> allCheckers = checkerPkgList.stream().map(CheckerPkgRspVO::getCheckerList)
                        .filter(CollectionUtils::isNotEmpty).flatMap(Collection::parallelStream)
                        .collect(Collectors.toMap(CheckerDetailVO::getCheckerKey, Function.identity(), (k, v) -> v));

                // 查询遗留告警列表
                List<DefectEntity> defectDetailList = defectRepository.findByTaskIdAndToolName(taskId, toolName);

                if (CollectionUtils.isNotEmpty(defectDetailList))
                {
                    for (DefectEntity covDefect : defectDetailList)
                    {
                        if (ComConstants.DefectStatus.NEW.value() != covDefect.getStatus())
                        {
                            continue;
                        }
                        // 统计各级别告警数
                        if (allCheckers.containsKey(covDefect.getCheckerName()))
                        {
                            CheckerDetailVO checkerPOModel = allCheckers.get(covDefect.getCheckerName());
                            updateCompileSeverityCount(covDefect.getSeverity(), coverityDefect);

                            // 统计各规则包告警数
                            if (checkerPOModel != null)
                            {
                                String kind = checkerPOModel.getPkgKind();
                                long currentCount = coverityDefect.getCheckerPkgCounts().get(kind);
                                coverityDefect.getCheckerPkgCounts().put(kind, currentCount + 1);
                            }
                        }
                    }
                }
            }
        }

        // 更新元数据
        updateCompileMetadata(toolName, coverityDefect, metadataModel, metadataCallback);
        updateValue(toolName + "_SECURITY", String.valueOf(coverityDefect.getCheckerPkgCounts().get(ComConstants.CheckerPkgKind.SECURITY.value())),
                metadataModel, metadataCallback);
        updateValue(toolName + "_MEMORY", String.valueOf(coverityDefect.getCheckerPkgCounts().get(ComConstants.CheckerPkgKind.MEMORY.value())),
                metadataModel, metadataCallback);
        updateValue(toolName + "_PERFORMANCE", String.valueOf(coverityDefect.getCheckerPkgCounts().get(ComConstants.CheckerPkgKind.PERFORMANCE.value())),
                metadataModel, metadataCallback);
        updateValue(toolName + "_COMPILE", String.valueOf(coverityDefect.getCheckerPkgCounts().get(ComConstants.CheckerPkgKind.COMPILE.value())),
                metadataModel, metadataCallback);
        updateValue(toolName + "_SYS_API", String.valueOf(coverityDefect.getCheckerPkgCounts().get(ComConstants.CheckerPkgKind.SYS_API.value())),
                metadataModel, metadataCallback);
        updateValue(toolName + "_EXPRESSION", String.valueOf(coverityDefect.getCheckerPkgCounts().get(ComConstants.CheckerPkgKind.EXPRESSION.value())),
                metadataModel, metadataCallback);
    }

    /**
     * 查询Klocwork分析结果
     *
     * @param taskInfo
     * @param metadataModel
     * @param metadataCallback
     * @param effectiveTools
     */
    public void getKwAnalysisResult(TaskBaseVO taskInfo, ToolMetaBaseVO toolMeta, Map<String, RedLineVO> metadataModel, PipelineRedLineCallbackVO metadataCallback,
            List<String> effectiveTools)
    {
        String toolName = ComConstants.Tool.KLOCWORK.name();
        RLCompileDefectVO kwDefect = new RLCompileDefectVO();
        boolean toolMatchProjLang = (toolMeta.getLang() & taskInfo.getCodeLang()) != 0;

        if (toolMatchProjLang)
        {
            // 语言符合但未接入工具时，数值全部为-1
            if (!effectiveTools.contains(toolName))
            {
                kwDefect.setRemainPrompt(FORBIDDEN_COUNT);
                kwDefect.setRemainNormal(FORBIDDEN_COUNT);
                kwDefect.setRemainSerious(FORBIDDEN_COUNT);
            }
            else
            {
                List<DefectEntity> defectList = defectRepository.findByTaskIdAndToolName(taskInfo.getTaskId(), toolName);
                if (CollectionUtils.isNotEmpty(defectList))
                {
                    for (DefectEntity kwDefectModel : defectList)
                    {
                        if (ComConstants.DefectStatus.NEW.value() != kwDefectModel.getStatus())
                        {
                            continue;
                        }
                        // 统计各级别告警数
                        updateCompileSeverityCount(kwDefectModel.getSeverity(), kwDefect);
                    }
                }
            }
        }
        else
        {
            // 语言不符合时，全设置为0表示通过，由于Klocwork中的指标都是原生数据类型，默认为0，所以不需要显式设置
        }

        // 更新元数据
        updateCompileMetadata(toolName, kwDefect, metadataModel, metadataCallback);
    }

    /**
     * 保存质量红线数据
     *
     * @param taskDetailVO
     * @param toolName
     * @param buildId
     */
    @Override
    public void saveRedLineData(TaskDetailVO taskDetailVO, String toolName, String buildId)
    {
        if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equals(taskDetailVO.getCreateFrom()))
        {
            Set<String> effectiveTools = Sets.newHashSet();
            if (CollectionUtils.isNotEmpty(taskDetailVO.getToolConfigInfoList()))
            {
                for (ToolConfigInfoVO toolConfigInfoVO : taskDetailVO.getToolConfigInfoList())
                {
                    if (toolConfigInfoVO.getFollowStatus() != ComConstants.FOLLOW_STATUS.WITHDRAW.value())
                    {
                        effectiveTools.add(toolConfigInfoVO.getToolName());
                    }
                }
            }
            PipelineRedLineCallbackVO pipelineRedLineCallbackVO = getRedLineIndicators(taskDetailVO, Lists.newArrayList(effectiveTools), toolName);
            if (pipelineRedLineCallbackVO != null && CollectionUtils.isNotEmpty(pipelineRedLineCallbackVO.getData()))
            {
                List<RedLineEntity> redLineEntities = Lists.newArrayList();
                for (RedLineVO redLineVO : pipelineRedLineCallbackVO.getData())
                {
                    RedLineEntity redLineEntity = new RedLineEntity();
                    BeanUtils.copyProperties(redLineVO, redLineEntity);
                    redLineEntity.setBuildId(buildId);
                    redLineEntities.add(redLineEntity);
                }
                redLineRepository.saveAll(redLineEntities);
            }
        }
    }

    @Override
    public PipelineRedLineCallbackVO getPipelineCallback(TaskDetailVO taskDetailVO, String buildId)
    {
        PipelineRedLineCallbackVO metadataCallback = new PipelineRedLineCallbackVO();
        String elementType = ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equals(taskDetailVO.getCreateFrom()) ? LINUX_PAAS_CODECC_SCRIPT
                : LINUX_CODECC_SCRIPT;
        metadataCallback.setElementType(elementType);
        metadataCallback.setData(Lists.newArrayList());
        List<RedLineEntity> redLineEntities = redLineRepository.findByBuildId(buildId);
        if (CollectionUtils.isNotEmpty(redLineEntities))
        {
            for (RedLineEntity redLineEntity : redLineEntities)
            {
                RedLineVO redLineVO = new RedLineVO();
                BeanUtils.copyProperties(redLineEntity, redLineVO);
                metadataCallback.getData().add(redLineVO);
            }
        }
        return metadataCallback;
    }

    /**
     * 查询圈复杂度和重复率分析结果
     *
     * @param taskInfo         任务信息
     * @param toolInfo         工具的基本信息[t_tool_meta]
     * @param metadataModel    元数据
     * @param metadataCallback 发送到蓝盾的元数据
     * @param effectiveTools   有效的工具
     * @param toolConfig       工具配置
     */
    private void getCcnAndDupcResult(TaskBaseVO taskInfo, ToolMetaBaseVO toolInfo, Map<String, RedLineVO> metadataModel,
                                     PipelineRedLineCallbackVO metadataCallback, List<String> effectiveTools, ToolConfigInfoVO toolConfig)
    {
        long taskId = taskInfo.getTaskId();
        String toolName = toolInfo.getName();
        RLCcnAndDupcDefectVO ccnDupcDefect = new RLCcnAndDupcDefectVO();
        boolean toolMatchCodeLang = (toolInfo.getLang() & taskInfo.getCodeLang()) != 0;

        // 初始化为通过
        if (ComConstants.Tool.CCN.name().equals(toolName))
        {
            updateValue(toolName + "_SINGLE_FUNC_MAX", String.valueOf(PASS_COUNT), metadataModel, metadataCallback);
            updateValue(toolName + "_NEW_SINGLE_FUNC_MAX", String.valueOf(PASS_COUNT), metadataModel, metadataCallback);
            updateValue(toolName + "_NEW_FUNC_COUNT", String.valueOf(PASS_COUNT), metadataModel, metadataCallback);
            updateValue(toolName + "_NEW_FUNC_BEYOND_THRESHOLD_SUM", String.valueOf(PASS_COUNT), metadataModel, metadataCallback);
            updateValue(toolName + "_HISTORY_FUNC_BEYOND_THRESHOLD_SUM", String.valueOf(PASS_COUNT), metadataModel, metadataCallback);
        }
        else if (ComConstants.Tool.DUPC.name().equals(toolName))
        {
            updateValue(toolName + "_SINGLE_FILE_MAX", String.valueOf(PASS_COUNT), metadataModel, metadataCallback);
        }

        if (toolMatchCodeLang)
        {
            // 语言符合但未接入工具时，数值全部为-1
            if (!effectiveTools.contains(toolName))
            {
                ccnDupcDefect.setAverage(FORBIDDEN_COUNT_F);
                ccnDupcDefect.setExtreme(FORBIDDEN_COUNT);
                ccnDupcDefect.setHigh(FORBIDDEN_COUNT);
                ccnDupcDefect.setMiddle(FORBIDDEN_COUNT);
                ccnDupcDefect.setSingleFileMax(FORBIDDEN_COUNT_F);
                ccnDupcDefect.setSingleFuncMax(FORBIDDEN_COUNT);
                ccnDupcDefect.setNewSingleFuncMax(FORBIDDEN_COUNT);
                ccnDupcDefect.setNewFuncCount(FORBIDDEN_COUNT);
                ccnDupcDefect.setNewFuncBeyondThresholdSum(FORBIDDEN_COUNT);
                ccnDupcDefect.setHistoryFuncBeyondThresholdSum(FORBIDDEN_COUNT);
            }
            else
            {
                // 查询圈复杂度和重复率的告警数量
                if (ComConstants.Tool.CCN.name().equals(toolName))
                {
                    getCcnDefectCount(ccnDupcDefect, taskId, toolConfig);
                    updateValue(toolName + "_SINGLE_FUNC_MAX", String.valueOf(ccnDupcDefect.getSingleFuncMax()), metadataModel, metadataCallback);
                    updateValue(toolName + "_NEW_SINGLE_FUNC_MAX", String.valueOf(ccnDupcDefect.getNewSingleFuncMax()), metadataModel, metadataCallback);
                    updateValue(toolName + "_NEW_FUNC_COUNT", String.valueOf(ccnDupcDefect.getNewFuncCount()), metadataModel, metadataCallback);
                    updateValue(toolName + "_NEW_FUNC_BEYOND_THRESHOLD_SUM", String.valueOf(ccnDupcDefect.getNewFuncBeyondThresholdSum()), metadataModel, metadataCallback);
                    updateValue(toolName + "_HISTORY_FUNC_BEYOND_THRESHOLD_SUM", String.valueOf(ccnDupcDefect.getHistoryFuncBeyondThresholdSum()), metadataModel, metadataCallback);
                }
                else if (ComConstants.Tool.DUPC.name().equals(toolName))
                {
                    getDupcDefectCount(ccnDupcDefect, taskId);
                    updateValue(toolName + "_SINGLE_FILE_MAX", String.valueOf(ccnDupcDefect.getSingleFileMax()), metadataModel, metadataCallback);
                }

                // 查询圈复杂度和重复率的统计数据
                getCcnAndDupcStatisticResult(taskId, toolName, ccnDupcDefect);
            }
        }

        updateValue(toolName + "_AVERAGE", String.valueOf(ccnDupcDefect.getAverage()), metadataModel, metadataCallback);
        updateValue(toolName + "_EXTREME", String.valueOf(ccnDupcDefect.getExtreme()), metadataModel, metadataCallback);
        updateValue(toolName + "_HIGH", String.valueOf(ccnDupcDefect.getHigh()), metadataModel, metadataCallback);
        updateValue(toolName + "_MIDDLE", String.valueOf(ccnDupcDefect.getMiddle()), metadataModel, metadataCallback);
    }

    /**
     * 拼装元数据
     *
     * @param metadataKey
     * @param value
     * @param metadataModel
     * @param metadataCallback
     */
    private void updateValue(String metadataKey, String value, Map<String, RedLineVO> metadataModel,
                             PipelineRedLineCallbackVO metadataCallback)
    {
        if (metadataModel.containsKey(metadataKey))
        {
            RedLineVO newSeriousMetadata = metadataModel.get(metadataKey);
            newSeriousMetadata.setValue(value);
            metadataCallback.getData().add(newSeriousMetadata);
        }
    }

    /**
     * 获取质量红线上报数据 [ key:enName value:RedLineEntity ]
     *
     * @return
     */
    private Map<String, RedLineVO> loadRedLineMetaData(String toolName)
    {
        List<RedLineMetaEntity> metaData = redLineMetaRepository.findByDetail(toolName);
        if (CollectionUtils.isNotEmpty(metaData))
        {
            return metaData.stream()
                    .map(meta ->
                    {
                        RedLineVO redLineVO = new RedLineVO();
                        BeanUtils.copyProperties(meta, redLineVO);
                        return redLineVO;
                    })
                    .collect(Collectors.toMap(RedLineVO::getEnName, Function.identity(), (k, v) -> v));
        }
        return null;
    }

    /**
     * 初始化LINT类工具数据
     *
     * @param checkerDetailList
     * @param lintRLModel
     * @param initValue
     */
    private void initRLLintPkg(List<CheckerDetailEntity> checkerDetailList, RLLintDefectVO lintRLModel, long initValue)
    {
        // 根据工具获取规则包
        if (CollectionUtils.isNotEmpty(checkerDetailList))
        {
            checkerDetailList.stream().map(CheckerDetailEntity::getPkgKind)
                    .distinct().forEach(pkgId ->
            {
                lintRLModel.getHistoryCheckerPkgCounts().put(pkgId, initValue);
                lintRLModel.getNewCheckerPkgCounts().put(pkgId, initValue);
            });
        }
    }


    /**
     * 保存各严重级别告警数
     *
     * @param severity
     * @param fileType
     * @param defectCountModel
     */
    private static void updateLintSeverityCount(int severity, ComConstants.FileType fileType, RLLintDefectVO defectCountModel)
    {
        if (ComConstants.SERIOUS == severity)
        {
            if (ComConstants.FileType.HISTORY.equals(fileType))
            {
                defectCountModel.setHistorySerious(defectCountModel.getHistorySerious() + 1L);
            }
            else
            {
                defectCountModel.setNewSerious(defectCountModel.getNewSerious() + 1L);
            }
        }
        else if (ComConstants.NORMAL == severity)
        {
            if (ComConstants.FileType.HISTORY.equals(fileType))
            {
                defectCountModel.setHistoryNormal(defectCountModel.getHistoryNormal() + 1L);
            }
            else
            {
                defectCountModel.setNewNormal(defectCountModel.getNewNormal() + 1L);
            }
        }
        else if (ComConstants.PROMPT_IN_DB == severity || ComConstants.PROMPT == severity)
        {
            if (ComConstants.FileType.HISTORY.equals(fileType))
            {
                defectCountModel.setHistoryPrompt(defectCountModel.getHistoryPrompt() + 1L);
            }
            else
            {
                defectCountModel.setNewPrompt(defectCountModel.getNewPrompt() + 1L);
            }
        }
    }


    /**
     * 更新规则包告警数
     *
     * @param checkerPkgCounts
     * @param checker
     * @param allCheckerMap
     */
    private void updateLintCheckerPkgCount(Map<String, Long> checkerPkgCounts, String checker, Map<String, CheckerDetailVO> allCheckerMap,
            Set<String> tosaCheckers, boolean isTosaPkgOpened)
    {
        // 如果打开了开源规范包，则同名规则以开源规范包为准
        CheckerDetailVO checkerDetail = allCheckerMap.get(checker);
        if (checkerDetail != null)
        {
            if (tosaCheckers.contains(checkerDetail.getCheckerKey()) && isTosaPkgOpened)
            {
                checkerDetail.setPkgKind(ComConstants.CheckerPkgKind.TOSA.value());
            }

            // 更新各规则包告警数
            Long currentPkgCountObj = checkerPkgCounts.get(checkerDetail.getPkgKind());
            if (currentPkgCountObj != null && currentPkgCountObj.longValue() != FORBIDDEN_COUNT)
            {
                long currentCount = currentPkgCountObj.longValue();
                checkerPkgCounts.put(checkerDetail.getPkgKind(), currentCount + 1);
            }
        }
    }

    /**
     * 初始化规则告警数
     *
     * @param toolName
     * @param defectCountModel
     * @param initValue
     */
    private static void initRLLintChecker(String toolName, RLLintDefectVO defectCountModel, long initValue)
    {
        if (RED_LINE_CHECKERS.containsKey(toolName))
        {
            for (String checker : RED_LINE_CHECKERS.get(toolName))
            {
                defectCountModel.getNewCheckerCounts().put(checker, initValue);
                defectCountModel.getHistoryCheckerCounts().put(checker, initValue);
            }
        }
    }

    /**
     * 判断规则包里的规则是否全部打开
     *
     * @param checkerPkg
     * @return
     */
    private boolean isAllCheckerOpened(CheckerPkgRspVO checkerPkg)
    {
        if (!checkerPkg.getPkgStatus())
        {
            return false;
        }
        else
        {
            if (CollectionUtils.isNotEmpty(checkerPkg.getCheckerList()))
            {
                for (CheckerDetailVO checkerInfo : checkerPkg.getCheckerList())
                {
                    if (!checkerInfo.getCheckerStatus())
                    {
                        return false;
                    }
                }
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    /**
     * 累计各规则告警数
     *
     * @param checkerCounts
     * @param checker
     * @param toolName
     */
    private void updateLintCheckerCount(Map<String, Long> checkerCounts, String checker, String toolName)
    {
        if (RED_LINE_CHECKERS.containsKey(toolName) && RED_LINE_CHECKERS.get(toolName).contains(checker))
        {
            checkerCounts.put(checker, checkerCounts.get(checker) + 1L);
        }
    }

    private void getCcnDefectCount(RLCcnAndDupcDefectVO ccnDupcDefect, long taskId, ToolConfigInfoVO toolConfig)
    {
        long succTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, ComConstants.Tool.CCN.name(), null);
        // 获取超标圈复杂度阈值，优先从规则里面取，取不到从个性化参数里面取，再取不到就是用默认值
        int ccnThreshold = checkerService.getCcnThreshold(toolConfig);

        List<CCNDefectEntity> ccnDefectList = ccnDefectRepository.findByTaskIdAndStatus(taskId, ComConstants.DefectStatus.NEW.value());
        long maxCcn = 0L;
        long newMaxCcn = 0L;
        int newDefectCount = 0;
        int newFuncBeyondThresholdSum = 0;
        int historyFuncBeyondThresholdSum = 0;
        if (CollectionUtils.isNotEmpty(ccnDefectList))
        {
            for (CCNDefectEntity defectModel : ccnDefectList)
            {
                // 获取圈复杂度和代码修改时间
                long ccn = defectModel.getCcn();
                long defectUpdateTime = defectModel.getLatestDateTime() == null ? 0L : DateTimeUtils.getThirteenTimestamp(defectModel.getLatestDateTime());

                // 获取新增代码圈复杂度最大值
                if (defectUpdateTime >= succTime)
                {
                    if (ccn > newMaxCcn)
                    {
                        newMaxCcn = ccn;
                    }
                    newDefectCount++;

                    // 统计新函数超过阈值部分的圈复杂度之和
                    newFuncBeyondThresholdSum += ccn - ccnThreshold;
                }
                else
                {
                    // 统计历史函数超过阈值部分的圈复杂度之和
                    historyFuncBeyondThresholdSum += ccn - ccnThreshold;
                }

                // 获取圈复杂度最大值
                if (ccn > maxCcn)
                {
                    maxCcn = ccn;
                }
            }
            ccnDupcDefect.setSingleFuncMax(maxCcn);
            ccnDupcDefect.setNewFuncCount(newDefectCount);
            ccnDupcDefect.setNewSingleFuncMax(newMaxCcn);
            ccnDupcDefect.setNewFuncBeyondThresholdSum(newFuncBeyondThresholdSum);
            ccnDupcDefect.setHistoryFuncBeyondThresholdSum(historyFuncBeyondThresholdSum);
        }
    }

    /**
     * 查询圈复杂度和重复率的告警数量
     *
     * @param ccnDupcDefect
     * @param taskId
     */
    private void getDupcDefectCount(RLCcnAndDupcDefectVO ccnDupcDefect, long taskId)
    {
        List<DUPCDefectEntity> dupcDefects = dupcDefectRepository.getByTaskIdAndStatus(taskId, ComConstants.DefectStatus.NEW.value());
        if (CollectionUtils.isNotEmpty(dupcDefects))
        {
            double maxDupc = 0.0D;
            for (DUPCDefectEntity fileInfoModel : dupcDefects)
            {
                double dupc;
                if (fileInfoModel.getDupRate().contains("%"))
                {
                    dupc = Double.valueOf(fileInfoModel.getDupRate().replace("%", "")) / 100;
                }
                else
                {
                    dupc = 0.0D;
                }

                if (dupc > maxDupc)
                {
                    maxDupc = dupc;
                }
            }
            ccnDupcDefect.setSingleFileMax(Double.valueOf(String.format("%.4f", maxDupc)));
        }
    }

    /**
     * 查询圈复杂度和重复率的统计数据
     *
     * @param taskId
     * @param toolName
     * @param ccnDupcDefect
     */
    private void getCcnAndDupcStatisticResult(long taskId, String toolName, RLCcnAndDupcDefectVO ccnDupcDefect)
    {
        if (ComConstants.Tool.CCN.name().equals(toolName))
        {
            CCNStatisticEntity ccnStatistic = ccnStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);
            ccnDupcDefect.setAverage(Double.valueOf(String.format("%.4f", ccnStatistic.getAverageCCN() == null ? 0.0F : ccnStatistic.getAverageCCN())));
            ccnDupcDefect.setExtreme(ccnStatistic.getSuperHighCount() == null ? 0 : ccnStatistic.getSuperHighCount());
            ccnDupcDefect.setHigh(ccnStatistic.getHighCount() == null ? 0 : ccnStatistic.getHighCount());
            ccnDupcDefect.setMiddle(ccnStatistic.getMediumCount() == null ? 0 : ccnStatistic.getMediumCount());
        }
        else if (ComConstants.Tool.DUPC.name().equals(toolName))
        {
            DUPCStatisticEntity dupcStatistic = dupcStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);
            float dupRate = dupcStatistic.getDupRate() == null ? 0.0F : dupcStatistic.getDupRate();
            ccnDupcDefect.setAverage(Double.valueOf(String.format("%.4f", dupRate / 100)));
            ccnDupcDefect.setExtreme(dupcStatistic.getSuperHighCount() == null ? 0 : dupcStatistic.getSuperHighCount());
            ccnDupcDefect.setHigh(dupcStatistic.getHighCount() == null ? 0 : dupcStatistic.getHighCount());
            ccnDupcDefect.setMiddle(dupcStatistic.getMediumCount() == null ? 0 : dupcStatistic.getMediumCount());
        }
    }

    /**
     * 保存各严重级别告警数
     *
     * @param severity
     * @param defect
     */
    private void updateCompileSeverityCount(int severity, RLCompileDefectVO defect)
    {
        if ((ComConstants.SERIOUS & severity) != 0)
        {
            defect.setRemainSerious(defect.getRemainSerious() + 1);
        }
        else if ((ComConstants.NORMAL & severity) != 0)
        {
            defect.setRemainNormal(defect.getRemainNormal() + 1);
        }
        else if ((ComConstants.PROMPT & severity) != 0)
        {
            defect.setRemainPrompt(defect.getRemainPrompt() + 1);
        }
    }

    /**
     * 更新Compile类工具元数据
     *
     * @param toolName
     * @param defect
     * @param metadataModel
     * @param metadataCallback
     */
    private void updateCompileMetadata(String toolName, RLCompileDefectVO defect, Map<String, RedLineVO> metadataModel,
            PipelineRedLineCallbackVO metadataCallback)
    {
        updateValue(toolName + "_SERIOUS", String.valueOf(defect.getRemainSerious()), metadataModel, metadataCallback);
        updateValue(toolName + "_NORMAL", String.valueOf(defect.getRemainNormal()), metadataModel, metadataCallback);
        updateValue(toolName + "_PROMPT", String.valueOf(defect.getRemainPrompt()), metadataModel, metadataCallback);
    }
}
