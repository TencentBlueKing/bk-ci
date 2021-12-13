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
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/config"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/api"

	// 初始化api资源
	_ "github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/api/v1"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/dashboard"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/manager"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/types"
	"github.com/Tencent/bk-ci/src/booster/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpserver"
)

// Server local server
type Server struct {
	conf       *config.ServerConfig
	manager    types.Mgr
	httpServer *httpserver.HTTPServer
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
	server.manager = manager.NewMgr(server.conf)
	go server.manager.Run()

	a := api.GetAPIResource()
	a.Manager = server.manager
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
