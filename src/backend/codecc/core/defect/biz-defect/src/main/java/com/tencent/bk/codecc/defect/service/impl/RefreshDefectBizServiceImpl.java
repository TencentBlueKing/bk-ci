package com.tencent.bk.codecc.defect.service.impl;

import com.mongodb.bulk.BulkWriteResult;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CLOCStatisticsDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.DefectDao;
import com.tencent.bk.codecc.defect.model.CLOCDefectEntity;
import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.service.RefreshDefectBizService;
import com.tencent.devops.common.util.StringCompress;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RefreshDefectBizServiceImpl implements RefreshDefectBizService {

    private static Logger logger = LoggerFactory.getLogger(RefreshDefectBizServiceImpl.class);

    @Autowired
    private DefectDao defectDao;

    @Autowired
    private CLOCStatisticsDao clocStatisticsDao;

    @Override
    public String freshClocDefectByPage(Set<Long> taskIds) {
        List<CLOCDefectEntity> clocEntities = defectDao.batchQueryClocDefect(null, taskIds);
        List<Long> taskIdList = clocEntities.stream().map(CLOCDefectEntity::getTaskId).collect(Collectors.toList());
        Set<Long> existsTaskIdList = clocStatisticsDao.batchQueryClocStaticByTaskId(taskIdList).stream()
            .map(CLOCStatisticEntity::getTaskId).collect(Collectors.toSet());
        logger.info("exist task id list are {}", existsTaskIdList);

        // 按照taskid分组
        Map<Long, List<CLOCDefectEntity>> clocEntitiyMap = new HashMap<>();
        for (CLOCDefectEntity entity: clocEntities) {
            if (existsTaskIdList.contains(entity.getTaskId())) continue;
            List<CLOCDefectEntity> subClocEntities = clocEntitiyMap.get(entity.getTaskId());
            if (subClocEntities == null) subClocEntities = new ArrayList<>();
            subClocEntities.add(entity);
            clocEntitiyMap.put(entity.getTaskId(), subClocEntities);
        }

        // 然后再按照语言分组
        StringBuilder resultStr = new StringBuilder();
        for (Map.Entry<Long, List<CLOCDefectEntity>> entry: clocEntitiyMap.entrySet()) {
            Map<String, CLOCStatisticEntity> langCountMap = new HashMap<>();
            for (CLOCDefectEntity clocEntry : entry.getValue()) {
                CLOCStatisticEntity countItem = langCountMap.get(clocEntry.getLanguage());
                if (countItem == null) {
                    countItem = new CLOCStatisticEntity();
                    countItem.setStreamName(clocEntry.getStreamName());
                    countItem.setTaskId(clocEntry.getTaskId());
                    countItem.setLanguage(clocEntry.getLanguage());
                    countItem.setSumBlank(0L);
                    countItem.setSumCode(0L);
                    countItem.setSumComment(0L);
                    countItem.setToolName(clocEntry.getLanguage());
                }
                countItem.setSumBlank(countItem.getSumBlank() + clocEntry.getBlank());
                countItem.setSumCode(countItem.getSumCode() + clocEntry.getCode());
                countItem.setSumComment(countItem.getSumComment() + clocEntry.getComment());
                langCountMap.put(clocEntry.getLanguage(), countItem);
            }

            // 写入数据
            logger.info("start to update task {}", entry.getKey());
            BulkWriteResult result = clocStatisticsDao.batchUpsertCLOCStatistic(langCountMap.values());
            resultStr.append("success to insert in task with result: ")
                .append(entry.getKey())
                .append(", ")
                .append(result.toString())
                .append("\n");
        }

        return resultStr.toString();
    }
}
