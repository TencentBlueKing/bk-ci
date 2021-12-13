package controller

import (
	"context"
	"fmt"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/store"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/types"
)

// Strategy provide a manager to decide the settings changes to distcc turbo settings.
type Strategy interface {
	Run(pCtx context.Context) error
}

// NewStrategy get a new strategy
func NewStrategy(
	conf *config.DistCCControllerConfig,
	ops store.Ops) Strategy {

	return &strategy{
		ops:           ops,
		strategyLevel: StrategyLevel(conf.StrategyLevel),
		resourceLevel: resourceLevel{
			cpuUnit:       conf.CPUUnit,
			cpuMaxLevel:   conf.CPUMaxLevel,
			cpuRedundancy: conf.CPURedundancy,
		},
		suggestionWhitelist: conf.SuggestionWhiteList,
	}
}

type strategy struct {
	ops store.Ops

	strategyLevel StrategyLevel
	resourceLevel resourceLevel

	suggestionWhitelist []string
}

// Run the strategy
func (s *strategy) Run(pCtx context.Context) error {
	go s.start(pCtx)
	return nil
}

func (s *strategy) start(pCtx context.Context) {
	blog.Infof("strategy start")
	checkTick := time.NewTicker(types.CheckProjectTimeGap)
	defer checkTick.Stop()

	ctx, _ := context.WithCancel(pCtx)

	for {
		select {
		case <-ctx.Done():
			blog.Infof("strategy shut down")
			return
		case <-checkTick.C:
			s.checkProject()
		}
	}
}

func (s *strategy) projectInWhiteList(projectID string) bool {
	if len(s.suggestionWhitelist) == 0 {
		return true
	}

	for _, wl := range s.suggestionWhitelist {
		if wl == projectID {
			return true
		}
	}

	return false
}

func (s *strategy) checkProject() {
	// list recent finished task, as the entry point to calculate their suggest CPUs by strategy.
	opts := store.NewListOptions()
	opts.Limit(-1)
	opts.Equal(types.ListKeyStatus, "finish")
	opts.Gt(types.ListKeyCreateTime, time.Now().Unix()-types.LastFinishedTaskTimeRangeSecond)
	taskList, _, err := s.ops.ListTask(opts)
	if err != nil {
		blog.Errorf("strategy: list recent finished task failed: %v", err)
		return
	}

	// list out all projects from those tasks, and calculate their data.
	var wg sync.WaitGroup
	projects := make(map[string]bool, 1000)
	for _, task := range taskList {
		if !s.projectInWhiteList(task.ProjectID) {
			continue
		}

		if _, ok := projects[task.ProjectID]; ok {
			continue
		}

		projects[task.ProjectID] = true
		calculator := &projectCalculator{
			ops:           s.ops,
			projectID:     task.ProjectID,
			strategyLevel: s.strategyLevel,
			resourceLevel: s.resourceLevel,
		}

		wg.Add(1)
		go calculator.calculate(&wg)
	}
	wg.Wait()
}

type projectCalculator struct {
	ops store.Ops

	projectID     string
	strategyLevel StrategyLevel
	resourceLevel resourceLevel
}

func (pc *projectCalculator) calculate(wg *sync.WaitGroup) {
	defer wg.Done()
	blog.Infof("strategy: try to calculate project(%s)'s behavior", pc.projectID)

	project, err := pc.getProject()
	if err != nil {
		blog.Errorf("strategy: get project(%s) failed: %v", pc.projectID, err)
		return
	}

	maxJobs := pc.getTaskMaxJobs(project.RequestCPU)
	if maxJobs < 0 {
		blog.Infof("strategy: effective tasks of project(%s) is no enough for calculating", pc.projectID)
		return
	}

	oldCPU := int64(project.SuggestCPU)
	if oldCPU <= 0 {
		oldCPU = int64(project.RequestCPU)
	}
	suggestCPU := pc.resourceLevel.getSuggestCPU(maxJobs, oldCPU)
	if suggestCPU == oldCPU {
		blog.Infof("strategy: project(%s) current(%.0f) is suggested the same as current suggest(%d), skip",
			pc.projectID, project.RequestCPU, oldCPU)
		return
	}

	// if suggestion is decreased from last, then it has several limits:
	//   1. can not suggest a new cpu num from last suggestion which has not been accepted yet.
	//   2. can not suggest a new cpu num from last suggestion which has been accepted
	//   but in a period under observation.
	// if suggestion is increased from last, then just increase.
	if suggestCPU < oldCPU {
		// last suggestion has not been accepted
		if project.SuggestCPU != 0 && project.RequestCPU != project.SuggestCPU {
			blog.Infof("strategy: project(%s) current(%.0f) is suggested down: %d -> %d, "+
				"but last suggestion is not accepted yet, skip", pc.projectID, project.RequestCPU, oldCPU, suggestCPU)
			return
		}

		// still in the period under observation
		lastAccept := time.Unix(project.AcceptedTime, 0)
		if lastAccept.Add(types.LastSuggestionAcceptedTimeGap).After(time.Now()) {
			blog.Infof("strategy: project(%s) current(%.0f) is suggested down: %d -> %d, "+
				"but it is still under observation from last accept time %s, skip",
				pc.projectID, project.RequestCPU, oldCPU, suggestCPU, lastAccept.String())
			return
		}
	}

	// give the new suggestion
	if err = pc.ops.CreateOrUpdateProjectSetting(nil, map[string]interface{}{
		types.ListKeyProjectID:  pc.projectID,
		types.ListKeySuggestCPU: float64(suggestCPU),
	}); err != nil {
		blog.Errorf("strategy: project(%s) current(%.0f) try to save suggested: %d -> %d failed: %v",
			pc.projectID, project.RequestCPU, oldCPU, suggestCPU, err)
		return
	}

	blog.Infof("strategy: project(%s) current(%.0f) is suggested success: %d -> %d",
		pc.projectID, project.RequestCPU, oldCPU, suggestCPU)
}

