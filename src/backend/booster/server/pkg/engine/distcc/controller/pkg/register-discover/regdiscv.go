package rd

import (
	"context"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"strings"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/ssl"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/types"

	etcdClient "github.com/coreos/etcd/clientv3"
	etcdConcurrency "github.com/coreos/etcd/clientv3/concurrency"
)

// RegisterDiscover takes responsibility to register and elect a
// master, also watching the clusters change to notify the RoleChangeEvent.
type RegisterDiscover interface {
	Quit() error
	Register() (types.RoleChangeEvent, error)
	IsMaster() (bool, *types.DistCCControllerInfo, error)
	GetServers() ([]*types.DistCCControllerInfo, error)
}

// NewRegisterDiscover get a new register-discover
func NewRegisterDiscover(conf *config.DistCCControllerConfig) (RegisterDiscover, error) {
	addr := strings.Split(conf.EtcdEndpoints, ",")
	blog.Infof("get new register-discover with addr: %v", addr)

	var tlsConf *tls.Config
	var err error
	if len(conf.EtcdCaFile) != 0 && len(conf.EtcdCertFile) != 0 && len(conf.EtcdKeyFile) != 0 {
		tlsConf, err = ssl.ClientTSLConfVerity(conf.EtcdCaFile, conf.EtcdCertFile, conf.EtcdKeyFile, conf.EtcdKeyPwd)
		if err != nil {
			blog.Errorf("generate etcd tls failed: %v", err)
			return nil, err
		}
	}
	client, err := etcdClient.New(etcdClient.Config{
		Endpoints:   addr,
		DialTimeout: 5 * time.Second,
		TLS:         tlsConf,
	})
	if err != nil {
		blog.Errorf("fail to connect to etcd. err: %v", err)
		return nil, err
	}

	rootPath := strings.TrimSuffix(conf.EtcdRootPath, "/")
	if rootPath == "" {
		return nil, fmt.Errorf("etcd root path is not allowed to be top")
	}
	electionPath := rootPath + "/" + types.DistCCControllerElectionPath
	return &etcdRegisterDiscover{conf: conf, client: client, electionPath: electionPath}, nil
}

type etcdRegisterDiscover struct {
	conf         *config.DistCCControllerConfig
	client       *etcdClient.Client
	session      *etcdConcurrency.Session
	election     *etcdConcurrency.Election
	electionPath string

	event      chan types.RoleType
	role       types.RoleType
	roleMutex  sync.RWMutex
	serverInfo []byte

	leader        *types.DistCCControllerInfo
	leaderUpdated bool
	leaderMutex   sync.RWMutex

	servers       []*types.DistCCControllerInfo
	serverUpdated bool
	serverMutex   sync.RWMutex
}

// Quit stop the connections to etcd
func (erd *etcdRegisterDiscover) Quit() error {
	if erd.session != nil {
		return erd.session.Close()
	}
	blog.Infof("session already quit")
	return nil
}

// Register register and get the role change event to watch the role
func (erd *etcdRegisterDiscover) Register() (types.RoleChangeEvent, error) {
	blog.Infof("register etcd for election")
	// register server info to etcd
	if err := erd.generateServerInfo(); err != nil {
		blog.Errorf("register server info failed: %v", err)
		return nil, err
	}

	// before election, the role should be slave
	erd.event = make(chan types.RoleType, 1)
	erd.setRole(types.DistCCControllerSlave)

	go erd.elect()
	go erd.watch()
	return erd.event, nil
}

// IsMaster check whether I am the master
func (erd *etcdRegisterDiscover) IsMaster() (isMaster bool, info *types.DistCCControllerInfo, err error) {
	erd.roleMutex.RLock()
	role := erd.role
	erd.roleMutex.RUnlock()

	erd.leaderMutex.RLock()
	leader := erd.leader
	if role == types.DistCCControllerMaster {
		erd.leaderMutex.RUnlock()
		return true, leader, nil
	}

	defer func() {
		if leader.IP == erd.conf.LocalIP && leader.Port == erd.conf.Port {
			err = fmt.Errorf("server is no ready")
		}
	}()
	if erd.leaderUpdated {
		erd.leaderMutex.RUnlock()
		return false, leader, nil
	}
	erd.leaderMutex.RUnlock()

	leader, err = erd.updateLeader()
	if err != nil {
		return false, nil, err
	}
	return false, leader, nil
}

// GetServers get all servers registered in etcd
func (erd *etcdRegisterDiscover) GetServers() ([]*types.DistCCControllerInfo, error) {
	erd.serverMutex.RLock()
	servers := erd.servers
	if erd.serverUpdated {
		erd.serverMutex.RUnlock()
		return servers, nil
	}

	erd.serverMutex.RUnlock()
	servers, err := erd.updateServers()
	if err != nil {
		return nil, err
	}
	return servers, nil
}

