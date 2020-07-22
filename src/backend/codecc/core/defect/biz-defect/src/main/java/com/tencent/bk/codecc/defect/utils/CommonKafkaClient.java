package com.tencent.bk.codecc.defect.utils;

import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.devops.common.kafka.KafkaClient;
import com.tencent.devops.common.kafka.KafkaTopic;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
}
