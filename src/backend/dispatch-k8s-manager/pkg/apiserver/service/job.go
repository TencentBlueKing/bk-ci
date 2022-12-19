package service

import (
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/db/mysql"
	"disaptch-k8s-manager/pkg/kubeclient"
	"disaptch-k8s-manager/pkg/task"
	"disaptch-k8s-manager/pkg/types"
	"fmt"
	"github.com/pkg/errors"
	corev1 "k8s.io/api/core/v1"
	"path/filepath"
	"time"
)

func CreateJob(job *Job) (taskId string, err error) {
	// 如果PodName存在需要查询节点名称来获取nodeName
	nodeName := ""
	volumeHostPathWorkloadName := ""
	if job.PodNameSelector != nil {
		pod, err := kubeclient.GetPod(job.PodNameSelector.Selector)
		if err != nil {
			return "", err
		}
		nodeName = pod.Spec.NodeName
		if job.PodNameSelector.UsePodData {
			volumeHostPathWorkloadName = pod.Labels[config.Config.Dispatch.Label]
		}
	}

	volumes, volumeMounts := getJobVolumeAndMount(job.Name, job.NFSs, volumeHostPathWorkloadName)

	var backOffLimit int32 = 0

	taskId = generateTaskId()

	if err := mysql.InsertTask(types.Task{
		TaskId:     taskId,
		TaskKey:    job.Name,
		TaskBelong: types.TaskBelongJob,
		Action:     types.TaskActionCreate,
		Status:     types.TaskWaiting,
		Message:    nil,
		ActionTime: time.Now(),
		UpdateTime: time.Now(),
	}); err != nil {
		return "", err
	}

	pullImageSecret := getPullImageDockerSecret(job.Name, []*types.Registry{job.ImageRegistry})

	resources, err := getResources(job.Resource)
	if err != nil {
		return "", errors.Wrap(err, "parse job resources error")
	}

	go task.DoCreateJob(taskId,
		&kubeclient.Job{
			Name:                  job.Name,
			NodeName:              nodeName,
			ActiveDeadlineSeconds: job.ActiveDeadlineSeconds,
			BackOffLimit:          &backOffLimit,
			Pod: kubeclient.Pod{
				Labels:        getDispatchLabel(job.Name, taskId, types.TaskActionCreate, types.JobTaskLabel),
				Volumes:       volumes,
				RestartPolicy: corev1.RestartPolicyNever,
				Containers: []kubeclient.Container{
					{
						Image:        job.Image,
						Resources:    *resources,
						Env:          getEnvs(job.Env),
						Command:      job.Command,
						VolumeMounts: volumeMounts,
					},
				},
				PullImageSecret: pullImageSecret,
			},
		},
	)

	return taskId, nil
}

