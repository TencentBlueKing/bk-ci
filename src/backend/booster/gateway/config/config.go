package config

import (
	"build-booster/common/conf"
	"build-booster/common/static"
)

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

type DistCCMySQL struct {
	Enable        bool   `json:"distcc_enable" value:"false" usage:"distcc mysql gateway enable"`
	Debug         bool   `json:"distcc_debug" value:"false" usage:"debug mode for mysql"`
	MySQLStorage  string `json:"distcc_mysql" value:"" usage:"distcc mysql address for storage, e.g. 127.0.0.1:3306"`
	MySQLDatabase string `json:"distcc_mysql_db" value:"" usage:"distcc mysql database for connecting."`
	MySQLUser     string `json:"distcc_mysql_user" value:"root" usage:"distcc mysql username"`
	MySQLPwd      string `json:"distcc_mysql_pwd" value:"" usage:"distcc mysql password, encrypted"`
}

type FastBuildMySQL struct {
	Enable        bool   `json:"fastbuild_enable" value:"false" usage:"fastbuild mysql gateway enable"`
	Debug         bool   `json:"fastbuild_debug" value:"false" usage:"debug mode for mysql"`
	MySQLStorage  string `json:"fastbuild_mysql" value:"" usage:"fastbuild mysql address for storage, e.g. 127.0.0.1:3306"`
	MySQLDatabase string `json:"fastbuild_mysql_db" value:"" usage:"fastbuild mysql database for connecting."`
	MySQLUser     string `json:"fastbuild_mysql_user" value:"root" usage:"fastbuild mysql username"`
	MySQLPwd      string `json:"fastbuild_mysql_pwd" value:"" usage:"fastbuild mysql password, encrypted"`
}

type ApisJobMySQL struct {
	Enable        bool   `json:"apisjob_enable" value:"false" usage:"apisjob mysql gateway enable"`
	Debug         bool   `json:"apisjob_debug" value:"false" usage:"debug mode for mysql"`
	MySQLStorage  string `json:"apisjob_mysql" value:"" usage:"apisjob mysql address for storage, e.g. 127.0.0.1:3306"`
	MySQLDatabase string `json:"apisjob_mysql_db" value:"" usage:"apisjob mysql database for connecting."`
	MySQLUser     string `json:"apisjob_mysql_user" value:"root" usage:"apisjob mysql username"`
	MySQLPwd      string `json:"apisjob_mysql_pwd" value:"" usage:"apisjob mysql password, encrypted"`
}

type DistTaskMySQL struct {
	Enable        bool   `json:"disttask_enable" value:"false" usage:"disttask mysql gateway enable"`
	Debug         bool   `json:"disttask_debug" value:"false" usage:"debug mode for mysql"`
	MySQLStorage  string `json:"disttask_mysql" value:"" usage:"disttask mysql address for storage, e.g. 127.0.0.1:3306"`
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

func NewConfig() *GatewayConfig {
	return &GatewayConfig{
		ServerCert: &CertConfig{
			CertPwd: static.ServerCertPwd,
			IsSSL:   false,
		},
	}
}

func (dsc *GatewayConfig) Parse() {
	conf.Parse(dsc)

	dsc.ServerCert.CertFile = dsc.ServerCertFile
	dsc.ServerCert.KeyFile = dsc.ServerKeyFile
	dsc.ServerCert.CAFile = dsc.CAFile

	if dsc.ServerCert.CertFile != "" && dsc.ServerCert.KeyFile != "" {
		dsc.ServerCert.IsSSL = true
	}
}
