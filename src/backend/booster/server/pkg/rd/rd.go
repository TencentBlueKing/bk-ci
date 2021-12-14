/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

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
	"github.com/Tencent/bk-ci/src/booster/server/config"
	selfMetric "github.com/Tencent/bk-ci/src/booster/server/pkg/metric"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/types"

	etcdClient "github.com/coreos/etcd/clientv3"
	etcdConcurrency "github.com/coreos/etcd/clientv3/concurrency"
)

// RegisterDiscover takes responsibility to register and elect a
// master, also watching the clusters change to notify the RoleChangeEvent.
type RegisterDiscover interface {
	Quit() error
	AddObserver(event chan types.RoleType) error
	IsMaster() (bool, *types.ServerInfo, error)
	GetServers() ([]*types.ServerInfo, error)
	IsServerIP(ip string) (bool, error)
	Put(path string, data []byte) error
	Get(path string) ([]byte, error)
	Del(path string) error
	Run() error
}

// NewRegisterDiscover : return server discover object
func NewRegisterDiscover(conf *config.ServerConfig) (RegisterDiscover, error) {
	addr := strings.Split(conf.EtcdEndpoints, ",")
	blog.Debugf("rd: get new register-discover with addr: %v", addr)

	var tlsConf *tls.Config
	var err error
	if len(conf.EtcdCaFile) != 0 && len(conf.EtcdCertFile) != 0 && len(conf.EtcdKeyFile) != 0 {
		tlsConf, err = ssl.ClientTSLConfVerity(conf.EtcdCaFile, conf.EtcdCertFile, conf.EtcdKeyFile, conf.EtcdKeyPwd)
		if err != nil {
			blog.Errorf("rd: generate etcd tls failed: %v", err)
			return nil, err
		}
	}
	client, err := etcdClient.New(etcdClient.Config{
		Endpoints:   addr,
		DialTimeout: 5 * time.Second,
		TLS:         tlsConf,
	})
	if err != nil {
		blog.Errorf("rd: fail to connect to etcd. err: %v", err)
		return nil, err
	}

	rootPath := strings.TrimSuffix(conf.EtcdRootPath, "/")
	if rootPath == "" {
		return nil, fmt.Errorf("rd: etcd root path is not allowed to be top")
	}
	electionPath := rootPath + "/" + types.ServerElectionPath
	dataPath := rootPath + "/" + types.ServerDataPath
	return &etcdRegisterDiscover{conf: conf, client: client, electionPath: electionPath, dataPathPrefix: dataPath}, nil
}

type etcdRegisterDiscover struct {
	conf           *config.ServerConfig
	client         *etcdClient.Client
	session        *etcdConcurrency.Session
	election       *etcdConcurrency.Election
	electionPath   string
	dataPathPrefix string

	// 保存关注主从切换事件的对象列表
	observerEvents []chan types.RoleType

	role      types.RoleType
	roleMutex sync.RWMutex

	// selfServerKey 是在etcd竞选之后生成的key，保存起来，便于确认自己被删除的事件
	selfServerKey string
	selfServer    *types.ServerInfo
	serverInfo    []byte

	leader        *types.ServerInfo
	leaderUpdated bool
	leaderMutex   sync.RWMutex

	servers       []*types.ServerInfo
	serverUpdated bool
	serverMutex   sync.RWMutex
}

// Quit stop the connections to etcd
func (erd *etcdRegisterDiscover) Quit() error {
	if erd.session != nil {
		return erd.session.Close()
	}
	blog.Infof("rd: session already quit")

	return nil
}

// AddObserver subscribe an observer to receive role change event
func (erd *etcdRegisterDiscover) AddObserver(observer chan types.RoleType) error {
	blog.Infof("rd: AddObserver [%+v]", observer)
	erd.observerEvents = append(erd.observerEvents, observer)
	return nil
}

// Run run rd
func (erd *etcdRegisterDiscover) Run() error {
	blog.Infof("Run etcd")
	// register server info to etcd
	if err := erd.initServerInfo(); err != nil {
		blog.Errorf("rd: register server info failed: %v", err)
		return err
	}

	//erd.event = make(chan types.RoleType, 1)
	erd.reset()

	go erd.elect()
	go erd.watch()
	//return erd.event, nil
	return nil
}

// IsMaster check whether I am the master
func (erd *etcdRegisterDiscover) IsMaster() (bool, *types.ServerInfo, error) {
	erd.roleMutex.RLock()
	defer erd.roleMutex.RUnlock()

	if erd.leader == nil {
		return false, nil, types.ErrorLeaderNoFound
	}
	return erd.role == types.ServerMaster, erd.leader, nil
}

// GetServers get all servers registered in etcd
func (erd *etcdRegisterDiscover) GetServers() ([]*types.ServerInfo, error) {
	erd.serverMutex.RLock()
	defer erd.serverMutex.RUnlock()

	return erd.servers, nil
}

// IsServerIP check if the given ip is one of the servers' ip
func (erd *etcdRegisterDiscover) IsServerIP(ip string) (bool, error) {
	servers, err := erd.GetServers()
	if err != nil {
		return false, err
	}

	for _, server := range servers {
		if server.IP == ip {
			return true, nil
		}
	}

	return false, nil
}

