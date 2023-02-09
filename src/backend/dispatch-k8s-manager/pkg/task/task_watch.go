package task

import (
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/kubeclient"
	"disaptch-k8s-manager/pkg/logs"
	"disaptch-k8s-manager/pkg/types"
	"fmt"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/watch"
	"strings"
)

func WatchTaskPod() {
	watchStopChan := make(chan bool)
	for {
		watcher, err := kubeclient.WatchPod(config.Config.Dispatch.Watch.Task.Label)
		if err != nil {
			logs.Error("WatchTaskPod WatchPod error", err)
			continue
		}

		go watchTaskPod(watcher, watchStopChan)

		<-watchStopChan
		logs.Warn("watchTaskPod chan reWake. ")
	}
}

func watchTaskPod(watcher watch.Interface, watchStopChan chan<- bool) {

	defer watcher.Stop()

loop:
	for {
		event, ok := <-watcher.ResultChan()
		if !ok {
			logs.Warn("watchTaskPod chan close. ")
			watchStopChan <- true
			break loop
		}

		pod, ok := event.Object.(*corev1.Pod)
		if !ok {
			continue
		}

		labelValue, ok := pod.Labels[config.Config.Dispatch.Watch.Task.Label]
		if !ok {
			logs.Warn(fmt.Sprintf("pod|%s no have task label", pod.Name))
			continue
		}

		taskId, labelType, action, ok := parseTaskLabelValue(labelValue)
		if !ok {
			logs.Error(fmt.Sprintf("watch task label value %s format error. ", labelValue))
			continue
		}

		switch labelType {
		case types.BuilderTaskLabel:
			watchBuilderTaskPodCreateOrStart(event, pod, taskId, action)
		case types.JobTaskLabel:
			watchJobTaskPodCreateOrStart(event, pod, taskId, action)
		default:
			logs.Error(fmt.Sprintf("watch task label labelType %s not support. ", labelType))
		}
	}
}

func parseTaskLabelValue(
	labelValue string,
) (taskId string, labelType types.TaskLabelType, action types.TaskAction, ok bool) {
	subs := strings.Split(labelValue, "-")
	if len(subs) < 3 {
		ok = false
		return
	}

	actionStr := subs[(len(subs) - 1)]
	labelTypeStr := subs[(len(subs) - 2)]

	taskId = strings.TrimSuffix(strings.TrimSuffix(labelValue, "-"+actionStr), "-"+labelTypeStr)
	labelType = types.TaskLabelType(labelTypeStr)
	action = types.TaskAction(actionStr)
	ok = true

	return taskId, labelType, action, ok
}

func WatchTaskDeployment() {
	watchStopChan := make(chan bool)
	for {
		watcher, err := kubeclient.WatchDeployment(config.Config.Dispatch.Watch.Task.Label)
		if err != nil {
			logs.Error("WatchTaskDeployment WatchDeployment error", err)
			continue
		}

		go watchTaskDeployment(watcher, watchStopChan)

		<-watchStopChan
		logs.Warn("watchTaskDeployment chan reWake. ")
	}
}

func watchTaskDeployment(watcher watch.Interface, watchStopChan chan<- bool) {

	defer watcher.Stop()

loop:
	for {
		event, ok := <-watcher.ResultChan()
		if !ok {
			logs.Warn("watchTaskDeployment chan close. ")
			watchStopChan <- true
			break loop
		}

		dep, ok := event.Object.(*appsv1.Deployment)
		if !ok {
			continue
		}
		labelValue, ok := dep.Labels[config.Config.Dispatch.Watch.Task.Label]
		if !ok {
			continue
		}

		taskId, labelType, action, ok := parseTaskLabelValue(labelValue)
		if !ok {
			logs.Error(fmt.Sprintf("watch task label value %s format error. ", labelValue))
		}

		switch labelType {
		case types.BuilderTaskLabel:
			watchBuilderTaskDeploymentStop(event, dep, taskId, action)
		default:
			logs.Error(fmt.Sprintf("watch task label labelType %s not support. ", labelType))
		}
	}
}
