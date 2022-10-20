package task

import (
	"disaptch-k8s-manager/pkg/db/mysql"
	"disaptch-k8s-manager/pkg/logs"
	"disaptch-k8s-manager/pkg/types"
	"github.com/pkg/errors"
)

func InitTask() {
	go WatchTaskPod()
	go WatchTaskDeployment()
}

func okTask(taskId string) {
	err := mysql.UpdateTask(taskId, types.TaskSucceeded, "")
	if err != nil {
		logs.Error(errors.Wrapf(err, "save okTask %s %s error. ", taskId, ""))
	}
}

func updateTask(taskId string, state types.TaskState) {
	err := mysql.UpdateTask(taskId, state, "")
	if err != nil {
		logs.Error(errors.Wrapf(err, "update okTask %s %s error. ", taskId, ""))
	}
}

func failTask(taskId string, message string) {
	err := mysql.UpdateTask(taskId, types.TaskFailed, message)
	if err != nil {
		logs.Error(errors.Wrapf(err, "save failTask %s %s error. ", taskId, message))
	}
}
