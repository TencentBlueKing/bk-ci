package com.tencent.bk.codecc.apiquery.task.dao

import com.google.common.collect.Lists
import com.mongodb.BasicDBObject
import com.tencent.bk.codecc.apiquery.task.model.CustomProjModel
import com.tencent.bk.codecc.apiquery.task.model.GongfengPublicProjModel
import com.tencent.bk.codecc.apiquery.task.model.TaskFailRecordModel
import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel
import com.tencent.bk.codecc.apiquery.task.model.ToolConfigInfoModel
import com.tencent.bk.codecc.apiquery.task.model.ToolMetaModel
import com.tencent.bk.codecc.apiquery.task.model.*
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.util.DateTimeUtils
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOptions
import org.springframework.data.mongodb.core.query.BasicQuery
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.ZoneOffset

@Repository
class TaskDao @Autowired constructor(
    private val taskMongoTemplate: MongoTemplate
) {

    /**
     * 根据bgid查询开源任务清单
     */
    fun findByBgId(
        bgId: Int,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): List<TaskInfoModel> {
        val match = Aggregation.match(
            Criteria.where("bg_id").`is`(bgId).and("create_from").`is`("gongfeng_scan").and("custom_proj_info").exists(false).and("project_id").nin("CUSTOMPROJ_TEG_CUSTOMIZED", "CUSTOMPROJ_PCG_RD")
        )
        val lookup =
            Aggregation.lookup("t_gongfeng_stat_project", "gongfeng_project_id", "id", "gongfeng_stat_proj_info")
        val sort =
            Aggregation.sort(
                try {
                    Sort.Direction.valueOf(sortType ?: "ASC")
                } catch (e: Exception) {
                    Sort.Direction.ASC
                }, sortField ?: "task_id"
            )
        val skip = Aggregation.skip((((pageNum ?: 1) - 1) * (pageSize ?: 100)).toLong())
        val limit = Aggregation.limit((pageSize ?: 100).toLong())
        //允许磁盘操作
        val options = AggregationOptions.Builder().allowDiskUse(true).build()
        val agg = Aggregation.newAggregation(match, lookup, sort, skip, limit).withOptions(options)
        return taskMongoTemplate.aggregate(agg, "t_task_detail", TaskInfoModel::class.java).mappedResults
    }


    /**
     * 根据bgid查询开源任务清单
     */
    fun findByProjectId(
        projectId: String,
        pageNum: Int?,
        pageSize: Int?,
        sortType: String?,
        sortField: String?
    ): List<TaskInfoModel> {
        val match = Aggregation.match(
            Criteria.where("project_id").`is`(projectId)
        )
        val lookup =
            Aggregation.lookup("t_gongfeng_stat_project", "gongfeng_project_id", "id", "gongfeng_stat_proj_info")
        val sort =
            Aggregation.sort(
                try {
                    Sort.Direction.valueOf(sortType ?: "ASC")
                } catch (e: Exception) {
                    Sort.Direction.ASC
                }, sortField ?: "task_id"
            )
        val skip = Aggregation.skip((((pageNum ?: 1) - 1) * (pageSize ?: 100)).toLong())
        val limit = Aggregation.limit((pageSize ?: 100).toLong())
        //允许磁盘操作
        val options = AggregationOptions.Builder().allowDiskUse(true).build()
        val agg = Aggregation.newAggregation(match, lookup, sort, skip, limit).withOptions(options)
        return taskMongoTemplate.aggregate(agg, "t_task_detail", TaskInfoModel::class.java).mappedResults
    }


    fun findCustomProjByTaskIds(
        taskIds: List<Long>,
        pageNum: Int?,
        pageSize: Int?,
        sortType: String?,
        sortField: String?
    ): List<CustomProjModel> {
        val match = Aggregation.match(
            Criteria("task_id").`in`(taskIds).and("custom_proj_source").`is`("TEG_CUSTOMIZED")
        )
        val lookup =
            Aggregation.lookup("t_gongfeng_stat_project", "format_url", "url", "gongfeng_stat_proj_info")
        val sort =
            Aggregation.sort(
                try {
                    Sort.Direction.valueOf(sortType ?: "ASC")
                } catch (e: Exception) {
                    Sort.Direction.ASC
                }, sortField ?: "task_id"
            )
        val skip = Aggregation.skip((((pageNum ?: 1) - 1) * (pageSize ?: 100)).toLong())
        val limit = Aggregation.limit((pageSize ?: 100).toLong())
        //允许磁盘操作
        val options = AggregationOptions.Builder().allowDiskUse(true).build()
        val agg = Aggregation.newAggregation(match, lookup, sort, skip, limit).withOptions(options)
        return taskMongoTemplate.aggregate(agg, "t_customized_project", CustomProjModel::class.java).mappedResults
    }

    /**
     * 根据工蜂id清单查询工蜂项目详细信息
     */
    fun findForkIdByGongfengIds(
        gongfengIds: List<Int>,
        pageable: Pageable?
    ): List<GongfengPublicProjModel> {
        val query = BasicQuery(BasicDBObject())
        query.addCriteria(
            Criteria.where("id").`in`(gongfengIds)
        )
        if (null != pageable) {
            query.with(pageable)
        }
        return taskMongoTemplate.find(query, GongfengPublicProjModel::class.java, "t_gongfeng_project")
    }

    fun findToolListByTaskIds(
        taskIds: List<Long>,
        followStatus: List<Int>?,
        pageable: Pageable?
    ): List<ToolConfigInfoModel> {
        val fieldsObj = BasicDBObject()
        fieldsObj["tool_name"] = true
        fieldsObj["task_id"] = true
        fieldsObj["follow_status"] = true
        val query = BasicQuery(BasicDBObject(), fieldsObj)
        query.addCriteria(
            Criteria.where("task_id").`in`(taskIds)
        )
        if (!followStatus.isNullOrEmpty()) {
            query.addCriteria(
                Criteria.where("follow_status").`in`(followStatus)
            )
        }
        if (null != pageable) {
            query.with(pageable)
        }
        return taskMongoTemplate.find(query, ToolConfigInfoModel::class.java, "t_tool_config")
    }

    /**
     * 根据任务id查询个性化项目信息
     */
    fun findCustomProjByTaskId(taskId: Long): CustomProjModel? {
        val query = BasicQuery(BasicDBObject())
        query.addCriteria(
            Criteria.where("task_id").`is`(taskId)
        )
        return taskMongoTemplate.findOne(query, CustomProjModel::class.java, "t_customized_project")
    }

    /**
     * 根据任务id查询开源扫描信息
     */
    fun findTegSecurityByBgId(bgId: Int): List<TaskInfoModel> {
        val fieldsObj = BasicDBObject()
        fieldsObj["task_id"] = true
        val query = BasicQuery(BasicDBObject(), fieldsObj)
        query.addCriteria(
            //todo 要通用化
            Criteria.where("bg_id").`is`(bgId).and("create_from").`is`("gongfeng_scan").and("project_id").nin(
                "CUSTOMPROJ_PCG_RD",
                "CUSTOMPROJ_BLUEKING_CODE"
            )
        )
        return taskMongoTemplate.find(query, TaskInfoModel::class.java, "t_task_detail")
    }

    /**
     * 根据任务id查询详情信息
     */
    fun findTaskById(taskId: Long): TaskInfoModel? {
        val query = BasicQuery(BasicDBObject())
        query.addCriteria(
            Criteria.where("task_id").`is`(taskId)
        )
        return taskMongoTemplate.findOne(query, TaskInfoModel::class.java, "t_task_detail")
    }

    /**
     * 根据工具名查询工具元数据信息
     */
    fun findToolMetaByName(toolName: String): ToolMetaModel? {
        val fieldsObj = BasicDBObject()
        fieldsObj["logo"] = false
        fieldsObj["graphic_details"] = false
        val query = BasicQuery(BasicDBObject(), fieldsObj)
        query.addCriteria(
            Criteria.where("name").`is`(toolName)
        )
        return taskMongoTemplate.findOne(query, ToolMetaModel::class.java, "t_tool_meta")
    }

    /**
     * 根据任务id清单查询详情
     */
    fun findTaskInfoListByTaskIds(
        taskIds: List<Long>
    ): List<TaskInfoModel> {
        val fieldsObj = BasicDBObject()
        val query = BasicQuery(BasicDBObject(), fieldsObj)
        query.addCriteria(
            Criteria.where("task_id").`in`(taskIds)
        )
        return taskMongoTemplate.find(query, TaskInfoModel::class.java, "t_task_detail")
    }

    /**
     * 按任务ID集合获取任务列表
     */
    fun findByTaskIdIn(taskIdSet: Collection<Long>): List<TaskInfoModel> {
        val fieldsObj = BasicDBObject()
        fieldsObj["default_filter_path"] = false
        fieldsObj["notify_custom_info"] = false
        fieldsObj["last_disable_task_info"] = false
        fieldsObj["custom_proj_info"] = false
        fieldsObj["gongfeng_stat_proj_info"] = false

        val query = BasicQuery(BasicDBObject(), fieldsObj)
        if (CollectionUtils.isNotEmpty(taskIdSet)) {
            query.addCriteria(Criteria.where("task_id").`in`(taskIdSet))
        }

        return taskMongoTemplate.find(query, TaskInfoModel::class.java, "t_task_detail")
    }

    fun findTaskInfoModelListByTaskIds(
        taskIds: List<Long>, pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): List<TaskInfoModel> {
        val match = Aggregation.match(
            Criteria.where("task_id").`in`(taskIds)
        )
        val lookup =
            Aggregation.lookup("t_gongfeng_stat_project", "gongfeng_project_id", "id", "gongfeng_stat_proj_info")
        val sort =
            Aggregation.sort(
                try {
                    Sort.Direction.valueOf(sortType ?: "ASC")
                } catch (e: Exception) {
                    Sort.Direction.ASC
                }, sortField ?: "task_id"
            )
        val skip = Aggregation.skip((((pageNum ?: 1) - 1) * (pageSize ?: 100)).toLong())
        val limit = Aggregation.limit((pageSize ?: 100).toLong())
        //允许磁盘操作
        val options = AggregationOptions.Builder().allowDiskUse(true).build()
        val agg = Aggregation.newAggregation(match, lookup, sort, skip, limit).withOptions(options)
        return taskMongoTemplate.aggregate(agg, "t_task_detail", TaskInfoModel::class.java).mappedResults
    }

    /**
     * 根据流水线ID列表获取任务信息
     */
    fun findByPipelineIdList(
        pipeLineIdList: List<String>,
        pageable: Pageable
    ): List<TaskInfoModel> {
        val criteria = Criteria.where("pipeline_id").`in`(pipeLineIdList)
        val match = Aggregation.match(criteria)

        val pageSize = pageable.pageSize
        val sort = Aggregation.sort(pageable.sort)
        val skip = Aggregation.skip((pageable.pageNumber * pageSize).toLong())
        val limit = Aggregation.limit(pageSize.toLong())

        val agg = Aggregation.newAggregation(match, sort, skip, limit)
        val queryResults = taskMongoTemplate.aggregate(agg, "t_task_detail", TaskInfoModel::class.java)

        return queryResults.mappedResults
    }

    /**
     * 多条件分页查询任务详情
     */
    fun findTaskInfoPage(reqReq: TaskToolInfoReqVO, pageable: Pageable): Page<TaskInfoModel> {
        val criteria = Criteria()
        val criteriaList: MutableList<Criteria?> = Lists.newArrayList()

        // 可选查询条件  指定taskId集合
        val taskIdSet = reqReq.taskIds
        if (CollectionUtils.isNotEmpty(taskIdSet)) {
            criteriaList.add(Criteria.where("task_id").`in`(taskIdSet))
        }
        // 1.事业群
        val bgId = reqReq.bgId
        if (bgId != null && bgId != 0) {
            criteriaList.add(Criteria.where("bg_id").`is`(bgId))
        }
        // 2.部门多选
        val deptIds = reqReq.deptIds
        if (CollectionUtils.isNotEmpty(deptIds)) {
            criteriaList.add(Criteria.where("dept_id").`in`(deptIds))
        }
        // 3.创建来源多选
        val createFrom = reqReq.createFrom
        if (CollectionUtils.isNotEmpty(createFrom)) {
            criteriaList.add(Criteria.where("create_from").`in`(createFrom))
        }
        // 4.1创建时间-大于等于
        val createStartTime = reqReq.startTime
        if (StringUtils.isNotEmpty(createStartTime)) {
            criteriaList.add(Criteria.where("create_date").gte(DateTimeUtils.getTimeStamp(createStartTime)))
        }
        // 4.2创建时间-小于等于
        val createEndTime = reqReq.endTime
        if (StringUtils.isNotEmpty(createEndTime)) {
            criteriaList.add(Criteria.where("create_date").lte(DateTimeUtils.getTimeStamp(createEndTime)))
        }
        // 5.任务状态 enum Status
        val taskStatus = reqReq.status
        if (taskStatus != null && (taskStatus == 0 || taskStatus == 1)) {
            criteriaList.add(Criteria.where("status").`is`(taskStatus))
        }
        // 6.工具筛选
        /*val tool = reqReq.toolName
        if (StringUtils.isNotEmpty(tool)) {
            criteriaList.add(Criteria.where("tool_config_info_list").elemMatch(Criteria.where("tool_name").`is`(tool)
                    .and("follow_status").ne(ComConstants.FOLLOW_STATUS.WITHDRAW.value())))
        }*/
        // 7.是否包含新手接入(V1迁移)
        val hasNoviceRegister = reqReq.hasNoviceRegister
        if (hasNoviceRegister != null && hasNoviceRegister != 1) {
            // 默认不包含,正则取反
            criteriaList.add(Criteria.where("name_cn").regex("新手接入_").not())
        }

        // 模糊匹配
        val nameCn = reqReq.nameCn
        if (StringUtils.isNotEmpty(nameCn)) {
            criteriaList.add(Criteria.where("name_cn").regex(nameCn))
        }
        val nameEn = reqReq.nameEn
        if (StringUtils.isNotEmpty(nameEn)) {
            criteriaList.add(Criteria.where("name_en").regex(nameEn))
        }
        val ldProjectName = reqReq.projectName
        if (StringUtils.isNotEmpty(ldProjectName)) {
            criteriaList.add(Criteria.where("project_name").regex(ldProjectName))
        }
        val ldProjectId = reqReq.projectId
        if (StringUtils.isNotEmpty(ldProjectId)) {
            criteriaList.add(Criteria.where("project_id").regex(ldProjectId))
        }
        val pipelineId = reqReq.pipelineId
        if (StringUtils.isNotEmpty(pipelineId)) {
            criteriaList.add(Criteria.where("pipeline_id").regex(pipelineId))
        }
        val description = reqReq.description
        if (StringUtils.isNotEmpty(description)) {
            criteriaList.add(Criteria.where("description").regex(description))
        }


        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(*criteriaList.toTypedArray())
        }

        // 获取满足条件的总数
        val totalCount: Long = taskMongoTemplate.count(Query(criteria), "t_task_detail")

        // 分页排序
        val pageSize = pageable.pageSize
        val pageNumber = pageable.pageNumber
        val sort = Aggregation.sort(pageable.sort)
        val skip = Aggregation.skip((pageNumber * pageSize).toLong())
        val limit = Aggregation.limit(pageSize.toLong())

        // 指定查询字段
        val project = Aggregation.project(
            "task_id", "name_en", "name_cn", "code_lang",
            "task_owner", "status", "project_id", "project_name", "pipeline_id", "create_from", "description",
            "tool_names", "bg_id", "dept_id", "center_id", "group_id", "create_date", "created_by", "updated_date",
            "updated_by"
        )

        // 允许磁盘操作(支持较大数据集合的处理)
        val options = AggregationOptions.Builder().allowDiskUse(true).build()
        val aggregation =
            Aggregation.newAggregation(Aggregation.match(criteria), sort, skip, project, limit).withOptions(options)
        val queryResults = taskMongoTemplate.aggregate(aggregation, "t_task_detail", TaskInfoModel::class.java)

        // 计算总页数
        var totalPageNum = 0
        if (totalCount > 0) {
            totalPageNum = (totalCount.toInt() + pageSize - 1) / pageSize
        }

        // 页码加1返回
        return Page(totalCount, pageNumber + 1, pageSize, totalPageNum, queryResults.mappedResults)
    }


    /**
     * 按查询条件获取所有任务ID
     */
    fun findTaskIdListByCondition(reqReq: TaskToolInfoReqVO, pageable: Pageable): Page<TaskInfoModel> {
        val criteriaList: MutableList<Criteria?> = Lists.newArrayList()

        // 1.事业群
        val bgId = reqReq.bgId
        if (bgId != null && bgId != 0) {
            criteriaList.add(Criteria.where("bg_id").`is`(bgId))
        }
        // 2.部门多选
        val deptIds = reqReq.deptIds
        if (CollectionUtils.isNotEmpty(deptIds)) {
            criteriaList.add(Criteria.where("dept_id").`in`(deptIds))
        }
        // 3.创建来源多选
        val createFrom = reqReq.createFrom
        if (CollectionUtils.isNotEmpty(createFrom)) {
            criteriaList.add(Criteria.where("create_from").`in`(createFrom))
        }

        // 4.任务状态 enum Status
        val taskStatus = reqReq.status
        if (taskStatus != null && (taskStatus == 0 || taskStatus == 1)) {
            criteriaList.add(Criteria.where("status").`is`(taskStatus))
        }

        // 分页排序
        val pageSize = pageable.pageSize
        val pageNumber = pageable.pageNumber
        val sort = Aggregation.sort(pageable.sort)
        val skip = Aggregation.skip((pageNumber * pageSize).toLong())
        val limit = Aggregation.limit(pageSize.toLong())

        val criteria = Criteria()
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(*criteriaList.toTypedArray())
        }

        // 获取满足条件的总数
        val totalCount: Long = taskMongoTemplate.count(Query(criteria), "t_task_detail")

        // 计算总页数
        var totalPageNum = 0
        if (totalCount > 0) {
            totalPageNum = (totalCount.toInt() + pageSize - 1) / pageSize
        }

        // 指定查询字段
        val project = Aggregation.project("task_id")

        val aggregation = Aggregation.newAggregation(Aggregation.match(criteria), sort, skip, project, limit)
        val queryResults = taskMongoTemplate.aggregate(aggregation, "t_task_detail", TaskInfoModel::class.java)

        return Page(totalCount, pageNumber + 1, pageSize, totalPageNum, queryResults.mappedResults)
    }
    /**
     * 查询任务执行失败清单
     */
    fun findTaskFailRecord(
        projectId: String?,
        taskIds: List<Long>?,
        pipelineId : String?,
        buildId : String?,
        pageable: Pageable?
    ): List<TaskFailRecordModel> {
        //todo 添加索引
        val query = Query()
        query.addCriteria(
            Criteria.where("create_from").`is`("gongfeng_scan")
        )
        query.addCriteria(
            Criteria.where("upload_time").gt(
                LocalDate.now().atStartOfDay(
                    ZoneOffset.ofHours(8)
                ).toInstant().toEpochMilli()
            )
        ).addCriteria(Criteria.where("retry_flag").`is`(false))
        if(projectId.isNullOrBlank()){
            query.addCriteria(Criteria.where("project_id").nin("CUSTOMPROJ_TEG_CUSTOMIZED", "CUSTOMPROJ_PCG_RD"))
        } else {
            query.addCriteria(Criteria.where("project_id").`is`(projectId))
        }
        if(!taskIds.isNullOrEmpty()){
            query.addCriteria(Criteria.where("task_id").`in`(taskIds))
        }
        if(!pipelineId.isNullOrBlank()){
            query.addCriteria(Criteria.where("pipeline_id").`is`(pipelineId))
        }
        if(!buildId.isNullOrBlank()){
            query.addCriteria(Criteria.where("build_id").`is`(buildId))
        }
        if(null != pageable){
            query.with(pageable)
        }

        return taskMongoTemplate.find(query, TaskFailRecordModel::class.java, "t_task_fail_record")
    }

    /**
     * 根据codeccBuildId查询映射表
     */
    fun findByCodeccBuildId(codeccBuildId: String): BuildIdRelationshipModel? {
        val query = Query()
        query.addCriteria(Criteria.where("codecc_build_id").`is`(codeccBuildId))

        return taskMongoTemplate.findOne(query, BuildIdRelationshipModel::class.java, "t_build_id_relationship")
    }

}