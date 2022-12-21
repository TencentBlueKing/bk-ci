/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package crm

import (
	"fmt"
	"strings"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/util"
	op "github.com/Tencent/bk-ci/src/booster/server/pkg/resource/crm/operator"
)

// NewBrokerSet get a new, empty broker set.
func NewBrokerSet() *BrokerSet {
	return &BrokerSet{
		brokers: make(map[string][]*Broker, 100),
	}
}

// BrokerSet contains multiple user's brokers. Each user has a list of brokers, calling apply will check
// every item in this list and use the first match broker to do the work.
type BrokerSet struct {
	sync.Mutex
	brokers map[string][]*Broker
}

// Recover take the responsibility to run all brokers goroutines
func (bs *BrokerSet) Recover() error {
	bs.Lock()
	defer bs.Unlock()

	for _, brokers := range bs.brokers {
		for _, broker := range brokers {
			if err := broker.Run(); err != nil {
				return err
			}
		}
	}

	return nil
}

// Add receive a new broker and add it into the user list which it belongs to.
func (bs *BrokerSet) Add(broker *Broker) {
	bs.Lock()
	defer bs.Unlock()

	if _, ok := bs.brokers[broker.user]; !ok {
		bs.brokers[broker.user] = make([]*Broker, 0, 100)
	}

	bs.brokers[broker.user] = append(bs.brokers[broker.user], broker)
}

// List get all brokers from broker set.
func (bs *BrokerSet) List() []*Broker {
	bs.Lock()
	defer bs.Unlock()

	l := make([]*Broker, 0, 100)
	for _, bl := range bs.brokers {
		l = append(l, bl...)
	}

	return l
}

// Apply receive the broker Apply params and an user.
// User is used to find out the broker list in the set, and call the broker Apply.
func (bs *BrokerSet) Apply(
	resourceID, user string,
	param ResourceParam,
	filterFunc op.InstanceFilterFunction) (string, error) {

	bs.Lock()
	defer bs.Unlock()

	brokers, ok := bs.brokers[user]
	if !ok {
		return "", ErrorBrokerParamNotFit
	}

	for _, broker := range brokers {
		if brokerID, err := broker.Apply(resourceID, param, filterFunc); err != ErrorBrokerParamNotFit {
			return brokerID, err
		}
	}
	return "", ErrorBrokerParamNotFit
}

// BrokerParam describe the params of one broker, contains the following fields:
// - Param resource param, use to launch broker resource automatically
// - Instance decide how many instances in one broker resource
// - FitFunc a function receive the broker resource param and request resource param,
// - ReleaseLoop indicate whether release resource with loop until succeed
// - IdleKeepSeconds to set wait seconds before release idle resource
//  check if the broker can serve this request.
type BrokerParam struct {
	Param           ResourceParam
	Instance        int
	FitFunc         BrokerParamFitFunction
	ReleaseLoop     bool
	IdleKeepSeconds int
}

// BrokerParamFitFunction define the function type of broker condition checker.
// Aim to compare the resource request with the broker to check if the broker can handle this request.
type BrokerParamFitFunction func(brokerParam, requestParam ResourceParam) bool

// NewBroker get a new broker.
// name and user define the broker name and the user in resource manager.
// mgr is the container resource manager instance.
// strategyType describe which strategy the broker runs and the strategy is a BrokerStrategy to do the strategy work.
// param describe the broker config such as resource and image.
func NewBroker(
	name, user string,
	mgr *resourceManager,
	strategyType StrategyType,
	strategy BrokerStrategy,
	param BrokerParam) *Broker {

	name = blockName(name, user)
	param.Param.BrokerName = name
	return &Broker{
		name:          name,
		user:          user,
		mgr:           mgr,
		strategyType:  strategyType,
		strategy:      strategy,
		param:         param,
		freeResources: make(map[string]freeResourceInfo, 100),
		initPool:      make(map[string]bool, 100),
		releasePool:   make(map[string]bool, 100),
	}
}

type freeResourceInfo struct {
	status   bool
	inittime time.Time
}

