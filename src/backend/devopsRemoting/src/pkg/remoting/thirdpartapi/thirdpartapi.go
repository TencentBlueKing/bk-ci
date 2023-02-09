package thirdpartapi

import "devopsRemoting/src/pkg/remoting/config"

type ThirdPartApi struct {
	Server *ServerApi
}

func InitThirdpartApi(config *config.Config) *ThirdPartApi {
	return &ThirdPartApi{
		Server: initServerApi(config.WorkSpace.BackendHost),
	}
}
