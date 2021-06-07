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

package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.FileDefectGatherEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * lint类工具持久层代码
 *
 * @version V1.0
 * @date 2019/5/10
 */
@Slf4j
@Repository
public class FileDefectGatherDao
{
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 插入或更新收敛文件信息
     *
     * @param gatherFiles
     */
    public void upsertGatherFileListByPath(List<FileDefectGatherEntity> gatherFiles)
    {
        if (CollectionUtils.isNotEmpty(gatherFiles))
        {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, FileDefectGatherEntity.class);
            for (FileDefectGatherEntity gatherFile : gatherFiles)
            {
                Query query = new Query();
                Criteria criteria = Criteria.where("task_id").is(gatherFile.getTaskId()).and("tool_name").is(gatherFile.getToolName());

                if (StringUtils.isNotEmpty(gatherFile.getRelPath()))
                {
                    criteria.and("rel_path").is(gatherFile.getRelPath());
                }
                else
                {
                    criteria.and("file_path").is(gatherFile.getFilePath());
                }
                query.addCriteria(criteria);
                Update update = new Update();
                update.set("task_id", gatherFile.getTaskId())
                        .set("tool_name", gatherFile.getToolName())
                        .set("file_path", gatherFile.getFilePath())
                        .set("rel_path", gatherFile.getRelPath())
                        .set("total", gatherFile.getTotal())
                        .set("status", gatherFile.getStatus())
                        .set("create_time", gatherFile.getCreateTime())
                        .set("fixed_time", gatherFile.getFixedTime())
                        .set("updated_date", gatherFile.getUpdatedDate());
                ops.upsert(query, update);
            }
            ops.execute();
        }
    }
}
