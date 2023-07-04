/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package api

type ThirdPartyAgentStartInfo struct {
	HostName      string `json:"hostname"`
	HostIp        string `json:"hostIp"`
	DetectOs      string `json:"detectOS"`
	MasterVersion string `json:"masterVersion"`
	SlaveVersion  string `json:"version"`
}

type ThirdPartyBuildInfo struct {
	ProjectId       string                     `json:"projectId"`
	BuildId         string                     `json:"buildId"`
	VmSeqId         string                     `json:"vmSeqId"`
	Workspace       string                     `json:"workspace"`
	PipelineId      string                     `json:"pipelineId"`
	ToDelTmpFiles   []string                   `json:"-"` // #5806 增加异常时清理脚本文件列表, 不序列化
	DockerBuildInfo *ThirdPartyDockerBuildInfo `json:"dockerBuildInfo"`
	ExecuteCount    *int                       `json:"executeCount"`
	ContainerHashId string                     `json:"containerHashId"`
}

type BuildJobType string

const (
	AllBuildType    BuildJobType = "ALL"
	DockerBuildType BuildJobType = "DOCKER"
	BinaryBuildType BuildJobType = "BINARY"
)

type ThirdPartyDockerBuildInfo struct {
	AgentId         string        `json:"agentId"`
	SecretKey       string        `json:"secretKey"`
	Image           string        `json:"image"`
	Credential      Credential    `json:"credential"`
	Options         DockerOptions `json:"options"`
	ImagePullPolicy string        `json:"imagePullPolicy"`
}

type ImagePullPolicyEnum string

const (
	ImagePullPolicyAlways       ImagePullPolicyEnum = "always"
	ImagePullPolicyIfNotPresent ImagePullPolicyEnum = "if-not-present"
)

func (i ImagePullPolicyEnum) String() string {
	return string(i)
}

type Credential struct {
	User     string `json:"user"`
	Password string `json:"password"`
	ErrMsg   string `json:"errMsg"`
}

type DockerOptions struct {
	Volumes []string `json:"volumes"`
	Gpus    string   `json:"gpus"`
	Mounts  []string `json:"mounts"`
}

type ThirdPartyBuildWithStatus struct {
	ThirdPartyBuildInfo
	Success bool   `json:"success"`
	Message string `json:"message"`
	Error   *Error `json:"error"`
}

type Error struct {
	ErrorType    ErrorTypes `json:"errorType"`
	ErrorMessage string     `json:"errorMessage"`
	ErrorCode    ErrorCode  `json:"errorCode"`
}

type PipelineResponse struct {
	SeqId    string `json:"seqId"`
	Status   string `json:"status"`
	Response string `json:"response"`
}

type AgentHeartbeatInfo struct {
	MasterVersion           string                     `json:"masterVersion"`
	SlaveVersion            string                     `json:"slaveVersion"`
	HostName                string                     `json:"hostName"`
	AgentIp                 string                     `json:"agentIp"`
	ParallelTaskCount       int                        `json:"parallelTaskCount"`
	AgentInstallPath        string                     `json:"agentInstallPath"`
	StartedUser             string                     `json:"startedUser"`
	TaskList                []ThirdPartyTaskInfo       `json:"taskList"`
	Props                   AgentPropsInfo             `json:"props"`
	DockerParallelTaskCount int                        `json:"dockerParallelTaskCount"`
	DockerTaskList          []ThirdPartyDockerTaskInfo `json:"dockerTaskList"`
}

type ThirdPartyTaskInfo struct {
	ProjectId string `json:"projectId"`
	BuildId   string `json:"buildId"`
	VmSeqId   string `json:"vmSeqId"`
	Workspace string `json:"workspace"`
}

type ThirdPartyDockerTaskInfo struct {
	ProjectId string `json:"projectId"`
	BuildId   string `json:"buildId"`
	VmSeqId   string `json:"vmSeqId"`
}

type AgentPropsInfo struct {
	Arch              string             `json:"arch"`
	JdkVersion        []string           `json:"jdkVersion"`
	DockerInitFileMd5 DockerInitFileInfo `json:"dockerInitFileMd5"`
}

type AgentHeartbeatResponse struct {
	MasterVersion           string            `json:"masterVersion"`
	SlaveVersion            string            `json:"slaveVersion"`
	AgentStatus             string            `json:"agentStatus"`
	ParallelTaskCount       int               `json:"parallelTaskCount"`
	Envs                    map[string]string `json:"envs"`
	Gateway                 string            `json:"gateway"`
	FileGateway             string            `json:"fileGateway"`
	Props                   AgentPropsResp    `json:"props"`
	DockerParallelTaskCount int               `json:"dockerParallelTaskCount"`
	Language                string            `json:"language"`
}

type AgentPropsResp struct {
	IgnoreLocalIps string `json:"ignoreLocalIps"`
	KeepLogsHours  int    `json:"keepLogsHours"`
}

type UpgradeInfo struct {
	WorkerVersion      string             `json:"workerVersion"`
	GoAgentVersion     string             `json:"goAgentVersion"`
	JdkVersion         []string           `json:"jdkVersion"`
	DockerInitFileInfo DockerInitFileInfo `json:"dockerInitFileInfo"`
}

type DockerInitFileInfo struct {
	FileMd5     string `json:"fileMd5"`
	NeedUpgrade bool   `json:"needUpgrade"`
}

type UpgradeItem struct {
	Agent          bool `json:"agent"`
	Worker         bool `json:"worker"`
	Jdk            bool `json:"jdk"`
	DockerInitFile bool `json:"dockerInitFile"`
}

func NewPipelineResponse(seqId string, status string, response string) *PipelineResponse {
	return &PipelineResponse{
		SeqId:    seqId,
		Status:   status,
		Response: response,
	}
}

type LogType string

const (
	LogtypeLog   LogType = "LOG"
	LogtypeDebug LogType = "DEBUG"
	LogtypeError LogType = "ERROR"
	LogtypeWarn  LogType = "WARN"
)

type LogMessage struct {
	Message      string  `json:"message"`
	Timestamp    int64   `json:"timestamp"` // Millis
	Tag          string  `json:"tag"`
	JobId        string  `json:"jobId"`
	LogType      LogType `json:"logType"`
	ExecuteCount *int    `json:"executeCount"`
	SubTag       *string `json:"subTag"`
}

type ImageDebug struct {
	ProjectId   string        `json:"projectId"`
	BuildId     string        `json:"buildId"`
	VmSeqId     string        `json:"vmSeqId"`
	Workspace   string        `json:"workspace"`
	PipelineId  string        `json:"pipelineId"`
	DebugUserId string        `json:"debugUserId"`
	DebugId     int64         `json:"debugId"`
	Image       string        `json:"image"`
	Credential  Credential    `json:"credential"`
	Options     DockerOptions `json:"options"`
}

type ImageDebugFinish struct {
	ProjectId  string `json:"projectId"`
	DebugId    int64  `json:"debugId"`
	PipelineId string `json:"pipelineId"`
	DebugUrl   string `json:"debugUrl"`
	Success    bool   `json:"success"`
	Error      *Error `json:"error"`
}
