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
 
package com.tencent.bk.codecc.defect.dao;

import org.bson.Document;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext;
import org.springframework.util.Assert;

/**
 * mongodb分组查询工具类
 * 
 * @date 2020/1/21
 * @version V1.0
 */
public class AddFieldOperation implements AggregationOperation
{
    private final Document document;

    public AddFieldOperation(final Document document)
    {
        Assert.notNull(document, "Criteria must not be null!");
        this.document = document;
    }

    @Override
    public Document toDocument(AggregationOperationContext context) {
        return new Document("$addFields", document);
    }
}
