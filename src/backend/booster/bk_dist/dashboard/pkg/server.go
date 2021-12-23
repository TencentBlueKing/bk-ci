/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package pkg

import (
	"fmt"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/dashboard/config"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/dashboard/pkg/api"

	// 初始化api资源
	_ "github.com/Tencent/bk-ci/src/booster/bk_dist/dashboard/pkg/api/v1"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/dashboard/pkg/dashboard"
	"github.com/Tencent/bk-ci/src/booster/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/encrypt"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpserver"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/disttask"
)

// Server local server
type Server struct {
	conf       *config.ServerConfig
	httpServer *httpserver.HTTPServer
}

func (server *Server) waitDistTaskResource() error {
	ticker := time.NewTicker(5 * time.Second)
	defer ticker.Stop()

	for range ticker.C {
		err := server.initDistTaskResource()
		if err == nil {
			blog.Infof("success to enable disttask mysql")
			return nil
		}

		blog.Errorf("init disttask resource failed: %v, retry later", err)
	}

	return fmt.Errorf("init disttask resource timeout")
}

func (server *Server) initDistTaskResource() error {
	a := api.GetAPIResource()

	pwd, err := encrypt.DesDecryptFromBase([]byte(server.conf.DistTaskMySQL.MySQLPwd))
	if err != nil {
		return err
	}
	a.DistTaskMySQL, err = disttask.NewMySQL(engine.MySQLConf{
		MySQLStorage:  server.conf.DistTaskMySQL.MySQLStorage,
		MySQLDatabase: server.conf.DistTaskMySQL.MySQLDatabase,
		MySQLUser:     server.conf.DistTaskMySQL.MySQLUser,
		MySQLPwd:      string(pwd),
		MySQLDebug:    server.conf.DistTaskMySQL.Debug,
	})
	if err != nil {
		return err
	}

	return nil
}

// NewServer return local server
func NewServer(conf *config.ServerConfig) (*Server, error) {
	s := &Server{conf: conf}

	// Http server
	s.httpServer = httpserver.NewHTTPServer(s.conf.Port, s.conf.Address, "")
	if s.conf.ServerCert.IsSSL {
		s.httpServer.SetSSL(
			s.conf.ServerCert.CAFile, s.conf.ServerCert.CertFile, s.conf.ServerCert.KeyFile, s.conf.ServerCert.CertPwd)
	}

	return s, nil
}

// Start : start listen
func (server *Server) Start() error {
	var err error

	if err := server.waitDistTaskResource(); err != nil {
		return err
	}

	a := api.GetAPIResource()
	a.Conf = server.conf
	if err = api.InitActionsFunc(); err != nil {
		return err
	}

	if err = a.RegisterWebServer(server.httpServer); err != nil {
		return err
	}

	if err = dashboard.RegisterStaticServer(server.httpServer); err != nil {
		return err
	}

	return server.httpServer.ListenAndServe()
}

// Run brings up the server
func Run(conf *config.ServerConfig) error {
	if err := common.SavePid(conf.ProcessConfig); err != nil {
		blog.Errorf("save pid failed: %v", err)
		return err
	}

	server, err := NewServer(conf)
	if err != nil {
		blog.Errorf("init server failed: %v", err)
		return err
	}

	return server.Start()
}
