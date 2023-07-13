package apis

import (
	"disaptch-k8s-manager/pkg/apiserver/service"
	"disaptch-k8s-manager/pkg/logs"
	"disaptch-k8s-manager/pkg/types"
	"fmt"
	"net/http"

	"github.com/gin-gonic/gin"
)

const (
	buildlessPrefix = "/buildless"
)

func initBuildLessApis(r *gin.RouterGroup) {
	buildless := r.Group(buildlessPrefix)
	{
		buildless.POST("/build/start", startBuildless)
		buildless.POST("/build/end", stopBuildless)
		buildless.GET("/build/claim", claimBuildless)
	}
}

// @Tags  buildless
// @Summary  启动无编译构建
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Success 200 {object} types.Result{data=string} "接口状态"
// @Router /buildless/build/start [post]
func startBuildless(c *gin.Context) {
	var buildlessStartInfo = &types.BuildLessStartInfo{}

	if err := c.BindJSON(buildlessStartInfo); err != nil {
		fail(c, http.StatusBadRequest, err)
		return
	}

	err := service.Executor(buildlessStartInfo)
	if err != nil {
		okFail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, "")
}

// @Tags  buildless
// @Summary  停止无编译构建
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Success 200 {object} types.Result{data=boolean} "接口状态"
// @Router /buildless/build/stop [delete]
func stopBuildless(c *gin.Context) {
	var buildLessEndInfo = &types.BuildLessEndInfo{}

	if err := c.BindJSON(buildLessEndInfo); err != nil {
		fail(c, http.StatusBadRequest, err)
	}

	err := service.StopBuildless(*buildLessEndInfo)
	if err != nil {
		okFail(c, http.StatusInternalServerError, err)
	}

	ok(c, "")
}

// @Tags  buildless
// @Summary  获取无编译构建任务
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  podId  path  string  true  "podId"
// @Success 200 {object} types.Result{data=service.BuildLessTask} "无编译构建任务"
// @Router /buildless/build/claim [get]
func claimBuildless(c *gin.Context) {
	podId := c.Query("podId")
	logs.Info(fmt.Sprintf("podId: %s start claim buildLessTask", podId))

	buildLessTask, err := service.ClaimBuildLessTask(podId)
	if err != nil {
		okFail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, buildLessTask)
}
