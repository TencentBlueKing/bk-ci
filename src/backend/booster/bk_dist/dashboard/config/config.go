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

// ServerConfig : server config
type ServerConfig struct {
	conf.FileConfig
	conf.ServiceConfig
	conf.LogConfig
	conf.ProcessConfig
	conf.ServerOnlyCertConfig
	conf.LocalConfig

	DistTaskMySQL DistTaskMySQL `json:"disttask_mysql"`

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

// DistTaskMySQL describe the disttask mysql connection settings
type DistTaskMySQL struct {
	Debug         bool   `json:"disttask_debug" value:"false" usage:"debug mode for mysql"`
	MySQLStorage  string `json:"disttask_mysql" value:"" usage:"disttask mysql address for storage"`
	MySQLDatabase string `json:"disttask_mysql_db" value:"" usage:"disttask mysql database for connecting"`
	MySQLUser     string `json:"disttask_mysql_user" value:"root" usage:"disttask mysql username"`
	MySQLPwd      string `json:"disttask_mysql_pwd" value:"" usage:"disttask mysql password, encrypted"`
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
