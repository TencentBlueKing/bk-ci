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

	"github.com/Tencent/bk-ci/src/booster/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/encrypt"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpserver"
	"github.com/Tencent/bk-ci/src/booster/gateway/config"
	"github.com/Tencent/bk-ci/src/booster/gateway/pkg/api"
	rd "github.com/Tencent/bk-ci/src/booster/gateway/pkg/register-discover"

	// 初始化api资源
	_ "github.com/Tencent/bk-ci/src/booster/gateway/pkg/api/v1"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/apisjob"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/disttask"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/fastbuild"
)

// GatewayServer describe the gateway http server
type GatewayServer struct {
	conf       *config.GatewayConfig
	httpServer *httpserver.HTTPServer
	rd         rd.RegisterDiscover
}

// NewGatewayServer get a new GatewayServer
func NewGatewayServer(conf *config.GatewayConfig) (*GatewayServer, error) {
	s := &GatewayServer{conf: conf}

	// Http server
	s.httpServer = httpserver.NewHTTPServer(s.conf.Port, s.conf.Address, "")
	if s.conf.ServerCert.IsSSL {
		s.httpServer.SetSSL(
			s.conf.ServerCert.CAFile, s.conf.ServerCert.CertFile, s.conf.ServerCert.KeyFile, s.conf.ServerCert.CertPwd)
	}

	return s, nil
}

func (dcs *GatewayServer) initHTTPServer() error {
	api.Rd = dcs.rd

	// Api v1
	return dcs.httpServer.RegisterWebServer(api.PathV1, nil, api.GetAPIV1Action())
}

func (dcs *GatewayServer) initDistCCResource() error {
	if !dcs.conf.DistCCMySQL.Enable {
		return nil
	}

	a := api.GetDistCCServerAPIResource()
	a.Conf = dcs.conf

	pwd, err := encrypt.DesDecryptFromBase([]byte(dcs.conf.DistCCMySQL.MySQLPwd))
	if err != nil {
		return err
	}
	a.MySQL, err = distcc.NewMySQL(engine.MySQLConf{
		MySQLStorage:  dcs.conf.DistCCMySQL.MySQLStorage,
		MySQLDatabase: dcs.conf.DistCCMySQL.MySQLDatabase,
		MySQLUser:     dcs.conf.DistCCMySQL.MySQLUser,
		MySQLPwd:      string(pwd),
		MySQLDebug:    dcs.conf.DistCCMySQL.Debug,
	})
	if err != nil {
		return err
	}

	blog.Infof("success to enable gateway for distcc mysql")
	return nil
}

func (dcs *GatewayServer) initFBResource() error {
	if !dcs.conf.FastBuildMySQL.Enable {
		return nil
	}

	a := api.GetFBServerAPIResource()
	a.Conf = dcs.conf

	pwd, err := encrypt.DesDecryptFromBase([]byte(dcs.conf.FastBuildMySQL.MySQLPwd))
	if err != nil {
		return err
	}
	a.MySQL, err = fastbuild.NewMySQL(engine.MySQLConf{
		MySQLStorage:  dcs.conf.FastBuildMySQL.MySQLStorage,
		MySQLDatabase: dcs.conf.FastBuildMySQL.MySQLDatabase,
		MySQLUser:     dcs.conf.FastBuildMySQL.MySQLUser,
		MySQLPwd:      string(pwd),
		MySQLDebug:    dcs.conf.FastBuildMySQL.Debug,
	})
	if err != nil {
		return err
	}

	blog.Infof("success to enable gateway for fastbuild mysql")
	return nil
}

func (dcs *GatewayServer) initAPISJobResource() error {
	if !dcs.conf.ApisJobMySQL.Enable {
		return nil
	}

	a := api.GetXNAPISServerAPIResource()
	a.Conf = dcs.conf

	pwd, err := encrypt.DesDecryptFromBase([]byte(dcs.conf.ApisJobMySQL.MySQLPwd))
	if err != nil {
		return err
	}
	a.MySQL, err = apisjob.NewMySQL(engine.MySQLConf{
		MySQLStorage:  dcs.conf.ApisJobMySQL.MySQLStorage,
		MySQLDatabase: dcs.conf.ApisJobMySQL.MySQLDatabase,
		MySQLUser:     dcs.conf.ApisJobMySQL.MySQLUser,
		MySQLPwd:      string(pwd),
		MySQLDebug:    dcs.conf.ApisJobMySQL.Debug,
	})
	if err != nil {
		return err
	}

	blog.Infof("success to enable gateway for apisjob mysql")
	return nil
}

func (dcs *GatewayServer) initDistTaskResource() error {
	if !dcs.conf.DistTaskMySQL.Enable {
		return nil
	}

	a := api.GetDistTaskServerAPIResource()
	a.Conf = dcs.conf

	pwd, err := encrypt.DesDecryptFromBase([]byte(dcs.conf.DistTaskMySQL.MySQLPwd))
	if err != nil {
		return err
	}
	a.MySQL, err = disttask.NewMySQL(engine.MySQLConf{
		MySQLStorage:  dcs.conf.DistTaskMySQL.MySQLStorage,
		MySQLDatabase: dcs.conf.DistTaskMySQL.MySQLDatabase,
		MySQLUser:     dcs.conf.DistTaskMySQL.MySQLUser,
		MySQLPwd:      string(pwd),
		MySQLDebug:    dcs.conf.DistTaskMySQL.Debug,
	})
	if err != nil {
		return err
	}

	blog.Infof("success to enable gateway for disttask mysql")
	return nil
}

// Start brings up the gateway http server
func (dcs *GatewayServer) Start() error {
	var err error
	if dcs.rd, err = rd.NewRegisterDiscover(dcs.conf); err != nil {
		blog.Errorf("get new register discover failed: %v", err)
		return err
	}

	if err = dcs.rd.Run(); err != nil {
		blog.Errorf("get register discover event chan failed: %v", err)
		return err
	}

	// init distCC server related resources
	if err = waitUntilResourceReady(dcs.initDistCCResource); err != nil {
		return err
	}

	// init fb server related resources
	if err = waitUntilResourceReady(dcs.initFBResource); err != nil {
		return err
	}

	// init apis job server related resources
	if err = waitUntilResourceReady(dcs.initAPISJobResource); err != nil {
		return err
	}

	// init disttask server related resources
	if err = waitUntilResourceReady(dcs.initDistTaskResource); err != nil {
		return err
	}

	if err := api.InitActionsFunc(); err != nil {
		return err
	}

	// register all APIs
	if err = dcs.initHTTPServer(); err != nil {
		return err
	}

	return dcs.httpServer.ListenAndServe()
}

// Run brings up the server
func Run(conf *config.GatewayConfig) error {
	if err := common.SavePid(conf.ProcessConfig); err != nil {
		blog.Errorf("save pid failed: %v", err)
		return err
	}

	server, err := NewGatewayServer(conf)
	if err != nil {
		blog.Errorf("init proxy server failed: %v", err)
		return err
	}

	return server.Start()
}

func waitUntilResourceReady(initF func() error) error {
	ticker := time.NewTicker(5 * time.Second)
	defer ticker.Stop()

	for range ticker.C {
		if err := initF(); err != nil {
			blog.Errorf("init resource failed: %v, retry later", err)
			continue
		}

		return nil
	}

	return fmt.Errorf("init resource timeout")
}
