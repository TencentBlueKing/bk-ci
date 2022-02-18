package com.tencent.bk.codecc.defect.service.impl.pipelinereport;

import com.tencent.bk.codecc.defect.dao.mongorepository.CLOCStatisticRepository;
import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.ClocSnapShotEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.ToolSnapShotEntity;
import com.tencent.bk.codecc.defect.service.ICheckReportBizService;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.devops.common.service.ToolMetaCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service("CLOCCheckerReportBizService")
public class CLOCCheckReportBizServiceImpl implements ICheckReportBizService {
    @Value("${bkci.public.url:#{null}}")
    private String devopsHost;

    @Autowired
    private TaskLogService taskLogService;

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Autowired
    private CLOCStatisticRepository clocStatisticRepository;

    @Override
    public ToolSnapShotEntity getReport(long taskId, String projectId, String toolName, String buildId) {
        ClocSnapShotEntity clocSnapShotEntity = new ClocSnapShotEntity();
        log.info("start to save cloc snap shot info, taskId: {} | projectId: {} | toolName: {} | buildId: {}",
                taskId, projectId, toolName, buildId);
        // 注入快照基本信息
        handleToolBaseInfo(clocSnapShotEntity, taskId, toolName, projectId);

        // 获取CLOC任务日志,保存任务开始、结束时间到快照信息
        TaskLogVO lastTaskLog = taskLogService.getLatestTaskLog(taskId, toolName);
        if (lastTaskLog != null) {
            log.info("get cloc time info from task log to snapshot, taskId: {} | projectId: {} | buildId: {}",
                    taskId, projectId, buildId);
            clocSnapShotEntity.setStartTime(lastTaskLog.getStartTime());
            clocSnapShotEntity.setEndTime(lastTaskLog.getEndTime());
        }

        // 获取CLOC工具统计结果信息
        List<CLOCStatisticEntity> clocStatisticEntityList =
                clocStatisticRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);

        if (CollectionUtils.isEmpty(clocStatisticEntityList)) {
            log.warn("cloc statistic entity is null, taskId: {} | buildId: {}",
                    taskId, buildId);
            return clocSnapShotEntity;
        }

        long totalBlank = 0;
        long totalCode = 0;
        long totalComment = 0;
        long totalBlankChange = 0;
        long totalCodeChange = 0;
        long totalCommentChange = 0;

        for (CLOCStatisticEntity  statisticEntity: clocStatisticEntityList
             ) {
            // 计算各语言的空白行、代码行、注释行总和
            totalBlank += statisticEntity.getSumBlank();
            totalCode += statisticEntity.getSumCode();
            totalComment += statisticEntity.getSumComment();
            // 计算各语言的空白行变化、代码行变化、注释行总和
            totalBlankChange += statisticEntity.getBlankChange();
            totalCodeChange += statisticEntity.getCodeChange();
            totalCommentChange += statisticEntity.getCommentChange();
        }

        clocSnapShotEntity.setTotalBlank(totalBlank);
        clocSnapShotEntity.setTotalCode(totalCode);
        clocSnapShotEntity.setTotalComment(totalComment);

        clocSnapShotEntity.setTotalBlankChange(totalBlankChange);
        clocSnapShotEntity.setTotalCodeChange(totalCodeChange);
        clocSnapShotEntity.setTotalCommentChange(totalCommentChange);

        // 注入行数、行数变化总和
        clocSnapShotEntity.setTotalLines(totalBlank + totalCode + totalComment);
        clocSnapShotEntity.setTotalChange(totalBlankChange + totalCodeChange
                + totalCommentChange);
        return clocSnapShotEntity;
    }

    private void handleToolBaseInfo(ClocSnapShotEntity clocSnapShotEntity, long taskId,
            String toolName, String projectId) {
        clocSnapShotEntity.setToolNameCn(toolMetaCacheService.getToolDisplayName(toolName));
        clocSnapShotEntity.setToolNameEn(toolName);
        if (StringUtils.isNotEmpty(projectId))
        {
            String defectDetailUrl = String.format("%s/console/codecc/%s/task/%s/defect/cloc/language",
                    devopsHost, projectId, taskId);
            clocSnapShotEntity.setDefectDetailUrl(defectDetailUrl);
            String defectReportUrl = String.format("%s/console/codecc/%s/task/%s/defect/cloc/language",
                    devopsHost, projectId, taskId);
            clocSnapShotEntity.setDefectReportUrl(defectReportUrl);
        }
    }
}
