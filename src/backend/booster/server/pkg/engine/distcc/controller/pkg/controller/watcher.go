package controller

import (
	"context"
	"fmt"
	"sync"
	"sync/atomic"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpclient"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/store"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/types"
)

// Watcher provide a manager to watch the distcc turbo task status.
type Watcher interface {
	Run(pCtx context.Context) error
}

// NewWatcher get a new watcher
func NewWatcher(conf *config.DistCCControllerConfig, ops store.Ops) Watcher {
	return &watcher{conf: conf, ops: ops}
}

type watcher struct {
	conf *config.DistCCControllerConfig
	ops  store.Ops

	watchPool     map[string]*taskWatch
	watchPoolLock sync.RWMutex

	ctx context.Context
}

// Run the watcher
func (w *watcher) Run(pCtx context.Context) error {
	go w.start(pCtx)
	return nil
}

func (w *watcher) start(pCtx context.Context) {
	blog.Infof("watcher start")

	// init the pool
	w.watchPoolLock.Lock()
	w.watchPool = make(map[string]*taskWatch, 100)
	w.watchPoolLock.Unlock()

	w.ctx, _ = context.WithCancel(pCtx)
	inspectTick := time.NewTicker(types.InspectRunningTaskTimeGap)
	defer inspectTick.Stop()

	for {
		select {
		case <-w.ctx.Done():
			blog.Infof("watcher shut down")
			return
		case <-inspectTick.C:
			w.checkTask()
		}
	}
}

func (w *watcher) checkTask() {
	opts := store.NewListOptions()
	opts.Limit(1000)
	opts.Equal(types.ListKeyStatus, "running")
	taskList, _, err := w.ops.ListTask(opts)
	if err != nil {
		blog.Errorf("watcher: list running task failed: %v", err)
		return
	}

	w.watchPoolLock.Lock()
	timestamp := time.Now().Unix()
	for _, task := range taskList {
		if w, ok := w.watchPool[task.TaskID]; ok {
			w.timestamp = timestamp
			continue
		}

		// if the task is already running for a long time, just ignore it,
		// we focus on the task which just begin to run.
		if timestamp-task.StatusChangeTime > types.IgnoreTaskAfterRunningTimeSecond {
			continue
		}

		// if the task's gcc_version is in blacklist, generally some specific version
		// with buggy distCC provided by others
		// we will not watch these tasks.
		if inList(task.GccVersion, w.conf.GccVersionBlackList) {
			continue
		}

		blog.Infof("watcher: task(%s) gcc_version(%s) new for watcher, will be watched during running",
			task.TaskID, task.GccVersion)
		s := &taskWatch{ops: w.ops, task: task, timestamp: timestamp, startTime: time.Now()}
		go s.watch(w.ctx)
		w.watchPool[task.TaskID] = s
	}

	// if task is no longer running, then quit the taskWatch
	for taskID, s := range w.watchPool {
		if s.timestamp != timestamp {
			blog.Infof("watcher: task(%s) no longer running, taskWatch quit", taskID)
			s.quit()
			delete(w.watchPool, taskID)
		}
	}

	w.watchPoolLock.Unlock()
}

type taskWatch struct {
	ops store.Ops

	timestamp int64
	task      *store.RecordTask

	ctx    context.Context
	cancel context.CancelFunc
	pCtx   context.Context

	client  *httpclient.HTTPClient
	targets []distCCTarget
	maxJobs int64

	startTime time.Time
}

func (s *taskWatch) watch(pCtx context.Context) {
	s.pCtx = pCtx
	s.ctx, s.cancel = context.WithCancel(pCtx)
	watchTicker := time.NewTicker(types.InspectDistCCStatTimeGap)
	defer watchTicker.Stop()

	s.client = httpclient.NewHTTPClient()
	s.client.SetTimeOut(2 * time.Second)
	s.maxJobs = -1
	s.analyseTargets()

	for {
		select {
		case <-s.ctx.Done():
			return
		case <-watchTicker.C:
			s.request()
		}
	}
}

func (s *taskWatch) analyseTargets() {
	if err := codec.DecJSON([]byte(s.task.Compilers), &s.targets); err != nil {
		blog.Errorf("watcher: task(%s) decode compilers failed: %v", s.task.TaskID, err)
	}
}

func (s *taskWatch) quit() {
	if s.cancel != nil {
		s.cancel()
	}

	if s.maxJobs != -1 {
		duration := time.Duration(time.Now().Unix()-s.startTime.Unix()) * time.Second
		blog.Infof("watcher: task(%s) watching for %s, max jobs is %d",
			s.task.TaskID, duration.String(), s.maxJobs)
		go s.writeBack()
	}
}

// writeBack writes the max jobs back to task, after the task is released.
func (s *taskWatch) writeBack() {
	opts := store.NewListOptions()
	opts.Limit(1)
	opts.Equal(types.ListKeyTaskID, s.task.TaskID)
	opts.Equal(types.ListKeyReleased, true)

	// inherit the watcher's context, not the taskWatch's, taskWatch will be released right after the task done.
	// after that writeBack should keep alive waiting the task "released"=true and write the max jobs back.
	ctx, _ := context.WithCancel(s.pCtx)

	for ; ; time.Sleep(5 * time.Second) {
		select {
		case <-ctx.Done():
			return
		default:
			_, length, err := s.ops.ListTask(opts)
			if err != nil {
				blog.Errorf("watcher: writeBack try to get task(%s) failed: %v", s.task.TaskID, err)
				return
			}

			if length <= 0 {
				continue
			}

			if err = s.ops.UpdateTask(s.task.TaskID, map[string]interface{}{
				"max_jobs": s.maxJobs,
				"observed": true,
			}); err != nil {
				blog.Errorf("watcher: writeBack try to write task(%s) with max jobs(%d) failed: %v",
					s.task.TaskID, s.maxJobs, err)
				return
			}

			blog.Infof("watcher: success to write task(%s) with max jobs(%d)", s.task.TaskID, s.maxJobs)
			return
		}
	}
}

func (s *taskWatch) request() {
	if len(s.targets) == 0 {
		return
	}

	var wg sync.WaitGroup
	var total int64 = 0
	for _, target := range s.targets {
		wg.Add(1)
		go func(t distCCTarget) {
			defer wg.Done()
			uri := fmt.Sprintf("http://%s:%d", t.IP, t.StatsPort)
			data, err := s.client.GET(uri, nil, nil)
			if err != nil {
				return
			}

			stats := new(store.StatsInfo)
			stats.ParseAll(data)
			load := stats.GetCurrentLoad()
			_ = atomic.AddInt64(&total, load.RunningJobs)
		}(target)
	}

	wg.Wait()
	if s.maxJobs < total {
		s.maxJobs = total
	}
}

type distCCTarget struct {
	CPU       float64 `json:"CPU"`
	IP        string  `json:"IP"`
	Mem       float64 `json:"Mem"`
	Message   string  `json:"Message"`
	Port      int     `json:"Port"`
	StatsPort int     `json:"StatsPort"`
}

func inList(base string, list []string) bool {
	for _, l := range list {
		if base == l {
			return true
		}
	}
	return false
}
