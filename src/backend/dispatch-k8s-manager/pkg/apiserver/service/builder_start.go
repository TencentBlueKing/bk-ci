package service

import (
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/db/mysql"
	"disaptch-k8s-manager/pkg/kubeclient"
	"disaptch-k8s-manager/pkg/logs"
	"disaptch-k8s-manager/pkg/task"
	"disaptch-k8s-manager/pkg/types"
	"encoding/json"
	"fmt"
	"github.com/pkg/errors"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"path/filepath"
	"sort"
	"time"
)

func CreateBuilder(builder *Builder) (taskId string, err error) {

	volumes, volumeMounts := getBuilderVolumeAndMount(builder.Name, builder.NFSs)

	var replicas int32 = 1

	tolers, nodeMatches := buildDedicatedBuilder(builder)

	taskId = generateTaskId()

	if err := mysql.InsertTask(types.Task{
		TaskId:     taskId,
		TaskKey:    builder.Name,
		TaskBelong: types.TaskBelongBuilder,
		Action:     types.TaskActionCreate,
		Status:     types.TaskWaiting,
		Message:    nil,
		ActionTime: time.Now(),
		UpdateTime: time.Now(),
	}); err != nil {
		return "", err
	}

	labels := getDispatchLabel(builder.Name, taskId, types.TaskActionCreate, types.BuilderTaskLabel)
	matchlabels := getDispatchCoreLabel(builder.Name)

	annotations, err := getBuilderAnnotations(builder.Name)
	if err != nil {
		return "", err
	}

	pullImageSecret := getPullImageDockerSecret(builder.Name, []*types.Registry{builder.ImageRegistry})

	resources, err := getResources(builder.Resource)
	if err != nil {
		return "", errors.Wrap(err, "parse builder resources error")
	}

	go task.DoCreateBuilder(
		taskId,
		&kubeclient.Deployment{
			Name:        builder.Name,
			Labels:      labels,
			MatchLabels: matchlabels,
			Replicas:    &replicas,
			Pod: kubeclient.Pod{
				Labels:      labels,
				Annotations: annotations,
				Volumes:     volumes,
				Containers: []kubeclient.Container{
					{
						Image:        builder.Image,
						Resources:    *resources,
						Env:          getEnvs(builder.Env),
						Command:      builder.Command,
						VolumeMounts: volumeMounts,
					},
				},
				NodeMatches:     nodeMatches,
				Tolerations:     tolers,
				PullImageSecret: pullImageSecret,
			},
		},
	)

	return taskId, nil
}

// getBuilderVolumeAndMount 获取一些构建机的常规的被挂载到pod上的volume和mount
func getBuilderVolumeAndMount(
	workloadName string,
	nFSs []types.NFS,
) (volumes []corev1.Volume, volumeMounts []corev1.VolumeMount) {
	volumes = getBuilderPodVolume(workloadName)
	volumeMounts = getBuilderPodVolumeMount()

	if len(nFSs) > 0 {
		for _, nfs := range nFSs {
			name := fmt.Sprintf("%s_%d", config.NfsVolumeNamePrefix, time.Now().UnixNano())
			volumes = append(volumes, corev1.Volume{
				Name: name,
				VolumeSource: corev1.VolumeSource{
					NFS: &corev1.NFSVolumeSource{
						Server:   nfs.Server,
						Path:     nfs.Path,
						ReadOnly: false,
					},
				},
			})

			volumeMounts = append(volumeMounts, corev1.VolumeMount{
				Name:      name,
				MountPath: nfs.MountPath,
			})
		}
	}

	return volumes, volumeMounts
}

