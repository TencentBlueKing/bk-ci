package thirdpartapi

import (
	"common/devops"
	"remoting/pkg/config"
)

type ThirdPartApi struct {
	Server *devops.RemoteDevClient
}

func InitThirdpartApi(config *config.Config) *ThirdPartApi {
	return &ThirdPartApi{
		Server: initServerApi(config.WorkSpace.BackendHost),
	}
}
