package job_docker

import (
	"context"
	"encoding/base64"
	"encoding/json"
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

const (
	LocalDockerBuildTmpDirName  = "docker_build_tmp"
	LocalDockerWorkSpaceDirName = "docker_workspace"
	DockerDataDir               = "/data/landun/workspace"
	DockerLogDir                = "/data/logs"
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

func parseApiDockerOptions(o api.DockerOptions) []string {
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

func ParseDockeroptions(dockerClient *client.Client, userOptions api.DockerOptions) (*ContainerConfig, error) {
	// 将指定好的options直接换成args
	argv := parseApiDockerOptions(userOptions)
	if len(argv) == 0 {
		return nil, nil
	}

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

// policy 为空，并且容器镜像的标签是 :latest， image-pull-policy 会自动设置为 always
// policy 为空，并且为容器镜像指定了非 :latest 的标签， image-pull-policy 就会自动设置为 if-not-present
func IfPullImage(localExist, islatest bool, policy string) bool {
	// 为空和枚举写错走一套逻辑
	switch policy {
	case api.ImagePullPolicyAlways.String():
		return true
	case api.ImagePullPolicyIfNotPresent.String():
		if !localExist {
			return true
		} else {
			return false
		}
	default:
		if islatest {
			return true
		} else {
			if !localExist {
				return true
			} else {
				return false
			}
		}
	}
}

// GenerateDockerAuth 创建拉取docker凭据
func GenerateDockerAuth(user, pass string) (string, error) {
	if user == "" || pass == "" {
		return "", nil
	}

	authConfig := types.AuthConfig{
		Username: user,
		Password: pass,
	}
	encodedJSON, err := json.Marshal(authConfig)
	if err != nil {
		return "", err
	}

	return base64.URLEncoding.EncodeToString(encodedJSON), nil
}
