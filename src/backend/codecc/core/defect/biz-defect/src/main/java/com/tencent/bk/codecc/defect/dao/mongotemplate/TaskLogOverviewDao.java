package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.TaskLogOverviewEntity;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.utils.PageableUtils;

import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class TaskLogOverviewDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    public List<TaskLogOverviewEntity> getTaskLogOverviewList(Long taskId, Pageable pageable) {

        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId));
        query.with(pageable);
        return mongoTemplate.find(query, TaskLogOverviewEntity.class);
    }

    /**
     * 查询任务维度的分析次数
     *
     * @param taskIds   任务ID集合
     * @param status    分析状态
     * @param startTime 开始范围
     * @param endTime   结束范围
     * @return long
     */
    public Long queryTaskAnalyzeCount(Collection<Long> taskIds, Integer status, Long startTime, Long endTime) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").in(taskIds));
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (startTime != null) {
            query.addCriteria(Criteria.where("start_time").gte(startTime).lte(endTime));
        }

        return mongoTemplate.count(query, TaskLogOverviewEntity.class);
    }

}
