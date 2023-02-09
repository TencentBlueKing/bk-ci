package config

import (
	"context"
	"devopsRemoting/common/logs"
	commonTypes "devopsRemoting/common/types"
	"devopsRemoting/src/pkg/remoting/constant"
	"os"
	"path/filepath"
	"sync"
	"time"

	"github.com/fsnotify/fsnotify"
	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"
)

type DevfileConfigInterface interface {
	Watch(ctx context.Context)
	Observe(ctx context.Context) <-chan *commonTypes.Devfile
}

type DevfileConfigService struct {
	path      string
	pathReady <-chan struct{}

	cond    *sync.Cond
	devfile *commonTypes.Devfile

	pollTimer *time.Timer

	logs *logrus.Entry

	ready     chan struct{}
	readyOnce sync.Once

	debounceDuration time.Duration
}

func NewDevfileConfigService(path string, pathReady <-chan struct{}, logs *logrus.Entry) *DevfileConfigService {
	return &DevfileConfigService{
		path:             path,
		pathReady:        pathReady,
		cond:             sync.NewCond(&sync.Mutex{}),
		logs:             logs.WithField("path", path),
		ready:            make(chan struct{}),
		debounceDuration: 100 * time.Millisecond,
	}
}

// Observe 返回一个每次用户变更配置时都会发送消息的chan
func (service *DevfileConfigService) Observe(ctx context.Context) <-chan *commonTypes.Devfile {
	configs := make(chan *commonTypes.Devfile)
	go func() {
		defer close(configs)

		<-service.ready

		service.cond.L.Lock()
		defer service.cond.L.Unlock()
		for {
			configs <- service.devfile

			service.cond.Wait()
			if ctx.Err() != nil {
				return
			}
		}
	}()
	return configs
}

const WathchEmpty = "watchEmpty"

// Watch 持续的监控devfile
func (service *DevfileConfigService) Watch(ctx context.Context) {
	service.logs.Info("devops remoting config watcher: starting...")

	select {
	case <-service.pathReady:
		// 兼容拉代码后使用默认devfile的情况,contentready后重新赋值一次
		if filepath.Base(service.path) == WathchEmpty {
			service.path = filepath.Join(filepath.Dir(service.path), constant.DefaultDevFileName)
			service.logs = logs.WithField("path", service.path)
			service.logs.Debug("devops remoting config watcher: change devfile to default")
		}
	case <-ctx.Done():
		return
	}

	_, err := os.Stat(service.path)
	if os.IsNotExist(err) {
		service.poll(ctx)
	}
	service.watch(ctx)
}

func (service *DevfileConfigService) markReady() {
	service.readyOnce.Do(func() {
		close(service.ready)
	})
}

func (service *DevfileConfigService) poll(ctx context.Context) {
	service.markReady()

	timer := time.NewTicker(2 * time.Second)
	defer timer.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-timer.C:
		}

		if _, err := os.Stat(service.path); !os.IsNotExist(err) {
			service.watch(ctx)
			return
		}
	}
}

func (service *DevfileConfigService) watch(ctx context.Context) {
	watcher, err := fsnotify.NewWatcher()
	defer func() {
		if err != nil {
			service.logs.WithError(err).Error("devops remoting config watcher: failed to start")
			return
		}

		service.logs.Info("devops remoting config watcher: started")
	}()
	if err != nil {
		return
	}

	err = watcher.Add(service.path)
	if err != nil {
		watcher.Close()
		return
	}

	go func() {
		defer service.logs.Info("devops remoting config watcher: stopped")
		defer watcher.Close()

		polling := make(chan struct{}, 1)
		service.scheduleUpdateConfig(ctx, polling)
		for {
			select {
			case <-polling:
				return
			case <-ctx.Done():
				return
			case err := <-watcher.Errors:
				service.logs.WithError(err).Error("devops remoting config watcher: failed to watch")
			case <-watcher.Events:
				service.scheduleUpdateConfig(ctx, polling)
			}
		}
	}()
}

func (service *DevfileConfigService) scheduleUpdateConfig(ctx context.Context, polling chan<- struct{}) {
	service.cond.L.Lock()
	defer service.cond.L.Unlock()
	if service.pollTimer != nil {
		service.pollTimer.Stop()
	}
	service.pollTimer = time.AfterFunc(service.debounceDuration, func() {
		err := service.updateDevfile()
		if os.IsNotExist(err) {
			polling <- struct{}{}
			go service.poll(ctx)
		} else if err != nil {
			service.logs.WithError(err).Error("devops remoting config watcher: failed to parse")
		}
	})
}

func (service *DevfileConfigService) updateDevfile() error {
	service.cond.L.Lock()
	defer service.cond.L.Unlock()

	devfile, err := service.parse()
	if err == nil || os.IsNotExist(err) {
		service.devfile = devfile
		service.markReady()
		service.cond.Broadcast()

		service.logs.WithField("devfile", service.devfile).Debug("devops remoting devfile watcher: updated")
	}

	return err
}

func (service *DevfileConfigService) parse() (*commonTypes.Devfile, error) {
	data, err := os.ReadFile(service.path)
	if err != nil {
		return nil, err
	}
	var config *commonTypes.Devfile
	err = yaml.Unmarshal(data, &config)
	return config, err
}
