package v1http

import (
	"io/ioutil"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	commonTypes "github.com/Tencent/bk-ci/src/booster/common/types"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/api"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/store"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/types"

	"github.com/emicklei/go-restful"
)

var (
	defaultOps  store.Ops
	defaultConf *config.DistCCControllerConfig
)

// ResetProjectResourceAndSuggestion provide the methods to reset the resource and suggestion settings in project db
func ResetProjectResourceAndSuggestion(req *restful.Request, resp *restful.Response) {
	param, err := getProjectResetParam(req)
	if err != nil {
		blog.Errorf("reset project resource and suggestion, get param failed, urls(%s): %v",
			req.Request.URL.String(), err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	for _, projectID := range param.ProjectIDList {
		err = defaultOps.CreateOrUpdateProjectSetting(nil, map[string]interface{}{
			types.ListKeyProjectID:  projectID,
			types.ListKeyRequestCPU: float64(param.RequestCPU),
			types.ListKeySuggestCPU: float64(param.SuggestCPU),
		})
		if err != nil {
			blog.Errorf("reset project resource and suggestion, update failed, urls(%s): %v",
				req.Request.URL.String(), err)
			api.ReturnRest(&api.RestResponse{
				Resp: resp, ErrCode: commonTypes.ServerErrUpdateProjectFailed, Message: err.Error(),
			})
		}
	}

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func getProjectResetParam(req *restful.Request) (*ProjectResetParam, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("get project reset param, get request body failed: %v", err)
		return nil, err
	}

	var param ProjectResetParam
	if err = codec.DecJSON(body, &param); err != nil {
		blog.Errorf("get project reset param, decode body failed: %v, body: %s", err, string(body))
		return nil, err
	}

	blog.Infof("get project reset param: %s", string(body))
	if param.RequestCPU <= 0 {
		param.RequestCPU = defaultConf.CPUUnit * int64(defaultConf.CPUMaxLevel)
	}
	blog.Infof("going to reset requestCPU(%d), suggestCPU(%d) in projects %v",
		param.RequestCPU, param.SuggestCPU, param.ProjectIDList)
	return &param, nil
}

// ProjectResetParam describe the param for resetting project resource settings
type ProjectResetParam struct {
	ProjectIDList []string `json:"project_id_list"`
	SuggestCPU    int64    `json:"suggest_cpu"`
	RequestCPU    int64    `json:"request_cpu"`
}

// InitAPI init api handlers to router
func InitAPI() (err error) {
	defaultOps = api.GetAPIResource().Ops
	defaultConf = api.GetAPIResource().Conf

	api.RegisterV1Action(api.Action{
		Verb: "PUT", Path: "/distcc/project/reset", Params: nil, Handler: ResetProjectResourceAndSuggestion,
	})
	return nil
}

func init() {
	api.RegisterInitFunc(InitAPI)
}
