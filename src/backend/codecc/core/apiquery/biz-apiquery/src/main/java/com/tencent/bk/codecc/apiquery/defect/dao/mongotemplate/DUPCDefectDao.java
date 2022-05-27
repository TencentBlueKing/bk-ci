/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate;

import com.tencent.bk.codecc.apiquery.defect.model.DUPCDefectModel;
import com.tencent.bk.codecc.apiquery.defect.model.DUPCStatisticModel;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * DUPC类告警的查詢持久化
 *
 * @version V1.0
 * @date 2019/5/21
 */
@Repository
public class DUPCDefectDao {
    @Autowired
    @Qualifier("defectMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 批量获取最新分析统计数据
     *
     * @param taskIdSet 任务ID集合
     * @param toolName  工具名称
     * @return list
     */
    public List<DUPCStatisticModel> batchFindByTaskIdInAndTool(Collection<Long> taskIdSet, String toolName) {
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIdSet).and("tool_name").is(toolName));
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "time");
        // 以taskId进行分组
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("tool_name").as("tool_name")
                .first("time").as("time")
                .first("defect_count").as("defect_count")
                .first("defect_change").as("defect_change")
                .first("last_defect_count").as("last_defect_count")
                .first("dup_rate").as("dup_rate")
                .first("last_dup_rate").as("last_dup_rate")
                .first("super_high_count").as("super_high_count")
                .first("high_count").as("high_count")
                .first("medium_count").as("medium_count");
        Aggregation agg = Aggregation.newAggregation(match, sort, group);

        AggregationResults<DUPCStatisticModel> queryResult =
                mongoTemplate.aggregate(agg, "t_dupc_statistic", DUPCStatisticModel.class);
        return queryResult.getMappedResults();
    }

    /**
     * 根据任务ID，作者和路径列表查询
     *
     * @param taskId   任务ID
     * @param author   作者筛选
     * @param fileList 文件路径筛选
     * @return list
     */
    public List<DUPCDefectModel> findByTaskIdAndAuthorAndRelPaths(long taskId, String author, Set<String> fileList) {
        Document fieldsObj = new Document();
        fieldsObj.put("block_list", false);
        Query query = new BasicQuery(new Document(), fieldsObj);
        query.addCriteria(Criteria.where("task_id").is(taskId).and("status").is(ComConstants.DefectStatus.NEW.value()));

        //作者过滤
        if (StringUtils.isNotEmpty(author)) {
            // 去掉人名的字符
            String authorParam = author.trim();
            if (author.contains("(") && author.endsWith(")")) {
                authorParam = author.substring(0, author.indexOf("("));
            }
            Pattern pattern = Pattern.compile(String.format("^.*%s.*$", authorParam), Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("author_list").regex(pattern));
        }

        //路径过滤
        List<Criteria> criteriaList = new ArrayList<>();
        List<Criteria> orCriteriaList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(fileList)) {
            fileList.forEach(file -> criteriaList.add(Criteria.where("rel_path").regex(file)));
            orCriteriaList.add(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
            query.addCriteria(new Criteria().andOperator(orCriteriaList.toArray(new Criteria[0])));
        }

        //查询总的数量，并且过滤计数
        return mongoTemplate.find(query, DUPCDefectModel.class, "t_dupc_defect");
    }

    /**
     * 批量获取任务指定的重复率分析结果
     *
     * @param taskIdSet 任务id集合
     * @param buildIds  构建id集合
     * @return list
     */
    public List<DUPCStatisticModel> batchFindByTaskIdAndBuildId(Collection<Long> taskIdSet,
            Collection<String> buildIds) {
        Document fieldsObj = new Document();
        fieldsObj.put("task_id", true);
        fieldsObj.put("build_id", true);
        fieldsObj.put("dup_rate", true);
        fieldsObj.put("defect_count", true);
        Query query = new BasicQuery(new Document(), fieldsObj);

        query.addCriteria(Criteria.where("task_id").in(taskIdSet).and("build_id").in(buildIds));

        return mongoTemplate.find(query, DUPCStatisticModel.class, "t_dupc_statistic");
    }

}