// Broker maintains a type of pre-launch-server, it has following features:
// - When someone call the Broker for Applying, the Broker check if it matches the conditions,
//   if so, give the caller the running server, and kicked the server out from its pool.
// - When the number of running servers in its pool is less or more than expected, the Broker launch or release servers
//   to match the expect number.
type Broker struct {
	name         string
	user         string
	param        BrokerParam
	strategyType StrategyType

	freeResourceLock sync.Mutex
	freeResources    map[string]freeResourceInfo

	currentNumLock sync.Mutex
	currentNum     int

	strategy BrokerStrategy
	mgr      *resourceManager

	initPoolLock sync.Mutex
	initPool     map[string]bool

	// to save resource ids which failed to release
	releasePoolLock sync.Mutex
	releasePool     map[string]bool

	coolingLock sync.Mutex
	cooling     bool
	coolingTime time.Time
}

const (
	brokerIDRandomLength = 8

	brokerTrackerTimeGap   = 1 * time.Second
	brokerCoolingTime      = 10 * time.Minute
	brokerWatchFreeTimeGap = 60 * time.Second
	releaseTimeGap         = 60 * time.Second
)

// Run the broker, begins to provide the apply service and ensure the pre-launch-server number.
func (b *Broker) Run() error {
	blog.Infof("crm broker: broker(%s) with user(%s) run", b.name, b.user)
	if err := b.recover(); err != nil {
		blog.Errorf("crm broker: broker(%s) with user(%s) recover failed: %v", b.name, b.user, err)
		return err
	}

	// watch free resource
	go b.watchFree()

	// release resources which failed to release
	go b.releaseLoop()

	return nil
}

// Apply receive origin resource's ID, params and filter function, and try to get a running-resource from
// broker pool to serve.
// First of all, the origin resource's params should match with the broker params
// Then use the provided filter function to check if the instances enough
// Then consume the running-resource from broker pool.
func (b *Broker) Apply(resourceID string, param ResourceParam, filterFunc op.InstanceFilterFunction) (string, error) {
	if !b.param.FitFunc(b.param.Param, param) {
		return "", ErrorBrokerParamNotFit
	}

	if _, err := filterFunc(b.param.Instance); err != nil {
		return "", ErrorBrokerNotEnoughResources
	}

	return b.consume(resourceID)
}

func (b *Broker) recover() error {
	rl, err := b.mgr.listResources(resourceStatusInit, resourceStatusDeploying, resourceStatusRunning)
	if err != nil {
		blog.Errorf("crm broker: recover resource failed: %v", err)
		return err
	}

	b.freeResourceLock.Lock()
	defer b.freeResourceLock.Unlock()

	b.currentNumLock.Lock()
	defer b.currentNumLock.Unlock()

	b.freeResources = make(map[string]freeResourceInfo, 100)
	b.currentNum = 0
	for _, r := range rl {
		if r.brokerName != b.name || r.brokerSold {
			continue
		}

		blog.Infof("crm broker: broker(%s) recover resource(%s): %s", b.name, r.status, r.resourceID)
		// recover running and deploying resource as current num.
		b.currentNum++

		if r.status != resourceStatusRunning {
			go b.tracker(r.resourceID)
			continue
		}

		b.freeResources[r.resourceID] = freeResourceInfo{
			status: true,
			// TODO : get the time from db
			inittime: time.Now().Local(),
		}
	}

	// load deleting resources
	rl, err = b.mgr.listResources(resourceStatusDeleting)
	if err == nil && rl != nil {
		for _, r := range rl {
			if r.brokerName != b.name {
				continue
			}
			blog.Infof("crm broker: broker(%s) recover resource(%s): %s", b.name, r.status, r.resourceID)
			b.addReleaseResource(r.resourceID)
		}
	}

	return nil
}

// CurrentNum return the current brokers number
func (b *Broker) CurrentNum() int {
	b.currentNumLock.Lock()
	defer b.currentNumLock.Unlock()

	return b.currentNum
}

// Launch a new resource and put it into pool
func (b *Broker) Launch() error {
	return b.launch()
}

