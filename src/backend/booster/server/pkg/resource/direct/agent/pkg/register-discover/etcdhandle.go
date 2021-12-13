/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package registerdiscover

import (
	"context"
	"crypto/tls"
	"fmt"
	"strings"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/ssl"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct/agent/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct/agent/pkg/types"

	etcdClient "github.com/coreos/etcd/clientv3"
	etcdConcurrency "github.com/coreos/etcd/clientv3/concurrency"
)

// RegisterDiscover takes responsibility to register and elect a
// master, also watching the clusters change to notify the RoleChangeEvent.
type RegisterDiscover interface {
	GetServers() ([]*types.DistCCServerInfo, error)
}

// NewRegisterDiscover : return server discover object
func NewRegisterDiscover(conf *config.ServerConfig) (RegisterDiscover, error) {
	addr := strings.Split(conf.EtcdEndpoints, ",")
	blog.Debugf("get new register-discover with addr: %v", addr)

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
	electionPath := rootPath + "/" + types.ServerElectionPath

	erd := &etcdRegisterDiscover{conf: conf, client: client, electionPath: electionPath}
	go erd.watch()

	return erd, nil
}

type etcdRegisterDiscover struct {
	conf         *config.ServerConfig
	client       *etcdClient.Client
	session      *etcdConcurrency.Session
	election     *etcdConcurrency.Election
	electionPath string

	event      chan types.RoleType
	role       types.RoleType
	roleMutex  sync.RWMutex
	serverInfo []byte

	leader        *types.DistCCServerInfo
	leaderUpdated bool
	leaderMutex   sync.RWMutex

	servers       []*types.DistCCServerInfo
	serverUpdated bool
	serverMutex   sync.RWMutex
}

// GetServers return the current registered server in etcd
func (erd *etcdRegisterDiscover) GetServers() ([]*types.DistCCServerInfo, error) {
	erd.serverMutex.RLock()
	defer erd.serverMutex.RUnlock()

	return erd.servers, nil
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

func (erd *etcdRegisterDiscover) updateServers() error {
	blog.Infof("rd: updateServers...")

	erd.serverMutex.Lock()
	defer erd.serverMutex.Unlock()

	erd.servers = make([]*types.DistCCServerInfo, 0, 10)
	resp, err := erd.client.Get(context.TODO(), erd.electionPath+"/", etcdClient.WithPrefix())
	if err != nil {
		blog.Errorf("rd: get current servers failed: %v", err)
		return err
	}

	for _, kv := range resp.Kvs {
		var server types.DistCCServerInfo
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
	_ = erd.updateServers()

	watchChan := erd.client.Watch(context.TODO(), erd.electionPath+"/", etcdClient.WithPrefix())
	for {
		select {
		case resp := <-watchChan:
			blog.Infof("rd: watch resp [%+v]", resp)
			_ = erd.updateServers()
		}
	}
}
