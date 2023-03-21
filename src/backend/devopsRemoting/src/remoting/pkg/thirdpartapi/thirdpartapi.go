package thirdpartapi

import "remoting/pkg/config"

type ThirdPartApi struct {
	Server *ServerApi
}

func InitThirdpartApi(config *config.Config) *ThirdPartApi {
	return &ThirdPartApi{
		Server: initServerApi(config.WorkSpace.BackendHost),
	}
}