// getBuilderPodVolume 获取一些构建机的常规的被挂载到pod上的volume，包括配置configmap和data目录hostpath
func getBuilderPodVolume(workloadName string) []corev1.Volume {
	dataHostPath := filepath.Join(config.Config.Dispatch.Volume.HostPath.DataHostDir, workloadName)
	logHostPath := filepath.Join(config.Config.Dispatch.Volume.HostPath.LogsHostDir, workloadName)

	var items []corev1.KeyToPath
	for _, v := range config.Config.Dispatch.Volume.BuilderConfigMap.Items {
		items = append(items, corev1.KeyToPath{
			Key:  v.Key,
			Path: v.Path,
		})
	}

	return []corev1.Volume{
		{
			Name: config.BuilderConfigVolumeName,
			VolumeSource: corev1.VolumeSource{
				ConfigMap: &corev1.ConfigMapVolumeSource{
					LocalObjectReference: corev1.LocalObjectReference{
						Name: config.Config.Dispatch.Volume.BuilderConfigMap.Name,
					},
					Items: items,
				},
			},
		},
		{
			Name: config.DataVolumeName,
			VolumeSource: corev1.VolumeSource{
				HostPath: &corev1.HostPathVolumeSource{
					Path: dataHostPath,
				},
			},
		},
		{
			Name: config.LogsVolumeName,
			VolumeSource: corev1.VolumeSource{
				HostPath: &corev1.HostPathVolumeSource{
					Path: logHostPath,
				},
			},
		},
		{
			Name: config.CfsVolumeName,
			VolumeSource: corev1.VolumeSource{
				HostPath: &corev1.HostPathVolumeSource{
					Path: config.Config.Dispatch.Volume.Cfs.Path,
				},
			},
		},
	}
}

// getBuilderPodVolumeMount 获取与 getBuilderPodVolume 相关的mount
func getBuilderPodVolumeMount() []corev1.VolumeMount {
	return []corev1.VolumeMount{
		{
			Name:      config.BuilderConfigVolumeName,
			MountPath: config.Config.Dispatch.VolumeMount.BuilderConfigMapPath,
			ReadOnly:  true,
		},
		{
			Name:      config.DataVolumeName,
			MountPath: config.Config.Dispatch.VolumeMount.DataPath,
		},
		{
			Name:      config.LogsVolumeName,
			MountPath: config.Config.Dispatch.VolumeMount.LogPath,
		},
		{
			Name:      config.CfsVolumeName,
			MountPath: config.Config.Dispatch.VolumeMount.Cfs.Path,
			ReadOnly:  config.Config.Dispatch.VolumeMount.Cfs.ReadOnly,
		},
	}
}

// getBuilderAnnotations 获取构建机注释配置
func getBuilderAnnotations(builderName string) (map[string]string, error) {
	info, err := mysql.SelectScheduledInfo(builderName)
	if err != nil {
		logs.Error(builderName, "|SelectScheduledInfo error", err)
		return nil, nil
	}
	if info == nil {
		return nil, nil
	}

	result := map[string]string{}

	// 获取节点记录，用来把构建机分配到已有的节点
	nodes := info.NodeHistory
	if len(info.NodeHistory) > config.BuilderNodeHisSize {
		nodes = info.NodeHistory[:config.BuilderNodeHisSize]
	}
	nodesJ, err := json.Marshal(nodes)
	if err != nil {
		logs.Error(builderName, "|json marshal BuilderNodeSet error", err)
	} else {
		result[config.Config.Dispatch.Builder.NodesAnnotation] = string(nodesJ)
	}

	// 获取RealResource记录
	res := info.ResourceHistory
	if len(info.ResourceHistory) > config.BuilderRealResourceHisSize {
		res = info.ResourceHistory[:config.BuilderRealResourceHisSize]
	}
	realResource := calculateRealResource(builderName, res)
	if err == nil && realResource != nil {
		var resJ []byte
		resJ, err = json.Marshal(realResource)
		if err != nil {
			logs.Error(builderName, "|json marshal BuilderResHis error", err)
		} else {
			result[config.Config.Dispatch.Builder.RealResource.RealResourceAnnotation] = string(resJ)
		}
	} else if err != nil {
		logs.Error(builderName, "|calculateRealResource error", err)
	}

	if len(result) == 0 {
		return nil, nil
	}

	return result, nil
}

