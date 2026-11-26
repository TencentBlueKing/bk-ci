/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.log.dao

import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.FieldSortBuilder
import org.elasticsearch.search.sort.SortOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LogPanelEsDaoQueryTest {

    @Test
    fun sortKeyUsesLineNoOnlyMatchingCursor() {
        val descSource = LogPanelEsDao.buildSearchSource(
            query = QueryBuilders.boolQuery(),
            pageSize = 500,
            desc = true,
            totalHitsMode = LogPanelEsDao.TotalHitsMode.UP_TO_PLUS_ONE
        )
        assertEquals(1, descSource.sorts().size)
        val sort = descSource.sorts()[0] as FieldSortBuilder
        assertEquals("lineNo", sort.fieldName)
        assertEquals(SortOrder.DESC, sort.order())
    }

    @Test
    fun trackTotalHitsMatchesDirectionNeeds() {
        val exact = LogPanelEsDao.buildSearchSource(
            query = QueryBuilders.boolQuery(),
            pageSize = 200,
            desc = true,
            totalHitsMode = LogPanelEsDao.TotalHitsMode.EXACT
        )
        assertEquals(Integer.MAX_VALUE, exact.trackTotalHitsUpTo())

        val upTo = LogPanelEsDao.buildSearchSource(
            query = QueryBuilders.boolQuery(),
            pageSize = 200,
            desc = false,
            totalHitsMode = LogPanelEsDao.TotalHitsMode.UP_TO_PLUS_ONE
        )
        assertEquals(201, upTo.trackTotalHitsUpTo())

        val none = LogPanelEsDao.buildSearchSource(
            query = QueryBuilders.boolQuery(),
            pageSize = 1000,
            desc = true,
            totalHitsMode = LogPanelEsDao.TotalHitsMode.NONE
        )
        assertEquals(-1, none.trackTotalHitsUpTo())
    }
}
