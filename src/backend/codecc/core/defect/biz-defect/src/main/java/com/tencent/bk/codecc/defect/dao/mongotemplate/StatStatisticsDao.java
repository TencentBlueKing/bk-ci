package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.StatStatisticEntity;
import java.util.List;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StatStatisticsDao {

    @Autowired MongoTemplate mongoTemplate;

    public boolean batchSave(List<Document> statisticEntityList) {
        if (statisticEntityList.isEmpty()) {
            return false;
        }
        mongoTemplate.insert(statisticEntityList, StatStatisticEntity.class);
        return true;
    }
}
