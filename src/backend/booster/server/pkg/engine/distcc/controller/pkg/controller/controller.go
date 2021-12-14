package controller

import (
	"context"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/store"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/types"
)

// Controller provide a manager to control the distcc turbo behavior.
type Controller interface {
	Run()
}

type controller struct {
	ops store.Ops

	event types.RoleChangeEvent

	conf *config.DistCCControllerConfig

	ctx    context.Context
	cancel context.CancelFunc

	watcher  Watcher
	strategy Strategy
	operator Operator
}

// NewController get a new Controller
func NewController(
	event types.RoleChangeEvent,
	conf *config.DistCCControllerConfig,
	ops store.Ops) Controller {

	return &controller{
		event:    event,
		conf:     conf,
		ops:      ops,
		watcher:  NewWatcher(conf, ops),
		strategy: NewStrategy(conf, ops),
		operator: NewOperator(conf, ops),
	}
}

// Run the controller
func (c *controller) Run() {
	blog.Infof("controller begin to watch RoleChangeEvent")

	for {
		select {
		case e := <-c.event:
			blog.Infof("receive new role change event: %s", e)
			switch e {
			case types.DistCCControllerMaster:
				c.start()
			case types.DistCCControllerSlave, types.DistCCControllerUnknown:
				c.stop()
			default:
				blog.Warnf("unknown role to manager, will not change manager state: %s", e)
			}
		}
	}
}

func (c *controller) start() {
	blog.Infof("controller start handler")
	if c.ctx != nil {
		blog.Errorf("controller has already start")
		return
	}

	c.ctx, c.cancel = context.WithCancel(context.Background())
	go func() {
		_ = c.watcher.Run(c.ctx)
	}()
	go func() {
		_ = c.strategy.Run(c.ctx)
	}()
	go func() {
		_ = c.operator.Run(c.ctx)
	}()
}

func (c *controller) stop() {
	blog.Infof("controller stop handler")
	if c.cancel == nil {
		blog.Errorf("controller has already stopped")
		return
	}

	c.cancel()
	c.ctx = nil
}
