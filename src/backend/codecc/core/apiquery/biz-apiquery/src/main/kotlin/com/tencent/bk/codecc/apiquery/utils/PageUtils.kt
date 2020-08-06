package com.tencent.bk.codecc.apiquery.utils

import com.mongodb.BasicDBObject
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

object PageUtils {

    /**
     * 将分页信息转换为pageable类
     */
    fun convertPageSizeToPageable(
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Pageable {
        val pageNumNotNull = if (pageNum == null) 0 else pageNum - 1
        //pagesize限制1000条
        val pageSizeNotNull = if (pageSize == null) 100 else if (pageSize >= 10000) 10000 else pageSize
        val sortFieldNotNull = sortField ?: "task_id"
        val sortTypeNotNull = try{
            Sort.Direction.valueOf(sortType ?: "ASC")
        } catch (e : Exception){
            Sort.Direction.ASC
        }
        val pageSort = Sort(sortTypeNotNull, sortFieldNotNull)
        return PageRequest(
            pageNumNotNull,
            pageSizeNotNull,
            pageSort
        )
    }

    /**
     * 配置过滤条件
     */
    fun getFilterFields(
        filterFields: List<String>?,
        fieldsObj: BasicDBObject
    ) {
        if (!filterFields.isNullOrEmpty()) {
            filterFields.forEach {
                fieldsObj[it] = true
            }
        }
    }

}