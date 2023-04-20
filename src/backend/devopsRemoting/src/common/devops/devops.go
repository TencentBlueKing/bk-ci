package devops

import (
	"common/logs"
	"encoding/json"
	"strings"

	"github.com/pkg/errors"
)

type DevopsHttpResult struct {
	Data    any    `json:"data"`
	Status  int    `json:"status"`
	Message string `json:"message"`
}

// ParseWsId2UserProjectId 从工作空间ID解析用户个人项目ID
// 项目ID为 _userid  工作空间ID为 userid(xx|x-xx)-xxx
func ParseWsId2UserProjectId(workspaceId string) string {
	workspaceSub := strings.Split(workspaceId, "-")
	if len(workspaceSub) < 2 || len(workspaceSub) > 3 {
		logs.Errorf("worksapceid %s format error", workspaceId)
		return ""
	}
	userId := "_" + workspaceSub[0]
	if len(workspaceSub) == 3 {
		userId = userId + "_" + workspaceSub[1]
	}
	return userId
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
