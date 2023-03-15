package job_docker

import (
	"context"
	"fmt"
	"strings"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/network"
	"github.com/docker/docker/client"
	"github.com/mattn/go-shellwords"
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

func ParseDockeroptions(dockerClient *client.Client, userOptionStr string) (config *ContainerConfig, err error) {
	// 解析用户输入为shell args
	argv, err := shellwords.Parse(userOptionStr)
	if err != nil {
		errMsg := fmt.Sprintf("解析用户docker options失败: %s", err.Error())
		return nil, errors.New(errMsg)
	}

	// 解析shell args为flagSet
	var copts *containerOptions
	copts = addFlags(pflag.CommandLine)
	err = pflag.CommandLine.Parse(argv)
	if err != nil {
		errMsg := fmt.Sprintf("解析用户docker options失败: %s", err.Error())
		return nil, errors.New(errMsg)
	}

	// 获取当前仅支持的flag
	options, err := getDockerOptions()
	if err != nil {
		errMsg := fmt.Sprintf("获取docker options支持列表失败: %s", err.Error())
		return nil, errors.New(errMsg)
	}

	// 校验用户option是否符合预期
	pflag.CommandLine.Visit(func(f *pflag.Flag) {
		check := false
		for _, op := range options {
			if f.Name == op {
				check = true
			}
		}
		if !check {
			errMsg := fmt.Sprintf("用户docker option %s 不符合当前未支持列表: %s", f.Name, strings.Join(options, ","))
			err = errors.New(errMsg)
			return
		}
	})
	if err != nil {
		return nil, err
	}

	// Ping daemon 获取os
	ping, err := dockerClient.Ping(context.Background())
	if err != nil {
		errMsg := fmt.Sprintf("ping docker daemon 错误: %s", err.Error())
		return nil, errors.New(errMsg)
	}

	// 解析配置为可用docker配置, 目前只有linux支持，所以只使用linux相关配置
	containerConfig, err := parse(pflag.CommandLine, copts, ping.OSType)
	if err != nil {
		errMsg := fmt.Sprintf("解析用户docker options 为docker配置 错误: %s", err.Error())
		return nil, errors.New(errMsg)
	}

	return containerConfig, nil
}

// getBuild 从服务器认领要构建的信息
func getDockerOptions() ([]string, error) {
	result, err := api.GetDockerOptions()
	if err != nil {
		return nil, err
	}

	if result.IsNotOk() {
		logs.Error("get docker options failed, message", result.Message)
		return nil, errors.New("get docker options failed")
	}

	if result.Data == nil {
		return nil, nil
	}

	options := []string{}
	err = util.ParseJsonToData(result.Data, options)
	if err != nil {
		return nil, err
	}

	return options, nil
}
