package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.MetricsRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.MetricsDao;
import com.tencent.bk.codecc.defect.model.MetricsEntity;
import com.tencent.bk.codecc.defect.service.MetricsService;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.vo.MetricsVO;
import com.tencent.bk.codecc.defect.vo.MetricsVO.Analysis;
import com.tencent.bk.codecc.task.api.ServiceBuildIdRelationshipResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.BuildIdRelationShipVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.analysisresult.CCNLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.CLOCLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.CommonLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.DUPCLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.LintLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants.ScanStatus;
import com.tencent.devops.common.constant.ComConstants.StepFlag;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.service.ToolMetaCacheService;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MetricsServiceImpl implements MetricsService {

    @Autowired
    ToolMetaCacheService toolMetaCacheService;
    @Autowired
    TaskLogService taskLogService;
    @Autowired
    private Client client;
    @Autowired
    private MetricsRepository metricsRepository;
    @Autowired
    private MetricsDao metricsDao;

    @Value("${bkci.public.url:#{null}}")
    private String codeccHost;

    @Value("${git.host:}")
    private String gitHost;

    @Override
    public MetricsVO getMetrics(String repoId, String buildId) {
        log.info("get metrics: repoId: {} | buildId: {}", repoId, buildId);
        // 根据 repoId 获取任务信息
        long startTime = System.currentTimeMillis();
        long st = System.currentTimeMillis();
        Result<TaskDetailVO> taskResult;
        try {
            if (StringUtils.isNumeric(repoId)) {
                taskResult = client.get(ServiceTaskRestResource.class).getTaskInfoByGongfengId(
                        Integer.valueOf(repoId));
            } else {
                taskResult = client.get(ServiceTaskRestResource.class).getTaskInfoByAliasName(
                        URLEncoder.encode(repoId, "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
            throw new CodeCCException("InvalidRepoId", new String[]{"远程调用失败"});
        }
        log.info("get task info cost {}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();

        TaskDetailVO taskDetail = taskResult.getData();
        MetricsVO metricsVO = null;
        log.info("get task status cost {}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();
        // 非成功状态直接返回
        if (StringUtils.isNotBlank(buildId)) {
            Result<BuildIdRelationShipVO> result = client.get(ServiceBuildIdRelationshipResource.class)
                    .getRelationShip(buildId);
            metricsVO = new MetricsVO();
            // 任务状态非成功时直接返回部分信息
            if (result.isNotOk() || result.getData() == null
                    || !result.getData()
                    .getStatus()
                    .equals(ScanStatus.SUCCESS.getCode())) {
                int status = 2;
                // task 已经创建，注入codecc概览页面链接
                if (taskDetail != null) {
                    metricsVO.setCodeccUrl(String.format("%s/codecc/%s/task/%s/detail",
                            codeccHost,
                            taskDetail.getProjectId(),
                            taskDetail.getTaskId()));
                }
                // 注入任务状态和失败信息
                if (result.isOk() && result.getData() != null) {
                    status = result.getData().getStatus();
                    metricsVO.setMessage(result.getData().getErrMsg());
                }
                metricsVO.setStatus(status);
                return metricsVO;
            }
        }
        log.info("get fail task info by id cost {}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();

        if (taskResult.isNotOk() || taskDetail == null) {
            log.error("fail to get task fro metrics: taskResult is null, repoId: {}, buildId: {}",
                    repoId, buildId);
            throw new CodeCCException("NoTask", new String[]{taskResult.getMessage()});
        }

        // 生成度量信息
        if (StringUtils.isNotBlank(buildId)) {
            if (taskResult.isOk() && taskResult.getData() != null) {
                metricsVO = getMerticsByBuildId(taskDetail, buildId);
            }
        } else {
            metricsVO = getLastMetrics(taskDetail);
        }

        log.info("get metrics info cost {}", System.currentTimeMillis() - startTime);

        // 当拿不到度量信息时
        if (metricsVO == null) {
            metricsVO = new MetricsVO();
            log.info("metrics is null by repoId: {}, buildId: {}", repoId, buildId);
            metricsVO.setStatus(ScanStatus.FAIL.getCode());
            metricsVO.setMessage("can not get any metrics info");
            return metricsVO;
        }
        metricsVO.setCodeccUrl(String.format("%s/codecc/%s/task/%s/detail",
                codeccHost,
                taskDetail.getProjectId(),
                taskDetail.getTaskId()));
        metricsVO.setRepoUrl(String.format("%s/%s/commit/%s",
                gitHost,
                repoId,
                metricsVO.getCommitId()));
        log.info("total cost {}", System.currentTimeMillis() - st);
        return metricsVO;
    }

    private MetricsVO getLastMetrics(TaskDetailVO taskDetail) {
        long startTime = System.currentTimeMillis();
        MetricsEntity metricsEntity =
                metricsDao.findLastByTaskId(taskDetail.getTaskId());
        MetricsVO metricsVO = new MetricsVO();
        if (metricsEntity == null) {
            log.info("get null metrics");
            return null;
        }

        metricsVO.setStatus(StepFlag.SUCC.value());
        metricsVO.setTaskId(taskDetail.getTaskId());
        metricsVO.setProjectId(taskDetail.getProjectId());
        try {
            BeanUtils.copyProperties(metricsVO, metricsEntity);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        log.info("get latest metrics info cost {}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();
        // 调用defect模块的接口获取工具的最近一次分析结果
        List<ToolLastAnalysisResultVO> taskLogs = taskLogService
                .getAnalysisResultsByBuildId(taskDetail.getTaskId(), metricsEntity.getBuildId());
        log.info("get taskLog info cost {}", System.currentTimeMillis() - startTime);

        generateMetricsVO(metricsVO, taskLogs, true);
        return metricsVO;
    }

    private MetricsVO getMerticsByBuildId(TaskDetailVO taskDetail, String buildId) {
        long startTime = System.currentTimeMillis();
        MetricsEntity metricsEntity = metricsRepository.findFirstByTaskIdAndBuildId(taskDetail.getTaskId(),
                buildId);
        MetricsVO metricsVO = new MetricsVO();
        if (metricsEntity == null) {
            log.info("get null metrics by buildId: {}", buildId);
            return null;
        }

        try {
            BeanUtils.copyProperties(metricsVO, metricsEntity);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        metricsVO.setStatus(ScanStatus.SUCCESS.getCode());
        metricsVO.setTaskId(taskDetail.getTaskId());
        metricsVO.setProjectId(taskDetail.getProjectId());
        log.info("get metrics info by id cost {}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();
        List<ToolLastAnalysisResultVO> taskLogs = taskLogService
                .getAnalysisResultsByBuildId(taskDetail.getTaskId(), buildId);
        log.info("get taskLog info cost {}", System.currentTimeMillis() - startTime);

        generateMetricsVO(metricsVO, taskLogs, false);
        return metricsVO;
    }

    private void generateMetricsVO(MetricsVO metricsVO,
                                   List<ToolLastAnalysisResultVO> taskLogs,
                                   boolean changeStatus) {
        long startTime = System.currentTimeMillis();
        List<Analysis> lastAnalyzeResultList = new ArrayList<>();
        taskLogs.forEach(toolLastAnalysisResultVO -> {
            Analysis analysis = new Analysis();
            try {
                BeanUtils.copyProperties(analysis, toolLastAnalysisResultVO);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            ToolMetaBaseVO toolMeta =
                    toolMetaCacheService.getToolBaseMetaCache(toolLastAnalysisResultVO.getToolName());
            analysis.setPattern(toolMeta.getPattern());
            analysis.setDisplayName(toolMeta.getDisplayName());
            analysis.setType(toolMeta.getType());
            analysis.setDefectUrl(String.format("%s/codecc/%s/task/%s/defect%s/%s/list",
                    codeccHost,
                    metricsVO.getProjectId(),
                    metricsVO.getTaskId(),
                    (toolMeta.getPattern().equals(ToolPattern.COVERITY.name())
                            || toolMeta.getPattern().equals(ToolPattern.TSCLUA.name())
                            || toolMeta.getPattern().equals(ToolPattern.KLOCWORK.name()))
                            ? "/compile" :
                            (toolMeta.getPattern().equals(ToolPattern.CCN.name())
                                    || toolMeta.getPattern().equals(ToolPattern.DUPC.name())
                                    || toolMeta.getPattern().equals(ToolPattern.CLOC.name()))
                                    ? "" : "/" + toolMeta.getPattern().toLowerCase(),
                    toolMeta.getName()));
            setDefectCount(analysis, toolLastAnalysisResultVO);
            lastAnalyzeResultList.add(analysis);
            if (changeStatus && toolLastAnalysisResultVO.getFlag()
                    != StepFlag.SUCC.value()) {
                metricsVO.setStatus(toolLastAnalysisResultVO.getFlag());
            }

            if (toolLastAnalysisResultVO.getStartTime() < metricsVO.getLastAnalysisTime()
                    || metricsVO.getLastAnalysisTime() == 0) {
                metricsVO.setLastAnalysisTime(toolLastAnalysisResultVO.getStartTime());
            }
        });
        log.info("generate analyze info cost {}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();

        // 设置commitId
        Result<BuildIdRelationShipVO> result = client.get(ServiceBuildIdRelationshipResource.class)
                .getRelationShip(taskLogs.get(0).getBuildId());
        log.info("get commit id info cost {}", System.currentTimeMillis() - startTime);
        if (result.isOk() && result.getData() != null) {
            metricsVO.setCommitId(result.getData().getCommitId());
        }

        metricsVO.setLastAnalysisResultList(lastAnalyzeResultList);
        // 任务状态转换，从TaskLog状态转换为任务状态
        if (changeStatus) {
            switch (metricsVO.getStatus()) {
                case 1: {
                    metricsVO.setStatus(ScanStatus.SUCCESS.getCode());
                    break;
                }
                case 2:
                case 4: {
                    metricsVO.setStatus(ScanStatus.FAIL.getCode());
                    break;
                }
                default: {
                    metricsVO.setStatus(ScanStatus.PROCESSING.getCode());
                    break;
                }
            }
        }
    }

    /**
     * 告警数注入，根据工具类型的不同，抽取告警信息中的告警数展示
     * @param analysis
     * @param toolLastAnalysisResultVO
     */
    private void setDefectCount(Analysis analysis, ToolLastAnalysisResultVO toolLastAnalysisResultVO) {
        String thisClass = toolLastAnalysisResultVO.getLastAnalysisResultVO().getClass().getSimpleName();
        switch (thisClass) {
            case "LintLastAnalysisResultVO": {
                analysis.setDefectCount(((LintLastAnalysisResultVO)
                        toolLastAnalysisResultVO.getLastAnalysisResultVO())
                        .getDefectCount());
                break;
            }
            case "CCNLastAnalysisResultVO": {
                analysis.setDefectCount(((CCNLastAnalysisResultVO)
                        toolLastAnalysisResultVO.getLastAnalysisResultVO())
                        .getDefectCount());
                break;
            }
            case "DUPCLastAnalysisResultVO": {
                analysis.setDefectCount(((DUPCLastAnalysisResultVO)
                        toolLastAnalysisResultVO.getLastAnalysisResultVO())
                        .getDefectCount());
                break;
            }
            case "CommonLastAnalysisResultVO": {
                analysis.setDefectCount(
                        ((CommonLastAnalysisResultVO)
                                toolLastAnalysisResultVO.getLastAnalysisResultVO())
                                .getExistCount());
                break;
            }
            case "CLOCLastAnalysisResultVO": {
                analysis.setDefectCount(((CLOCLastAnalysisResultVO)
                        toolLastAnalysisResultVO.getLastAnalysisResultVO())
                        .getTotalLines().intValue());
                break;
            }
            default:
                break;
        }
    }

    @Override
    public MetricsVO getMetrics(Long taskId, String buildId) {
        MetricsEntity metricsEntity = metricsRepository.findFirstByTaskIdAndBuildId(taskId, buildId);
        MetricsVO metricsVO = new MetricsVO();
        if (metricsEntity == null) {
            return metricsVO;
        }

        try {
            BeanUtils.copyProperties(metricsVO, metricsEntity);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("", e);
        }

        return metricsVO;
    }
}
