package service

import (
	"disaptch-k8s-manager/pkg/db/mysql"
)

func GetTaskStatus(taskId string) (*TaskStatus, error) {
	task, err := mysql.SelectTaskStatus(taskId)
	if err != nil {
		return nil, err
	}

	if task == nil {
		return nil, nil
	}

	return &TaskStatus{
		Status: *task.Status,
		Detail: string(task.Message),
	}, nil
}
