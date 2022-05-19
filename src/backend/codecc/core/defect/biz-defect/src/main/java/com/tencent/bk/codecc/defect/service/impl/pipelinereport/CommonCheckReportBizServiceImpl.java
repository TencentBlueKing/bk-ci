package com.tencent.bk.codecc.defect.service.impl.pipelinereport;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.mongorepository.CLOCStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.model.NotRepairedAuthorEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.CompileSnapShotEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.ToolSnapShotEntity;
import com.tencent.bk.codecc.defect.service.ICheckReportBizService;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.CommonLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.service.ToolMetaCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Coverity流水线产出物报告实现类
 *
 * @version V1.0
 * @date 2019/12/11
 */
@Slf4j
@Service("CommonCheckerReportBizService")
public class CommonCheckReportBizServiceImpl implements ICheckReportBizService
{
    @Value("${bkci.public.url:#{null}}")
    private String DEVOPS_HOST;

    @Autowired
    private TaskLogService taskLogService;

    @Autowired
    private DefectRepository defectRepository;

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Autowired
    private CLOCStatisticRepository clocStatisticRepository;
    /**
     * 获取检查报告
     *
     * @param taskId
     * @param projectId
     * @param toolName
     * @return
     */
    @Override
    public ToolSnapShotEntity getReport(long taskId, String projectId, String toolName, String buildId)
    {
        CompileSnapShotEntity covReport = new CompileSnapShotEntity();

        handleToolBaseInfo(covReport, taskId, toolName, projectId, buildId);

        //分析开始和结束时间
        TaskLogVO lastTask = taskLogService.getLatestTaskLog(taskId, toolName);
        if (lastTask != null)
        {
            covReport.setStartTime(lastTask.getStartTime());
            covReport.setEndTime(lastTask.getEndTime());
        }

        //最近一次分析概要信息（新增数、关闭数、遗留数）
        BaseLastAnalysisResultVO lastAnalysisResultVO = taskLogService.getLastAnalysisResult(new ToolLastAnalysisResultVO(taskId, toolName), toolName);
        if (lastAnalysisResultVO == null)
        {
            return covReport;
        }
        CommonLastAnalysisResultVO newestCovData = (CommonLastAnalysisResultVO)lastAnalysisResultVO;
        covReport.setLatestNewAddCount(null == newestCovData.getNewCount() ? 0 : newestCovData.getNewCount());
        covReport.setLatestClosedCount(null == newestCovData.getCloseCount() ? 0 : newestCovData.getCloseCount());
        covReport.setLatestExistCount(null == newestCovData.getExistCount() ? 0 : newestCovData.getExistCount());

        // cloc加上总行数
        if (Tool.CLOC.name().equals(toolName)) {
            // 获取总行数
            List<CLOCStatisticEntity> clocStatisticEntityList = clocStatisticRepository.findByTaskIdAndToolName(taskId, toolName);
            log.info("get cloc entity list for task: {}, {}", taskId, buildId);
            long totalLines = 0;
            long totalBlankLines = 0;
            long totalCommentLines = 0;
            if (clocStatisticEntityList != null) {
                totalLines = clocStatisticEntityList.stream().mapToLong(CLOCStatisticEntity::getSumCode).sum();
                totalBlankLines = clocStatisticEntityList.stream().mapToLong(CLOCStatisticEntity::getSumBlank).sum();
                totalCommentLines = clocStatisticEntityList.stream()
                        .mapToLong(CLOCStatisticEntity::getSumComment)
                        .sum();
            }
            covReport.setTotalLines(totalLines);
            covReport.setTotalBlankLines(totalBlankLines);
            covReport.setTotalCommentLines(totalCommentLines);
        }

        //所有告警状态(待修复、已忽略、已屏蔽、已修复)
        List<DefectEntity> defectList = defectRepository.findByTaskIdAndToolName(taskId, toolName);
        if (CollectionUtils.isEmpty(defectList))
        {
            log.info("get defect list is empty, taskId: {}, toolName:{}", taskId, toolName);
            return covReport;
        }
        int totalNew = 0;
        int totalClose = 0;
        int totalIgnore = 0;
        int totalExcluded = 0;
        int newSerious = 0;
        int newNormal = 0;
        int newPrompt = 0;
        Map<String, NotRepairedAuthorEntity> notRepairedAuthorEntityMap = Maps.newHashMap();
        for (DefectEntity defectEntity : defectList)
        {
            // 遗留告警
            if (ComConstants.DefectStatus.NEW.value() == defectEntity.getStatus())
            {
                // 统计各作者告警信息
                if (CollectionUtils.isNotEmpty(defectEntity.getAuthorList()))
                {
                    for (String author : defectEntity.getAuthorList())
                    {
                        NotRepairedAuthorEntity notRepairedAuthorEntity = notRepairedAuthorEntityMap.get(author);
                        if (notRepairedAuthorEntityMap.get(author) == null)
                        {
                            notRepairedAuthorEntity = new NotRepairedAuthorEntity();
                            notRepairedAuthorEntity.setName(author);
                            notRepairedAuthorEntityMap.put(author, notRepairedAuthorEntity);
                        }
                        notRepairedAuthorEntity.setTotalCount(notRepairedAuthorEntity.getTotalCount() + 1);
                        if (ComConstants.SERIOUS == defectEntity.getSeverity())
                        {
                            notRepairedAuthorEntity.setSeriousCount(notRepairedAuthorEntity.getSeriousCount() + 1);
                        }
                        else if (ComConstants.NORMAL == defectEntity.getSeverity())
                        {
                            notRepairedAuthorEntity.setNormalCount(notRepairedAuthorEntity.getNormalCount() + 1);
                        }
                        else if (ComConstants.PROMPT == defectEntity.getSeverity())
                        {
                            notRepairedAuthorEntity.setPromptCount(notRepairedAuthorEntity.getPromptCount() + 1);
                        }
                    }
                }

                totalNew++;

                if (ComConstants.SERIOUS == defectEntity.getSeverity())
                {
                    newSerious++;
                }
                else if (ComConstants.NORMAL == defectEntity.getSeverity())
                {
                    newNormal++;
                }
                else if (ComConstants.PROMPT == defectEntity.getSeverity())
                {
                    newPrompt++;
                }
            }
            else
            {
                // 已修复告警
                if ((defectEntity.getStatus() | ComConstants.DefectStatus.FIXED.value()) > 0)
                {
                    totalClose++;
                }

                // 忽略告警
                if ((defectEntity.getStatus() | ComConstants.DefectStatus.IGNORE.value()) > 0)
                {
                    totalIgnore++;
                }

                // 屏蔽告警
                if ((defectEntity.getStatus() | ComConstants.DefectStatus.PATH_MASK.value()) > 0
                        || (defectEntity.getStatus() | ComConstants.DefectStatus.CHECKER_MASK.value()) > 0)
                {
                    totalExcluded++;
                }
            }
        }
        covReport.setTotalNew(totalNew);
        covReport.setTotalClose(totalClose);
        covReport.setTotalIgnore(totalIgnore);
        covReport.setTotalExcluded(totalExcluded);

        //待修复告警级别
        covReport.setTotalNewSerious(newSerious);
        covReport.setTotalNewNormal(newNormal);
        covReport.setTotalNewPrompt(newPrompt);

        //待修复告警作者
        if (MapUtils.isNotEmpty(notRepairedAuthorEntityMap))
        {
            covReport.setAuthorList(Lists.newArrayList(notRepairedAuthorEntityMap.values()));
        }

        return covReport;
    }

    /**
     * 设置工具报告基本信息
     *
     * @param compileSnapShotEntity
     * @param taskId
     * @param toolName
     * @param projectId
     */
    public void handleToolBaseInfo(CompileSnapShotEntity compileSnapShotEntity, long taskId, String toolName, String projectId, String buildId)
    {
        compileSnapShotEntity.setToolNameCn(toolMetaCacheService.getToolDisplayName(toolName));
        compileSnapShotEntity.setToolNameEn(toolName);
        if(StringUtils.isNotEmpty(projectId))
        {
            String defectDetailUrl = String.format("%s/console/codecc/%s/task/%d/defect/compile/%s/list?buildId=%s&status=7",
                    DEVOPS_HOST, projectId, taskId,
                    toolName, buildId);
            compileSnapShotEntity.setDefectDetailUrl(defectDetailUrl);
            String defectReportUrl = String.format("%s/console/codecc/%s/task/%d/defect/compile/%s/charts",
                    DEVOPS_HOST, projectId, taskId, toolName);
            compileSnapShotEntity.setDefectReportUrl(defectReportUrl);
        }
    }
}
