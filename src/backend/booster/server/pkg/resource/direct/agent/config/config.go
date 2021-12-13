/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package config

import (
	"github.com/Tencent/bk-ci/src/booster/common/conf"
	"github.com/Tencent/bk-ci/src/booster/common/static"
)

// define server run mode
var (
	Mode = SMServer
)

// define server run mode
var (
	SMServer = "server"
	SMProxy  = "proxy"
)

// ServerConfig : server config
type ServerConfig struct {
	conf.FileConfig
	conf.ServiceConfig
	conf.LogConfig
	conf.ProcessConfig
	conf.ServerOnlyCertConfig
	conf.LocalConfig

	EtcdEndpoints string `json:"etcd_endpoints" value:"" usage:"etcd endpoints for register and discover"`
	EtcdRootPath  string `json:"etcd_root_path" value:"" usage:"etcd root path"`
	EtcdCaFile    string `json:"etcd_ca_file" value:"" usage:"etcd ca file"`
	EtcdCertFile  string `json:"etcd_cert_file" value:"" usage:"etcd cert file"`
	EtcdKeyFile   string `json:"etcd_key_file" value:"" usage:"etcd key file"`
	EtcdKeyPwd    string `json:"etcd_key_password" value:"" usage:"etcd key password"`

	DebugMode         bool `json:"debug" value:"false" usage:"*ATTENTION* debug mode will disable some handlers such as task heartbeat checking"`
	BcsCPUPerInstance int  `json:"bcs_cpu_per_instance" value:"4" usage:"bcs cpu per instance of distccd"`

	City             string `json:"city" value:"" usage:"resource city"`
	PrescribedCPUNum int    `json:"prescribed_cpu_num" value:"0" usage:"cpu number ready to use"`

	AgentMinPort          uint16   `json:"agent_min_port" value:"31264" usage:"agent min port to launch application"`
	AgentMaxPort          uint16   `json:"agent_max_port" value:"32264" usage:"agent max port to launch application"`
	AgentRemouteCmd       []string `json:"agent_remote_cmds" value:"" usage:"agent remote cmds to launch application"`
	UpdateCPURealtime     bool     `json:"update_cpu_realtime" value:"false" usage:"if set true, we will report cpu resource by cpu usage"`
	AgentRemouteCmdPrefix string   `json:"agent_remote_cmd_prefix" value:"FBuildWorker" usage:"agent remote cmd prefix to judge whether process is running"`

	AgentReleaseCmds []string `json:"agent_release_cmd" value:"" usage:"locals cmds to release application"`

	ServerCert *CertConfig // cert of the server
}

// CertConfig  configuration of Cert
type CertConfig struct {
	CAFile   string
	CertFile string
	KeyFile  string
	CertPwd  string
	IsSSL    bool
}

// NewConfig : return config of server
func NewConfig() *ServerConfig {
	return &ServerConfig{
		ServerCert: &CertConfig{
			CertPwd: static.ServerCertPwd,
			IsSSL:   false,
		},
	}
}

// Parse : parse server config
func (dsc *ServerConfig) Parse() {
	conf.Parse(dsc)

	dsc.ServerCert.CertFile = dsc.ServerCertFile
	dsc.ServerCert.KeyFile = dsc.ServerKeyFile
	dsc.ServerCert.CAFile = dsc.CAFile

	if dsc.ServerCert.CertFile != "" && dsc.ServerCert.KeyFile != "" {
		dsc.ServerCert.IsSSL = true
	}
}
