package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.GongFengTriggerParamEntity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GongfengTriggerParamRepository extends MongoRepository<GongFengTriggerParamEntity, ObjectId> {

    GongFengTriggerParamEntity findFirstByGongfengId(Integer gongfengId);
}
