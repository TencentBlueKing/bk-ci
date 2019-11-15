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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.*;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.service.IConfigCheckerPkgBizService;
import com.tencent.bk.codecc.defect.service.RedLineReportService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.ToolMetaBaseVO;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.bk.codecc.defect.constant.DefectConstants.*;

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

    @Autowired
    private Client client;

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Autowired
    private DUPCDefectRepository dupcDefectRepository;

    @Autowired
    private LintDefectRepository lintDefectRepository;

    @Autowired
    private BsRLMetadataRepository bsRLMetadataRepository;

    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;

    @Autowired
    private DUPCStatisticRepository dupcStatisticRepository;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    private static Logger logger = LoggerFactory.getLogger(RedLineReportServiceImpl.class);


    /**
     * 查询质量红线指标数据
     *
     * @param taskInfo
     * @param effectiveTools
     * @return
     */
    @Override
    public BsRLMetadataCallbackVO getRedLineIndicators(TaskBaseVO taskInfo, List<String> effectiveTools)
    {
        // 拼装请求数据
        BsRLMetadataCallbackVO metadataCallback = new BsRLMetadataCallbackVO();
        String elementType = StringUtils.isNotEmpty(taskInfo.getProjectId()) ? LINUX_PAAS_CODECC_SCRIPT : LINUX_CODECC_SCRIPT;
        metadataCallback.setElementType(elementType);
        metadataCallback.setData(Lists.newArrayList());

        // 查询元数据模板
        Map<String, BsRLMetadataVO> metadataModel = loadBsRLMetaData();
        if (MapUtils.isEmpty(metadataModel))
        {
            return metadataCallback;
        }

        // 获取所有的工具名称
        Set<String> toolNames = Sets.newHashSet();
        metadataModel.values().forEach(meta -> toolNames.add(meta.getDetail()));

        // 获取所有的基础工具信息
        Map<String, ToolMetaBaseVO> toolMetaMap = thirdPartySystemCaller.getToolMeta();

        // 查询元数据
        for (String toolName : toolNames)
        {
            ToolMetaBaseVO toolMeta = toolMetaMap.get(toolName);
            if (Objects.nonNull(toolMeta))
            {
                logger.info("[ red line ] current reporting tool: {}", toolName);
                String pattern = toolMeta.getPattern();
                if (ComConstants.ToolPattern.LINT.name().equals(pattern))
                {
                    getLintAnalysisResult(taskInfo, toolMeta, metadataModel, metadataCallback, effectiveTools);
                }
                else if (ComConstants.ToolPattern.CCN.name().equals(pattern)
                        || ComConstants.ToolPattern.DUPC.name().equals(pattern))
                {
                    getCcnAndDupcResult(taskInfo, toolMeta, metadataModel, metadataCallback, effectiveTools);
                }
            }
        }

        return metadataCallback;
    }


    /**
     * 查询Lint类工具分析结果
     *
     * @param taskInfo         任务信息
     * @param toolInfo         工具的基本信息[t_tool_meta]
     * @param metadataModel    元数据
     * @param metadataCallback 发送到蓝盾的元数据
     * @param effectiveTools   有效的工具
     */
    private void getLintAnalysisResult(TaskBaseVO taskInfo, ToolMetaBaseVO toolInfo, Map<String, BsRLMetadataVO> metadataModel,
                                       BsRLMetadataCallbackVO metadataCallback, List<String> effectiveTools)
    {
        long taskId = taskInfo.getTaskId();
        String toolName = toolInfo.getName();
        RLLintDefectVO lintRLModel = new RLLintDefectVO();
        lintRLModel.setHistoryCheckerPkgCounts(Maps.newHashMap());
        lintRLModel.setNewCheckerPkgCounts(Maps.newHashMap());

        // 项目配置的编程语言是否与该工具支持的语言相符
        boolean toolMatchCodeLang = (toolInfo.getLang() & taskInfo.getCodeLang()) != 0;

        // 工具与语言不符合的，数值全部为0，不做拦截
        if (!toolMatchCodeLang)
        {
            logger.info("[ red line ] The tool language {} is different from the task language : {}", toolInfo.getLang(), taskInfo.getCodeLang());
            initRLLintPkg(toolName, lintRLModel, PASS_COUNT);
        }
        else
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
                initRLLintPkg(toolName, lintRLModel, FORBIDDEN_COUNT);
                logger.info("[ red line ] The tool {} not a effective tools", toolName);
            }
            else
            {
                // 规则包列表 [包含规则详情]
                List<CheckerPkgRspVO> checkerPkgList = checkerPkgList(taskId, toolName);
                // 初始化接入前/后lint类文件 规则包告警数数量
                initCheckerPkgCounts(lintRLModel, checkerPkgList);
                // 统计lint类告警规则包
                updateLintDefectChecker(taskId, toolName, lintRLModel, checkerPkgList);

            }
        }

        // 更新元数据
        updateValue(toolName + "_NEW_SERIOUS", String.valueOf(lintRLModel.getNewSerious()), metadataModel, metadataCallback);
        updateValue(toolName + "_NEW_NORMAL", String.valueOf(lintRLModel.getNewNormal()), metadataModel, metadataCallback);
        updateValue(toolName + "_NEW_PROMPT", String.valueOf(lintRLModel.getNewPrompt()), metadataModel, metadataCallback);
        updateValue(toolName + "_HISTORY_SERIOUS", String.valueOf(lintRLModel.getHistorySerious()), metadataModel, metadataCallback);
        updateValue(toolName + "_HISTORY_NORMAL", String.valueOf(lintRLModel.getHistoryNormal()), metadataModel, metadataCallback);
        updateValue(toolName + "_HISTORY_PROMPT", String.valueOf(lintRLModel.getHistoryPrompt()), metadataModel, metadataCallback);

    }


    /**
     * 初始化接入前、后文件 规则包告警数数量
     *
     * @param lintRLModel
     * @param checkerPkgList
     */
    private void initCheckerPkgCounts(RLLintDefectVO lintRLModel, List<CheckerPkgRspVO> checkerPkgList)
    {
        // 初始化规则包告警数
        if (CollectionUtils.isNotEmpty(checkerPkgList))
        {
            // 初始化规则包告警数量，设置规则未全部打开的规则包告警数量为-1，全打开的设置为0
            checkerPkgList.forEach(checker ->
            {
                if (checker.getPkgStatus())
                {
                    lintRLModel.getNewCheckerPkgCounts().put(checker.getPkgId(), 0L);
                    lintRLModel.getHistoryCheckerPkgCounts().put(checker.getPkgId(), 0L);
                }
                else
                {
                    lintRLModel.getNewCheckerPkgCounts().put(checker.getPkgId(), FORBIDDEN_COUNT);
                    lintRLModel.getHistoryCheckerPkgCounts().put(checker.getPkgId(), FORBIDDEN_COUNT);
                }
            });
        }
    }


    /**
     * 统计lint类告警规则包
     *
     * @param taskId
     * @param toolName
     * @param lintRLModel
     * @param checkerPkgList
     */
    private void updateLintDefectChecker(long taskId, String toolName, RLLintDefectVO lintRLModel, List<CheckerPkgRspVO> checkerPkgList)
    {
        // 获取lint文件信息
        List<LintFileEntity> lintFileEntity = lintDefectRepository.findByTaskIdAndToolNameAndStatus(taskId, toolName, ComConstants.FileType.NEW.value());
        if (CollectionUtils.isNotEmpty(lintFileEntity))
        {
            // 取出defectList中的status为new告警的文件 [ key: new/history ]
            Map<Integer, List<LintDefectEntity>> lintDefectMap = lintFileEntity.stream()
                    .filter(lint -> CollectionUtils.isNotEmpty(lint.getDefectList()))
                    .map(LintFileEntity::getDefectList).flatMap(Collection::parallelStream)
                    .filter(lint -> ComConstants.DefectStatus.NEW.value() == lint.getStatus())
                    .collect(Collectors.groupingBy(LintDefectEntity::getDefectType));

            // 按照规则名称映射规则详情
            Map<String, CheckerDetailVO> checkerMap = checkerPkgList.stream().map(CheckerPkgRspVO::getCheckerList)
                    .filter(CollectionUtils::isNotEmpty).flatMap(Collection::parallelStream)
                    .collect(Collectors.toMap(CheckerDetailVO::getCheckerKey, Function.identity(), (k, v) -> v));

            // 查询接入后告警详情
            List<LintDefectEntity> newDefectList = lintDefectMap.get(ComConstants.DefectType.NEW.value());
            // 查询接入后告警详情
            List<LintDefectEntity> historyDefectList = lintDefectMap.get(ComConstants.DefectType.HISTORY.value());

            // 统计接入后告警数量
            if (CollectionUtils.isNotEmpty(newDefectList))
            {
                for (LintDefectEntity defectModel : newDefectList)
                {
                    updateLintSeverityCount(defectModel.getSeverity(), ComConstants.FileType.NEW, lintRLModel);

                    // 统计各规则包告警数，工具与项目语言不符合的不做统计
                    updateLintCheckerPkgCount(lintRLModel.getNewCheckerPkgCounts(), defectModel.getChecker(), checkerMap);
                }
            }

            // 统计接入前告警数量
            if (CollectionUtils.isNotEmpty(historyDefectList))
            {
                for (LintDefectEntity defectModel : historyDefectList)
                {
                    updateLintSeverityCount(defectModel.getSeverity(), ComConstants.FileType.HISTORY, lintRLModel);

                    /**
                     * 统计各规则包告警数，工具与项目语言不符合的不做统计
                     * 由于目前腾讯开源包告警数量是统计接入前和接入后之和，所以此处把接入后的数据累加到接入前的数据中
                     */
                    updateLintCheckerPkgCount(lintRLModel.getNewCheckerPkgCounts(), defectModel.getChecker(), checkerMap);
                }
            }

        }
    }


    /**
     * 查询圈复杂度和重复率分析结果
     *
     * @param taskInfo         任务信息
     * @param toolInfo         工具的基本信息[t_tool_meta]
     * @param metadataModel    元数据
     * @param metadataCallback 发送到蓝盾的元数据
     * @param effectiveTools   有效的工具
     */
    private void getCcnAndDupcResult(TaskBaseVO taskInfo, ToolMetaBaseVO toolInfo, Map<String, BsRLMetadataVO> metadataModel,
                                     BsRLMetadataCallbackVO metadataCallback, List<String> effectiveTools)
    {
        long taskId = taskInfo.getTaskId();
        String toolName = toolInfo.getName();
        RLCcnAndDupcDefectVO ccnDupcDefect = new RLCcnAndDupcDefectVO();

        // 确认基本数据工具是否匹配任务中的工具
        boolean toolMatchCodeLang = (toolInfo.getLang() & taskInfo.getCodeLang()) != 0;

        // 语言符合事进入条件，语言不符合时，全设置为0表示通过，由于CCN和DUPC中的指标都是原生数据类型，默认为0，所以不需要显式设置
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
            }
            else
            {
                //获取风险系数值
                Map<String, String> riskConfigMap = thirdPartySystemCaller.getRiskFactorConfig(toolName);

                // 圈复杂度上报质量红线数据
                if (ComConstants.Tool.CCN.name().equals(toolName))
                {
                    updateCcnValue(metadataModel, metadataCallback, taskId, toolName, ccnDupcDefect, riskConfigMap);

                    CCNStatisticEntity ccnStatistic = ccnStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);
                    if (Objects.nonNull(ccnStatistic))
                    {
                        ccnDupcDefect.setAverage(Double.valueOf(String.format("%.4f", ccnStatistic.getAverageCCN())));
                    }
                }
                // 重复率上报质量红线数据
                else if (ComConstants.Tool.DUPC.name().equals(toolName))
                {
                    updateDupcValue(metadataModel, metadataCallback, taskId, toolName, ccnDupcDefect, riskConfigMap);

                    DUPCStatisticEntity dupcStatistic = dupcStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);
                    if (Objects.nonNull(dupcStatistic))
                    {
                        ccnDupcDefect.setAverage(Double.valueOf(String.format("%.4f", dupcStatistic.getDupRate() / 100)));
                    }
                }
            }
        }

        updateValue(toolName + "_AVERAGE", String.valueOf(ccnDupcDefect.getAverage()), metadataModel, metadataCallback);
        updateValue(toolName + "_EXTREME", String.valueOf(ccnDupcDefect.getExtreme()), metadataModel, metadataCallback);
        updateValue(toolName + "_HIGH", String.valueOf(ccnDupcDefect.getHigh()), metadataModel, metadataCallback);
        updateValue(toolName + "_MIDDLE", String.valueOf(ccnDupcDefect.getMiddle()), metadataModel, metadataCallback);
    }


    /**
     * 更新CCN上报数据
     *
     * @param metadataModel
     * @param metadataCallback
     * @param taskId
     * @param toolName
     * @param ccnDupcDefect
     * @param riskConfigMap
     */
    private void updateCcnValue(Map<String, BsRLMetadataVO> metadataModel, BsRLMetadataCallbackVO metadataCallback,
                                long taskId, String toolName, RLCcnAndDupcDefectVO ccnDupcDefect, Map<String, String> riskConfigMap)
    {
        // 查询圈复杂度的告警[t_ccn_defect]
        List<CCNDefectEntity> ccnDefectList = ccnDefectRepository.findByTaskIdAndStatus(taskId, DefectStatus.NEW.value());

        if (CollectionUtils.isNotEmpty(ccnDefectList))
        {
            ccnDefectList.stream()
                    .map(CCNDefectEntity::getCcn)
                    .forEach(ccn -> setLevel(ccn, ccnDupcDefect, riskConfigMap));

            // 取到最大的圈复杂度ccn
            long ccnDefect = ccnDefectList.stream()
                    .mapToLong(CCNDefectEntity::getCcn)
                    .max().orElse(ComConstants.COMMON_NUM_0L);

            ccnDupcDefect.setSingleFuncMax(ccnDefect);
            updateValue(toolName + "_SINGLE_FUNC_MAX", String.valueOf(ccnDupcDefect.getSingleFuncMax()), metadataModel, metadataCallback);
        }
        //当没有查出来时 应该给0
        else
        {
            updateValue(toolName + "_SINGLE_FUNC_MAX", String.valueOf(0), metadataModel, metadataCallback);
        }
    }


    /**
     * 更新DUPC上报数据
     *
     * @param metadataModel
     * @param metadataCallback
     * @param taskId
     * @param toolName
     * @param ccnDupcDefect
     * @param riskConfigMap
     */
    private void updateDupcValue(Map<String, BsRLMetadataVO> metadataModel, BsRLMetadataCallbackVO metadataCallback,
                                 long taskId, String toolName, RLCcnAndDupcDefectVO ccnDupcDefect, Map<String, String> riskConfigMap)
    {
        // 查询重复率列表[t_dupc_defect]
        List<DUPCDefectEntity> dupcDefectList = dupcDefectRepository.getByTaskIdAndStatus(taskId, DefectStatus.NEW.value());
        if (CollectionUtils.isNotEmpty(dupcDefectList))
        {
            // 取到重复率dupc
            double dupcMax = dupcDefectList.stream()
                    .map(DUPCDefectEntity::getDupRate)
                    .filter(dupc -> dupc.contains("%"))
                    .mapToDouble(dupc -> Double.valueOf(dupc.replace("%", "")) / 100)
                    .max().orElse(ComConstants.COMMON_NUM_0D);

            ccnDupcDefect.setSingleFileMax(Double.valueOf(String.format("%.4f", dupcMax)));
            updateValue(toolName + "_SINGLE_FILE_MAX", String.valueOf(ccnDupcDefect.getSingleFileMax()), metadataModel, metadataCallback);

            dupcDefectList.stream()
                    .map(dupc -> convertDupRate2Float(dupc.getDupRate()))
                    .forEach(dupc -> setLevel(dupc, ccnDupcDefect, riskConfigMap));
        }
        //当没有查出数据时，应该给0
        else
        {
            updateValue(toolName + "_SINGLE_FILE_MAX", String.valueOf(0.0D), metadataModel, metadataCallback);
        }
    }


    /**
     * 拼装元数据
     *
     * @param metadataKey
     * @param value
     * @param metadataModel
     * @param metadataCallback
     */
    private void updateValue(String metadataKey, String value, Map<String, BsRLMetadataVO> metadataModel,
                             BsRLMetadataCallbackVO metadataCallback)
    {
        if (metadataModel.containsKey(metadataKey))
        {
            BsRLMetadataVO newSeriousMetadata = metadataModel.get(metadataKey);
            newSeriousMetadata.setValue(value);
            metadataCallback.getData().add(newSeriousMetadata);
        }
    }


    /**
     * 设置严重程度数量
     *
     * @param ccn
     * @param ccnAndDupcDefect
     */
    private void setLevel(float ccn, RLCcnAndDupcDefectVO ccnAndDupcDefect, Map<String, String> riskConfigMap)
    {
        // 统计的数据
        long serious = ccnAndDupcDefect.getExtreme();
        long normal = ccnAndDupcDefect.getHigh();
        long prompt = ccnAndDupcDefect.getMiddle();

        if (MapUtils.isNotEmpty(riskConfigMap))
        {
            float sh = Float.valueOf(riskConfigMap.get(ComConstants.RiskFactor.SH.name()));
            float h = Float.valueOf(riskConfigMap.get(ComConstants.RiskFactor.H.name()));
            float m = Float.valueOf(riskConfigMap.get(ComConstants.RiskFactor.M.name()));

            if (ccn >= m && ccn < h)
            {
                ++serious;
            }
            else if (ccn >= h && ccn < sh)
            {
                ++normal;
            }
            else if (ccn >= sh)
            {
                ++prompt;
            }
        }

        ccnAndDupcDefect.setExtreme(serious);
        ccnAndDupcDefect.setHigh(normal);
        ccnAndDupcDefect.setMiddle(prompt);
    }


    /**
     * 获取质量红线上报数据 [ key:enName value:BsRLMetadataEntity ]
     *
     * @return
     */
    private Map<String, BsRLMetadataVO> loadBsRLMetaData()
    {
        List<BsRLMetadataEntity> metaData = bsRLMetadataRepository.findAll();
        if (CollectionUtils.isNotEmpty(metaData))
        {
            return metaData.stream()
                    .map(meta ->
                    {
                        BsRLMetadataVO bsRLMetadataVO = new BsRLMetadataVO();
                        BeanUtils.copyProperties(meta, bsRLMetadataVO);
                        return bsRLMetadataVO;
                    })
                    .collect(Collectors.toMap(BsRLMetadataVO::getEnName, Function.identity(), (k, v) -> v));
        }
        return null;
    }


    /**
     * 将重复率的百分数转换成浮点数
     * 工具侧上报的文件代码重复率是带%号的，比如12.57%，所以要先去除掉后面的百分号比较
     *
     * @param dupRateStr
     * @return
     */
    private float convertDupRate2Float(String dupRateStr)
    {
        float dupRate = 0;
        if (StringUtils.isNotBlank(dupRateStr))
        {
            dupRate = Float.valueOf(dupRateStr.substring(0, dupRateStr.length() - 1));
        }
        return dupRate;
    }


    /**
     * 初始化LINT类工具数据
     *
     * @param toolName
     * @param lintRLModel
     * @param initValue
     */
    private void initRLLintPkg(String toolName, RLLintDefectVO lintRLModel, long initValue)
    {
        // 根据工具获取规则包
        List<CheckerDetailEntity> checkerDetailList = checkerRepository.findByToolName(toolName);
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
        else if (ComConstants.PROMPT_IN_DB == severity)
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
    private void updateLintCheckerPkgCount(Map<String, Long> checkerPkgCounts, String checker,
                                           Map<String, CheckerDetailVO> allCheckerMap)
    {
        // 通过规则名称获取规则明细
        CheckerDetailVO checkerDetail = allCheckerMap.get(checker);
        if (checkerDetail != null)
        {
            // 更新各规则包告警数
            Long currentPkgCount = checkerPkgCounts.get(checkerDetail.getPkgKind());
            if (currentPkgCount != null && currentPkgCount != FORBIDDEN_COUNT)
            {
                // 统计规则包告警数量
                checkerPkgCounts.put(checkerDetail.getPkgKind(), currentPkgCount + 1);
            }
        }
    }


    @Autowired
    private CheckerRepository checkerRepository;

    @Autowired
    private BizServiceFactory<IConfigCheckerPkgBizService> fileAndConfigCheckerPkgFactory;

    private List<CheckerPkgRspVO> checkerPkgList(Long taskId, String toolName)
    {
        IConfigCheckerPkgBizService bizService = fileAndConfigCheckerPkgFactory
                .createBizService(toolName, ComConstants.BusinessType.CONFIG_PKG.value(), IConfigCheckerPkgBizService.class);
        return bizService.getConfigCheckerPkg(taskId, toolName);
    }


}
