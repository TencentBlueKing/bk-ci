package com.tencent.devops.process.callback.listener

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.listener.Listener
import com.tencent.devops.process.engine.control.CallBackControl
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.mq.ProjectBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectCreateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectEnableStatusBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateLogoBroadCastEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectCallbackEventListener @Autowired constructor(
    val callbackControl: CallBackControl,
    val client: Client
) : Listener<ProjectBroadCastEvent> {
    /**
     * 默认实现了Listener的消息处理方法做转换处理
     * @param event ProjectBroadCastEvent抽象类的处理，如有扩展请到子类操作
     */
    override fun execute(event: ProjectBroadCastEvent) {
        logger.info("Receive ProjectEvent from MQ [$event]")
        when (event) {
            is ProjectUpdateBroadCastEvent -> {
                onReceiveProjectUpdate(event)
            }

            is ProjectUpdateLogoBroadCastEvent -> {
                onReceiveProjectUpdateLogo(event)
            }

            is ProjectEnableStatusBroadCastEvent -> {
                onReceiveProjectEnable(event)
            }

            is ProjectCreateBroadCastEvent -> {
                onReceiveProjectCreate(event)
            }
        }
    }

    /**
     *  处理创建项目事件
     *  @param event ProjectCreateBroadCastEvent
     */
    fun onReceiveProjectCreate(event: ProjectCreateBroadCastEvent) {
        callbackControl.projectCreate(event.projectInfo.englishName, event.projectInfo.projectName)
    }

    /**
     *  处理更新项目事件
     *  @param event ProjectUpdateBroadCastEvent
     */
    fun onReceiveProjectUpdate(event: ProjectUpdateBroadCastEvent) {
        callbackControl.projectUpdate(event.projectInfo.englishName, event.projectInfo.projectName)
    }

    /**
     *  处理更新Logo项目事件
     *  @param event ProjectUpdateLogoBroadCastEvent
     */
    fun onReceiveProjectUpdateLogo(event: ProjectUpdateLogoBroadCastEvent) {
        // 此处的projectId为项目的ID
        // 参考：com.tencent.devops.project.service.impl.AbsProjectServiceImpl.update
        // 参考：com.tencent.devops.project.service.impl.AbsProjectServiceImpl.updateLogo
        getProject(projectId = event.projectId)?.let {
            callbackControl.projectUpdateLogo(event.projectId, it.projectName)
        }
    }

    /**
     *  处理项目禁用事件
     *  @param event ProjectEnableStatusBroadCastEvent
     */
    fun onReceiveProjectEnable(event: ProjectEnableStatusBroadCastEvent) {
        // 此处的projectId为项目的英文名
        // 参考：com.tencent.devops.project.service.impl.AbsProjectServiceImpl.updateUsableStatus
        getProject(projectEnglishName = event.projectId)?.let {
            if (event.enabled) {
                callbackControl.projectEnable(event.projectId, it.projectName)
            } else {
                callbackControl.projectDisable(event.projectId, it.projectName)
            }
        }
    }

    private fun getProject(projectId: String? = null, projectEnglishName: String? = null): ProjectVO? {
        return try {
            client.get(ServiceProjectResource::class).let {
                when {
                    !projectId.isNullOrBlank() -> {
                        it.getById(projectId).data
                    }

                    !projectEnglishName.isNullOrBlank() -> {
                        it.get(projectEnglishName).data
                    }

                    else -> null
                }
            }
        } catch (ignored: Exception) {
            logger.warn(
                "fail to get project info|projectId[$projectId]|projectEnglishName[$projectEnglishName]",
                ignored
            )
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectCallbackEventListener::class.java)
    }
}