// Release a free resource from pool
func (b *Broker) Release() error {
	return b.release()
}

func (b *Broker) consume(resourceID string) (string, error) {
	b.freeResourceLock.Lock()
	defer b.freeResourceLock.Unlock()

	brokerID := ""
	for id := range b.freeResources {
		brokerID = id
		break
	}
	if brokerID == "" {
		return "", ErrorBrokerNotEnoughResources
	}

	b.mgr.lockResource(brokerID)
	defer b.mgr.unlockResource(brokerID)
	brokerResource, err := b.mgr.getResources(brokerID)
	if err != nil {
		return "", err
	}

	brokerResource.brokerSold = true
	if err = b.mgr.saveResources(brokerResource); err != nil {
		return "", err
	}
	delete(b.freeResources, brokerID)

	// minus 1 from current num
	b.currentNumLock.Lock()
	b.currentNum--
	b.currentNumLock.Unlock()

	blog.Infof("crm broker: success to apply origin-resource(%s) and get running-resource(%s) from broker(%s)",
		resourceID, brokerResource.resourceID, b.name)
	return brokerID, nil
}

func (b *Broker) launch() error {
	if !b.mgr.running {
		return ErrorManagerNotRunning
	}

	b.coolingLock.Lock()
	defer b.coolingLock.Unlock()
	if b.cooling && b.coolingTime.Add(brokerCoolingTime).After(time.Now().Local()) {
		return ErrorBrokeringUnderCoolingTime
	}

	brokerID := ""
	b.initPoolLock.Lock()
	for brokerID = range b.initPool {
		delete(b.initPool, brokerID)
		break
	}
	b.initPoolLock.Unlock()

	if brokerID == "" {
		brokerID = b.generateID()
		if err := b.mgr.init(brokerID, b.user, b.param.Param); err != nil {
			return err
		}
	}

	err := b.mgr.launch(brokerID, b.user, "", func(availableInstance int) (need int, err error) {
		if availableInstance >= b.param.Instance {
			return b.param.Instance, nil
		}

		return 0, ErrorBrokerNotEnoughResources
	}, false)
	if err == ErrorBrokerNotEnoughResources {
		b.initPoolLock.Lock()
		b.initPool[brokerID] = true
		b.initPoolLock.Unlock()
		return err
	}
	if err != nil {
		b.cooling = true
		b.coolingTime = time.Now().Local()
		err1 := b.mgr.release(brokerID, b.user)
		if err1 != nil {
			b.addReleaseResource(brokerID)
		}
		return err
	}

	// add 1 to current num
	b.currentNumLock.Lock()
	b.currentNum++
	b.currentNumLock.Unlock()

	go b.tracker(brokerID)
	blog.Infof("crm broker: success to launch resource(%s) for broker(%s) with user(%s)",
		brokerID, b.name, b.user)
	return nil
}

func (b *Broker) release() error {
	if !b.mgr.running {
		return ErrorManagerNotRunning
	}

	b.freeResourceLock.Lock()
	defer b.freeResourceLock.Unlock()

	resourceID := ""
	for id := range b.freeResources {
		resourceID = id
		break
	}
	if resourceID == "" {
		return nil
	}
	delete(b.freeResources, resourceID)

	// minus 1 from current num
	b.currentNumLock.Lock()
	b.currentNum--
	b.currentNumLock.Unlock()

	if err := b.mgr.release(resourceID, b.user); err != nil {
		blog.Errorf("crm broker: try to release resource(%s) from broker(%s) with user(%s) failed: %v",
			resourceID, b.name, b.user, err)
		b.addReleaseResource(resourceID)

		return err
	}

	blog.Infof("crm broker: success to release resource(%s) for broker(%s) with user(%s)",
		resourceID, b.name, b.user)
	return nil
}

