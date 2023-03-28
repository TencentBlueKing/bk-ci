package job_docker

import (
	"context"
	"fmt"
	"os"
	"strings"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/api"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/network"
	"github.com/docker/docker/client"
	"github.com/pkg/errors"
	"github.com/spf13/pflag"
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

func parseApiDockerOptions(o *api.DockerOptions) []string {
	args := []string{}
	if o.Volumes != nil && len(o.Volumes) > 0 {
		for _, v := range o.Volumes {
			args = append(args, "--volume", strings.TrimSpace(v))
		}
	}

	if o.Mounts != nil && len(o.Mounts) > 0 {
		for _, m := range o.Mounts {
			args = append(args, "--mount", strings.TrimSpace(m))
		}
	}

	if o.Gpus != "" {
		args = append(args, "--gpus", strings.TrimSpace(o.Gpus))
	}

	return args
}

func ParseDockeroptions(dockerClient *client.Client, userOptions *api.DockerOptions) (*ContainerConfig, error) {
	// 将指定好的options直接换成args
	argv := parseApiDockerOptions(userOptions)

	// 解析args为flagSet
	flagset := pflag.NewFlagSet(os.Args[0], pflag.ContinueOnError)
	copts := addFlags(flagset)
	err := flagset.Parse(argv)
	if err != nil {
		errMsg := fmt.Sprintf("解析用户docker options失败: %s", err.Error())
		return nil, errors.New(errMsg)
	}

	// Ping daemon 获取os
	ping, err := dockerClient.Ping(context.Background())
	if err != nil {
		errMsg := fmt.Sprintf("ping docker daemon 错误: %s", err.Error())
		return nil, errors.New(errMsg)
	}

	// 解析配置为可用docker配置, 目前只有linux支持，所以只使用linux相关配置
	containerConfig, err := parse(flagset, copts, ping.OSType)
	if err != nil {
		errMsg := fmt.Sprintf("解析用户docker options 为docker配置 错误: %s", err.Error())
		return nil, errors.New(errMsg)
	}

	return containerConfig, nil
}
