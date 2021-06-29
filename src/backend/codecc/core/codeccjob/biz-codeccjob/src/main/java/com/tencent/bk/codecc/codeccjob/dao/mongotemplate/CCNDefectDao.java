/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.codeccjob.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.vo.DefectAuthorGroupStatisticVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 圈复杂度持久代码
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Repository
public class CCNDefectDao
{
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 按作者维度查询统计数据
     */
    public List<DefectAuthorGroupStatisticVO> findStatisticGroupByAuthor(long taskId, int status) {
        Criteria criteria = new Criteria();
        criteria.and("task_id").is(taskId).and("status").is(status);
        MatchOperation match = Aggregation.match(criteria);

        // 以author进行分组
        GroupOperation group = Aggregation.group("task_id", "author")
            .last("author").as("authorName")
            .count().as("defectCount");
        Aggregation agg = Aggregation.newAggregation(match, group)
            .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<DefectAuthorGroupStatisticVO> queryResult = mongoTemplate
            .aggregate(agg, "t_ccn_defect", DefectAuthorGroupStatisticVO.class);

        return queryResult.getMappedResults();
    }
}
