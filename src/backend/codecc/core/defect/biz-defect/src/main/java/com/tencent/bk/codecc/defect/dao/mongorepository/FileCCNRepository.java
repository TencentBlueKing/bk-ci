package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.FileCCNEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileCCNRepository extends MongoRepository<FileCCNEntity, String>
{
    /**
     * 根据任务ID查询
     *
     * @param taskId
     * @return
     */
    List<FileCCNEntity> findByTaskId(Long taskId);

    /**
     * 根据任务ID删除
     *
     * @param taskId
     */
    void deleteByTaskId(Long taskId);

    /**
     * 根据任务ID和文件路径列表删除
     *
     * @param taskId
     * @param filePathList
     */
    void deleteByTaskIdIsAndFilePathIn(Long taskId, List<String> filePathList);
}
