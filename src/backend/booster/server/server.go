package pkg

import (
	"build-booster/common"
	"build-booster/common/blog"
	"build-booster/common/encrypt"
	"build-booster/common/http/httpserver"
	"build-booster/common/metric"
	"build-booster/common/metric/controllers"
	"build-booster/server/config"
	"build-booster/server/pkg/api"
	_ "build-booster/server/pkg/api/v1"
	_ "build-booster/server/pkg/api/v2"
	"build-booster/server/pkg/engine"
	"build-booster/server/pkg/engine/apisjob"
	"build-booster/server/pkg/engine/distcc"
	"build-booster/server/pkg/engine/distcc_mac"
	"build-booster/server/pkg/engine/disttask"
	"build-booster/server/pkg/engine/fastbuild"
	"build-booster/server/pkg/manager"
	"build-booster/server/pkg/manager/normal"
	selfMetric "build-booster/server/pkg/metric"
	selfController "build-booster/server/pkg/metric/controllers"
	"build-booster/server/pkg/rd"
	"build-booster/server/pkg/resource/crm"
	"build-booster/server/pkg/resource/direct"
	"build-booster/server/pkg/types"
	"fmt"
)

// Server define this main server
type Server struct {
	conf       *config.ServerConfig
	httpServer *httpserver.HTTPServer
	rd         rd.RegisterDiscover
	manager    manager.Manager
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

// NewServer return the new main server
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

// Start init object and start http server
func (s *Server) Start() error {
	var err error
	if s.rd, err = rd.NewRegisterDiscover(s.conf); err != nil {
		blog.Errorf("get new register discover failed: %v", err)
		return err
	}

	// must be launched before others which are observed
	go func() {
		err := initMetricServer(s.conf)
		blog.Errorf("metric server failed: %v", err)
	}()

	directResourceManager, err := s.initDirectResourceManager(&s.conf.DirectResourceConfig, s.rd)
	if err != nil {
		blog.Errorf("init direct resource manager failed: %v", err)
		return err
	}

	containerResourceManager, err := s.initContainerResourceManager(&s.conf.ContainerResourceConfig, s.rd)
	if err != nil {
		blog.Errorf("init container resource manager failed: %v", err)
		return err
	}

	k8sContainerResourceManager, err := s.initContainerResourceManager(&s.conf.K8sContainerResourceConfig, s.rd)
	if err != nil {
		blog.Errorf("init k8s container resource manager failed: %v", err)
		return err
	}

	engineList := make([]engine.Engine, 0, 10)
	egn, err := s.initDistccEngine(containerResourceManager)
	if err != nil {
		blog.Errorf("server start init distcc engines failed: %v", err)
	} else {
		engineList = append(engineList, egn)
	}

	egn, err = s.initDisttaskEngine(containerResourceManager, k8sContainerResourceManager, directResourceManager)
	if err != nil {
		blog.Errorf("server start init disttask engines failed: %v", err)
	} else {
		engineList = append(engineList, egn)
	}

	egn, err = s.initDistccMacEngine(directResourceManager)
	if err != nil {
		blog.Errorf("server start init distcc_mac engines failed: %v", err)
	} else {
		engineList = append(engineList, egn)
	}

	egn, err = s.initApisEngine(k8sContainerResourceManager, directResourceManager)
	if err != nil {
		blog.Errorf("server start init apis engines failed: %v", err)
	} else {
		engineList = append(engineList, egn)
	}

	// init fast build engine
	egn, err = s.initFastbuildEngine(directResourceManager)
	if err != nil {
		blog.Errorf("server start init fastbuild engines failed: %v", err)
	} else {
		engineList = append(engineList, egn)
	}

	// register role event
	roleEvent := make(chan types.RoleType, 1)
	if err = s.rd.AddObserver(roleEvent); err != nil {
		blog.Errorf("add register discover observer failed: %v", err)
		return err
	}
	s.manager = normal.NewManager(roleEvent, s.conf.DebugMode, s.getEngineQueueBriefInfoList(), engineList...)
	go s.manager.Run()

	if err = s.rd.Run(); err != nil {
		blog.Errorf("register discover run failed: %v", err)
		return err
	}

	if err = s.initHTTPServer(); err != nil {
		return err
	}

	return s.httpServer.ListenAndServe()
}

func (s *Server) initHTTPServer() error {

	// Send a initialized store into apiResource and then call InitActionsFunc to send it into the actions for
	// doing operations while handling api requests
	a := api.GetAPIResource()
	a.Manager = s.manager
	a.Rd = s.rd
	a.Conf = s.conf
	if err := api.InitActionsFunc(); err != nil {
		return err
	}

	return a.RegisterWebServer(s.httpServer)
}

func (s *Server) getEngineQueueBriefInfoList() []engine.QueueBriefInfo {
	l := make([]engine.QueueBriefInfo, 0, 100)
	for _, name := range s.conf.ApisJobQueueList {
		l = append(l, engine.QueueBriefInfo{
			EngineName: apisjob.EngineName,
			QueueName:  name,
		})
	}

	for _, name := range s.conf.FastBuildQueueList {
		l = append(l, engine.QueueBriefInfo{
			EngineName: fastbuild.EngineName,
			QueueName:  name,
		})
	}

	for _, name := range s.conf.DistCCQueueList {
		l = append(l, engine.QueueBriefInfo{
			EngineName: distcc.EngineName,
			QueueName:  name,
		})
	}

	for _, name := range s.conf.DisttaskQueueList {
		l = append(l, engine.QueueBriefInfo{
			EngineName: disttask.EngineName,
			QueueName:  name,
		})
	}

	for _, name := range s.conf.DistccMacQueueList {
		l = append(l, engine.QueueBriefInfo{
			EngineName: distcc_mac.EngineName,
			QueueName:  name,
		})
	}
	return l
}

func (s *Server) initDistccEngine(r crm.ResourceManager) (engine.Engine, error) {
	if !s.conf.EngineDistCCConfig.Enable {
		return nil, fmt.Errorf("engine %s not enable", distcc.EngineName)
	}
	if r == nil {
		return nil, fmt.Errorf("crm not enable")
	}

	dccResourceMgr, err := r.RegisterUser(distcc.EngineName)
	if err != nil {
		blog.Errorf("init engine(%s), get resource manager failed: %v", distcc.EngineName, err)
		return nil, err
	}

	pwd, err := encrypt.DesDecryptFromBase([]byte(s.conf.EngineDistCCConfig.MySQLPwd))
	if err != nil {
		blog.Errorf("init engine(%s), decode mysql pwd failed: %v", err)
		return nil, err
	}

	dccEngine, err := distcc.NewDistccEngine(distcc.EngineConfig{
		MySQLConf: engine.MySQLConf{
			MySQLStorage:     s.conf.EngineDistCCConfig.MySQLStorage,
			MySQLDatabase:    s.conf.EngineDistCCConfig.MySQLDatabase,
			MySQLUser:        s.conf.EngineDistCCConfig.MySQLUser,
			MySQLPwd:         string(pwd),
			MySQLDebug:       s.conf.EngineDistCCConfig.MySQLDebug,
			MysqlTableOption: s.conf.EngineDistCCConfig.MysqlTableOption,
		},
		Rd:                  s.rd,
		ClusterID:           s.conf.ContainerResourceConfig.BcsClusterID,
		CPUPerInstance:      s.conf.ContainerResourceConfig.BcsCPUPerInstance,
		MemPerInstance:      s.conf.ContainerResourceConfig.BcsMemPerInstance,
		LeastJobServer:      s.conf.EngineDistCCConfig.LeastJobServer,
		JobServerTimesToCPU: s.conf.EngineDistCCConfig.JobServerTimesToCPU,
		Brokers:             s.conf.EngineDistCCConfig.BrokerConfig,
	}, dccResourceMgr)
	if err != nil {
		blog.Errorf("init engine(%s) failed: %v", distcc.EngineName, err)
		return nil, err
	}

	blog.Infof("success to init engine %s", distcc.EngineName)
	return dccEngine, nil
}

func (s *Server) initDisttaskEngine(cr crm.ResourceManager, k8sCr crm.ResourceManager, dr direct.ResourceManager) (engine.Engine, error) {
	if !s.conf.EngineDisttaskConfig.Enable {
		return nil, fmt.Errorf("engine %s not enable", disttask.EngineName)
	}

	crMgr, err := cr.RegisterUser(disttask.EngineName)
	if err != nil {
		blog.Errorf("init engine(%s), get container resource manager failed: %v", disttask.EngineName, err)
		return nil, err
	}

	var k8sCrMgr crm.HandlerWithUser
	if cr != nil {
		if k8sCr != nil {
			if k8sCrMgr, err = k8sCr.RegisterUser(disttask.EngineName); err != nil {
				blog.Errorf("init engine(%s), get k8s container resource manager failed: %v", disttask.EngineName, err)
				return nil, err
			}
		}
	}

	var drMgr direct.HandleWithUser
	if dr != nil {
		drMgr, err = dr.RegisterUser(disttask.EngineName, nil)
		if err != nil {
			blog.Errorf("init engine(%s), get direct resource manager failed: %v", disttask.EngineName, err)
			return nil, err
		}
	}

	pwd, err := encrypt.DesDecryptFromBase([]byte(s.conf.EngineDisttaskConfig.MySQLPwd))
	if err != nil {
		blog.Errorf("init engine(%s), decode mysql pwd failed: %v", err)
		return nil, err
	}

	dtEngine, err := disttask.NewDisttaskEngine(disttask.EngineConfig{
		MySQLConf: engine.MySQLConf{
			MySQLStorage:     s.conf.EngineDisttaskConfig.MySQLStorage,
			MySQLDatabase:    s.conf.EngineDisttaskConfig.MySQLDatabase,
			MySQLUser:        s.conf.EngineDisttaskConfig.MySQLUser,
			MySQLPwd:         string(pwd),
			MySQLDebug:       s.conf.EngineDisttaskConfig.MySQLDebug,
			MysqlTableOption: s.conf.EngineDisttaskConfig.MysqlTableOption,
		},
		Rd:                   s.rd,
		CRMClusterID:         s.conf.ContainerResourceConfig.BcsClusterID,
		CRMCPUPerInstance:    s.conf.ContainerResourceConfig.BcsCPUPerInstance,
		CRMMemPerInstance:    s.conf.ContainerResourceConfig.BcsMemPerInstance,
		K8SCRMClusterID:      s.conf.K8sContainerResourceConfig.BcsClusterID,
		K8SCRMCPUPerInstance: s.conf.K8sContainerResourceConfig.BcsCPUPerInstance,
		K8SCRMMemPerInstance: s.conf.K8sContainerResourceConfig.BcsMemPerInstance,
		Brokers:              s.conf.EngineDisttaskConfig.BrokerConfig,
	}, crMgr, k8sCrMgr, drMgr)
	if err != nil {
		blog.Errorf("init engine(%s) failed: %v", disttask.EngineName, err)
		return nil, err
	}

	blog.Infof("success to init engine %s", disttask.EngineName)
	return dtEngine, nil
}

func (s *Server) initDistccMacEngine(r direct.ResourceManager) (engine.Engine, error) {
	if !s.conf.EngineDistCCMacConfig.Enable {
		return nil, fmt.Errorf("engine %s not enable", distcc_mac.EngineName)
	}
	if r == nil {
		return nil, fmt.Errorf("drm not enable")
	}

	dccResourceMgr, err := r.RegisterUser(distcc_mac.EngineName, distcc_mac.GetReleaseCommand())
	if err != nil {
		blog.Errorf("init engine(%s), get resource manager failed: %v", distcc_mac.EngineName, err)
		return nil, err
	}

	pwd, err := encrypt.DesDecryptFromBase([]byte(s.conf.EngineDistCCMacConfig.MySQLPwd))
	if err != nil {
		blog.Errorf("init engine(%s), decode mysql pwd failed: %v", err)
		return nil, err
	}

	dccMacEngine, err := distcc_mac.NewDistccMacEngine(distcc_mac.EngineConfig{
		MySQLConf: engine.MySQLConf{
			MySQLStorage:     s.conf.EngineDistCCMacConfig.MySQLStorage,
			MySQLDatabase:    s.conf.EngineDistCCMacConfig.MySQLDatabase,
			MySQLUser:        s.conf.EngineDistCCMacConfig.MySQLUser,
			MySQLPwd:         string(pwd),
			MySQLDebug:       s.conf.EngineDistCCMacConfig.MySQLDebug,
			MysqlTableOption: s.conf.EngineDistCCMacConfig.MysqlTableOption,
		},
		Rd:                  s.rd,
		ClusterID:           "",
		LeastJobServer:      s.conf.EngineDistCCMacConfig.LeastJobServer,
		JobServerTimesToCPU: s.conf.EngineDistCCMacConfig.JobServerTimesToCPU,
	}, dccResourceMgr)
	if err != nil {
		blog.Errorf("init engine(%s) failed: %v", distcc_mac.EngineName, err)
		return nil, err
	}

	blog.Infof("success to init engine %s", distcc_mac.EngineName)
	return dccMacEngine, nil
}

func (s *Server) initApisEngine(k8sCr crm.ResourceManager, dr direct.ResourceManager) (engine.Engine, error) {
	if !s.conf.EngineApisJobConfig.Enable {
		return nil, fmt.Errorf("engine %s not enable", apisjob.EngineName)
	}

	var err error
	var ajResourceMgr direct.HandleWithUser
	if dr != nil {
		ajResourceMgr, err = dr.RegisterUser(apisjob.EngineName, nil)
		if err != nil {
			blog.Errorf("init engine(%s), get resource manager failed: %v", apisjob.EngineName, err)
			return nil, err
		}
	}

	var k8sCrMgr crm.HandlerWithUser
	if k8sCr != nil {
		if k8sCrMgr, err = k8sCr.RegisterUser(apisjob.EngineName); err != nil {
			blog.Errorf("init engine(%s), get k8s container resource manager failed: %v", apisjob.EngineName, err)
			return nil, err
		}
	}

	pwd, err := encrypt.DesDecryptFromBase([]byte(s.conf.EngineApisJobConfig.MySQLPwd))
	if err != nil {
		blog.Errorf("init engine(%s), decode mysql pwd failed: %v", err)
		return nil, err
	}

	ajEngine, err := apisjob.NewApisEngine(apisjob.EngineConfig{
		MySQLConf: engine.MySQLConf{
			MySQLStorage:     s.conf.EngineApisJobConfig.MySQLStorage,
			MySQLDatabase:    s.conf.EngineApisJobConfig.MySQLDatabase,
			MySQLUser:        s.conf.EngineApisJobConfig.MySQLUser,
			MySQLPwd:         string(pwd),
			MySQLDebug:       s.conf.EngineApisJobConfig.MySQLDebug,
			MysqlTableOption: s.conf.EngineApisJobConfig.MysqlTableOption,
		},
		K8SCRMCPUPerInstance: s.conf.K8sContainerResourceConfig.BcsCPUPerInstance,
		K8SCRMMemPerInstance: s.conf.K8sContainerResourceConfig.BcsMemPerInstance,
	}, k8sCrMgr, ajResourceMgr)
	if err != nil {
		blog.Errorf("init engine(%s) failed: %v", apisjob.EngineName, err)
		return nil, err
	}

	blog.Infof("success to init engine %s", apisjob.EngineName)
	return ajEngine, nil
}

func (s *Server) initFastbuildEngine(r direct.ResourceManager) (engine.Engine, error) {
	if !s.conf.EngineFastBuildConfig.Enable {
		return nil, fmt.Errorf("engine %s not enable", fastbuild.EngineName)
	}
	if r == nil {
		return nil, fmt.Errorf("drm not enable")
	}

	fbResourceMgr, err := r.RegisterUser(fastbuild.EngineName, nil)
	if err != nil {
		blog.Errorf("init engine(%s), get resource manager failed: %v", fastbuild.EngineName, err)
		return nil, err
	}

	pwd, err := encrypt.DesDecryptFromBase([]byte(s.conf.EngineFastBuildConfig.MySQLPwd))
	if err != nil {
		blog.Errorf("init engine(%s), decode mysql pwd failed: %v", err)
		return nil, err
	}

	fbEngine, err := fastbuild.NewFastbuildEngine(fastbuild.EngineConfig{
		MySQLConf: engine.MySQLConf{
			MySQLStorage:     s.conf.EngineFastBuildConfig.MySQLStorage,
			MySQLDatabase:    s.conf.EngineFastBuildConfig.MySQLDatabase,
			MySQLUser:        s.conf.EngineFastBuildConfig.MySQLUser,
			MySQLPwd:         string(pwd),
			MySQLDebug:       s.conf.EngineFastBuildConfig.MySQLDebug,
			MysqlTableOption: s.conf.EngineFastBuildConfig.MysqlTableOption,
		},
		SpecialFBCmd:                      s.conf.EngineFastBuildConfig.SpecialFBCmd,
		TaskMaxRunningSeconds:             s.conf.EngineFastBuildConfig.TaskMaxRunningSeconds,
		TaskBKMainNoSubTaskTimeoutSeconds: s.conf.EngineFastBuildConfig.TaskBKMainNoSubTaskTimeoutSeconds,
	}, fbResourceMgr)
	if err != nil {
		blog.Errorf("init engine(%s) failed: %v", fastbuild.EngineName, err)
		return nil, err
	}

	blog.Infof("success to init engine %s", fastbuild.EngineName)
	return fbEngine, nil
}

func (s *Server) initDirectResourceManager(conf *config.DirectResourceConfig, registerDiscover rd.RegisterDiscover) (direct.ResourceManager, error) {
	if !conf.Enable {
		return nil, nil
	}

	// init direct resource manager here
	roleEvent := make(chan types.RoleType, 1)
	if err := registerDiscover.AddObserver(roleEvent); err != nil {
		blog.Errorf("add register discover observer failed: %v", err)
		return nil, err
	}

	pwd, err := encrypt.DesDecryptFromBase([]byte(conf.MySQLPwd))
	if err != nil {
		blog.Errorf("init initDirectResourceManager, decode mysql pwd failed: %v", err)
		return nil, err
	}
	conf.MySQLPwd = string(pwd)

	resourceManager, err := direct.NewResourceManager(conf, roleEvent)
	if err != nil {
		blog.Errorf("initDirectResourceManager failed: %v", err)
		return nil, err
	}

	go func() {
		_ = resourceManager.Run()
	}()

	blog.Infof("success to init direct resource manager")
	return resourceManager, nil
}

func (s *Server) initContainerResourceManager(conf *config.ContainerResourceConfig, registerDiscover rd.RegisterDiscover) (crm.ResourceManager, error) {
	if !conf.Enable {
		return nil, nil
	}

	// init container resource manager here
	roleEvent := make(chan types.RoleType, 1)
	if err := registerDiscover.AddObserver(roleEvent); err != nil {
		blog.Errorf("add register discover observer failed: %v", err)
		return nil, err
	}

	pwd, err := encrypt.DesDecryptFromBase([]byte(conf.MySQLPwd))
	if err != nil {
		blog.Errorf("init initContainerResourceManager, decode mysql pwd failed: %v", err)
		return nil, err
	}
	conf.MySQLPwd = string(pwd)

	resourceManager, err := crm.NewResourceManager(conf, roleEvent)
	if err != nil {
		blog.Errorf("initContainerResourceManager failed: %v", err)
		return nil, err
	}

	go func() {
		_ = resourceManager.Run()
	}()

	blog.Infof("success to init crm resource manager %s", conf.BcsClusterID)
	return resourceManager, nil
}

func initMetricServer(opts *config.ServerConfig) error {
	selfMetric.HttpRequestController = controllers.NewHttpRequestController(nil)
	selfMetric.ElectionStatusController = controllers.NewElectionStatusController()
	selfMetric.MySQLOperationController = controllers.NewMySQLOperationController()
	selfMetric.ResourceStatusController = controllers.NewResourceStatusController()
	selfMetric.CheckFailController = selfController.NewCheckFailController()
	selfMetric.TaskNumController = selfController.NewTaskNumController()
	selfMetric.TaskTimeController = selfController.NewTaskTimeController()
	selfMetric.TaskRunningTimeController = selfController.NewTaskRunningTimeController()

	server, err := metric.NewMetricServer(&metric.Config{
		IP:         opts.Address,
		MetricPort: opts.MetricPort,
	},
		selfMetric.HttpRequestController,
		selfMetric.ElectionStatusController,
		selfMetric.MySQLOperationController,
		selfMetric.ResourceStatusController,
		selfMetric.CheckFailController,
		selfMetric.TaskNumController,
		selfMetric.TaskTimeController,
		selfMetric.TaskRunningTimeController,
	)

	if err != nil {
		return err
	}

	blog.Infof("success to start metric server")
	return server.Start()
}
