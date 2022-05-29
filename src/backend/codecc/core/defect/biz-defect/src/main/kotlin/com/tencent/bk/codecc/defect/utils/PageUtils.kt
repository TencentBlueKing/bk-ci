package com.tencent.bk.codecc.defect.utils

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
        val pageSort = Sort.by(sortTypeNotNull, sortFieldNotNull)
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
     * 列表等长分割
     */
    fun <T> averageAssignFixLength(source: List<T>?, splitItemNum: Int): List<List<T>> {
        val result = ArrayList<List<T>>()

        if (source != null && source.isNotEmpty() && splitItemNum > 0) {
            if (source.size <= splitItemNum) {
                // 源List元素数量小于等于目标分组数量
                result.add(source)
            } else {
                // 计算拆分后list数量
                val splitNum = if (source.size % splitItemNum == 0) {
                    source.size / splitItemNum
                } else {
                    source.size / splitItemNum + 1
                }

                var value: List<T>?
                for (i in 0 until splitNum) {
                    value = if (i < splitNum - 1) {
                        source.subList(i * splitItemNum, (i + 1) * splitItemNum)
                    } else {
                        // 最后一组
                        source.subList(i * splitItemNum, source.size)
                    }

                    result.add(value)
                }
            }
        }

        return result
    }


    /**
     * 生成分页类(不限制每页条目数,仅用于OP分页导出)
     */
    fun generaPageableUnlimitedPageSize(pageNum: Int?, pageSize: Int?, sortField: String?,
                                        sortType: String?): Pageable {
        val pageNumNotNull = if (pageNum == null) 0 else pageNum - 1
        // 不限制
        val pageSizeNotNull = pageSize ?: 100
        val sortFieldNotNull = sortField ?: "task_id"
        val sortTypeNotNull = try {
            Sort.Direction.valueOf(sortType ?: Sort.Direction.ASC.name)
        } catch (e: Exception) {
            Sort.Direction.ASC
        }
        val pageSort = Sort.by(sortTypeNotNull, sortFieldNotNull)
        return PageRequest.of(pageNumNotNull, pageSizeNotNull, pageSort
        )
    }

}