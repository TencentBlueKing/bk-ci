package service

type DockerInspectInfo struct {
	Name       string     `json:"name" binding:"required"` // 任务名称，唯一
	Ref        string     `json:"ref" binding:"required"`  // docker镜像信息 如：docker:latest
	Credential Credential `json:"cred"`                    // 拉取镜像凭据
}

type Credential struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type DockerInspectResp struct {
	Architecture string `json:"arch"`      // 架构
	Os           string `json:"os"`        // 系统
	Size         int64  `json:"size"`      // 大小
	Created      string `json:"created"`   // 创建时间
	Id           string `json:"id"`        // id
	Author       string `json:"author"`    // 作者
	Parent       string `json:"parent"`    // 父镜像信息
	OsVersion    string `json:"osVersion"` // 系统版本
}
