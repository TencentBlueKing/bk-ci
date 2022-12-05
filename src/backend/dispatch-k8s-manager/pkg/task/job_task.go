package task

import (
	"disaptch-k8s-manager/pkg/kubeclient"
	"disaptch-k8s-manager/pkg/logs"
	"disaptch-k8s-manager/pkg/types"
	"fmt"
	"github.com/pkg/errors"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/watch"
)

func DoCreateBuildAndPushImageJob(
	taskId string,
	job *kubeclient.Job,
	kanikoSecret *kubeclient.DockerSecret,
) {
	// 创建镜像拉取凭据
	_, err := kubeclient.CreateDockerRegistry(job.Pod.PullImageSecret)
	if err != nil {
		failTask(taskId, errors.Wrap(err, "create build and push image job pull image secret error").Error())
		return
	}

	if _, err = kubeclient.CreateDockerRegistry(kanikoSecret); err != nil {
		failTask(taskId, errors.Wrap(err, "create build and push image push secret error").Error())
		return
	}

	err = kubeclient.CreateJob(job)
	if err == nil {
		return
	}

	failTask(taskId, errors.Wrap(err, "create job error").Error())
	deleteJobLinkRes(job.Name)
}

func DoCreateJob(taskId string, job *kubeclient.Job) {
	_, err := kubeclient.CreateDockerRegistry(job.Pod.PullImageSecret)
	if err != nil {
		failTask(taskId, errors.Wrap(err, "create job pull image secret error").Error())
		return
	}

	err = kubeclient.CreateJob(job)
	if err == nil {
		return
	}

	// 创建失败后的操作
	failTask(taskId, errors.Wrap(err, "create job error").Error())
	deleteJobLinkRes(job.Name)

}

func DoDeleteJob(taskId string, jobName string) {
	err := kubeclient.DeleteJob(jobName)
	if err != nil {
		failTask(taskId, errors.Wrap(err, "delete job error").Error())
		return
	}

	deleteJobLinkRes(jobName)

	okTask(taskId)
}

// deleteJobLinkRes 删除JOB相关联的kubernetes资源
func deleteJobLinkRes(jobName string) {
	// 删除相关连的其他资源
	deleteSecret(jobName)
}

func deleteSecret(jobName string) {
	sl, err := kubeclient.ListSecret(jobName)
	if err != nil {
		logs.Warn(fmt.Sprintf("delete builder get secret %s error ", jobName), err)
		return
	}
	if len(sl) == 0 {
		return
	}

	for _, s := range sl {
		if err = kubeclient.DeleteSecret(s.Name); err != nil {
			logs.Error(fmt.Sprintf("delete builder remove secret %s error ", s.Name), err)
		}
	}
}

func watchJobTaskPodCreateOrStart(event watch.Event, pod *corev1.Pod, taskId string, action types.TaskAction) {
	// 只观察start或者create相关，stop和stop不watch
	if action != types.TaskActionCreate && action != types.TaskActionStart {
		return
	}

	podStatus := pod.Status
	logs.Info(fmt.Sprintf("job|task|%s|pod|%s|statue|%s|type|%s", taskId, pod.Name, podStatus.Phase, event.Type))

	switch event.Type {
	case watch.Added, watch.Modified:
		{
			switch podStatus.Phase {
			case corev1.PodPending:
				updateTask(taskId, types.TaskRunning)
			// 对于task的start/create来说，启动了就算成功，而不关系启动成功还是失败了
			case corev1.PodRunning, corev1.PodSucceeded, corev1.PodFailed:
				okTask(taskId)
			case corev1.PodUnknown:
				updateTask(taskId, types.TaskUnknown)
			}
		}
	case watch.Error:
		{
			logs.Error("add job error. ", pod)
			failTask(taskId, podStatus.Message+"|"+podStatus.Reason)
		}
	}
}