// getJobVolumeAndMount 获取一些构建机的常规的被挂载到pod上的volume和mount
func getJobVolumeAndMount(
	workloadName string,
	nFSs []types.NFS,
	volumeHostPathWorkloadName string,
) (volumes []corev1.Volume, volumeMounts []corev1.VolumeMount) {
	dataHostPath := filepath.Join(config.Config.Dispatch.Volume.HostPath.DataHostDir, workloadName)
	if volumeHostPathWorkloadName != "" {
		dataHostPath = filepath.Join(config.Config.Dispatch.Volume.HostPath.DataHostDir, volumeHostPathWorkloadName)
	}
	volumes = []corev1.Volume{
		{
			Name: config.DataVolumeName,
			VolumeSource: corev1.VolumeSource{
				HostPath: &corev1.HostPathVolumeSource{
					Path: dataHostPath,
				},
			},
		},
	}

	volumeMounts = []corev1.VolumeMount{
		{
			Name:      config.DataVolumeName,
			MountPath: config.Config.Dispatch.VolumeMount.DataPath,
		},
	}

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

func GetJobLogs(jobName string, sinceTime *int64) (string, error) {
	podName, containerName, err := getPodAndContainerName(jobName)
	if err != nil {
		return "", err
	}
	if podName == "" || containerName == "" {
		return "", nil
	}

	log, err := kubeclient.LogPod(podName, containerName, sinceTime)
	if err != nil {
		return "", err
	}

	return log, err
}

func GetJobStatus(workloadName string) (*JobStatus, error) {
	podList, err := kubeclient.ListPod(workloadName)
	if err != nil {
		return nil, err
	}

	if len(podList) == 0 {
		return &JobStatus{
			State:   JobUnknown,
			Message: fmt.Sprintf("job pod with %s is null", workloadName),
		}, nil
	}
	podStatus := podList[0].Status

	switch podStatus.Phase {
	case corev1.PodPending:
		return &JobStatus{
			State: JobPending,
			PodIp: podStatus.PodIP,
		}, nil
	case corev1.PodRunning:
		return &JobStatus{
			State: JobRunning,
			PodIp: podStatus.PodIP,
		}, nil
	case corev1.PodSucceeded:
		return &JobStatus{
			State: JobSucceeded,
			PodIp: podStatus.PodIP,
		}, nil
	case corev1.PodFailed:
		return &JobStatus{
			State:   JobFailed,
			Message: podStatus.Message + "|" + podStatus.Reason,
			PodIp:   podStatus.PodIP,
		}, nil
	case corev1.PodUnknown:
		return &JobStatus{
			State: JobUnknown,
			PodIp: podStatus.PodIP,
		}, nil
	}

	return &JobStatus{
		State:   JobUnknown,
		Message: fmt.Sprintf("job pod with %s status %v unknow", workloadName, podStatus),
		PodIp:   podStatus.PodIP,
	}, nil
}

func DeleteJob(jobName string) (taskId string, err error) {
	taskId = generateTaskId()

	if err := mysql.InsertTask(types.Task{
		TaskId:     taskId,
		TaskKey:    jobName,
		TaskBelong: types.TaskBelongJob,
		Action:     types.TaskActionDelete,
		Status:     types.TaskWaiting,
		Message:    nil,
		ActionTime: time.Now(),
		UpdateTime: time.Now(),
	}); err != nil {
		return "", err
	}

	go task.DoDeleteJob(taskId, jobName)

	return taskId, nil
}

const (
	kanikoVolueName = "kaniko-secret"
)

func BuildAndPushImage(info *BuildAndPushImageInfo) (taskId string, err error) {
	// 如果PodName存在需要查询节点名称来获取nodeName
	var nodeName string
	var volumeHostPathWorkloadName string

	pod, err := kubeclient.GetPod(info.PodNameSelector.Selector)
	if err != nil {
		return "", err
	}
	nodeName = pod.Spec.NodeName
	if info.PodNameSelector.UsePodData {
		volumeHostPathWorkloadName = pod.Labels[config.Config.Dispatch.Label]
	}

	// 增加kaniko推送镜像需要的凭据信息
	kanikoSecretName := info.Name + "-kaniko-secret"
	volumes := getKanikoPodVolume(info.Name, volumeHostPathWorkloadName, kanikoSecretName)
	volumeMounts := getKanikoPodVolumeMount()
	kanikoSecret := getKanikoSecret(kanikoSecretName, info.Name, info.Info.Registries)

	var backOffLimit int32 = 0

	taskId = generateTaskId()

	if err := mysql.InsertTask(types.Task{
		TaskId:     taskId,
		TaskKey:    info.Name,
		TaskBelong: types.TaskBelongJob,
		Action:     types.TaskActionCreate,
		Status:     types.TaskWaiting,
		Message:    nil,
		ActionTime: time.Now(),
		UpdateTime: time.Now(),
	}); err != nil {
		return "", err
	}

	pullImageSecret := getPullImageDockerSecret(info.Name, []*types.Registry{{
		Server:   config.Config.BuildAndPushImage.PullImageRegistry.Server,
		UserName: config.Config.BuildAndPushImage.PullImageRegistry.Username,
		Password: config.Config.BuildAndPushImage.PullImageRegistry.Password,
	}})

	resources, err := getResources(info.Resource)
	if err != nil {
		return "", errors.Wrap(err, "parse job resources error")
	}

	kanikoArgs := []string{
		"--dockerfile=" + info.Info.DockerFilePath,
		"--context=" + info.Info.ContextPath,
	}
	for _, destination := range info.Info.Destinations {
		kanikoArgs = append(kanikoArgs, "--destination="+destination)
	}
	if info.Info.BuildArgs != nil {
		for k, v := range info.Info.BuildArgs {
			kanikoArgs = append(kanikoArgs, "--build-arg="+k+"="+v)
		}
	}

	go task.DoCreateBuildAndPushImageJob(
		taskId,
		&kubeclient.Job{
			Name:                  info.Name,
			NodeName:              nodeName,
			ActiveDeadlineSeconds: info.ActiveDeadlineSeconds,
			BackOffLimit:          &backOffLimit,
			Pod: kubeclient.Pod{
				Labels:        getDispatchLabel(info.Name, taskId, types.TaskActionCreate, types.JobTaskLabel),
				Volumes:       volumes,
				RestartPolicy: corev1.RestartPolicyNever,
				Containers: []kubeclient.Container{
					{
						Image:        config.Config.BuildAndPushImage.Image,
						Resources:    *resources,
						Env:          nil,
						Args:         kanikoArgs,
						VolumeMounts: volumeMounts,
					},
				},
				PullImageSecret: pullImageSecret,
			},
		},
		kanikoSecret,
	)

	return taskId, nil
}

// getKanikoPodVolume 获取kaniko需要的挂载盘
func getKanikoPodVolume(workloadName, volumeHostPathWorkloadName, kanikoSercretName string) []corev1.Volume {
	hostPath := filepath.Join(config.Config.Dispatch.Volume.HostPath.DataHostDir, workloadName)
	if volumeHostPathWorkloadName != "" {
		hostPath = filepath.Join(config.Config.Dispatch.Volume.HostPath.DataHostDir, volumeHostPathWorkloadName)
	}

	kanikoSercretVolume := &corev1.SecretVolumeSource{
		SecretName: kanikoSercretName,
		Items: []corev1.KeyToPath{
			{Key: corev1.DockerConfigJsonKey, Path: "config.json"},
		},
	}

	return []corev1.Volume{
		{
			Name: config.DataVolumeName,
			VolumeSource: corev1.VolumeSource{
				HostPath: &corev1.HostPathVolumeSource{
					Path: hostPath,
				},
			},
		},
		{
			Name: kanikoVolueName,
			VolumeSource: corev1.VolumeSource{
				Secret: kanikoSercretVolume,
			},
		},
	}
}

// getKanikoPodVolumeMount 获取与 getKanikoPodVolume 相关的mount
func getKanikoPodVolumeMount() []corev1.VolumeMount {
	return []corev1.VolumeMount{
		{
			Name:      config.DataVolumeName,
			MountPath: config.Config.Dispatch.VolumeMount.DataPath,
		},
		{
			Name:      kanikoVolueName,
			MountPath: "/kaniko/.docker/",
		},
	}
}

func getKanikoSecret(kanikoSecretName, workLoadName string, registries []types.Registry) *kubeclient.DockerSecret {
	if len(registries) == 0 {
		return nil
	}

	var rs []types.Registry
	for _, r := range registries {
		if r.UserName != "" && r.Server != "" && r.Password != "" {
			rs = append(rs, r)
		}
	}

	if len(rs) == 0 {
		return nil
	}

	return &kubeclient.DockerSecret{
		Name:       kanikoSecretName,
		Labels:     map[string]string{config.Config.Dispatch.Label: workLoadName},
		Registries: rs,
	}
}
