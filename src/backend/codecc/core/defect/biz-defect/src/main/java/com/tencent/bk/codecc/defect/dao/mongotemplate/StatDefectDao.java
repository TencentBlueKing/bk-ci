package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.StatDefectEntity;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class StatDefectDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 批量失效告警
     *
     * @param taskId 任务
     * @param toolName 工具
     * */
    public void batchDisableStatInfo(long taskId, String toolName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId).and("tool_name").is(toolName).and("status").is("ENABLED"));
        Update update = new Update();
        update.set("status", "DISABLED");
        mongoTemplate.updateMulti(query, update, StatDefectEntity.class);
    }

    /**
     * 批量写入增量告警
     *
     * @param taskId 任务ID
     * @param toolName 工具名
     * @param customTollParam 自定义参数集
     * @param defectEntityList 告警列表
     * */
    public void upsert(long taskId, String toolName, List<StatDefectEntity> defectEntityList, Map<String, String> customTollParam) {
        if (CollectionUtils.isNotEmpty(defectEntityList) && !customTollParam.isEmpty())
        {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, StatDefectEntity.class);
            defectEntityList.forEach(defectEntity ->
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(taskId).and("tool_name").is(toolName));
                Update update = new Update();
                update.set("status", "ENABLED");
                customTollParam.keySet().forEach(field -> update.set(field, defectEntity.get(field)));
                ops.upsert(query, update);
            });
            ops.execute();
        }
    }

    public List<Document> getByTaskIdAndToolNameAndTime(long taskId, String toolName) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("task_id").is(taskId).and("tool_name").is(toolName).and("status").is("ENABLED"));
        return mongoTemplate.find(query, Document.class, "t_stat_defect");
    }

    public List<StatDefectEntity> getByTaskIdAndToolNameOrderByTime(long taskId, String toolName) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("task_id").is(taskId).and("tool_name").is(toolName).and("status").is("ENABLED"));
        query.with(Sort.by(new Order(Direction.DESC, "time_stamp")));
        query.limit(1);
        return mongoTemplate.find(query, StatDefectEntity.class, "t_stat_defect");
    }
}
