package controller

import (
	"context"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/store"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/types"
)

// Operator provide a manager to modify the distcc turbo settings.
type Operator interface {
	Run(pCtx context.Context) error
}

// NewOperator get a new Operator
func NewOperator(
	conf *config.DistCCControllerConfig,
	ops store.Ops) Operator {

	return &operator{
		ops:                ops,
		operationWhitelist: conf.OperationWhiteList,
	}
}

type operator struct {
	ops store.Ops

	operationWhitelist []string
}

// Run the operator
func (o *operator) Run(pCtx context.Context) error {
	go o.start(pCtx)
	return nil
}

func (o *operator) start(pCtx context.Context) {
	blog.Infof("operator start")
	checkTick := time.NewTicker(types.CheckProjectTimeGap)
	defer checkTick.Stop()

	ctx, _ := context.WithCancel(pCtx)

	for {
		select {
		case <-ctx.Done():
			blog.Infof("operator shut down")
			return
		case <-checkTick.C:
			o.checkProject()
		}
	}
}

func (o *operator) projectInWhiteList(projectID string) bool {
	if len(o.operationWhitelist) == 0 {
		return true
	}

	for _, wl := range o.operationWhitelist {
		if wl == projectID {
			return true
		}
	}

	return false
}

func (o *operator) checkProject() {
	opts := store.NewListOptions()
	opts.Limit(-1)
	opts.Equal(types.ListKeyStatus, "finish")
	opts.Gt(types.ListKeyCreateTime, time.Now().Unix()-types.LastFinishedTaskTimeRangeSecond)
	taskList, _, err := o.ops.ListTask(opts)
	if err != nil {
		blog.Errorf("strategy: list recent finished task failed: %v", err)
		return
	}

	var wg sync.WaitGroup
	projects := make(map[string]bool, 1000)
	for _, task := range taskList {
		if !o.projectInWhiteList(task.ProjectID) {
			continue
		}

		if _, ok := projects[task.ProjectID]; ok {
			continue
		}

		projects[task.ProjectID] = true
		adjuster := &projectAdjuster{
			ops:       o.ops,
			projectID: task.ProjectID,
		}

		wg.Add(1)
		go adjuster.adjust(&wg)
	}
	wg.Wait()
}

type projectAdjuster struct {
	ops store.Ops

	projectID string
}

func (pa *projectAdjuster) adjust(wg *sync.WaitGroup) {
	defer wg.Done()
	blog.Infof("operator: try to adjust project(%s) settings", pa.projectID)

	opts := store.NewListOptions()
	opts.Equal(types.ListKeyProjectID, pa.projectID)
	opts.Limit(1)
	projectList, _, err := pa.ops.ListProject(opts)
	if err != nil {
		blog.Errorf("operator: get project(%s) failed: %v", pa.projectID, err)
		return
	}

	if len(projectList) != 1 {
		blog.Errorf("operator: get project(%s) failed: project no found", pa.projectID)
		return
	}

	project := projectList[0]
	if project.SuggestCPU <= 0 || project.SuggestCPU == project.RequestCPU {
		return
	}

	oldRequestCPU := project.RequestCPU
	project.RequestCPU = project.SuggestCPU
	if err = pa.ops.CreateOrUpdateProjectSetting(nil, map[string]interface{}{
		types.ListKeyProjectID:  pa.projectID,
		types.ListKeyRequestCPU: project.SuggestCPU,
		types.ListKeyAcTime:     time.Now().Unix(),
	}); err != nil {
		blog.Errorf("operator: update project(%s) requestCPU(%.f) to suggestCPU(%.f) failed: %v",
			pa.projectID, project.RequestCPU, project.SuggestCPU, err)
		return
	}

	blog.Infof("operator: success to update project(%s) from requestCPU(%.f) to suggestCPU(%.f)",
		pa.projectID, oldRequestCPU, project.SuggestCPU)
}
