package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetHisEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 规则集持久化
 *
 * @version V4.0
 * @date 2019/10/29
 */
@Repository
public interface CheckerSetHisRepository extends MongoRepository<CheckerSetHisEntity, String> {

    void deleteByToolNameAndVersion(String toolName, int version);

    void deleteByToolNameAndVersionNot(String toolName, int version);

    List<CheckerSetHisEntity> findByToolNameInAndVersion(String toolName, int version);
}