func (pc *projectCalculator) getProject() (*store.CombinedProject, error) {
	opts := store.NewListOptions()
	opts.Equal(types.ListKeyProjectID, pc.projectID)
	opts.Limit(1)
	project, _, err := pc.ops.ListProject(opts)
	if err != nil {
		return nil, err
	}

	if len(project) != 1 {
		return nil, fmt.Errorf("project not found")
	}

	return project[0], err
}

func (pc *projectCalculator) getTaskMaxJobs(requestCPU float64) (maxJobs int64) {
	maxJobs = -1
	limit, lastSeconds := pc.strategyLevel.GetCondition()

	// first try to list tasks in a current duration, such as the last week or the last 2weeks.
	opts := store.NewListOptions()
	opts.Select([]string{types.ListKeyMaxJobs, types.ListKeyCPUTotal})
	opts.Equal(types.ListKeyProjectID, pc.projectID)
	opts.Equal(types.ListKeyStatus, "finish")
	opts.Equal(types.ListKeyObserved, true)
	opts.Gt(types.ListKeyCPUTotal, requestCPU-1)
	opts.Order([]string{"-" + types.ListKeyMaxJobs})
	opts.Limit(limit * types.MaxJobsSampleLimitMaxTimes)
	opts.Gt(types.ListKeyCreateTime, time.Now().Unix()-lastSeconds)
	taskList, length, err := pc.ops.ListTask(opts)
	if err != nil {
		blog.Errorf("strategy: list tasks of project(%s) by duration failed: %v", pc.projectID, err)
		return
	}

	// if the length of the tasks listed by duration is less than required, then just list tasks by required num.
	if length < int64(limit) {
		blog.Infof("strategy: list tasks of project(%s) by duration and it's less than required(%d < %d), "+
			"will list again by times", pc.projectID, length, limit)
		opts := store.NewListOptions()
		opts.Select([]string{types.ListKeyMaxJobs, types.ListKeyCPUTotal})
		opts.Equal(types.ListKeyProjectID, pc.projectID)
		opts.Equal(types.ListKeyStatus, "finish")
		opts.Equal(types.ListKeyObserved, true)
		opts.Gt(types.ListKeyCPUTotal, requestCPU-1)
		opts.Order([]string{"-" + types.ListKeyCreateTime})
		opts.Limit(limit)
		taskList, length, err = pc.ops.ListTask(opts)
		if err != nil {
			blog.Errorf("strategy: list tasks of project(%s) by times failed: %v", pc.projectID, err)
			return
		}
	}

	if length < int64(limit) {
		return
	}

	for _, task := range taskList {
		if task.MaxJobs > maxJobs {
			maxJobs = task.MaxJobs
		}
	}

	return maxJobs
}

// StrategyLevel describe the levels of strategy
type StrategyLevel int

// GetCondition get the limit and last-time for a specific strategy level
func (sl *StrategyLevel) GetCondition() (limit int, lastSeconds int64) {
	day := int64(time.Hour.Seconds()) * 24
	week := 7 * day

	switch *sl {
	case StrategyNever:
		return 0, 0
	case Strategy1W10T:
		return 10, week
	case Strategy2W20T:
		return 20, 2 * week
	case Strategy1M30T:
		return 30, 4 * week
	default:
		return 30, 4 * week
	}
}

// String return the string of strategy level
func (sl *StrategyLevel) String() string {
	switch *sl {
	case StrategyNever:
		return "StrategyNever"
	case Strategy1W10T:
		return "Strategy1WeekAnd10Times"
	case Strategy2W20T:
		return "Strategy2WeeksAnd20Times"
	case Strategy1M30T:
		return "Strategy1MonthAnd30Times"
	default:
		return "StrategyUnknown"
	}
}

const (
	StrategyNever StrategyLevel = iota
	Strategy1W10T
	Strategy2W20T
	Strategy1M30T
)

type resourceLevel struct {
	cpuUnit       int64
	cpuMaxLevel   int
	cpuRedundancy float64
}

func (rl *resourceLevel) getSuggestCPU(maxJobs, oldCPU int64) int64 {
	suggestCondition := float64(maxJobs) * rl.cpuRedundancy
	suggestLeast := oldCPU - rl.cpuUnit

	var suggest int64 = 0
	for i := 0; i < rl.cpuMaxLevel; i++ {
		suggest += rl.cpuUnit

		if float64(suggest) >= suggestCondition {
			// make sure that suggest cpu will not decreased from old jobs too much.
			// it will decrease unit by unit. But it will increase to the top immediately.
			if suggest >= suggestLeast {
				return suggest
			}
			return suggestLeast
		}
	}

	return suggest
}
