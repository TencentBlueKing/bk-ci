package com.tencent.devops.dispatch.kubernetes.utils

import com.tencent.devops.dispatch.kubernetes.common.CONFIG_VOLUME_NAME
import com.tencent.devops.dispatch.kubernetes.common.DATA_VOLUME_MOUNT_PATH
import com.tencent.devops.dispatch.kubernetes.common.DATA_VOLUME_NAME
import com.tencent.devops.dispatch.kubernetes.config.DispatchBuildConfig
import com.tencent.devops.dispatch.kubernetes.config.KubernetesClientConfig
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.ConfigMap
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.ConfigMapVolume
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.HostPath
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.HostPathVolume
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.Volume
import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.VolumeMount
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class KubernetesDataUtils @Autowired constructor(
    private val k8sConfig: KubernetesClientConfig,
    private val dispatchBuildConfig: DispatchBuildConfig
) {
    /**
     * 获取pod的目录
     */
    fun getPodVolume(): List<Volume> {
        return listOf(
            ConfigMapVolume(
                name = CONFIG_VOLUME_NAME,
                configMap = ConfigMap(
                    name = k8sConfig.configMapName!!,
                    key = dispatchBuildConfig.volumeConfigMapKey!!,
                    path = dispatchBuildConfig.volumeConfigMapPath!!
                )
            ),
            HostPathVolume(
                name = DATA_VOLUME_NAME,
                hostPath = HostPath(
                    path = dispatchBuildConfig.volumeHostPathHostDir!!
                )
            )
        )
    }

    /**
     * 获取挂载目录
     */
    fun getPodVolumeMount(): List<VolumeMount> {
        return listOf(
            VolumeMount(
                name = CONFIG_VOLUME_NAME,
                mountPath = dispatchBuildConfig.volumeMountPath!!
            ),
            VolumeMount(
                name = DATA_VOLUME_NAME,
                mountPath = DATA_VOLUME_MOUNT_PATH
            )
        )
    }
}
