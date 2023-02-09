package ports

import (
	"devopsRemoting/src/pkg/remoting/types"
	"errors"
	"sync"
)

// 用来订阅ports状态更新事件
type Subscription struct {
	updates chan []*types.PortsStatus
	Close   func() error
}

const maxSubscriptions = 10

var (
	// ErrClosed portmanager关闭时
	ErrClosed = errors.New("closed")
	// ErrTooManySubscriptions 超过订阅上限了
	ErrTooManySubscriptions = errors.New("too many subscriptions")
)

func (pm *PortsManager) Subscribe() (*Subscription, error) {
	pm.rwLock.Lock()
	defer pm.rwLock.Unlock()

	if pm.closed {
		return nil, ErrClosed
	}

	if len(pm.subscriptions) > maxSubscriptions {
		return nil, ErrTooManySubscriptions
	}

	sub := &Subscription{updates: make(chan []*types.PortsStatus, 5)}
	var once sync.Once
	sub.Close = func() error {
		pm.rwLock.Lock()
		defer pm.rwLock.Unlock()

		once.Do(func() {
			close(sub.updates)
		})
		delete(pm.subscriptions, sub)

		return nil
	}
	pm.subscriptions[sub] = struct{}{}

	sub.updates <- pm.getStatus()
	return sub, nil
}

func (s *Subscription) Updates() <-chan []*types.PortsStatus {
	return s.updates
}
