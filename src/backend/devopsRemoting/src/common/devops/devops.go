package devops

import (
	"common/logs"
	"strings"
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
