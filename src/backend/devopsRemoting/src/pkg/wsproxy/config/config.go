package config

import (
	"devopsRemoting/common/util"
	"encoding/json"
	"os"
)

type WsPorxyConfig struct {
	Ingress HostBasedIngressConfig `json:"ingress"`
	Proxy   ProxyConfig            `json:"proxy"`
	// DevRemotingBackend 后台配置相关
	DevRemotingBackend DevRemotingBackend `json:"devRemotingBackend"`
	// SSHHostKeyPath 存放ssh网关服务的私钥的地方
	SSHHostKeyPath string `json:"sshHostKeyPath"`
	// KubemanagerType kubernetes集群信息获取的类型
	KubemanagerType string `json:"kubemanagerType"`
	// KubeConfig kubenetes配置相关
	KubeConfig *KubeConfig `json:"kubeConfig"`
	// DevcloudConfig devcloud配置相关
	DevcloudConfig *DevcloudConfig `json:"devcloudConfig"`
}

type KubemanagerType string

const (
	//KubeApi kubernetes原生api作为集群管理接口
	KubeApi KubemanagerType = "KUBE_API"
	//Devcloud 使用devcloud作为集群管理接口
	Devcloud KubemanagerType = "DEVCLOUD"
	//Backend 调用后台dispatch作为集群管理接口
	Backend KubemanagerType = "BACKEND"
)

type KubeConfig struct {
	// k8scontroller 命名空间
	NameSpace string `json:"namespace"`
	// 指定的kubeConfig内容
	KubeConfig string `json:"kubeconfigfile"`
}

type DevcloudConfig struct {
	Host string `json:"Host"`
}

func GetConfig(path string) (*WsPorxyConfig, error) {
	fc, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}

	var cfg WsPorxyConfig
	err = json.Unmarshal(fc, &cfg)
	if err != nil {
		return nil, err
	}

	return &cfg, nil
}

type HostBasedIngressConfig struct {
	HTTPAddress  string `json:"httpAddress"`
	HTTPSAddress string `json:"httpsAddress"`
	Header       string `json:"header"`
}

type ProxyConfig struct {
	HTTPS struct {
		Key         string `json:"key"`
		Certificate string `json:"crt"`
	} `json:"https,omitempty"`
	TransportConfig    *TransportConfig    `json:"transportConfig"`
	WorkspacePodConfig *WorkspacePodConfig `json:"workspacePodConfig"`
	BuiltinPages       BuiltinPagesConfig  `json:"builtinPages"`
}
type TransportConfig struct {
	ConnectTimeout      util.Duration `json:"connectTimeout"`
	IdleConnTimeout     util.Duration `json:"idleConnTimeout"`
	MaxIdleConns        int           `json:"maxIdleConns"`
	MaxIdleConnsPerHost int           `json:"maxIdleConnsPerHost"`
}

type WorkspacePodConfig struct {
	VscodePort   uint16 `json:"vscodePort"`
	RemotingPort uint16 `json:"remotingPort"`
}

type BuiltinPagesConfig struct {
	Location string `json:"location"`
}

type DevRemotingBackend struct {
	HostName            string `json:"hostName"`
	WorkspaceHostSuffix string `json:"workspaceHostSuffix"`
	SHA1Key             string `json:"sha1key"`
	OauthRedirectUrl    string `json:"oauthRedirectUrl"`
}