// calculateRealResource 根基构建机历史数据计算其真实的资源使用量
func calculateRealResource(builderName string, res []types.ContainerResourceUsage) []types.ContainerResourceUsage {
	if len(res) == 0 {
		return nil
	}

	var cpus []*resource.Quantity
	var mems []*resource.Quantity
	for _, re := range res {
		if re.Cpu != "" {
			cpu, err := resource.ParseQuantity(re.Cpu)
			if err == nil {
				cpus = append(cpus, &cpu)
			} else {
				logs.Error(builderName, " ", re.Cpu, "parse cpu quantity error ", err)
			}
		}

		if re.Memory != "" {
			mem, err := resource.ParseQuantity(re.Memory)
			if err == nil {
				mems = append(mems, &mem)
			} else {
				logs.Error(builderName, " ", re.Memory, "parse mem quantity error ", err)
			}
		}
	}

	if len(cpus) > 1 {
		sort.SliceStable(cpus, func(i, j int) bool {
			return cpus[i].MilliValue() > cpus[j].MilliValue()
		})
	}
	if len(mems) > 1 {
		sort.SliceStable(mems, func(i, j int) bool {
			return mems[i].Value() > mems[j].Value()
		})
	}

	resCpus := [config.BuilderRealResourceScheduleSize]*resource.Quantity{}
	copy(resCpus[0:], cpus[0:])
	resMems := [config.BuilderRealResourceScheduleSize]*resource.Quantity{}
	copy(resMems[0:], mems[0:])
	var result []types.ContainerResourceUsage
	for i := 0; i < config.BuilderRealResourceScheduleSize; i++ {
		if resCpus[i] == nil && resMems[i] == nil {
			continue
		}

		realCpu := ""
		if resCpus[i] != nil {
			realCpu = resCpus[i].String()
		}
		realMem := ""
		if resMems[i] != nil {
			realMem = resMems[i].String()
		}
		result = append(result, types.ContainerResourceUsage{
			Cpu:    realCpu,
			Memory: realMem,
		})
	}

	return result
}

// buildDedicatedBuilder 获取污点和节点亲和度配置
func buildDedicatedBuilder(builder *Builder) ([]corev1.Toleration, []kubeclient.NodeMatch) {
	// 优先读取专机配置
	if builder.PrivateBuilder != nil && builder.PrivateBuilder.Name != "" {
		return []corev1.Toleration{
				{
					Key:      config.Config.Dispatch.PrivateMachine.Label,
					Operator: corev1.TolerationOpEqual,
					Value:    builder.PrivateBuilder.Name,
					Effect:   corev1.TaintEffectNoSchedule,
				},
			}, []kubeclient.NodeMatch{
				{
					Key:      config.Config.Dispatch.PrivateMachine.Label,
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{builder.PrivateBuilder.Name},
				},
			}
	}

	// 读取具有特殊配置的机器
	if builder.SpecialBuilder != nil && builder.SpecialBuilder.Name != "" {
		return nil, []kubeclient.NodeMatch{
			{
				Key:      config.Config.Dispatch.SpecialMachine.Label,
				Operator: corev1.NodeSelectorOpIn,
				Values:   []string{builder.SpecialBuilder.Name},
			},
		}
	}

	// 如果配置中配置了节点选择器则使用节点选择器
	if config.Config.Dispatch.Builder.NodeSelector.Label != "" && config.Config.Dispatch.Builder.NodeSelector.Value != "" {
		return nil, []kubeclient.NodeMatch{
			{
				Key:      config.Config.Dispatch.Builder.NodeSelector.Label,
				Operator: corev1.NodeSelectorOpIn,
				Values:   []string{config.Config.Dispatch.Builder.NodeSelector.Value},
			},
		}
	}

	return nil, nil
}

func StartBuilder(builderName string, start *BuilderStart) (taskId string, err error) {
	taskId = generateTaskId()

	labels := getDispatchLabel(builderName, taskId, types.TaskActionStart, types.BuilderTaskLabel)

	// 重新拿一次node缓存，方便在启动构建机时重新调度
	annotations, err := getBuilderAnnotations(builderName)
	if err != nil {
		return "", err
	}

	data, err := json.Marshal([]map[string]interface{}{
		{
			"op":    "replace",
			"path":  "/spec/replicas",
			"value": 1,
		},
		{
			"op":    "replace",
			"path":  "/spec/template/spec/containers/0/env",
			"value": getEnvs(start.Env),
		},
		{
			"op":    "replace",
			"path":  "/spec/template/spec/containers/0/command",
			"value": start.Command,
		},
		{
			"op":    "replace",
			"path":  "/metadata/labels",
			"value": labels,
		},
		{
			"op":    "replace",
			"path":  "/spec/template/metadata/labels",
			"value": labels,
		},
		{
			"op":    "replace",
			"path":  "/spec/template/metadata/annotations",
			"value": annotations,
		},
	})
	if err != nil {
		return "", err
	}

	if err := mysql.InsertTask(types.Task{
		TaskId:     taskId,
		TaskKey:    builderName,
		TaskBelong: types.TaskBelongBuilder,
		Action:     types.TaskActionStart,
		Status:     types.TaskWaiting,
		Message:    nil,
		ActionTime: time.Now(),
		UpdateTime: time.Now(),
	}); err != nil {
		return "", err
	}

	go task.DoStartBuilder(taskId, builderName, data)

	return taskId, nil
}
