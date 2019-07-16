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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
    ProjectId string `json:"projectId"`
    BuildId   string `json:"buildId"`
    VmSeqId   string `json:"vmSeqId"`
    Workspace string `json:"workspace"`
}

type PipelineResponse struct {
    SeqId    string `json:"seqId"`
    Status   string `json:"status"`
    Response string `json:"response"`
}

type AgentHeartbeatInfo struct {
    MasterVersion     string                `json:"masterVersion"`
    SlaveVersion      string                `json:"slaveVersion"`
    HostName          string                `json:"hostName"`
    AgentIp           string                `json:"agentIp"`
    ParallelTaskCount int                   `json:"parallelTaskCount"`
    AgentInstallPath  string                `json:"agentInstallPath"`
    StartedUser       string                `json:"startedUser"`
    TaskList          []ThirdPartyBuildInfo `json:"taskList"`
}

type AgentHeartbeatResponse struct {
    MasterVersion     string            `json:"masterVersion"`
    SlaveVersion      string            `json:"slaveVersion"`
    AgentStatus       string            `json:"agentStatus"`
    ParallelTaskCount int               `json:"parallelTaskCount"`
    Envs              map[string]string `json:"envs"`
}

func NewPipelineResponse(seqId string, status string, response string) *PipelineResponse {
    return &PipelineResponse{
        SeqId:    seqId,
        Status:   status,
        Response: response,
    }
}
