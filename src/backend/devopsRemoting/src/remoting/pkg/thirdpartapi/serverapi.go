package thirdpartapi

import (
	"common/devops"
)

var (
	Sha1key string
)

func initServerApi(host string) *devops.RemoteDevClient {
	return devops.NewRemoteDevClient(host, Sha1key)
}
