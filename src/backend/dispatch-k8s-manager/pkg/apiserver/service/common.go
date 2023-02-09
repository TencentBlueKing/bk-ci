package service

import (
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/kubeclient"
	"disaptch-k8s-manager/pkg/types"
	"disaptch-k8s-manager/pkg/util"
	"fmt"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"strconv"
	"time"
)

// generateTaskId 获取任务id，需要唯一 t-1654567330641390000-xsActwac
func generateTaskId() string {
	return "t-" + strconv.FormatInt(time.Now().UnixNano(), 10) + "-" + util.RandomString(8)
}

// getDispatchLabel 获取调度查询使用的label，需要唯一
func getDispatchLabel(
	coreName string,
	taskId string,
	taskAction types.TaskAction,
	labelType types.TaskLabelType,
) map[string]string {
	labels := map[string]string{}
	labels[config.Config.Dispatch.Label] = coreName

	labels[config.Config.Dispatch.Watch.Task.Label] =
		fmt.Sprintf("%s-%s-%s", taskId, string(labelType), string(taskAction))

	return labels
}

// getDispatchCoreLabel 获取调度查询使用的label，用于 Match label
func getDispatchCoreLabel(coreName string) map[string]string {
	return map[string]string{config.Config.Dispatch.Label: coreName}
}

// getResources 获取构建机资源配置
func getResources(re CommonWorkLoadResource) (*corev1.ResourceRequirements, error) {
	requestCpu, err := resource.ParseQuantity(re.RequestCPU)
	if err != nil {
		return nil, err
	}
	requestMem, err := resource.ParseQuantity(re.RequestMemory)
	if err != nil {
		return nil, err
	}
	limitCpu, err := resource.ParseQuantity(re.LimitCPU)
	if err != nil {
		return nil, err
	}
	limitMem, err := resource.ParseQuantity(re.LimitMemory)
	if err != nil {
		return nil, err
	}

	limit := map[corev1.ResourceName]resource.Quantity{
		corev1.ResourceCPU:    limitCpu,
		corev1.ResourceMemory: limitMem,
	}

	request := map[corev1.ResourceName]resource.Quantity{
		corev1.ResourceCPU:    requestCpu,
		corev1.ResourceMemory: requestMem,
	}

	return &corev1.ResourceRequirements{
		Limits:   limit,
		Requests: request,
	}, nil
}

// getEnvs 获取k8s的环境变量
func getEnvs(env map[string]string) (envs []corev1.EnvVar) {
	if len(env) > 0 {
		for key, value := range env {
			envs = append(envs, corev1.EnvVar{
				Name:  key,
				Value: value,
			})
		}
	}
	return envs
}

// getPodAndContainerName 获取pod和其容器名称
func getPodAndContainerName(workloadLabel string) (podName string, containerName string, err error) {
	podList, err := kubeclient.ListPod(workloadLabel)
	if err != nil {
		return "", "", err
	}

	var pod *corev1.Pod
	if len(podList) > 0 {
		pod = podList[0]
	} else {
		return "", "", nil
	}

	var container corev1.Container
	if len(pod.Spec.Containers) > 0 {
		container = pod.Spec.Containers[0]
	} else {
		return "", "", nil
	}

	return pod.Name, container.Name, nil
}

func getPullImageDockerSecret(workLoadName string, registries []*types.Registry) *kubeclient.DockerSecret {
	if len(registries) == 0 {
		return nil
	}

	var rs []types.Registry
	for _, r := range registries {
		if r != nil && r.UserName != "" && r.Server != "" && r.Password != "" {
			rs = append(rs, *r)
		}
	}

	if len(rs) == 0 {
		return nil
	}

	return &kubeclient.DockerSecret{
		Name:       workLoadName + "-secret",
		Labels:     map[string]string{config.Config.Dispatch.Label: workLoadName},
		Registries: rs,
	}
}
