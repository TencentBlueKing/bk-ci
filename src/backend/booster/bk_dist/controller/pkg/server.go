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
	"build-booster/bk_dist/controller/config"
	"build-booster/bk_dist/controller/pkg/api"
	// 初始化api资源
	_ "build-booster/bk_dist/controller/pkg/api/v1"
	"build-booster/bk_dist/controller/pkg/dashboard"
	"build-booster/bk_dist/controller/pkg/manager"
	"build-booster/bk_dist/controller/pkg/types"
	"build-booster/common"
	"build-booster/common/blog"
	"build-booster/common/http/httpserver"
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
