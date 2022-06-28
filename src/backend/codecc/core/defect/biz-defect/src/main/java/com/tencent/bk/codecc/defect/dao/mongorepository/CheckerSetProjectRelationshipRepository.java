package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetProjectRelationshipEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Set;

/**
 * 规则集与其他对象关联持久化
 *
 * @version V1.0
 * @date 2020/1/5
 */
public interface CheckerSetProjectRelationshipRepository extends MongoRepository<CheckerSetProjectRelationshipEntity,
        String> {
    /**
     * 根据规则集ID，关联方编码查询
     *
     * @param checkerSetId
     * @param projectId
     * @return
     */
    CheckerSetProjectRelationshipEntity findFirstByCheckerSetIdAndProjectId(String checkerSetId, String projectId);

    /**
     * 根据规则集ID，类型查询
     *
     * @param checkerSetId
     * @return
     */
    List<CheckerSetProjectRelationshipEntity> findByCheckerSetId(String checkerSetId);

    /**
     * 根据类型，规则集ID列表查询
     *
     * @param checkerSetIds
     * @return
     */
    List<CheckerSetProjectRelationshipEntity> findByCheckerSetIdIn(Set<String> checkerSetIds);

    /**
     * 根据规则集ID列表及项目ID查询
     *
     * @param checkerSetIds
     * @return
     */
    List<CheckerSetProjectRelationshipEntity> findByCheckerSetIdInAndProjectId(Set<String> checkerSetIds,
                                                                               String projectId);

    /**
     * 通过项目id查找
     *
     * @param projectId
     * @return
     */
    List<CheckerSetProjectRelationshipEntity> findByProjectId(String projectId);

    /**
     * 根据规则集ID，是否使用最新版本查询
     *
     * @param checkerSetId
     * @return
     */
    List<CheckerSetProjectRelationshipEntity> findByCheckerSetIdAndUselatestVersion(String checkerSetId,
                                                                                    Boolean uselatestVersion);
}
