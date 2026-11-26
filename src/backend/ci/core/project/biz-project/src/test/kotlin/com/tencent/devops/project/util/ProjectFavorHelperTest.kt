/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
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

package com.tencent.devops.project.util

import com.tencent.devops.project.pojo.ProjectCollation
import com.tencent.devops.project.pojo.ProjectSortType
import com.tencent.devops.project.pojo.ProjectVO
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProjectFavorHelperTest {

    @Test
    fun favorFirstThenInterleaveByFirstLetter() {
        val projects = listOf(
            project("测试11", "ceshi11"),
            project("zhenwtest", "zhenwtest"),
            project("啊项目", "axiangmu"),
            project("bkdevops", "bkdevops")
        )

        val sorted = ProjectFavorHelper.attachAndSort(
            projects = projects,
            favorProjectIds = setOf("zhenwtest"),
            sortType = ProjectSortType.PROJECT_NAME,
            collation = ProjectCollation.DEFAULT
        )

        assertEquals(listOf("zhenwtest", "啊项目", "bkdevops", "测试11"), sorted.map { it.projectName })
        assertEquals(true, sorted.first().favor)
    }

    @Test
    fun descendingReversesNonFavorOrder() {
        val projects = listOf(
            project("啊项目", "axiangmu"),
            project("bkdevops", "bkdevops"),
            project("测试11", "ceshi11"),
            project("zhenwtest", "zhenwtest")
        )

        val sorted = ProjectFavorHelper.attachAndSort(
            projects = projects,
            favorProjectIds = setOf("axiangmu"),
            sortType = ProjectSortType.PROJECT_NAME,
            collation = ProjectCollation.DESC
        )

        assertEquals(listOf("啊项目", "zhenwtest", "测试11", "bkdevops"), sorted.map { it.projectName })
    }

    @Test
    fun sortDoesNotThrowForLevel2Chinese() {
        val projects = listOf(
            project("测试11", "ceshi11"),
            project("亍项目", "chuxiangmu"),
            project("aaa-rbac-dev", "aaa-rbac-dev"),
            project("兀项目", "wuxiangmu")
        )

        val sorted = assertDoesNotThrow<List<ProjectVO>> {
            ProjectFavorHelper.attachAndSort(
                projects = projects,
                favorProjectIds = emptySet(),
                sortType = ProjectSortType.PROJECT_NAME,
                collation = ProjectCollation.DEFAULT
            )
        }

        assertEquals(setOf("亍项目", "兀项目"), sorted.subList(0, 2).map { it.projectName }.toSet())
        assertEquals(listOf("aaa-rbac-dev", "测试11"), sorted.subList(2, 4).map { it.projectName })
    }

    @Suppress("DEPRECATION")
    private fun project(projectName: String, englishName: String) = ProjectVO(
        id = 0,
        projectId = englishName,
        projectName = projectName,
        projectCode = englishName,
        projectType = null,
        approvalStatus = null,
        approvalTime = null,
        approver = null,
        ccAppId = null,
        ccAppName = null,
        createdAt = null,
        creator = null,
        dataId = null,
        deployType = null,
        bgId = null,
        bgName = null,
        centerId = null,
        centerName = null,
        deptId = null,
        deptName = null,
        businessLineId = null,
        businessLineName = null,
        description = null,
        englishName = englishName,
        extra = null,
        offlined = null,
        secrecy = null,
        helmChartEnabled = null,
        kind = null,
        logoAddr = null,
        remark = null,
        updatedAt = null,
        updator = null,
        useBk = null,
        enabled = null,
        gray = false,
        hybridCcAppId = null,
        enableExternal = null,
        hybrid_cc_app_id = null,
        project_id = null,
        project_name = null,
        project_code = null,
        cc_app_id = null,
        cc_app_name = null,
        routerTag = null,
        relationId = null,
        properties = null,
        subjectScopes = null,
        authSecrecy = null
    )
}
