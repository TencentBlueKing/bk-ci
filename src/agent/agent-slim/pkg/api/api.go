package api

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg/constant"
	"github.com/pkg/errors"
)

var devopsClient *devopsClientType

type devopsClientType struct {
	client *http.Client
}

func init() {
	devopsClient = &devopsClientType{
		client: http.DefaultClient,
	}
}

type PersistenceBuildInfo struct {
	ProjectId       string `json:"projectId"`
	BuildId         string `json:"buildId"`
	VmSeqId         string `json:"vmSeqId"`
	Workspace       string `json:"workspace"`
	PipelineId      string `json:"pipelineId"`
	AgentId         string `json:"agentId"`
	SecretKey       string `json:"secretKey"`
	ExecuteCount    *int   `json:"executeCount"`
	ContainerHashId string `json:"containerHashId"`
}

func StartUp() (*PersistenceBuildInfo, error) {
	url := fmt.Sprintf("%s/ms/dispatch-devcloud/api/buildAgent/agent/devcloud/startup", config.Config.GateWay)

	req, err := http.NewRequestWithContext(context.Background(), "GET", url, nil)

	req.Header = GetAuthHeaderMap()
	resp, err := devopsClient.client.Do(req)
	if err != nil {
		return nil, errors.Wrap(err, "request StartUp failed")
	}
	defer resp.Body.Close()

	data, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, errors.Wrap(err, "read StartUp error")
	}
	result, err := IntoDevopsResult[PersistenceBuildInfo](data)
	if err != nil {
		return nil, errors.Wrap(err, "parse StartUp result error")
	}

	return result, nil
}

type PersistenceBuildWithStatus struct {
	PersistenceBuildInfo
	Success bool   `json:"success"`
	Message string `json:"message"`
	Error   *Error `json:"error"`
}

type Error struct {
	ErrorType    ErrorTypes `json:"errorType"`
	ErrorMessage string     `json:"errorMessage"`
	ErrorCode    ErrorCode  `json:"errorCode"`
}

func WorkerBuildFinish(buildInfo *PersistenceBuildWithStatus) (bool, error) {
	url := fmt.Sprintf("%s/ms/dispatch-devcloud/api/buildAgent/agent/devcloud/workerBuildFinish", config.Config.GateWay)

	body, err := json.Marshal(buildInfo)
	if err != nil {
		return false, err
	}
	logs.Debug("WorkerBuildFinish body", string(body))
	req, err := http.NewRequestWithContext(context.Background(), "POST", url, bytes.NewReader(body))

	req.Header = GetAuthHeaderMap()
	req.Header.Set("Content-Type", "application/json; charset=UTF-8")
	resp, err := devopsClient.client.Do(req)
	if err != nil {
		return false, errors.Wrap(err, "request WorkerBuildFinish failed")
	}
	defer resp.Body.Close()

	data, err := io.ReadAll(resp.Body)
	if err != nil {
		return false, errors.Wrap(err, "read WorkerBuildFinish result error")
	}
	result, err := IntoDevopsResult[bool](data)
	if err != nil {
		return false, errors.Wrap(err, "parse WorkerBuildFinish result error")
	}
	if result == nil {
		return false, nil
	}

	return *result, nil
}

// GetAuthHeaderMap 生成鉴权头部
func GetAuthHeaderMap() http.Header {
	authHeaderMap := make(http.Header)
	authHeaderMap.Set(constant.AuthHeaderBuildType, constant.AuthHeaderBuildTypeValue)
	authHeaderMap.Set(constant.AuthHeaderProjectId, config.Config.ProjectId)
	authHeaderMap.Set(constant.AuthHeaderAgentId, config.Config.ContainerName)
	return authHeaderMap
}

type DevopsHttpResult struct {
	Data    any    `json:"data"`
	Status  int    `json:"status"`
	Message string `json:"message"`
}

// IntoDevopsResult 解析devops后台数据类型
func IntoDevopsResult[T any](body []byte) (*T, error) {
	res := &DevopsHttpResult{}
	err := json.Unmarshal(body, res)
	if err != nil {
		return nil, errors.Wrap(err, "parse devops result error")
	}

	if res.Status != 0 {
		return nil, errors.Errorf("devops result status error %s", res.Message)
	}

	if res.Data == nil {
		return nil, nil
	}

	data, err := json.Marshal(res.Data)
	if err != nil {
		return nil, errors.Wrap(err, "marshal davops result data error")
	}

	result := new(T)
	err = json.Unmarshal(data, result)
	if err != nil {
		return nil, errors.Wrap(err, "parse devops result data error")
	}

	return result, err
}
