package service

import "disaptch-k8s-manager/pkg/types"

type TaskId struct {
	TaskId string `json:"taskId"`
}

type TaskStatus struct {
	Status types.TaskState `json:"status"`
	Detail string          `json:"detail"`
}
