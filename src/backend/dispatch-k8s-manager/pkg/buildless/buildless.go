package buildless

import (
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/kubeclient"
	"disaptch-k8s-manager/pkg/logs"
	"math/rand"
	"path/filepath"
	"time"

	"github.com/pkg/errors"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
)

func InitBuildLess() error {

	logs.Info("Start init buildless.")
	if !config.Config.BuildLess.Enabled {
		return nil

	}

	logs.Info("buildless enabled")

	label := map[string]string{"bkci.dispatch.kubenetes/buildless": "buildless-pool"}

	var replicas int32 = int32(config.Config.BuildLess.Replicas)

	resources, err := getResources()
	if err != nil {
		return errors.Wrap(err, "parse builder resources error")
	}

	var deploymentBody = &kubeclient.Deployment{
		Name:        "buildless-pool",
		Labels:      label,
		MatchLabels: label,
		Replicas:    &replicas,
		Pod: kubeclient.Pod{
			Labels:      label,
			Annotations: nil,
			Volumes:     getBuilderPodVolume("buildless-pool"),
			Containers: []kubeclient.Container{
				{
					Image:        config.Config.BuildLess.Image,
					Resources:    *resources,
					Env:          getEnvs(),
					Command:      []string{"/bin/sh", "/data/devops/config/init.sh"},
					VolumeMounts: getBuilderPodVolumeMount(),
				},
			},
			NodeMatches:     nil,
			Tolerations:     nil,
			PullImageSecret: nil,
		},
	}

	kubeclient.CreateDeployment(deploymentBody)

	return nil
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
	}
}

// getBuilderPodVolumeMount 获取与 getBuilderPodVolume 相关的mount
func getBuilderPodVolumeMount() []corev1.VolumeMount {
	return []corev1.VolumeMount{
		{
			Name:      config.BuilderConfigVolumeName,
			MountPath: config.Config.BuildLess.VolumeMount.BuilderConfigMapPath,
			ReadOnly:  true,
		},
		{
			Name:      config.DataVolumeName,
			MountPath: config.Config.BuildLess.VolumeMount.DataPath,
		},
		{
			Name:      config.LogsVolumeName,
			MountPath: config.Config.BuildLess.VolumeMount.LogPath,
		},
	}
}

// getResources 获取构建机资源配置
func getResources() (*corev1.ResourceRequirements, error) {
	requestCpu, err := resource.ParseQuantity("1")
	if err != nil {
		return nil, err
	}
	requestMem, err := resource.ParseQuantity("1024Mi")
	if err != nil {
		return nil, err
	}
	limitCpu, err := resource.ParseQuantity("1")
	if err != nil {
		return nil, err
	}
	limitMem, err := resource.ParseQuantity("1024Mi")
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

// getEnvs 获取buildless的环境变量
func getEnvs() (envs []corev1.EnvVar) {
	return []corev1.EnvVar{
		{
			Name:  "JOB_POOL",
			Value: "K8S_BUILD_LESS",
		},
		{
			Name:  "devops_gateway",
			Value: "bk-ci-bk-ci-gateway",
		},
		{
			Name:  "kubernetes_manager_host",
			Value: "http://kubernetes-manager",
		},
		{
			Name: "pod_name",
			ValueFrom: &corev1.EnvVarSource{
				FieldRef: &corev1.ObjectFieldSelector{
					FieldPath: "metadata.name",
				},
			},
		},
		{
			Name:  "random_str",
			Value: randomString(16),
		},
	}
}

const charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

func stringWithCharset(length int, charset string) string {
	b := make([]byte, length)
	rand.Seed(time.Now().UnixNano())
	for i := range b {
		b[i] = charset[rand.Intn(len(charset))]
	}
	return string(b)
}

func randomString(length int) string {
	return stringWithCharset(length, charset)
}
