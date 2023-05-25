package task

import (
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/db/mysql"
	"disaptch-k8s-manager/pkg/db/redis"
	"disaptch-k8s-manager/pkg/kubeclient"
	"disaptch-k8s-manager/pkg/logs"
	"disaptch-k8s-manager/pkg/prometheus"
	"disaptch-k8s-manager/pkg/types"
	"fmt"
	"github.com/pkg/errors"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/watch"
	"time"
)

func DoCreateBuilder(taskId string, dep *kubeclient.Deployment) {
	_, err := kubeclient.CreateDockerRegistry(dep.Pod.PullImageSecret)
	if err != nil {
		failTask(taskId, errors.Wrap(err, "create builder pull image secret error").Error())
		return
	}

	err = kubeclient.CreateDeployment(dep)
	if err == nil {
		return
	}

	// 创建失败后的操作
	failTask(taskId, errors.Wrap(err, "create builder error").Error())
	deleteBuilderLinkRes(dep.Name)
}

func DoStartBuilder(taskId string, builderName string, data []byte) {
	err := kubeclient.PatchDeployment(builderName, data)
	if err != nil {
		failTask(taskId, errors.Wrap(err, "start builder error").Error())
		return
	}
}

func DoStopBuilder(taskId string, builderName string, data []byte) {
	// 停止前获取pod状态给停止后使用
	var err error
	var pods []*corev1.Pod
	if config.UseRealResourceUsage() {
		pods, err = kubeclient.ListPod(builderName)
		if err != nil {
			logs.Error(fmt.Sprintf("DoStopBuilder %s list pod error ", builderName), err)
			pods = nil
		}
	}

	err = kubeclient.PatchDeployment(builderName, data)
	if err != nil {
		failTask(taskId, errors.Wrap(err, "stop builder error").Error())
		return
	}

	// 停止之后计算其资源使用量
	if err = saveRealResourceUsage(builderName, pods); err != nil {
		logs.Error("DoStopBuilder|saveRealResourceUsage error", err)
	}
}

// saveRealResourceUsage 计算并保存真实资源用量
func saveRealResourceUsage(builderName string, pods []*corev1.Pod) error {
	if !config.UseRealResourceUsage() {
		return nil
	}
	if pods == nil || len(pods) < 1 {
		return nil
	}
	if len(pods[0].Spec.Containers) < 1 {
		return fmt.Errorf("DoStopBuilder pod %s no container", pods[0].Name)
	}

	podName := pods[0].Name
	containerName := pods[0].Spec.Containers[0].Name
	// 时间起始时间秒数至0，结束时间分钟向后一位，秒数至0，方便计算
	podCreateTime := pods[0].ObjectMeta.CreationTimestamp.Time
	podCreateTime = podCreateTime.Add(time.Duration(-1*podCreateTime.Second()) * time.Second)
	stopTime := time.Now()
	stopTime = stopTime.Add(time.Minute)
	stopTime = stopTime.Add(time.Duration(-1*stopTime.Second()) * time.Second)

	usage, err := prometheus.QueryRangeContainerResourceUsage(podName, containerName, podCreateTime, stopTime)
	if err != nil {
		return err
	}
	if usage == nil {
		return nil
	}

	b, err := mysql.SelectScheduledInfo(builderName)
	if err != nil {
		return err
	}
	var builder *types.ScheduledInfo
	if b != nil {
		builder = b
	} else {
		builder = new(types.ScheduledInfo)
	}
	builder.BuilderName = builderName
	// 最新的节点添加在头部
	builder.ResourceHistory = append(builder.ResourceHistory, *usage)
	copy(builder.ResourceHistory[1:], builder.ResourceHistory[0:])
	builder.ResourceHistory[0] = *usage
	if len(builder.ResourceHistory) > config.BuilderRealResourceHisSize {
		builder.ResourceHistory = builder.ResourceHistory[:10]
	}

	if err = mysql.InsertOrUpdateScheduledInfo(*builder); err != nil {
		return err
	}

	return nil
}

func DoDeleteBuilder(taskId string, builderName string) {
	err := kubeclient.DeleteDeployment(builderName)
	if err != nil {
		failTask(taskId, errors.Wrap(err, "delete builder error").Error())
		return
	}

	deleteBuilderLinkRes(builderName)
	deleteBuilderLinkDbData(builderName)

	okTask(taskId)
}

