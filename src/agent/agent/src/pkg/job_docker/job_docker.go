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
			if len(strings.TrimSpace(v)) == 0 {
				continue
			}
			args = append(args, "--volume", strings.TrimSpace(v))
		}
	}

	if len(o.Mounts) > 0 {
		for _, m := range o.Mounts {
			if len(strings.TrimSpace(m)) == 0 {
				continue
			}
			args = append(args, "--mount", strings.TrimSpace(m))
		}
	}

	if len(strings.TrimSpace(o.Gpus)) != 0 {
		args = append(args, "--gpus", strings.TrimSpace(o.Gpus))
	}

	if o.Privileged != false {
		args = append(args, "--privileged")
	}

	if len(o.Network) > 0 {
		for _, n := range o.Network {
			if len(strings.TrimSpace(n)) == 0 {
				continue
			}
			args = append(args, "--network", strings.TrimSpace(n))
		}
	}

	if len(strings.TrimSpace(o.User)) != 0 {
		args = append(args, "--user", strings.TrimSpace(o.User))
	}

	// --cpus 限制容器可用的 CPU 数量（支持小数，如 "1.5"）。
	if len(strings.TrimSpace(o.Cpus)) != 0 {
		args = append(args, "--cpus", strings.TrimSpace(o.Cpus))
	}

	// --memory 限制容器可用的内存上限（支持带单位，如 "512m" / "4g"）。
	if len(strings.TrimSpace(o.Memory)) != 0 {
		args = append(args, "--memory", strings.TrimSpace(o.Memory))
	}

	return args
}

func BuildUserDockerArgs(userOptions api.DockerOptions) ([]string, error) {
	argv := parseApiDockerOptions(userOptions)
	for i := 0; i < len(argv); i++ {
		switch argv[i] {
		case "--volume", "--mount", "--network", "--user", "--gpus", "--cpus", "--memory":
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
	return v
}

func HasCustomNetwork(userOptions api.DockerOptions) bool {
	return len(userOptions.Network) > 0
}

// ParseExtraDockerArgs 将用户通过环境变量指定的额外 docker 启动参数字符串切分为参数列表，
// 用法与命令行一致：以空白分隔，支持用单引号或双引号包裹带空格的值。
// 例如 `--shm-size 256m --dns 8.8.8.8 --label "team=ci build"`
// 会被切分为 ["--shm-size","256m","--dns","8.8.8.8","--label","team=ci build"]。
func ParseExtraDockerArgs(raw string) []string {
	if strings.TrimSpace(raw) == "" {
		return nil
	}
	var args []string
	var cur strings.Builder
	var quote rune // 0 表示不在引号内，否则为 '\'' 或 '"'
	inToken := false
	for _, r := range raw {
		switch {
		case quote != 0:
			if r == quote {
				quote = 0
			} else {
				cur.WriteRune(r)
			}
		case r == '\'' || r == '"':
			quote = r
			inToken = true
		case r == ' ' || r == '\t' || r == '\n' || r == '\r':
			if inToken {
				args = append(args, cur.String())
				cur.Reset()
				inToken = false
			}
		default:
			cur.WriteRune(r)
			inToken = true
		}
	}
	if inToken {
		args = append(args, cur.String())
	}
	return args
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
