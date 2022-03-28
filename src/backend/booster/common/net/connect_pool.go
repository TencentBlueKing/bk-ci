/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package net

import (
	"context"
	"net"
	"strings"
	"sync"
	"time"
)

const (
	defaultHostCheckTimeGap = 500 * time.Millisecond
	defaultHostCheckTimeout = 200 * time.Millisecond
	defaultBreakerFailTimes = 3
	defaultBreakerTryTimes  = 5
)

type connectType string

const (
	connectRobin connectType = "robin"
	connectFirst connectType = "first"
)

// ConnectPool describe a connection pools,
// provides health check and circuit breaker to maintains those connection via different address.
type ConnectPool struct {
	ConnectType      connectType
	HostCheckTimeGap time.Duration
	HostCheckTimeout time.Duration
	BreakerFailTimes int
	BreakerTryTimes  int

	address   []string
	available []string
	avaLock   sync.RWMutex
	status    map[string]*circuitBreaker
	lastPick  int
	lastLock  sync.Mutex
	ctx       context.Context
	cancel    context.CancelFunc
}

// NewConnectPool get a new ConnectPool with given address.
// Those address should be the same service and equal to each other.
func NewConnectPool(address []string) *ConnectPool {
	return &ConnectPool{
		ConnectType:      connectRobin,
		HostCheckTimeGap: defaultHostCheckTimeGap,
		HostCheckTimeout: defaultHostCheckTimeout,
		BreakerFailTimes: defaultBreakerFailTimes,
		BreakerTryTimes:  defaultBreakerTryTimes,

		address:  address,
		lastPick: -1,
	}
}

// Start the connect pool.
func (cp *ConnectPool) Start() {
	cp.ctx, cp.cancel = context.WithCancel(context.Background())
	cp.init()
	go cp.start()
}

// Stop the connect pool.
func (cp *ConnectPool) Stop() {
	if cp.cancel != nil {
		cp.cancel()
	}
}

// GetAddress return a available address(for now) in address list.
func (cp *ConnectPool) GetAddress() string {
	cp.avaLock.RLock()
	defer cp.avaLock.RUnlock()

	total := len(cp.available)
	if total == 0 {
		return ""
	}

	switch cp.ConnectType {
	case connectRobin:
		cp.lastLock.Lock()
		cp.lastPick = (cp.lastPick + 1) % total
		cp.lastLock.Unlock()
		return cp.available[cp.lastPick]
	case connectFirst:
		return cp.available[0]
	default:
		return cp.available[0]
	}
}

func (cp *ConnectPool) init() {
	cp.status = make(map[string]*circuitBreaker)
	for _, addr := range cp.address {
		cp.status[addr] = &circuitBreaker{
			address:          addr,
			host:             strings.TrimPrefix(strings.TrimPrefix(addr, "http://"), "https://"),
			status:           cbOpen,
			hostCheckTimeout: cp.HostCheckTimeout,
			breakerTryTimes:  cp.BreakerTryTimes,
			breakerFailTimes: cp.BreakerFailTimes,
		}
	}

	cp.available = cp.address
}

func (cp *ConnectPool) start() {
	tick := time.NewTicker(cp.HostCheckTimeGap)

	for {
		select {
		case <-cp.ctx.Done():
			return
		case <-tick.C:
			cp.check()
		}
	}
}

func (cp *ConnectPool) check() {
	var wg sync.WaitGroup
	for _, cb := range cp.status {
		wg.Add(1)
		go cb.check(&wg)
	}
	wg.Wait()

	// if there is only one address, do not do the fresh.
	if len(cp.address) == 1 {
		return
	}

	newAvailable := make([]string, 0)
	for _, addr := range cp.address {
		if cb, ok := cp.status[addr]; ok && cb.isOpen() {
			newAvailable = append(newAvailable, addr)
		}
	}

	cp.avaLock.Lock()
	cp.available = newAvailable
	cp.avaLock.Unlock()
}

// A circuit breaker work for host checking, which contains just two status: open/close.
// This circuit breaker does not guarantee save for goroutine, it should be called in order.
type circuitBreaker struct {
	address          string
	host             string
	successTime      int
	failureTime      int
	status           cbStatus
	breakerTryTimes  int
	breakerFailTimes int
	hostCheckTimeout time.Duration
}

func (cb *circuitBreaker) isOpen() bool {
	return cb.status == cbOpen
}

func (cb *circuitBreaker) check(wg *sync.WaitGroup) {
	defer wg.Done()

	_, err := net.DialTimeout("tcp", cb.host, cb.hostCheckTimeout)
	if err != nil {
		cb.doFailure()
		return
	}

	cb.doSuccess()
}

func (cb *circuitBreaker) doSuccess() {
	cb.failureTime = 0
	cb.successTime += 1
	if cb.status == cbClose && cb.successTime >= cb.breakerTryTimes {
		cb.status = cbOpen
	}
	cb.successTime %= cb.breakerTryTimes
}

func (cb *circuitBreaker) doFailure() {
	cb.successTime = 0
	cb.failureTime += 1
	if cb.status == cbOpen && cb.failureTime >= cb.breakerFailTimes {
		cb.status = cbClose
	}
	cb.failureTime %= cb.breakerFailTimes
}

type cbStatus string

const (
	cbOpen  cbStatus = "open"
	cbClose cbStatus = "close"
)
