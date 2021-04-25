package com.tencent.bk.codecc.defect.service.statistic;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.redis.StatisticDao;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.pojo.CommonDefectStatisticModel;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class CommonDefectStatisticService {
    @Autowired
    private StatisticDao statisticDao;

    /**
     * COV/KW/PP类工具告警统计，结合newDefectJudgeTime逻辑统计新旧
     *
     * @param newDefectJudgeTime
     * @param defectList
     * @return
     */
    public CommonDefectStatisticModel statistic(long newDefectJudgeTime, List<DefectEntity> defectList) {
        int existPromptCount = 0;
        int existNormalCount = 0;
        int existSeriousCount = 0;
        int newPromptCount = 0;
        int newNormalCount = 0;
        int newSeriousCount = 0;

        Set<String> newAuthors = Sets.newHashSet();
        Set<String> newPromptAuthors = Sets.newHashSet();
        Set<String> newNormalAuthors = Sets.newHashSet();
        Set<String> newSeriousAuthors = Sets.newHashSet();
        // 仅指存量告警的处理人；原来的existAuthors指新+存量，2021.03.11
        Set<String> existAuthors = Sets.newHashSet();
        // 存量不同级别告警处理人
        Set<String> existPromptAuthors = Sets.newHashSet();
        Set<String> existNormalAuthors = Sets.newHashSet();
        Set<String> existSeriousAuthors = Sets.newHashSet();

        for (DefectEntity defectEntity : defectList) {
            if (ComConstants.DefectStatus.NEW.value() != defectEntity.getStatus()) {
                continue;
            }

            Set<String> authors = CollectionUtils.isEmpty(defectEntity.getAuthorList())
                    ? new HashSet<>() : defectEntity.getAuthorList();

            if (defectEntity.getCreateTime() > newDefectJudgeTime) {
                newAuthors.addAll(authors);
                if ((defectEntity.getSeverity() & ComConstants.PROMPT) > 0) {
                    newPromptCount++;
                    existPromptCount++;
                    newPromptAuthors.addAll(authors);
                }
                if ((defectEntity.getSeverity() & ComConstants.NORMAL) > 0) {
                    newNormalCount++;
                    existNormalCount++;
                    newNormalAuthors.addAll(authors);
                }
                if ((defectEntity.getSeverity() & ComConstants.SERIOUS) > 0) {
                    newSeriousCount++;
                    existSeriousCount++;
                    newSeriousAuthors.addAll(authors);
                }
            } else {
                existAuthors.addAll(authors);
                if ((defectEntity.getSeverity() & ComConstants.PROMPT) > 0) {
                    existPromptCount++;
                    existPromptAuthors.addAll(authors);
                }
                if ((defectEntity.getSeverity() & ComConstants.NORMAL) > 0) {
                    existNormalCount++;
                    existNormalAuthors.addAll(authors);
                }
                if ((defectEntity.getSeverity() & ComConstants.SERIOUS) > 0) {
                    existSeriousCount++;
                    existSeriousAuthors.addAll(authors);
                }
            }
        }

        return new CommonDefectStatisticModel(
                newPromptCount, newNormalCount, newSeriousCount,
                existPromptCount, existNormalCount, existSeriousCount,
                newAuthors, newPromptAuthors, newNormalAuthors, newSeriousAuthors,
                existAuthors, existPromptAuthors, existNormalAuthors, existSeriousAuthors
        );
    }

    /**
     * 保存统计信息至redis
     *
     * @param statistic
     * @param taskId
     * @param toolName
     * @param buildNum
     */
    public void saveStatisticToRedis(CommonDefectStatisticModel statistic, long taskId, String toolName,
                                     String buildNum) {
        List<Pair<ComConstants.StaticticItem, Integer>> counterList = Arrays.asList(
            ImmutablePair.of(ComConstants.StaticticItem.EXIST_PROMPT, statistic.getExistPromptCount()),
            ImmutablePair.of(ComConstants.StaticticItem.EXIST_NORMAL, statistic.getExistNormalCount()),
            ImmutablePair.of(ComConstants.StaticticItem.EXIST_SERIOUS, statistic.getExistSeriousCount()),
            ImmutablePair.of(ComConstants.StaticticItem.NEW_PROMPT, statistic.getNewPromptCount()),
            ImmutablePair.of(ComConstants.StaticticItem.NEW_NORMAL, statistic.getNewNormalCount()),
            ImmutablePair.of(ComConstants.StaticticItem.NEW_SERIOUS, statistic.getNewSeriousCount()));

        statisticDao.increaseDefectCountByStatusBatch(taskId, toolName, buildNum, counterList);
        statisticDao.addNewAndExistAuthors(taskId, toolName, buildNum, statistic.getNewAuthors(),
                statistic.getExistAuthors());
        statisticDao.addSeverityAuthors(taskId, toolName, buildNum, statistic.getNewPromptAuthors(),
                statistic.getNewNormalAuthors(), statistic.getNewSeriousAuthors());
        statisticDao.addExistSeverityAuthors(taskId, toolName, buildNum, statistic.getExistPromptAuthors(),
                statistic.getExistNormalAuthors(), statistic.getExistSeriousAuthors());
    }
}
