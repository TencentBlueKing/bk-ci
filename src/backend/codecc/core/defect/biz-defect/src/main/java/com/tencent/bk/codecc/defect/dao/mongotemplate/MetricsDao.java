package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.MetricsEntity;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class MetricsDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    public MetricsEntity findLastByTaskId(Long taskId) {
        Criteria criteria = Criteria.where("task_id").is(taskId);
        Query query = Query.query(criteria);
        Sort sort = Sort.by(Direction.DESC, "_id");
        query.with(sort);
        query.limit(1);
        List<MetricsEntity> metricsEntityList = mongoTemplate.find(query, MetricsEntity.class);
        if (metricsEntityList.isEmpty()) {
            return null;
        }
        return metricsEntityList.get(0);
    }

    public boolean upsert(MetricsEntity metricsEntity) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(metricsEntity.getTaskId())
                .and("build_id").is(metricsEntity.getBuildId()));

        Update update = new Update();
        update.set("task_id", metricsEntity.getTaskId())
                .set("build_id", metricsEntity.getBuildId())
                .set("code_style_score", metricsEntity.getCodeStyleScore())
                .set("code_security_score", metricsEntity.getCodeSecurityScore())
                .set("code_measure_score", metricsEntity.getCodeMeasureScore())
                .set("rd_indicators_score", metricsEntity.getRdIndicatorsScore())
                .set("is_open_scan", metricsEntity.isOpenScan())
                .set("code_ccn_score", metricsEntity.getCodeCcnScore())
                .set("code_defect_score", metricsEntity.getCodeDefectScore())
                .set("average_thousand_defect", metricsEntity.getAverageThousandDefect())
                .set("code_style_normal_defect_count", metricsEntity.getCodeStyleNormalDefectCount())
                .set("code_style_serious_defect_count", metricsEntity.getCodeStyleSeriousDefectCount())
                .set("code_defect_normal_defect_count", metricsEntity.getCodeDefectNormalDefectCount())
                .set("code_defect_serious_defect_count", metricsEntity.getCodeDefectSeriousDefectCount())
                .set("code_security_normal_defect_count", metricsEntity.getCodeSecurityNormalDefectCount())
                .set("code_security_serious_defect_count", metricsEntity.getCodeSecuritySeriousDefectCount());
        mongoTemplate.upsert(query, update, MetricsEntity.class);
        return true;
    }
}