// Put put data into etcd path
func (erd *etcdRegisterDiscover) Put(path string, data []byte) error {
	return erd.put(path, data)
}

// Get get data from etcd path
func (erd *etcdRegisterDiscover) Get(path string) ([]byte, error) {
	return erd.get(path)
}

// Del delete data from etcd path
func (erd *etcdRegisterDiscover) Del(path string) error {
	return erd.del(path)
}

func (erd *etcdRegisterDiscover) reset() {
	blog.Infof("reset...")

	erd.selfServerKey = ""

	erd.roleMutex.Lock()
	erd.setRole(types.ServerUnknown)
	erd.leader = nil
	erd.roleMutex.Unlock()

	erd.serverMutex.Lock()
	erd.servers = make([]*types.ServerInfo, 0, 10)
	erd.serverMutex.Unlock()

}

func (erd *etcdRegisterDiscover) connect() {
	blog.Infof("connect...")

	done := make(chan struct{}, 1)
	go erd.doConnect(done)
	select {
	case <-done:
		return
	}
}

func (erd *etcdRegisterDiscover) doConnect(done chan struct{}) {
	blog.Infof("rd: try to build a new session with etcd")
	session, err := etcdConcurrency.NewSession(erd.client, etcdConcurrency.WithTTL(5))
	if erd.session != nil {
		_ = erd.session.Close()
	}
	erd.session = session
	if err != nil {
		blog.Errorf("rd: build etcd session failed: %v, will retry in 5 seconds", err)
		time.Sleep(5 * time.Second)
		go erd.doConnect(done)
		return
	}
	erd.election = etcdConcurrency.NewElection(session, erd.electionPath)
	blog.Infof("rd: build etcd session successfully")
	done <- struct{}{}
}

func (erd *etcdRegisterDiscover) waitElectionInit() {
	if erd.election == nil {
		for ; ; time.Sleep(1 * time.Second) {
			if erd.election != nil {
				return
			}
		}
	}
}

func (erd *etcdRegisterDiscover) updateLeaderAndRole() error {
	blog.Infof("rd: updateLeaderAndRole...")

	// erd.election 可能没有初始化好，需要判断等待
	erd.waitElectionInit()

	// Leader 是个阻塞操作
	var reterr error
	resp, err := erd.election.Leader(context.TODO())

	erd.roleMutex.Lock()
	defer erd.roleMutex.Unlock()

	if err != nil {
		blog.Errorf("rd: get current leader failed: %v", err)
		erd.leader = nil
		reterr = err
		goto SETROLE
	}
	if len(resp.Kvs) == 0 {
		blog.Errorf("rd: get current leader failed: returned data is empty")
		erd.leader = nil
		reterr = fmt.Errorf("rd: leader data is empty")
		goto SETROLE
	}

	erd.leader = new(types.ServerInfo)
	if err := codec.DecJSON(resp.Kvs[0].Value, erd.leader); err != nil {
		blog.Errorf("rd: decode leader data failed: %v", err)
		erd.leader = nil
		reterr = err
		goto SETROLE
	}

SETROLE:
	blog.Infof("rd: check role now...")
	// 判断role是否有变化
	roleChanged := false
	isMaster := false
	newRole := types.ServerSlave

	if erd.leader != nil {
		if sameServer(erd.leader, erd.selfServer) {
			isMaster = true
		}
	}

	if erd.role != types.ServerMaster && isMaster {
		newRole = types.ServerMaster
		roleChanged = true
	} else if erd.role == types.ServerMaster && !isMaster {
		newRole = types.ServerSlave
		roleChanged = true
	}
	if roleChanged {
		erd.setRole(newRole)
	}

	return reterr
}

func (erd *etcdRegisterDiscover) updateServers() error {
	blog.Infof("rd: updateServers...")

	erd.serverMutex.Lock()
	defer erd.serverMutex.Unlock()

	erd.servers = make([]*types.ServerInfo, 0, 10)
	resp, err := erd.client.Get(context.TODO(), erd.electionPath+"/", etcdClient.WithPrefix())
	if err != nil {
		blog.Errorf("rd: get current servers failed: %v", err)
		return err
	}

	for _, kv := range resp.Kvs {
		var server types.ServerInfo
		if err := codec.DecJSON(kv.Value, &server); err != nil {
			blog.Errorf("rd: decode server data failed: %v", err)
			return err
		}
		erd.servers = append(erd.servers, &server)
	}

	// for debug
	for _, v := range erd.servers {
		blog.Infof("rd: after updateServers server info[%+v]", *v)
	}

	return nil
}

