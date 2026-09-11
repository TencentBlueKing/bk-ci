/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.project.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectLabelDao
import com.tencent.devops.project.dao.ProjectLabelRelDao
import com.tencent.devops.project.pojo.ProjectLabelVO
import com.tencent.devops.project.pojo.enums.ProjectLabel
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

@Service
class ProjectLabelService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectLabelDao: ProjectLabelDao,
    private val projectLabelRelDao: ProjectLabelRelDao
) {

    /**
     * 按标签查询业务项目 ID（englishName）。
     * 标签无绑定项目时返回空列表。
     */
    fun listProjectIdsByLabel(label: ProjectLabel): List<String> {
        return projectLabelDao.listEnglishNamesByLabelName(dslContext, label.name)
    }

    /**
     * 更新项目标签。
     * [labels] 为 null 时不改；空列表清空；非空则按枚举名查询已有字典后全量替换。
     * 字典中不存在的标签会直接报错，且不会先删除原关联。
     */
    fun replaceIfPresent(
        dslContext: DSLContext,
        projectUuid: String,
        labels: List<ProjectLabel>?
    ) {
        if (labels == null) {
            return
        }
        val distinct = labels.distinct()
        if (distinct.size > MAX_LABELS_PER_PROJECT) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("labels"),
                defaultMessage = "A project can have at most $MAX_LABELS_PER_PROJECT labels"
            )
        }
        replaceProjectLabels(
            dslContext = dslContext,
            projectUuid = projectUuid,
            labels = distinct.map { it.name }
        )
    }

    fun listAll(): List<ProjectLabelVO> {
        return projectLabelDao.getAllProjectLabel(dslContext)?.map { record ->
            ProjectLabelVO(
                id = record.id,
                labelName = record.labelName,
                createTime = DateTimeUtil.toDateTime(record.createTime),
                updateTime = DateTimeUtil.toDateTime(record.updateTime)
            )
        }.orEmpty()
    }

    fun create(labelName: String) {
        val name = labelName.trim()
        if (name.isEmpty()) {
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.NAME_EMPTY,
                defaultMessage = "Label name cannot be empty"
            )
        }
        if (name.length > MAX_LABEL_NAME_LENGTH) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_LENGTH_TOO_LONG,
                params = arrayOf(MAX_LABEL_NAME_LENGTH.toString()),
                defaultMessage = "Label name cannot exceed $MAX_LABEL_NAME_LENGTH characters"
            )
        }
        if (projectLabelDao.getByName(dslContext, name) != null) {
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.LABLE_EXIST,
                defaultMessage = "Project label [$name] already exists"
            )
        }
        try {
            projectLabelDao.add(dslContext, name)
        } catch (ignored: DuplicateKeyException) {
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.LABLE_EXIST,
                defaultMessage = "Project label [$name] already exists"
            )
        }
    }

    fun delete(labelId: String) {
        projectLabelDao.getProjectLabel(dslContext, labelId) ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.ID_INVALID,
            defaultMessage = "Project label [$labelId] does not exist"
        )
        projectLabelRelDao.deleteByLabelId(dslContext, labelId)
        projectLabelDao.delete(dslContext, labelId)
    }

    private fun replaceProjectLabels(
        dslContext: DSLContext,
        projectUuid: String,
        labels: List<String>
    ) {
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val labelIdList = labels.map { getExistingLabelId(context, it) }
            projectLabelRelDao.deleteByProjectId(context, projectUuid)
            if (labelIdList.isEmpty()) {
                return@transaction
            }
            projectLabelRelDao.batchAdd(
                dslContext = context,
                projectId = projectUuid,
                labelIdList = labelIdList
            )
        }
    }

    private fun getExistingLabelId(dslContext: DSLContext, labelName: String): String {
        val existed = projectLabelDao.getByName(dslContext, labelName)
            ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(labelName),
                defaultMessage = "Project label [$labelName] does not exist"
            )
        return existed.id
    }

    companion object {
        private const val MAX_LABELS_PER_PROJECT = 20
        private const val MAX_LABEL_NAME_LENGTH = 45
    }
}
