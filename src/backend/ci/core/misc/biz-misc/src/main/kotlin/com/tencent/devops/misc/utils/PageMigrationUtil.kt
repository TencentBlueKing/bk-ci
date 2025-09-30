package com.tencent.devops.misc.utils

object PageMigrationUtil {

    const val SHORT_PAGE_SIZE = 5
    const val MEDIUM_PAGE_SIZE = 100
    const val LONG_PAGE_SIZE = 1000

    /**
     * 分页迁移数据工具函数
     *
     * @param pageSize 每页数据大小
     * @param fetch 数据获取函数，接收偏移量(offset)和每页大小(limit)作为参数，返回当前页的数据列表
     * @param migrate 数据迁移函数，接收当前页的数据列表，并对每页数据进行迁移操作
     *
     * 函数逻辑：
     * 1. 从偏移量0开始，每次获取pageSize条数据
     * 2. 如果获取到的数据不为空，则执行迁移操作
     * 3. 更新偏移量，准备获取下一页数据
     * 4. 当获取到的数据量等于pageSize时，说明还有下一页数据，继续循环
     * 5. 当获取到的数据量小于pageSize时，说明已是最后一页，结束循环
     */
    fun <T> migrateByPage(
        pageSize: Int,
        fetch: (offset: Int, limit: Int) -> List<T>,
        migrate: (List<T>) -> Unit
    ) {
        var offset = 0
        do {
            // 获取当前页的数据
            val records = fetch(offset, pageSize)
            // 如果当前页有数据，则执行迁移操作
            if (records.isNotEmpty()) {
                migrate(records)
            }
            // 更新偏移量，准备获取下一页
            offset += pageSize
        } while (records.size == pageSize)
    }
}
