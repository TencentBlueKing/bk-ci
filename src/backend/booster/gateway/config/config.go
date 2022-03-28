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

// GatewayConfig describe the gateway server config
type GatewayConfig struct {
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

	DistCCMySQL    DistCCMySQL    `json:"distcc_mysql"`
	FastBuildMySQL FastBuildMySQL `json:"fastbuild_mysql"`
	ApisJobMySQL   ApisJobMySQL   `json:"apisjob_mysql"`
	DistTaskMySQL  DistTaskMySQL  `json:"disttask_mysql"`

	ServerCert *CertConfig // cert of the server
}

// DistCCMySQL describe engine distcc mysql connection settings
type DistCCMySQL struct {
	Enable        bool   `json:"distcc_enable" value:"false" usage:"distcc mysql gateway enable"`
	Debug         bool   `json:"distcc_debug" value:"false" usage:"debug mode for mysql"`
	MySQLStorage  string `json:"distcc_mysql" value:"" usage:"distcc mysql address for storage"`
	MySQLDatabase string `json:"distcc_mysql_db" value:"" usage:"distcc mysql database for connecting."`
	MySQLUser     string `json:"distcc_mysql_user" value:"root" usage:"distcc mysql username"`
	MySQLPwd      string `json:"distcc_mysql_pwd" value:"" usage:"distcc mysql password, encrypted"`
}

// FastBuildMySQL describe engine fastbuild mysql connection settings
type FastBuildMySQL struct {
	Enable        bool   `json:"fastbuild_enable" value:"false" usage:"fastbuild mysql gateway enable"`
	Debug         bool   `json:"fastbuild_debug" value:"false" usage:"debug mode for mysql"`
	MySQLStorage  string `json:"fastbuild_mysql" value:"" usage:"fastbuild mysql address for storage"`
	MySQLDatabase string `json:"fastbuild_mysql_db" value:"" usage:"fastbuild mysql database for connecting."`
	MySQLUser     string `json:"fastbuild_mysql_user" value:"root" usage:"fastbuild mysql username"`
	MySQLPwd      string `json:"fastbuild_mysql_pwd" value:"" usage:"fastbuild mysql password, encrypted"`
}

// ApisJobMySQL describe engine apis mysql connection settings
type ApisJobMySQL struct {
	Enable        bool   `json:"apisjob_enable" value:"false" usage:"apisjob mysql gateway enable"`
	Debug         bool   `json:"apisjob_debug" value:"false" usage:"debug mode for mysql"`
	MySQLStorage  string `json:"apisjob_mysql" value:"" usage:"apisjob mysql address for storage"`
	MySQLDatabase string `json:"apisjob_mysql_db" value:"" usage:"apisjob mysql database for connecting."`
	MySQLUser     string `json:"apisjob_mysql_user" value:"root" usage:"apisjob mysql username"`
	MySQLPwd      string `json:"apisjob_mysql_pwd" value:"" usage:"apisjob mysql password, encrypted"`
}

// DistTaskMySQL describe engine disttask mysql connection settings
type DistTaskMySQL struct {
	Enable        bool   `json:"disttask_enable" value:"false" usage:"disttask mysql gateway enable"`
	Debug         bool   `json:"disttask_debug" value:"false" usage:"debug mode for mysql"`
	MySQLStorage  string `json:"disttask_mysql" value:"" usage:"disttask mysql address for storage"`
	MySQLDatabase string `json:"disttask_mysql_db" value:"" usage:"disttask mysql database for connecting."`
	MySQLUser     string `json:"disttask_mysql_user" value:"root" usage:"disttask mysql username"`
	MySQLPwd      string `json:"disttask_mysql_pwd" value:"" usage:"disttask mysql password, encrypted"`
}

// CertConfig  configuration of Cert
type CertConfig struct {
	CAFile   string
	CertFile string
	KeyFile  string
	CertPwd  string
	IsSSL    bool
}

// NewConfig get a new GatewayConfig
func NewConfig() *GatewayConfig {
	return &GatewayConfig{
		ServerCert: &CertConfig{
			CertPwd: static.ServerCertPwd,
			IsSSL:   false,
		},
	}
}

// Parse parse config from command line or file
func (dsc *GatewayConfig) Parse() {
	conf.Parse(dsc)

	dsc.ServerCert.CertFile = dsc.ServerCertFile
	dsc.ServerCert.KeyFile = dsc.ServerKeyFile
	dsc.ServerCert.CAFile = dsc.CAFile

	if dsc.ServerCert.CertFile != "" && dsc.ServerCert.KeyFile != "" {
		dsc.ServerCert.IsSSL = true
	}
}
