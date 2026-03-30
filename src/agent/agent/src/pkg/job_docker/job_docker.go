package job_docker

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
)

const (
	LocalDockerBuildTmpDirName  = "docker_build_tmp"
	LocalDockerWorkSpaceDirName = "docker_workspace"
	DockerLogDir                = "/data/devops/logs"
)

func parseApiDockerOptions(o api.DockerOptions) []string {
	var args []string
	if len(o.Volumes) > 0 {
		for _, v := range o.Volumes {
			args = append(args, "--volume", strings.TrimSpace(v))
		}
	}

	if len(o.Mounts) > 0 {
		for _, m := range o.Mounts {
			args = append(args, "--mount", strings.TrimSpace(m))
		}
	}

	if o.Gpus != "" {
		args = append(args, "--gpus", strings.TrimSpace(o.Gpus))
	}

	if o.Privileged != false {
		args = append(args, "--privileged")
	}

	if len(o.Network) > 0 {
		for _, n := range o.Network {
			args = append(args, "--network", strings.TrimSpace(n))
		}
	}

	if o.User != "" {
		args = append(args, "--user", strings.TrimSpace(o.User))
	}

	return args
}

func BuildUserDockerArgs(userOptions api.DockerOptions) ([]string, error) {
	argv := parseApiDockerOptions(userOptions)
	for i := 0; i < len(argv); i++ {
		switch argv[i] {
		case "--volume", "--mount", "--network", "--user", "--gpus":
			if i+1 >= len(argv) || strings.TrimSpace(argv[i+1]) == "" {
				return nil, fmt.Errorf("docker option %s requires a non-empty value", argv[i])
			}
			if argv[i] == "--volume" {
				argv[i+1] = normalizeVolumeArg(argv[i+1])
			}
			i++
		}
	}
	return argv, nil
}

func normalizeVolumeArg(v string) string {
	// Only normalize relative host paths. Absolute Unix/Windows paths and named volumes
	// are returned as-is to avoid incorrectly splitting Windows drive-letter paths.
	if v == "." || strings.HasPrefix(v, "."+string(filepath.Separator)) || strings.HasPrefix(v, "./") || strings.HasPrefix(v, ".\\") {
		host, target, ok := strings.Cut(v, ":")
		if !ok {
			return v
		}
		if abs, err := filepath.Abs(host); err == nil {
			host = abs
		}
		return host + ":" + target
	}
	if !strings.Contains(v, ":") {
		return v
	}
	return v
}

func HasCustomNetwork(userOptions api.DockerOptions) bool {
	return len(userOptions.Network) > 0
}

// IfPullImage policy 为空，并且容器镜像的标签是 :latest， image-pull-policy 会自动设置为 always
// policy 为空，并且为容器镜像指定了非 :latest 的标签， image-pull-policy 就会自动设置为 if-not-present
func IfPullImage(localExist, isLatest bool, policy string) bool {
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
		if isLatest {
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

func NeedLocalImageInspect(isLatest bool, policy string) bool {
	switch policy {
	case api.ImagePullPolicyAlways.String():
		return false
	case api.ImagePullPolicyIfNotPresent.String():
		return true
	default:
		return !isLatest
	}
}

func EnsureDockerWorkspaceDirs() error {
	if err := os.MkdirAll(LocalDockerBuildTmpDirName, os.ModePerm); err != nil {
		return err
	}
	return os.MkdirAll(LocalDockerWorkSpaceDirName, os.ModePerm)
}
