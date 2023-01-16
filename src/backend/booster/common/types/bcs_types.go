/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package types

import (
	"time"
)

// ReplicaControllerStatus define the bcs application status.
type ReplicaControllerStatus string

const (
	RCStaging   ReplicaControllerStatus = "Staging"
	RCDeploying ReplicaControllerStatus = "Deploying"
	RCRunning   ReplicaControllerStatus = "Running"
	RCOperating ReplicaControllerStatus = "Operating"
	RCFinish    ReplicaControllerStatus = "Finish"
	RCError     ReplicaControllerStatus = "Error"
)

// ObjectMeta define the bcs resource meta data.
type ObjectMeta struct {
	Name              string            `json:"name"`
	NameSpace         string            `json:"namespace"`
	CreationTimestamp time.Time         `json:"creationTimestamp,omitempty"`
	Labels            map[string]string `json:"labels,omitempty"`
	Annotations       map[string]string `json:"annotations,omitempty"`
	ClusterName       string            `json:"clusterName,omitempty"`
}

// BcsReplicaControllerStatus define ReplicaController status
type BcsReplicaControllerStatus struct {
	ObjectMeta     `json:"metadata"`
	CreateTime     time.Time               `json:"createTime"`
	LastUpdateTime time.Time               `json:"lastUpdateTime,omitempty"`
	ReportTime     time.Time               `json:"reportTime,omitempty"`
	Status         ReplicaControllerStatus `json:"status"`
	LastStatus     ReplicaControllerStatus `json:"lastStatus,omitempty"`
	Message        string                  `json:"message, omitempty"`
	Pods           []*BcsPodIndex          `json:"pods"`

	// Instance is the number of requested instance
	Instance int `json:"instance"`
	// BuildedInstance is the number of actual instance
	BuildedInstance int `json:"buildedInstance"`
	// RunningInstance is the number of running status instance
	RunningInstance int `json:"runningInstance"`
}

// PodStatus define the bcs task group status
type PodStatus string

const (
	PodStaging  PodStatus = "Staging"
	PodStarting PodStatus = "Starting"
	PodRunning  PodStatus = "Running"
	PodError    PodStatus = "Error"
	PodKilling  PodStatus = "Killing"
	PodKilled   PodStatus = "Killed"
	PodFailed   PodStatus = "Failed"
	PodFinish   PodStatus = "Finish"
)

// BcsPodIndex describe the pod name data in application status.
type BcsPodIndex struct {
	Name string `json:"name"`
}

// BcsPodStatus define pod status
type BcsPodStatus struct {
	ObjectMeta        `json:"metadata"`
	RcName            string                `json:"rcname, omitempty"`
	Status            PodStatus             `json:"status, omitempty"`
	LastStatus        PodStatus             `json:"lastStatus, omitempty"`
	HostIP            string                `json:"hostIP, omitempty"`
	HostName          string                `json:"hostName"`
	PodIP             string                `json:"podIP, omitempty"`
	Message           string                `json:"message, omitempty"`
	StartTime         time.Time             `json:"startTime, omitempty"`
	LastUpdateTime    time.Time             `json:"lastUpdateTime, omitempty"`
	ReportTime        time.Time             `json:"reportTime, omitempty"`
	ContainerStatuses []*BcsContainerStatus `json:"containerStatuses, omitempty"`
	BcsMessage        string                `json:"bcsMessage, omitempty"`
}

// ContainerStatus define the bcs container status.
type ContainerStatus string

// There are the valid statuses of container
const (
	ContainerStaging  ContainerStatus = "Staging"
	ContainerStarting ContainerStatus = "Starting"
	ContainerRunning  ContainerStatus = "Running"
	ContainerKilling  ContainerStatus = "Killing"
	ContainerKilled   ContainerStatus = "Killed"
	ContainerFinish   ContainerStatus = "Finish"
	ContainerFailed   ContainerStatus = "Failed"
	ContainerError    ContainerStatus = "Error"
)

// BcsContainerStatus define container status
type BcsContainerStatus struct {
	Name              string               `json:"name"`
	ContainerID       string               `json:"containerID"`
	RestartCount      int32                `json:"restartCount, omitempty"`
	Status            ContainerStatus      `json:"status, omitempty"`
	LastStatus        ContainerStatus      `json:"lastStatus, omitempty"`
	TerminateExitCode int                  `json:"exitcode, omitempty"`
	Image             string               `json:"image"`
	Message           string               `json:"message, omitempty"`
	StartTime         time.Time            `json:"startTime,omitempty"`
	LastUpdateTime    time.Time            `json:"lastUpdateTime,omitempty"`
	FinishTime        time.Time            `json:"finishTime,omitempty"`
	Ports             []ContainerPort      `json:"containerPort,omitempty"`
	Command           string               `json:"command,omitempty"`
	Args              []string             `json:"args,omitempty"`
	Network           string               `json:"networkMode,omitempty"`
	Labels            map[string]string    `json:"labels,omitempty"`
	Resources         ResourceRequirements `json:"resources,omitempty"`
	Env               map[string]string    `json:"env,omitempty"`
}

// ContainerPort represents a network port in a single container
type ContainerPort struct {
	//each named port in a pod must have a unique name.
	Name          string `json:"name"`
	HostPort      int    `json:"hostPort,omitempty"`
	ContainerPort int    `json:"containerPort"`
	HostIP        string `json:"hostIP,omitempty"`
	Protocol      string `json:"protocol"`
}

// ResourceRequirements describes the compute resource requirement
type ResourceRequirements struct {
	Limits   ResourceItem `json:"limits,omitempty"`
	Requests ResourceItem `json:"requests,omitempty"`
}

// ResourceItem define the resource.
type ResourceItem struct {
	CPU     string `json:"cpu,omitempty"`
	Mem     string `json:"memory,omitempty"`
	Storage string `json:"storage,omitempty"`
}

// Image define a worker iamge on devops
type Image struct {
	Name             string   `json:"param_name"`
	Value            string   `json:"param_value"`
	ProjectWhitelist []string `json:"visual_range"`
}

// WorkerImage define a worker iamge on devops
type WorkerImage struct {
	Mesos []Image `json:"mesos"`
	K8s   []Image `json:"k8s"`
}