func (b *Broker) tracker(resourceID string) {
	blog.Infof("crm broker: start tracking resource(%s) from broker(%s) with user(%s)",
		resourceID, b.name, b.user)
	ticker := time.NewTicker(brokerTrackerTimeGap)
	defer ticker.Stop()

	startTime := time.Now().Local()

	for {
		select {
		case <-b.mgr.ctx.Done():
			blog.Warnf("crm broker: resource(%s) from broker(%s) with user(%s) tracking done",
				resourceID, b.name, b.user)
			return
		case <-ticker.C:
			if b.track(resourceID, startTime) {
				blog.Infof("crm broker: finshed track resource(%s) of broker(%s) with user(%s) ",
					resourceID, b.name, b.user)
				return
			}
		}
	}
}

func (b *Broker) track(resourceID string, startTime time.Time) bool {
	b.mgr.lockResource(resourceID)
	resource, err := b.mgr.getResources(resourceID)
	b.mgr.unlockResource(resourceID)
	if err != nil {
		blog.Errorf("crm broker: track resource(%s) from broker(%s) with user(%s), get resource failed: %v",
			resourceID, b.name, b.user, err)
		return false
	}

	if resource.status == resourceStatusRunning {
		// resourceStatusRunning means all instance status of this resource confirmed(succeed or failed),
		// but we will only put resource which all succeed to free pool,
		// so we need check resource's all instance status here
		instanceok := false
		info, err := b.mgr.getServiceInfo(resourceID, b.user)
		if err == nil && info != nil {
			if len(info.AvailableEndpoints) > (b.param.Instance / 2) {
				instanceok = true
			}
		}

		if !instanceok {
			blog.Errorf("crm broker: track resource(%s) from broker(%s) with user(%s), not found expected %d instances, release it",
				resourceID, b.name, b.user, b.param.Instance)
			// release this resource
			err = b.mgr.release(resource.resourceID, b.user)
			if err != nil {
				b.addReleaseResource(resource.resourceID)
			}

			// status init should be release and minus 1 from current num
			b.currentNumLock.Lock()
			b.currentNum--
			b.currentNumLock.Unlock()
			return true
		}

		b.freeResourceLock.Lock()
		blog.Infof("crm broker: add complete resource(%s) in status(%s)"+
			"to broker(%s) with user(%s), resource data[%+v]", resourceID, resource.status.String(),
			b.name, b.user, *resource)
		b.freeResources[resource.resourceID] = freeResourceInfo{
			status:   true,
			inittime: time.Now().Local(),
		}
		b.freeResourceLock.Unlock()
		return true
	}

	// dirty data which created as init but no one launch it.
	// if a broker is in deploying status for more then 2min, then release it.
	if resource.status == resourceStatusInit ||
		(resource.status == resourceStatusDeploying && time.Now().Local().After(startTime.Add(2*time.Minute))) {
		blog.Infof("crm broker: clean dirty resource(%s) in status(%s) from broker(%s) with user(%s)",
			resourceID, resource.status.String(), b.name, b.user)
		err = b.mgr.release(resource.resourceID, b.user)
		if err != nil {
			b.addReleaseResource(resource.resourceID)
		}

		// status init should be release and minus 1 from current num
		b.currentNumLock.Lock()
		b.currentNum--
		b.currentNumLock.Unlock()
		return true
	}

	if resource.status != resourceStatusDeploying {
		blog.Errorf("crm broker: track resource(%s) from broker(%s) with user(%s) has status(%s), no need track",
			resourceID, b.name, b.user, resource.status.String())
		return true
	}

	return false
}

func (b *Broker) watchFree() {
	blog.Infof("crm broker: start watch free resources of broker(%s) with user(%s)", b.name, b.user)
	if b.param.IdleKeepSeconds <= 0 {
		blog.Infof("crm broker: do not watch from broker(%s) with user(%s) for idle keep seconds %d",
			b.name, b.user, b.param.IdleKeepSeconds)
		return
	}

	ticker := time.NewTicker(brokerWatchFreeTimeGap)
	defer ticker.Stop()

	for {
		select {
		case <-b.mgr.ctx.Done():
			blog.Warnf("crm broker: broker(%s) with user(%s) watch done", b.name, b.user)
			return
		case <-ticker.C:
			b.doWatchFree(b.param.IdleKeepSeconds)
		}
	}
}

