package com.tencent.bk.codecc.task.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.task.component.GongfengProjectChecker
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository
import com.tencent.bk.codecc.task.dao.mongorepository.CustomProjRepository
import com.tencent.bk.codecc.task.dao.mongorepository.GongfengFailPageRepository
import com.tencent.bk.codecc.task.dao.mongorepository.GongfengStatProjRepository
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.dao.mongotemplate.GongfengPublicProjDao
import com.tencent.bk.codecc.task.dao.mongotemplate.GongfengStatProjDao
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao
import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.pojo.ActiveProjParseModel
import com.tencent.bk.codecc.task.pojo.GongfengProjPageModel
import com.tencent.bk.codecc.task.pojo.GongfengStatPageModel
import com.tencent.bk.codecc.task.pojo.TriggerPipelineModel
import com.tencent.bk.codecc.task.service.GongfengPublicProjService
import com.tencent.bk.codecc.task.service.PipelineIdRelationService
import com.tencent.bk.codecc.task.service.PipelineService
import com.tencent.bk.codecc.task.service.TaskRegisterService
import com.tencent.bk.codecc.task.service.TaskService
import com.tencent.bk.codecc.task.service.impl.OpenSourceTaskRegisterServiceImpl
import com.tencent.bk.codecc.task.tof.TofClientApi
import com.tencent.devops.common.client.Client
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class GongfengCreateTaskListener @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val gongfengPublicProjService: GongfengPublicProjService,
    private val pipelineService: PipelineService,
    @Qualifier("pipelineTaskRegisterService") private val taskRegisterService: TaskRegisterService,
    private val rabbitTemplate: RabbitTemplate,
    private val taskService: TaskService,
    private val gongfengFailPageRepository: GongfengFailPageRepository,
    private val taskRepository: TaskRepository,
    private val tofClientApi: TofClientApi,
    private val client: Client,
    private val gongfengStatProjDao: GongfengStatProjDao,
    private val gongfenPublicProjDao: GongfengPublicProjDao,
    private val gongfengStatProjRepository: GongfengStatProjRepository,
    private val baseDataRepository: BaseDataRepository,
    private val gongfengProjectChecker: GongfengProjectChecker,
    private val openSourceTaskRegisterService: OpenSourceTaskRegisterServiceImpl,
    private val taskDao: TaskDao,
    private val customProjRepository: CustomProjRepository,
    private val pipelineIdRelationService: PipelineIdRelationService
) {

    @Value("\${git.path:#{null}}")
    private val gitCodePath: String? = null

    @Value("\${codecc.privatetoken:#{null}}")
    private val gitPrivateToken: String? = null

    @Value("\${codecc.classurl:#{null}}")
    private val publicClassUrl: String? = null

    @Value("\${codecc.public.account:#{null}}")
    private val codeccPublicAccount: String? = null

    @Value("\${codecc.public.password:#{null}}")
    private val codeccPublicPassword: String? = null

    @Value("\${devops.imageName:#{null}}")
    private val imageName: String? = null

    fun executeCreateTask(gongfengPageModel: GongfengProjPageModel) {
        // TODO("not implemented")
    }

    /**
     * 更新工蜂统计信息
     */
    fun updateGongfengStatInfo(gongfengPageModel: GongfengStatPageModel) {
        // TODO("not implemented")
    }

    /**
     * 同步工蜂统计信息
     */
    fun syncGongfengStatInfo(bgId: String) {
        // TODO("not implemented")
    }

    fun executeTriggerPipeline(triggerPipelineModel: TriggerPipelineModel) {
        // TODO("not implemented")
    }

    /**
     * 为首次触发的失效项目补齐数据
     */
    fun setValueToInvalidTask(triggerPipelineModel: TriggerPipelineModel): TaskInfoEntity? {
        // TODO("not implemented")
        return null
    }

    fun executeActiveProjectTask(activeProjParseModel: ActiveProjParseModel) {
        // TODO("not implemented")
    }

    fun addGongfengJob(gongfengProjectId: Int, userId: String, projectId: String, pipelineId: String) {
        // TODO("not implemented")
    }
}
