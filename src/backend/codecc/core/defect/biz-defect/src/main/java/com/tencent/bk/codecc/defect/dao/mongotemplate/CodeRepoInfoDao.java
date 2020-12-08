package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.TaskLogGroupEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * 代码仓库信息DAO
 *
 * @version V4.0
 * @date 2019/10/16
 */
@Repository
@Slf4j
public class CodeRepoInfoDao
{
    @Autowired
    private MongoTemplate mongoTemplate;


    public List<CodeRepoInfoEntity> findFirstByTaskIdOrderByCreatedDate(Set<Long> taskIds)
    {
        //以taskid进行过滤
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIds));
        //根据创建时间排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "create_date");

        //以toolName进行分组，并且取第一个的endTime字段
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("repo_list").as("repo_list");
        Aggregation agg = Aggregation.newAggregation(match, sort, group);
        AggregationResults<CodeRepoInfoEntity> queryResult = mongoTemplate.aggregate(agg, "t_code_repo_info", CodeRepoInfoEntity.class);
        return queryResult.getMappedResults();
    }

}
