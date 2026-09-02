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
import java.text.Collator
import java.util.Locale

object ProjectFavorHelper {

    fun attachAndSort(
        projects: List<ProjectVO>,
        favorProjectIds: Set<String>,
        sortType: ProjectSortType?,
        collation: ProjectCollation?
    ): List<ProjectVO> {
        val withFavor = projects.map { project ->
            project.copy(favor = favorProjectIds.contains(project.englishName))
        }
        return sort(projects = withFavor, sortType = sortType, collation = collation)
    }

    fun sort(
        projects: List<ProjectVO>,
        sortType: ProjectSortType?,
        collation: ProjectCollation?
    ): List<ProjectVO> {
        val descending = collation == ProjectCollation.DESC
        val nameCollator = Collator.getInstance(Locale.CHINA)
        return projects.sortedWith(
            compareByDescending<ProjectVO> { it.favor == true }
                .thenComparator { left, right ->
                    val result = when (sortType) {
                        ProjectSortType.ENGLISH_NAME ->
                            left.englishName.compareTo(right.englishName, ignoreCase = true)
                        else -> ProjectNamePinyinHelper.compare(
                            left = left.projectName,
                            right = right.projectName,
                            collator = nameCollator
                        )
                    }
                    if (descending) -result else result
                }
        )
    }
}
