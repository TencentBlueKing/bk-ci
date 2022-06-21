package pkg

import (
	"github.com/Tencent/bk-ci/src/booster/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpserver"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/api"
	rd "github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/register-discover"

	// 初始化api资源
	_ "github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/api/v1http"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/controller"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/store"
)

// DistCCController describe the controller http server
type DistCCController struct {
	conf       *config.DistCCControllerConfig
	httpServer *httpserver.HTTPServer
	rd         rd.RegisterDiscover
}

// NewDistCCController get a new controller http server
func NewDistCCController(conf *config.DistCCControllerConfig) (*DistCCController, error) {
	s := &DistCCController{conf: conf}

	// Http server
	s.httpServer = httpserver.NewHTTPServer(s.conf.Port, s.conf.Address, "")
	if s.conf.ServerCert.IsSSL {
		s.httpServer.SetSSL(
			s.conf.ServerCert.CAFile, s.conf.ServerCert.CertFile, s.conf.ServerCert.KeyFile, s.conf.ServerCert.CertPwd)
	}

	return s, nil
}

// Start the controller http server
func (dcc *DistCCController) Start() error {
	var err error
	if dcc.rd, err = rd.NewRegisterDiscover(dcc.conf); err != nil {
		blog.Errorf("get new register discover failed: %v", err)
		return err
	}

	ops, err := store.NewOps(dcc.conf)
	if err != nil {
		blog.Errorf("get new ops failed: %v", err)
		return err
	}

	event, err := dcc.rd.Register()
	if err != nil {
		blog.Errorf("get register discover event chan failed: %v", err)
		return err
	}

	ctr := controller.NewController(event, dcc.conf, ops)
	go ctr.Run()

	// Send a initialized store into apiResource and then call InitActionsFunc to send it into the actions for
	// doing operations while handling api requests
	a := api.GetAPIResource()
	a.Ops = ops
	a.Rd = dcc.rd
	a.Conf = dcc.conf
	if err = api.InitActionsFunc(); err != nil {
		return err
	}

	// Init routes in actions
	a.InitActions()
	if err = dcc.initHTTPServer(); err != nil {
		return err
	}

	return dcc.httpServer.ListenAndServe()
}

func (dcc *DistCCController) initHTTPServer() error {
	a := api.GetAPIResource()

	// Api v1
	return dcc.httpServer.RegisterWebServer(api.PathV1, nil, a.ActionsV1)
}

// Run brings up the distcc-controller server
func Run(conf *config.DistCCControllerConfig) error {
	if err := common.SavePid(conf.ProcessConfig); err != nil {
		blog.Errorf("save pid failed: %v", err)
		return err
	}

	server, err := NewDistCCController(conf)
	if err != nil {
		blog.Errorf("init distCC controller failed: %v", err)
		return err
	}

	return server.Start()
}
