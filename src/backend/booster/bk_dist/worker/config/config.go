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

	dcConfig "github.com/Tencent/bk-ci/src/booster/bk_dist/common/config"
)

// ServerConfig : server config
type ServerConfig struct {
	conf.FileConfig
	conf.ServiceConfig
	conf.LogConfig
	conf.ProcessConfig
	conf.ServerOnlyCertConfig
	conf.LocalConfig

	ServerCert *CertConfig // cert of the server
	WorkerConfig
}

// CertConfig  configuration of Cert
type CertConfig struct {
	CAFile   string
	CertFile string
	KeyFile  string
	CertPwd  string
	IsSSL    bool
}

// WorkerConfig  configuration of worker
type WorkerConfig struct {
	//MaxParallelJobs int    `json:"max_parallel_jobs" value:"8" usage:"max parallel jobs"`
	DefaultWorkDir string `json:"default_work_dir" value:"./default_work_dir" usage:"default work dir to execute scmd"`
	// CommonFileTypes []string `json:"need_clean_files" value:".pch,.gch" usage:"file types which need to save as common files"`
	CmdReplaceRules []dcConfig.CmdReplaceRule `json:"cmd_replace_rules" value:"" usage:"rules to replace input cmd"`
	CleanTempFiles  bool                      `json:"clean_temp_files" value:"true" usage:"enable temp files clean when task finished"`
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
