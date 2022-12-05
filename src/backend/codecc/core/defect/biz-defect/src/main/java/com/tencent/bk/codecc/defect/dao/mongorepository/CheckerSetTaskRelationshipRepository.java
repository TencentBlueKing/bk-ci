package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetProjectRelationshipEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetTaskRelationshipEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Set;

/**
 * 规则集与其他对象关联持久化
 *
 * @version V1.0
 * @date 2020/1/5
 */
public interface CheckerSetTaskRelationshipRepository extends MongoRepository<CheckerSetTaskRelationshipEntity, String>
{
    /**
     * 根据规则集ID查询
     *
     * @param checkerSetId
     * @return
     */
    List<CheckerSetTaskRelationshipEntity> findByCheckerSetId(String checkerSetId);

    /**
     * 根据任务ID查询
     *
     * @param taskId
     * @return
     */
    List<CheckerSetTaskRelationshipEntity> findByTaskId(Long taskId);

    /**
     * 根据规则集ID列表查询
     *
     * @param checkerSetIds
     * @return
     */
    List<CheckerSetTaskRelationshipEntity> findByCheckerSetIdIn(Set<String> checkerSetIds);

	/**
     * 根据规则集ID，类型和关联方编码查询
     *
     * @param checkerSetId
     * @param taskId
     * @return
     */
    CheckerSetTaskRelationshipEntity findFirstByCheckerSetIdAndTaskId(String checkerSetId, Long taskId);

    /**
     * 通过项目id查询
     * @param projectId
     * @return
     */
    List<CheckerSetTaskRelationshipEntity> findByProjectId(String projectId);

    /**
     * 通过项目id查询
     * @param checkerSetId
     * @param projectIdSet
     * @return
     */
    List<CheckerSetTaskRelationshipEntity> findByCheckerSetIdAndProjectIdIn(String checkerSetId,
                                                                            Set<String> projectIdSet);
}
