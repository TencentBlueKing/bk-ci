package com.tencent.bk.codecc.codeccjob.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.MetricsEntity;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public class MetricsDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    public boolean upsert(MetricsEntity metricsEntity) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id")
                .is(metricsEntity.getTaskId())
                .and("build_id")
                .is(metricsEntity.getBuildId()));

        Update update = new Update();
        update.set("task_id", metricsEntity.getTaskId())
                .set("build_id", metricsEntity.getBuildId())
                .set("code_style_score", metricsEntity.getCodeStyleScore())
                .set("code_security_score", metricsEntity.getCodeSecurityScore())
                .set("code_measure_score", metricsEntity.getCodeMeasureScore())
                .set("rd_indicators_score", metricsEntity.getRdIndicatorsScore());
        mongoTemplate.upsert(query, update, MetricsEntity.class);
        return true;
    }

    /**
     * 批量更新度量数据
     * @param metricsEntities
     */
    public boolean batchUpsert(Collection<MetricsEntity> metricsEntities) {
        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, MetricsEntity.class);
        if (CollectionUtils.isNotEmpty(metricsEntities)) {
            for (MetricsEntity metricsEntity : metricsEntities) {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id")
                        .is(metricsEntity.getTaskId())
                        .and("build_id")
                        .is(metricsEntity.getBuildId()));

                Update update = new Update();
                update.set("task_id", metricsEntity.getTaskId())
                        .set("build_id", metricsEntity.getBuildId())
                        .set("code_style_score", metricsEntity.getCodeStyleScore())
                        .set("code_security_score", metricsEntity.getCodeSecurityScore())
                        .set("code_measure_score", metricsEntity.getCodeMeasureScore())
                        .set("rd_indicators_score", metricsEntity.getRdIndicatorsScore());

                ops.upsert(query, update);
            }
            ops.execute();
        }
        return Boolean.TRUE;
    }
}