// deleteBuilderLinkRes 删除构建机相关联的kubernetes资源
func deleteBuilderLinkRes(builderName string) {
	// 删除关联的secret凭据
	sl, err := kubeclient.ListSecret(builderName)
	if err != nil {
		logs.Warn(fmt.Sprintf("delete builder get secret %s error ", builderName), err)
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

// deleteBuilderLinkDbData 删除构建机相关联的DB数据
func deleteBuilderLinkDbData(builderName string) {
	if err := mysql.DeleteScheduledInfoByName(builderName); err != nil {
		logs.Error(fmt.Sprintf("delete builder %s scheduled info data error", builderName), err)
	}
}

const builderTaskStatusUpdateLock = "kubernetes-manager:builder:task-update-lock:"

func watchBuilderTaskPodCreateOrStart(event watch.Event, pod *corev1.Pod, taskId string, action types.TaskAction) {
	// 只观察start或者create相关，stop和stop不通过这个逻辑观察
	if action != types.TaskActionCreate && action != types.TaskActionStart {
		return
	}

	podStatus := pod.Status
	logs.Info(fmt.Sprintf("builder|task|%s|pod|%s|statue|%s|type|%s", taskId, pod.Name, podStatus.Phase, event.Type))

	builderName, _ := pod.Labels[config.Config.Dispatch.Label]

	switch event.Type {
	case watch.Added, watch.Modified:
		{
			switch podStatus.Phase {
			case corev1.PodPending:
				updateTask(taskId, types.TaskRunning)
			// 对于task的start/create来说，启动了就算成功，而不关心启动成功还是失败了
			case corev1.PodRunning, corev1.PodSucceeded, corev1.PodFailed:
				{
					// 获取是否存在已有的task
					oldTask, err := mysql.SelectTaskStatus(taskId)
					if err != nil {
						logs.Error("mysql select task ", taskId, " error", err)
					}

					// 首先判断是否已经存在更新过状态的
					if oldTask != nil && oldTask.Status != nil &&
						(*oldTask.Status == types.TaskSucceeded || *oldTask.Status == types.TaskFailed) {
						return
					}

					// 因为节点调度历史存在一致性问题，需要加锁
					key := builderTaskStatusUpdateLock + taskId
					lock, err := redis.Lock(key, 30*time.Second)
					if err != nil {
						// 如果出错则继续更新，当前逻辑无一直性问题，不更新反而可能卡住构建机
						logs.Error("builder ", builderName, " update status redis lock error", err)
					}
					if !lock {
						// 未抢占成功直接退出，说明已经有写入逻辑在进行，不用重复写入了
						return
					}
					defer redis.UnLock(key)

					okTask(taskId)

					// mysql中保存分配至节点成功的构建机最近三次节点信息，用来做下一次调度的依据
					if builderName == "" {
						logs.Warn(taskId, "add builder builder name is null")
						return
					}
					err = saveNodeScheduledInfo(builderName, pod.Spec.NodeName)
					if err != nil {
						logs.Error("add builder scheduled info error ", err)
						return
					}

					return
				}
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

func saveNodeScheduledInfo(builderName string, nodeName string) error {
	b, err := mysql.SelectScheduledInfo(builderName)
	if err != nil {
		return err
	}
	var builder *types.ScheduledInfo
	if b != nil {
		builder = b
	} else {
		builder = new(types.ScheduledInfo)
	}
	builder.BuilderName = builderName
	// 最新的节点添加在头部
	builder.NodeHistory = append(builder.NodeHistory, nodeName)
	copy(builder.NodeHistory[1:], builder.NodeHistory[0:])
	builder.NodeHistory[0] = nodeName
	if len(builder.NodeHistory) > config.BuilderNodeHisSize {
		builder.NodeHistory = builder.NodeHistory[:config.BuilderNodeHisSize]
	}

	if err = mysql.InsertOrUpdateScheduledInfo(*builder); err != nil {
		return err
	}

	return nil
}

func watchBuilderTaskDeploymentStop(event watch.Event, dep *appsv1.Deployment, taskId string, action types.TaskAction) {
	if action == types.TaskActionStop {
		switch event.Type {
		case watch.Modified:
			if dep.Spec.Replicas != nil && *dep.Spec.Replicas == 0 {
				okTask(taskId)
			}
		case watch.Error:
			logs.Error("stop builder error. ", dep)
			if len(dep.Status.Conditions) > 0 {
				failTask(taskId, dep.Status.Conditions[0].String())
			} else {
				failTask(taskId, "stop builder error")
			}
		}
	}
}
