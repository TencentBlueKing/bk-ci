package task

import (
	"disaptch-k8s-manager/pkg/db/mysql"
	"disaptch-k8s-manager/pkg/logs"
	"disaptch-k8s-manager/pkg/types"
)

func InitTask() {
	go WatchTaskPod()
	go WatchTaskDeployment()
}

func OkTask(taskId string) {
	err := mysql.UpdateTask(taskId, types.TaskSucceeded, "")
	if err != nil {
		logs.Errorf("save OkTask %s error %s", taskId, err.Error())
	}
}

func OkTaskWithMessage(taskId string, message string) {
	err := mysql.UpdateTask(taskId, types.TaskSucceeded, message)
	if err != nil {
		logs.Errorf("save OkTaskWithMessage %s %s error %s", taskId, message, err.Error())
	}
}

func UpdateTask(taskId string, state types.TaskState) {
	err := mysql.UpdateTask(taskId, state, "")
	if err != nil {
		logs.Errorf("save UpdateTask %s %s error %s", taskId, state, err.Error())
	}
}

func FailTask(taskId string, message string) {
	err := mysql.UpdateTask(taskId, types.TaskFailed, message)
	if err != nil {
		logs.Errorf("save FailTask %s %s error %s", taskId, message, err.Error())
	}
}