func (erd *etcdRegisterDiscover) connect() {
	done := make(chan struct{}, 1)
	go erd.doConnect(done)
	select {
	case <-done:
		return
	}
}

func (erd *etcdRegisterDiscover) doConnect(done chan struct{}) {
	blog.Infof("try to build a new session with etcd")
	session, err := etcdConcurrency.NewSession(erd.client, etcdConcurrency.WithTTL(5))
	if erd.session != nil {
		_ = erd.session.Close()
	}
	erd.session = session
	if err != nil {
		blog.Errorf("build etcd session failed: %v, will retry in 5 seconds", err)
		time.Sleep(5 * time.Second)
		go erd.doConnect(done)
		return
	}
	erd.election = etcdConcurrency.NewElection(session, erd.electionPath)
	blog.Infof("build etcd session successfully")
	done <- struct{}{}
}

func (erd *etcdRegisterDiscover) updateLeader() (*types.DistCCControllerInfo, error) {
	erd.leaderMutex.Lock()
	defer erd.leaderMutex.Unlock()

	resp, err := erd.election.Leader(context.TODO())
	if err != nil {
		blog.Errorf("get current leader failed: %v", err)
		return nil, err
	}
	if len(resp.Kvs) == 0 {
		blog.Errorf("get current leader failed: returned data is empty")
		return nil, fmt.Errorf("leader data is empty")
	}

	erd.leader = new(types.DistCCControllerInfo)
	if err := codec.DecJSON(resp.Kvs[0].Value, erd.leader); err != nil {
		blog.Errorf("decode leader data failed: %v", err)
		return nil, err
	}

	blog.Infof("success to update leader: %s", string(resp.Kvs[0].Value))
	erd.leaderUpdated = true
	return erd.leader, nil
}

func (erd *etcdRegisterDiscover) updateServers() ([]*types.DistCCControllerInfo, error) {
	erd.serverMutex.Lock()
	defer erd.serverMutex.Unlock()

	erd.servers = make([]*types.DistCCControllerInfo, 0, 10)
	resp, err := erd.client.Get(context.TODO(), erd.electionPath+"/", etcdClient.WithPrefix())
	if err != nil {
		blog.Errorf("get current servers failed: %v", err)
		return nil, err
	}

	for _, kv := range resp.Kvs {
		var server types.DistCCControllerInfo
		if err := codec.DecJSON(kv.Value, &server); err != nil {
			blog.Errorf("decode server data failed: %v", err)
			return nil, err
		}
		erd.servers = append(erd.servers, &server)
	}

	erd.serverUpdated = true
	return erd.servers, nil
}

func (erd *etcdRegisterDiscover) watch() {
	blog.Infof("begin to watch election path: %s", erd.electionPath)
	watchChan := erd.client.Watch(context.TODO(), erd.electionPath+"/", etcdClient.WithPrefix())
	for {
		select {
		case <-watchChan:
			blog.Infof("registered services changed, leader & server will be both set no-updated")
			erd.leaderMutex.Lock()
			erd.leaderUpdated = false
			erd.leaderMutex.Unlock()

			erd.serverMutex.Lock()
			erd.serverUpdated = false
			erd.serverMutex.Unlock()
		}
	}
}

func (erd *etcdRegisterDiscover) elect() {
	erd.connect()
	blog.Infof("prepare to elect leader in etcd with server info: %s", erd.serverInfo)
	err := erd.election.Campaign(context.TODO(), string(erd.serverInfo))
	if err != nil {
		blog.Errorf("elect campaign failed: %v, will retry in 5 seconds", err)
		time.Sleep(5 * time.Second)
		go erd.elect()
		return
	}

	blog.Infof("elect as a master")
	erd.setRole(types.DistCCControllerMaster)

	select {
	case <-erd.session.Done():
		blog.Errorf("elect: etcd session expired")
		erd.event <- types.DistCCControllerUnknown
	}
	go erd.elect()
}

func (erd *etcdRegisterDiscover) setRole(role types.RoleType) {
	erd.roleMutex.Lock()
	defer erd.roleMutex.Unlock()
	erd.role = role
	erd.event <- role
}

func (erd *etcdRegisterDiscover) generateServerInfo() error {
	serverInfo := new(types.DistCCControllerInfo)
	serverInfo.IP = erd.conf.LocalIP
	serverInfo.Port = erd.conf.Port
	serverInfo.Scheme = "http"
	if erd.conf.ServerCert.IsSSL {
		serverInfo.Scheme = "https"
	}

	data, err := json.Marshal(serverInfo)
	if err != nil {
		blog.Errorf("fail to marshal server info to json. err: %v", err)
		return err
	}
	erd.serverInfo = data
	return nil
}
