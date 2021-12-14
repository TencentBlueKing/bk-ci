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
	"net"
	"strconv"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/config"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/pkg/manager"
	"github.com/Tencent/bk-ci/src/booster/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// Server server
type Server struct {
	conf        *config.ServerConfig
	tcplistener *net.TCPListener
	handle      manager.Manager
}

// NewServer return server
func NewServer(conf *config.ServerConfig) (*Server, error) {
	handle, err := manager.NewManager(conf)
	if err != nil {
		return nil, err
	}

	s := &Server{
		conf:   conf,
		handle: handle,
	}

	return s, nil
}

func (s *Server) listen() error {
	port := s.conf.Port
	envport := env.GetEnv(env.KeyWorkerPort)
	if envport != "" {
		intval, err := strconv.ParseInt(envport, 10, 64)
		if err == nil && intval > 0 {
			port = uint(intval)
		}
	}

	confserver := fmt.Sprintf("%s:%d", s.conf.Address, port)
	laddr, err := net.ResolveTCPAddr("tcp4", confserver)
	if err != nil {
		blog.Errorf("failed to resolve tcp addr %s", confserver)
		return err
	}

	s.tcplistener, err = net.ListenTCP("tcp4", laddr)
	if err != nil {
		blog.Errorf("listen tcp failed: %v\n", err)
		return err
	}

	return nil
}

func (s *Server) serve() {
	for {
		conn, err := s.tcplistener.AcceptTCP()
		if err != nil {
			blog.Errorf("failed to AcceptTCP error : %v", err)
			continue
		}

		_ = s.handle.DealTCPConn(conn)
	}
}

// Start : start listen
func (s *Server) Start() error {
	if err := s.listen(); err != nil {
		blog.Errorf("listen tcp failed: %v\n", err)
		return err
	}

	s.serve()
	return nil
}

// Run brings up the server
func Run(conf *config.ServerConfig) error {
	if err := common.SavePid(conf.ProcessConfig); err != nil {
		blog.Errorf("save pid failed: %v", err)
		return err
	}

	server, err := NewServer(conf)
	if err != nil {
		blog.Errorf("init bk-dist-worker failed: %v", err)
		return err
	}

	return server.Start()
}
