package job_docker

import (
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/network"
)

type DockerHostInfo struct {
	ContainerCreateInfo ContainerCreateInfo
}

type ImagePullInfo struct {
	ImageName string
	AuthType  types.AuthConfig
}

type ContainerCreateInfo struct {
	ContainerName    string
	Config           *container.Config
	HostConfig       *container.HostConfig
	NetWorkingConfig *network.NetworkingConfig
}
