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

package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 代码检查任务持久层代码
 *
 * @version V1.0
 * @date 2019/4/24
 */
@Repository
public interface ToolMetaRepository extends MongoRepository<ToolMetaEntity, String>
{
    /**
     * 通过状态进行查询
     *
     * @param status
     * @return
     */
    List<ToolMetaEntity> findByStatus(String status);

    /**
     * 通过工具名进行查询
     *
     * @param toolName
     * @return
     */
    ToolMetaEntity findByName(String toolName);

    /**
     * 根据调试流名称进行查询
     *
     * @param debugPipelineId
     * @return
     */
    ToolMetaEntity findByDebugPipelineId(String debugPipelineId);

    boolean existsByName(String name);

    /**
     * 查询所有工具的名字和类型
     * @return
     */
    @Query(fields = "{'name':1,'type':1}")
    List<ToolMetaEntity> findAllByEntityIdIsNotNull();

    /**
     * 按照名字查询工具元数据
     * @param name
     * @return
     */
    ToolMetaEntity findFirstByName(String name);
}
