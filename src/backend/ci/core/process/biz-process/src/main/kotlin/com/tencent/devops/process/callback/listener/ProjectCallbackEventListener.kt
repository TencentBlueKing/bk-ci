package com.tencent.devops.process.callback.listener

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.listener.EventListener
import com.tencent.devops.process.engine.control.CallBackControl
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.mq.ProjectBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectCreateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectEnableStatusBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectCallbackEventListener @Autowired constructor(
    val callbackControl: CallBackControl,
    val client: Client
) : EventListener<ProjectBroadCastEvent> {
    /**
     * 默认实现了Listener的消息处理方法做转换处理
     * @param event ProjectBroadCastEvent抽象类的处理，如有扩展请到子类操作
     */
    override fun execute(event: ProjectBroadCastEvent) {
        logger.info("Receive ProjectEvent from MQ [$event]")
        try {
            when (event) {
                is ProjectUpdateBroadCastEvent -> {
                    onReceiveProjectUpdate(event)
                }

                is ProjectEnableStatusBroadCastEvent -> {
                    onReceiveProjectEnable(event)
                }

                is ProjectCreateBroadCastEvent -> {
                    onReceiveProjectCreate(event)
                }
            }
        } catch (ignored: Exception) {
            logger.error("BKSystemMonitor| project callback listener execute error", ignored)
        }
    }

    /**
     *  处理创建项目事件
     *  @param event ProjectCreateBroadCastEvent
     */
    fun onReceiveProjectCreate(event: ProjectCreateBroadCastEvent) {
        callbackControl.projectCreate(
            projectId = event.projectInfo.englishName,
            projectName = event.projectInfo.projectName,
            userId = event.userId
        )
    }

    /**
     *  处理更新项目事件
     *  @param event ProjectUpdateBroadCastEvent
     */
    fun onReceiveProjectUpdate(event: ProjectUpdateBroadCastEvent) {
        callbackControl.projectUpdate(
            projectId = event.projectInfo.englishName,
            projectName = event.projectInfo.projectName,
            userId = event.userId
        )
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
                callbackControl.projectEnable(
                    projectId = event.projectId,
                    projectName = it.projectName,
                    userId = event.userId
                )
            } else {
                callbackControl.projectDisable(
                    projectId = event.projectId,
                    projectName = it.projectName,
                    userId = event.userId
                )
            }
        }
    }

    private fun getProject(projectEnglishName: String? = null): ProjectVO? {
        if (projectEnglishName.isNullOrBlank()) return null
        return try {
            client.get(ServiceProjectResource::class).get(projectEnglishName).data
        } catch (ignored: Exception) {
            logger.warn(
                "fail to get project info|projectEnglishName[$projectEnglishName]",
                ignored
            )
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectCallbackEventListener::class.java)
    }
}
