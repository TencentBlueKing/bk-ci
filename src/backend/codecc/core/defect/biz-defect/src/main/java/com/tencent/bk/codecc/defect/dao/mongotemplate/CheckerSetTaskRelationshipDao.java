package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetProjectRelationshipEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetTaskRelationshipEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 规则集与其他对象关联DAO
 *
 * @version V1.0
 * @date 2020/1/5
 */
@Repository
public class CheckerSetTaskRelationshipDao
{
    @Autowired
    private MongoTemplate mongoTemplate;

    public void delete(String checkerSetId, int version, String type, String code)
    {
        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CheckerSetTaskRelationshipEntity.class);
        Query query = new Query();
        query.addCriteria(Criteria.where("checker_set_id").is(checkerSetId).and("version").is(version).and("type").is(type).and("code").is(code));
        Update update = new Update();
        ops.upsert(query, update);
        ops.execute();
    }
}
