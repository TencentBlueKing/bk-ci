package main

import (
	"bytes"
	"devopsRemoting/common/logs"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"syscall"
	"time"

	"devopsRemoting/common/types"

	"github.com/Netflix/go-env"
	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"
)

var (
	Service = "codeHelper"
	// 当前服务版本，构建时注入
	Version = ""
)

const (
	Code                = "/ide/bin/devopsRemoting-vscode"
	ProductJsonLocation = "/ide/product.json"
)

type WorkspaceInfo struct {
	// RepoRoot Git 存储库位置
	GitRepoRootPath string `env:"DEVOPS_REMOTING_GIT_REPO_ROOT_PATH"`
	// DevopsRemotingYaml 远程开发yaml名称
	DevopsRemotingYaml string `env:"DEVOPS_REMOTING_YAML_NAME"`
}

func main() {
	enableDebug := os.Getenv("DEVOPS_REMOTING_DEBUG_ENABLE") == "true"

	logs.Init(Service, Version, true, false)
	logs.Info("codehelper started")
	startTime := time.Now()

	// 打印版本
	if len(os.Args) == 2 && os.Args[1] == "version" {
		fmt.Println(Version)
		os.Exit(0)
	}

	if err := replaceOpenVSXUrl(); err != nil {
		logs.WithError(err).Error("failed to replace OpenVSX URL")
	}

	wsInfo, err := loadWorkspaceConfigFromEnv()
	if err != nil {
		logs.WithError(err).Error("load workspace config error")
	}

	// vscode 服务的启动参数
	args := []string{}

	if enableDebug {
		args = append(args, "--inspect", "--log=trace")
	}

	// 解析并获取插件
	uniqMap := map[string]struct{}{}
	extensions, err := getExtensions(wsInfo)
	if err != nil {
		logs.WithError(err).Error("get extensions failed")
	}
	logs.WithField("ext", extensions).Info("get extensions")

	for _, ext := range extensions {
		if _, ok := uniqMap[ext.Local]; ok {
			continue
		}
		uniqMap[ext.Local] = struct{}{}
		if !ext.IsRemote {
			args = append(args, "--install-extension", ext.Local)
		} else {
			continue
		}
	}

	// 安装preci插件
	args = append(args, "--install-extension", "/preci/preci-remote.vsix")
	args = append(args, "--install-extension", "/preci/preci.vsix")

	args = append(args, os.Args[1:]...)
	args = append(args, "--do-not-sync")
	args = append(args, "--start-server")
	logs.WithField("cost", time.Now().Local().Sub(startTime).Milliseconds()).Info("starting server")
	if err := syscall.Exec(Code, append([]string{"devopsRemoting-vscode"}, args...), os.Environ()); err != nil {
		logs.WithError(err).Error("install ext and start code server failed")
	}
}

type Extension struct {
	// 需要远程下载的插件
	IsRemote bool `json:"remote"`
	// 本地就可以安装的插件
	Local string `json:"local"`
}

func getExtensions(wsInfo *WorkspaceInfo) ([]Extension, error) {
	if wsInfo == nil {
		return nil, nil
	}

	var (
		sourceCodePath = wsInfo.GitRepoRootPath
		yamlName       = wsInfo.DevopsRemotingYaml
	)
	if sourceCodePath == "" {
		return nil, errors.New("sourceCodePath is empty")
	}
	data, err := os.ReadFile(filepath.Join(sourceCodePath, yamlName))
	if err != nil {
		return nil, errors.Wrapf(err, "read yaml file %s error", yamlName)
	}

	var config *types.Devfile
	if err = yaml.Unmarshal(data, &config); err != nil {
		return nil, errors.Wrapf(err, "unmarshal yaml file %s failed", yamlName)
	}
	// 没有vscode相关配置直接退出
	if config == nil || config.Vscode == nil {
		return nil, nil
	}

	var extensions []Extension
	for _, ext := range config.Vscode.Extensions {
		lower := strings.ToLower(ext)
		if isRemote(lower) {
			continue
		} else {
			extensions = append(extensions, Extension{
				IsRemote: false,
				Local:    lower,
			})
		}
	}

	return extensions, nil
}

// loadWorkspaceConfigFromEnv 从环境变量中加载工作空间相关配置
func loadWorkspaceConfigFromEnv() (*WorkspaceInfo, error) {
	var res WorkspaceInfo
	_, err := env.UnmarshalFromEnviron(&res)
	if err != nil {
		return nil, errors.Errorf("cannot load workspace config: %s", err.Error())
	}

	return &res, nil
}

func isRemote(lowerCaseIdOrUrl string) bool {
	isUrl, _ := regexp.MatchString(`http[s]?://`, lowerCaseIdOrUrl)
	return isUrl
}

func replaceOpenVSXUrl() error {
	b, err := os.ReadFile(ProductJsonLocation)
	if err != nil {
		return errors.New("failed to read product.json: " + err.Error())
	}
	registryUrl := os.Getenv("VSX_REGISTRY_URL")
	if registryUrl != "" {
		b = bytes.ReplaceAll(b, []byte("https://open-vsx.org"), []byte(registryUrl))
	}
	b = bytes.ReplaceAll(b, []byte("{{extensionsGalleryItemUrl}}"), []byte("https://open-vsx.org/vscode/item"))
	b = bytes.ReplaceAll(b, []byte("{{trustedDomain}}"), []byte("https://open-vsx.org"))
	if err := os.WriteFile(ProductJsonLocation, b, 0644); err != nil {
		return errors.New("failed to write product.json: " + err.Error())
	}
	return nil
}