func (erd *etcdRegisterDiscover) watch() {
	watchChan := erd.client.Watch(context.TODO(), erd.electionPath+"/", etcdClient.WithPrefix())
	for {
		select {
		case resp := <-watchChan:
			blog.Infof("rd: watch resp [%+v]", resp)
			for _, ev := range resp.Events {
				blog.Infof("rd: watch event [%s %q : %q]", ev.Type, ev.Kv.Key, ev.Kv.Value)
				if ev.Type == etcdClient.EventTypePut {
					if string(ev.Kv.Value) == string(erd.serverInfo) {
						blog.Infof("rd: watch resp [found self succeed put]")
						erd.selfServerKey = string(ev.Kv.Key)
						blog.Infof("rd: watch resp erd.selfServerKey is[%s]", erd.selfServerKey)
					}
				} else if ev.Type == etcdClient.EventTypeDelete {
					if string(ev.Kv.Key) == erd.selfServerKey {
						blog.Infof("rd: watch resp [found self succeed delete]")
						erd.selfServerKey = ""
						// 通知manager重置
						erd.reset()
					}
				}
			}

			erd.updateServerInfo()
		}
	}
}

func (erd *etcdRegisterDiscover) updateServerInfo() {
	blog.Infof("rd: updateServerInfo...")
	// 刷新leader
	_ = erd.updateLeaderAndRole()

	// 刷新server列表
	_ = erd.updateServers()
}

func (erd *etcdRegisterDiscover) elect() {
	blog.Infof("rd: elect...")
	erd.connect()
	blog.Infof("rd: prepare to elect leader in etcd with server info: %s", erd.serverInfo)

	// erd.election 可能没有初始化好，需要判断等待
	erd.waitElectionInit()

	ctx, cancel := context.WithCancel(context.Background())
	go erd.watchSession(cancel)

	err := erd.election.Campaign(ctx, string(erd.serverInfo))
	if err != nil {
		blog.Errorf("rd: elect campaign failed: %v, will retry in 5 seconds", err)
		time.Sleep(5 * time.Second)
		go erd.elect()
		return
	}

	blog.Infof("rd: elected as a master")

	select {
	case <-ctx.Done():
		blog.Infof("rd: elect: etcd session expired,try new elect now")
	}

	go erd.elect()
}

func (erd *etcdRegisterDiscover) watchSession(cancel context.CancelFunc) {
	blog.Infof("rd: watchSession...")
	select {
	case <-erd.session.Done():
		blog.Errorf("rd: elect: etcd session expired")
		cancel()
	}
}

func (erd *etcdRegisterDiscover) setRole(role types.RoleType) {
	blog.Infof("rd: prepare to setRole:[%s]", string(role))

	erd.role = role
	for _, v := range erd.observerEvents {
		blog.Infof("rd: send role [%s] to [%+v]", string(role), v)
		v <- role
	}

	switch role {
	case types.ServerMaster:
		selfMetric.ElectionStatusController.BecomeLeader()
	case types.ServerSlave:
		selfMetric.ElectionStatusController.BecomeFollower()
	default:
		selfMetric.ElectionStatusController.BecomeUnknown()
	}
}

func (erd *etcdRegisterDiscover) initServerInfo() error {
	blog.Infof("rd: initServerInfo with[%s:%d]", erd.conf.LocalIP, erd.conf.Port)

	serverInfo := new(types.ServerInfo)
	serverInfo.IP = erd.conf.LocalIP
	serverInfo.Port = erd.conf.Port
	serverInfo.ResourcePort = erd.conf.DirectResourceConfig.ListenPort
	serverInfo.Scheme = "http"
	if erd.conf.ServerCert.IsSSL {
		serverInfo.Scheme = "https"
	}

	data, err := json.Marshal(serverInfo)
	if err != nil {
		blog.Errorf("rd: fail to marshal server info to json. err: %v", err)
		return err
	}
	erd.serverInfo = data
	erd.selfServer = serverInfo
	return nil
}

func (erd *etcdRegisterDiscover) getDataPath(path string) string {
	return strings.TrimRight(erd.dataPathPrefix, "/") + "/" + strings.TrimLeft(path, "/")
}

func (erd *etcdRegisterDiscover) put(path string, data []byte) error {
	path = erd.getDataPath(path)

	blog.Infof("rd: etcd put %s with data(%s)", path, string(data))
	if _, err := erd.client.Put(context.TODO(), path, string(data)); err != nil {
		blog.Errorf("rd: etcd put %s with data(%s) failed: %v", path, string(data), err)
		return err
	}

	return nil
}

func (erd *etcdRegisterDiscover) get(path string) ([]byte, error) {
	path = erd.getDataPath(path)

	data, err := erd.client.Get(context.TODO(), path)
	if err != nil {
		blog.Errorf("rd: etcd get %s failed: %v", path, err)
		return nil, err
	}

	if len(data.Kvs) == 0 {
		return nil, types.ErrorDataPathNoFound
	}

	return data.Kvs[0].Value, nil
}

func (erd *etcdRegisterDiscover) del(path string) error {
	path = erd.getDataPath(path)

	blog.Infof("rd: etcd delete %s", path)
	if _, err := erd.client.Delete(context.TODO(), path); err != nil {
		blog.Errorf("rd: etcd delete %s failed: %v", path, err)
		return err
	}

	return nil
}

func sameServer(s1 *types.ServerInfo, s2 *types.ServerInfo) bool {
	return s1.IP == s2.IP && s1.Port == s2.Port
}
