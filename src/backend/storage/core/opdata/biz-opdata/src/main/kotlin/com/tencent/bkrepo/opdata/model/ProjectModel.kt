/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.opdata.model

import com.tencent.bkrepo.opdata.constant.OPDATA_PROJECT
import com.tencent.bkrepo.opdata.pojo.enums.ProjectType
import com.tencent.bkrepo.repository.pojo.project.ProjectInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service

@Service
class ProjectModel @Autowired constructor(
    private var mongoTemplate: MongoTemplate
) {

    fun getProjectNum(projectType: ProjectType): Long {
        val query = when (projectType) {
            ProjectType.ALL -> Query()
            ProjectType.BLUEKING -> Query(
                Criteria().andOperator(
                    where(ProjectInfo::name).not().regex(ProjectType.CODECC.prefix),
                    where(ProjectInfo::name).not().regex(ProjectType.GIT.prefix)
                )
            )
            ProjectType.CODECC -> Query(where(ProjectInfo::name).regex(ProjectType.CODECC.prefix))
            ProjectType.GIT -> Query(where(ProjectInfo::name).regex(ProjectType.GIT.prefix))
        }
        return mongoTemplate.count(query, OPDATA_PROJECT)
    }

    fun getProjectList(): List<ProjectInfo> {
        return mongoTemplate.findAll(ProjectInfo::class.java, OPDATA_PROJECT)
    }
}
