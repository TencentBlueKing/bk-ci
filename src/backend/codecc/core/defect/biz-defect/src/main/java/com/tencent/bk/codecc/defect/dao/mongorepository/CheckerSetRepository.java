package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.devops.common.constant.ComConstants;
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
public interface CheckerSetRepository extends MongoRepository<CheckerSetEntity, String>
{
    /**
     * ---------已废弃--------通过工具名称查询
     *
     * @param toolName
     * @return
     */
    List<CheckerSetEntity> findByToolName(String toolName);

    /**
     * @param toolName
     * @param version
     * @return
     */
    List<CheckerSetEntity> findByToolNameAndVersion(String toolName, int version);

    /**
     * ---------已废弃--------通过工具名称、规则集ID、规则集版本号查询
     *
     * @param toolName
     * @param checkerSetId
     * @param version
     * @return
     */
    CheckerSetEntity findFirstByToolNameAndCheckerSetIdAndVersion(String toolName, String checkerSetId, int version);

    /**
     * ---------已废弃--------通过工具名称、规则集ID查询
     *
     * @param toolName
     * @param checkerSetId
     * @return
     */
    List<CheckerSetEntity> findByToolNameAndCheckerSetId(String toolName, String checkerSetId);

    /**
     * ---------已废弃--------通过工具名称、创建人查询
     *
     * @param toolName
     * @param creator
     * @return
     */
    List<CheckerSetEntity> findByToolNameAndCreator(String toolName, String creator);

    /**
     * ---------已废弃--------通过工具名称和规则集名称查询
     *
     * @param toolName
     * @param checkerSetName
     * @return
     */
    List<CheckerSetEntity> findByToolNameAndCheckerSetName(String toolName, String checkerSetName);

    /**
     * 通过规则集ID查询
     *
     * @param checkerSetId
     * @return
     */
    List<CheckerSetEntity> findByCheckerSetId(String checkerSetId);

    /**
     * 通过规则集ID和版本号查询
     *
     * @param checkerSetId
     * @param version
     * @return
     */
    CheckerSetEntity findFirstByCheckerSetIdAndVersion(String checkerSetId, int version);

    /**
     * 通过规则集ID和版本号批量查询
     *
     * @param checkerSetIds
     * @param version
     * @return
     */
    List<CheckerSetEntity> findByCheckerSetIdInAndVersion(Collection<String> checkerSetIds, Integer version);

    /**
     * 通过规则集ID列表查询
     *
     * @param checkerSetIds
     * @return
     */
    List<CheckerSetEntity> findByCheckerSetIdIn(Collection<String> checkerSetIds);

    /**
     * 根据规则集ID或者规则集名称查询
     *
     * @param checkerSetId
     * @param checkerSetName
     * @return
     */
    List<CheckerSetEntity> findByCheckerSetIdOrCheckerSetName(String checkerSetId, String checkerSetName);

    /**
     * 根据可见范围查询
     *
     * @param scope
     * @return
     */
    List<CheckerSetEntity> findByScope(Integer scope);

    /**
     * 根据项目ID查询
     *
     * @param projectId
     * @return
     */
    List<CheckerSetEntity> findByProjectId(String projectId);

    /**
     * 根据规则集id和version进行删除
     *
     * @return
     */
    void deleteByCheckerSetIdInAndVersion(Collection<String> checkerSetIds, int version);
}
