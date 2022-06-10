package com.tencent.bk.codecc.defect.consumer;

import com.tencent.bk.codecc.defect.dao.mongorepository.StatStatisticRepository;
import com.tencent.bk.codecc.defect.model.StatStatisticEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StatFastIncrementConsumer extends AbstractFastIncrementConsumer {

    @Autowired
    private StatStatisticRepository statStatisticRepository;

    @Override protected void generateResult(AnalyzeConfigInfoVO analyzeConfigInfoVO) {
        long taskId = analyzeConfigInfoVO.getTaskId();
        String toolName = analyzeConfigInfoVO.getMultiToolType();
        String buildId = analyzeConfigInfoVO.getBuildId();

        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository
                .findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);

        // 因为代码没有变更，默认代码统计不变，所以直接取上一个分析的代码统计
        String baseBuildId;
        if (toolBuildStackEntity == null) {
            log.info("last success stat task build is null, taskId {} | buildId | {} | toolName {}", taskId, buildId,
                    toolName);
            ToolBuildInfoEntity toolBuildINfoEntity = toolBuildInfoRepository.findFirstByTaskIdAndToolName(taskId, toolName);
            baseBuildId =
                    toolBuildINfoEntity != null && StringUtils.isNotEmpty(toolBuildINfoEntity.getDefectBaseBuildId())
                            ? toolBuildINfoEntity.getDefectBaseBuildId() : "";
        } else {
            log.info("last success stat task build is {}, taskId {} | buildId | {} | toolName {}",
                    toolBuildStackEntity.getBuildId(), taskId, buildId, toolName);
            baseBuildId = StringUtils.isNotEmpty(toolBuildStackEntity.getBaseBuildId()) ? toolBuildStackEntity
                    .getBaseBuildId() : "";
        }
        List<StatStatisticEntity> lastStatStatisticEntityList = statStatisticRepository
                .findByTaskIdAndBuildIdAndToolName(taskId, baseBuildId, toolName);

        lastStatStatisticEntityList.forEach(statStatisticEntity -> {
            statStatisticEntity.setEntityId(ObjectId.get().toString());
            statStatisticEntity.setBuildId(buildId);
        });

        statStatisticRepository.saveAll(lastStatStatisticEntityList);

        // 更新告警快照基准构建ID
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);
    }
}