func (b *Broker) doWatchFree(keepseconds int) {
	b.freeResourceLock.Lock()
	defer b.freeResourceLock.Unlock()

	blog.Infof("crm broker: watch %d free resources for broker(%s) currentNum %d", len(b.freeResources), b.name, b.currentNum)

	for id, info := range b.freeResources {

		if time.Now().Local().After(info.inittime.Add(time.Duration(keepseconds) * time.Second)) {
			delete(b.freeResources, id)

			blog.Infof("crm broker: ready release overtime free resources %s created at %s of broker(%s) ",
				id, info.inittime.String(), b.name)

			// minus 1 from current num
			b.currentNumLock.Lock()
			b.currentNum--
			b.currentNumLock.Unlock()

			// release
			err := b.mgr.release(id, b.user)
			if err != nil {
				b.addReleaseResource(id)
			}

			// TODO : need opt free strategy here
			// to avoid delete all resource one time, will free one task's resource per minute now
			break
		}
	}
}

func (b *Broker) releaseLoop() {
	blog.Infof("crm broker: start run release resources loop of broker(%s) with user(%s)", b.name, b.user)
	if !b.param.ReleaseLoop {
		blog.Infof("crm broker: do not run release loop from broker(%s) with user(%s)",
			b.name, b.user)
		return
	}

	ticker := time.NewTicker(releaseTimeGap)
	defer ticker.Stop()

	for {
		select {
		case <-b.mgr.ctx.Done():
			blog.Warnf("crm broker: broker(%s) with user(%s) release loop done", b.name, b.user)
			return
		case <-ticker.C:
			b.doReleaseLoop()
		}
	}
}

func (b *Broker) doReleaseLoop() {
	b.releasePoolLock.Lock()
	defer b.releasePoolLock.Unlock()

	blog.Infof("crm broker: do release %d resources for broker(%s) currentNum %d", len(b.releasePool), b.name, b.currentNum)

	for id := range b.releasePool {
		// release
		err := b.mgr.release(id, b.user)
		if err == nil {
			delete(b.releasePool, id)
		}
	}
}

func (b *Broker) addReleaseResource(id string) {
	b.releasePoolLock.Lock()
	defer b.releasePoolLock.Unlock()

	if _, ok := b.releasePool[id]; !ok {
		// update resource status
		r, err := b.mgr.getResources(id)
		if err == nil && r != nil {
			r.status = resourceStatusDeleting
			b.mgr.saveResources(r)
		}
		b.releasePool[id] = true
		blog.Infof("crm broker: added resources %s for broker(%s) to realease pool currentNum %d", id, b.name, b.currentNum)
	}
}

func (b *Broker) generateID() string {
	return strings.ReplaceAll(strings.ToLower(fmt.Sprintf("b%s-%s",
		b.param.Param.BrokerName, util.RandomString(brokerIDRandomLength),
	)), "_", "-")
}

type StrategyType int

const (
	StrategyConst StrategyType = iota
)

// BrokerStrategy describe a interface for strategy in broker launcher.
// It decides how many resource should the broker add or reduce in its pool.
type BrokerStrategy interface {
	// ask strategy now how many resources should we adjust? more(>0) or less(<0)
	Ask(param interface{}) int
}

// NewConstBrokerStrategy get a new const strategy
func NewConstBrokerStrategy(num int) BrokerStrategy {
	return &constBrokerStrategy{
		constNum: num,
	}
}

// constBrokerStrategy describe a broker strategy that keep a const number of idle broker resources.
// For instance, if const num is 2, then there should be always 2 idle broker resources waiting for applying.
type constBrokerStrategy struct {
	sync.Mutex
	constNum int
}

// Ask receive a int which describe the current idle broker num.
// Return a int describe the expected delta num of idle broker.
func (dbs *constBrokerStrategy) Ask(param interface{}) int {
	dbs.Lock()
	defer dbs.Unlock()

	currentNum, ok := param.(int)
	if !ok {
		return 0
	}
	return dbs.constNum - currentNum
}

func blockName(name, user string) string {
	return strings.ReplaceAll(fmt.Sprintf("%s-%s", user, name), "_", "-")
}
