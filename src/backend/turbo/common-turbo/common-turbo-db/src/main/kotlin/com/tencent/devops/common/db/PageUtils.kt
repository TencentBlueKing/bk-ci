package com.tencent.devops.common.db

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
        // pagesize限制1000条
        val pageSizeNotNull = if (pageSize == null) 100 else if (pageSize >= 10000) 10000 else pageSize
        val sortFieldNotNull = sortField ?: "pipeline_id"
        val sortTypeNotNull = try {
            Sort.Direction.valueOf(sortType ?: "ASC")
        } catch (e: Exception) {
            Sort.Direction.ASC
        }
        val pageSort = Sort.by(sortTypeNotNull, sortFieldNotNull)
        return PageRequest.of(
            pageNumNotNull,
            pageSizeNotNull,
            pageSort
        )
    }

    /**
     * 多字段信息转换为pageable类
     */
    fun convertPageWithMultiFields(
        pageNum: Int?,
        pageSize: Int?,
        sortField: Array<String>,
        sortType: String?
    ): Pageable {
        val pageNumNotNull = if (pageNum == null) 0 else pageNum - 1
        // pagesize限制1000条
        val pageSizeNotNull = if (pageSize == null) 100 else if (pageSize >= 10000) 10000 else pageSize
        val sortFieldNotNull = if (sortField.isNullOrEmpty()) arrayOf("pipeline_id") else sortField
        val sortTypeNotNull = try {
            Sort.Direction.valueOf(sortType ?: "ASC")
        } catch (e: Exception) {
            Sort.Direction.ASC
        }
        val pageSort = Sort.by(sortTypeNotNull, *sortFieldNotNull)
        return PageRequest.of(
            pageNumNotNull,
            pageSizeNotNull,
            pageSort
        )
    }

    /**
     * 多字段多方向信息转换为pageable类
     */
    fun convertPageWithMultiFieldsAndDirection(
        pageNum: Int?,
        pageSize: Int?,
        sortMap: Map<String, String?>
    ): Pageable {
        val pageNumNotNull = if (pageNum == null) 0 else pageNum - 1
        // pagesize限制1000条
        val pageSizeNotNull = if (pageSize == null) 100 else if (pageSize >= 10000) 10000 else pageSize

        val orderArray = sortMap.map { (t, u) ->
            val sortTypeNotNull = try {
                Sort.Direction.valueOf(u ?: "ASC")
            } catch (e: Exception) {
                Sort.Direction.ASC
            }
            Sort.Order(sortTypeNotNull, t)
        }
        val pageSort = Sort.by(*orderArray.toTypedArray())
        return PageRequest.of(
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

    /**
     * 生成分页类(不限制每页条目数)
     */
    fun generaPageableUnlimitedPageSize(
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Pageable {
        val pageNumNotNull = if (pageNum == null) 0 else pageNum - 1
        // 不限制
        val pageSizeNotNull = pageSize ?: 100
        val sortFieldNotNull = sortField ?: "pipeline_id"
        val sortTypeNotNull = try {
            Sort.Direction.valueOf(sortType ?: Sort.Direction.ASC.name)
        } catch (e: Exception) {
            Sort.Direction.ASC
        }
        val pageSort = Sort.by(sortTypeNotNull, sortFieldNotNull)
        return PageRequest.of(
            pageNumNotNull, pageSizeNotNull, pageSort
        )
    }
}
