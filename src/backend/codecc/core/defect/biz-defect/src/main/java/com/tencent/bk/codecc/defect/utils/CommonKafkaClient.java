package com.tencent.bk.codecc.defect.utils;

import com.tencent.bk.codecc.defect.model.*;
import com.tencent.devops.common.kafka.KafkaClient;
import com.tencent.devops.common.kafka.KafkaTopic;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommonKafkaClient {

    @Autowired
    private KafkaClient kafkaClient;

    public void pushDefectEntityToKafka(List<DefectEntity> defectEntityList) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        if (defectEntityList.size() > 0) {
            defectEntityList.forEach(defectEntity -> {
                Map<String, Object> map = JsonUtil.INSTANCE.toMap(defectEntity);
                String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                map.put("washTime", dateString);
                mapList.add(map);
            });

        }

        try {
            kafkaClient.send(KafkaTopic.SINGLE_STATISTIC_TOPIC, JsonUtil.INSTANCE.toJson(mapList));
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 推送编译型工具统计信息到数据平台
     * @param commonStatisticEntity
     */
    public void pushCommonStatisticToKafka(CommonStatisticEntity commonStatisticEntity) {
        Map<String, Object> statisticMap = JsonUtil.INSTANCE.toMap(commonStatisticEntity);
        String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        statisticMap.put("washTime", dateString);

        try{
            kafkaClient.send(KafkaTopic.STATISTIC_TOPIC, JsonUtil.INSTANCE.toJson(statisticMap));
        } catch (Exception e) {
            log.error("send common statistic info to kafka failed!", e);
        }
    }

    /**
     * 推送lint类工具统计信息到数据平台
     * @param lintStatisticEntity
     */
    public void pushLintStatisticToKafka(LintStatisticEntity lintStatisticEntity) {
        Map<String, Object> statisticMap = JsonUtil.INSTANCE.toMap(lintStatisticEntity);
        String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        statisticMap.put("washTime", dateString);

        try{
            kafkaClient.send(KafkaTopic.LINT_STATISTIC_TOPIC, JsonUtil.INSTANCE.toJson(statisticMap));
        } catch (Exception e) {
            log.error("send common statistic info to kafka failed!", e);
        }
    }


    /**
     * 推送lint类工具统计信息到数据平台
     * @param ccnStatisticEntity
     */
    public void pushCCNStatisticToKafka(CCNStatisticEntity ccnStatisticEntity) {
        Map<String, Object> statisticMap = JsonUtil.INSTANCE.toMap(ccnStatisticEntity);
        String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        statisticMap.put("washTime", dateString);

        try{
            kafkaClient.send(KafkaTopic.CNN_STATISTIC_TOPIC, JsonUtil.INSTANCE.toJson(statisticMap));
        } catch (Exception e) {
            log.error("send common statistic info to kafka failed!", e);
        }
    }

    /**
     * 推送lint类工具统计信息到数据平台
     * @param dupcStatisticEntity
     */
    public void pushDUPCStatisticToKafka(DUPCStatisticEntity dupcStatisticEntity) {
        Map<String, Object> statisticMap = JsonUtil.INSTANCE.toMap(dupcStatisticEntity);
        String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        statisticMap.put("washTime", dateString);

        try{
            kafkaClient.send(KafkaTopic.DUPC_STATISTIC_TOPIC, JsonUtil.INSTANCE.toJson(statisticMap));
        } catch (Exception e) {
            log.error("send common statistic info to kafka failed!", e);
        }
    }


    /**
     * 推送代码行统计信息到数据平台
     * @param clocStatisticEntityList
     */
    public void pushCLOCStatisticToKafka(Collection<CLOCStatisticEntity> clocStatisticEntityList) {
        String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        if(CollectionUtils.isNotEmpty(clocStatisticEntityList))
        {
            List<Map<String, Object>> statisticMapList = clocStatisticEntityList.stream().map(clocStatisticEntity -> {
                Map<String, Object> statisticMap = JsonUtil.INSTANCE.toMap(clocStatisticEntity);
                statisticMap.put("washTime", dateString);
                return statisticMap;
            }).collect(Collectors.toList());
            try{
                kafkaClient.send("tendata-bkdevops-296-topic-cloc-statistic", JsonUtil.INSTANCE.toJson(statisticMapList));
            } catch (Exception e) {
                log.error("send common statistic info to kafka failed!", e);
            }
        }
    }
}
