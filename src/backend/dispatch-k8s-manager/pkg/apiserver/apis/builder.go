package apis

import (
	"disaptch-k8s-manager/pkg/apiserver/authz"
	"disaptch-k8s-manager/pkg/apiserver/service"
	"disaptch-k8s-manager/pkg/kubeclient"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
	"github.com/pkg/errors"
)

var loadDebugPod = kubeclient.GetPod
var nowFunc = time.Now

const (
	builderPrefix   = "/builders"
	builderDebugUrl = "/debug/:podName/:containerName"
)

func initBuilderApis(r *gin.RouterGroup) {
	builders := r.Group(builderPrefix)
	{
		builders.GET("/:builderName/status", getBuilderStatus)
		builders.POST("", createBuilder)
		builders.PUT("/:builderName/stop", stopBuilder)
		builders.PUT("/:builderName/start", startBuilder)
		builders.DELETE("/:builderName", deleteBuilder)
		builders.GET("/:builderName/terminal", debugBuilderUrl)
		builders.GET(builderDebugUrl, debugBuilder)
	}
}

// @Tags  builder
// @Summary  获取构建机状态
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  builderName  path  string  true  "构建机名称"
// @Success 200 {object} types.Result{data=service.BuilderStatus} "构建机状态"
// @Router /builders/{builderName}/status [get]
func getBuilderStatus(c *gin.Context) {
	builderName := c.Param("builderName")

	if !checkBuilderName(c, builderName) {
		return
	}

	status, err := service.GetBuilderStatus(builderName)
	if err != nil {
		okFail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, status)
}

// @Tags  builder
// @Summary  创建构建机
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  builder  body  service.Builder  true  "构建机信息"
// @Success 200 {object} types.Result{data=service.TaskId} "任务ID"
// @Router /builders [post]
func createBuilder(c *gin.Context) {
	builder := &service.Builder{}

	if err := c.BindJSON(builder); err != nil {
		fail(c, http.StatusBadRequest, err)
		return
	}

	owner := builderOwnerFromRequest(c, builder.Env)
	taskId, err := service.CreateBuilder(builder, owner)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, service.TaskId{TaskId: taskId})
}

// @Tags  builder
// @Summary  启动构建机
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  builderName  path  string  true  "构建机名称"
// @Param  builder  body  service.BuilderStart  true  "构建机启动信息"
// @Success 200 {object} types.Result{data=service.TaskId} "任务ID"
// @Router /builders/{builderName}/start [put]
func startBuilder(c *gin.Context) {
	builderName := c.Param("builderName")

	if !checkBuilderName(c, builderName) {
		return
	}

	start := &service.BuilderStart{}

	if err := c.BindJSON(start); err != nil {
		fail(c, http.StatusBadRequest, err)
		return
	}

	owner := builderOwnerFromRequest(c, start.Env)
	taskId, err := service.StartBuilder(builderName, start, owner)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, service.TaskId{TaskId: taskId})
}

// @Tags  builder
// @Summary  停止构建机
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  builderName  path  string  true  "构建机名称"
// @Success 200 {object} types.Result{data=service.TaskId} "任务ID"
// @Router /builders/{builderName}/stop [put]
func stopBuilder(c *gin.Context) {
	builderName := c.Param("builderName")

	if !checkBuilderName(c, builderName) {
		return
	}

	taskId, err := service.StopBuilder(builderName)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, service.TaskId{TaskId: taskId})
}

// @Tags  builder
// @Summary  删除构建机
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  builderName  path  string  true  "构建机名称"
// @Success 200 {object} types.Result{data=service.TaskId} "任务ID"
// @Router /builders/{builderName} [delete]
func deleteBuilder(c *gin.Context) {
	builderName := c.Param("builderName")

	if !checkBuilderName(c, builderName) {
		return
	}

	taskId, err := service.DeleteBuilder(builderName)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, service.TaskId{TaskId: taskId})
}

// @Tags  builder
// @Summary  获取远程登录链接
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  builderName  path  string  true  "构建机名称"
// @Success 200 {object} types.Result{data=string} "远程登录链接"
// @Router /builders/{builderName}/terminal [get]
func debugBuilderUrl(c *gin.Context) {
	builderName := c.Param("builderName")

	if !checkBuilderName(c, builderName) {
		return
	}

	caller, okCaller := requireTenantCaller(c)
	if !okCaller {
		return
	}

	debugUrl, err := service.DebugBuilderUrl("/api/builders/debug", builderName, caller)
	if err != nil {
		status := http.StatusInternalServerError
		if errors.Is(err, authz.ErrForbidden) || errors.Is(err, authz.ErrObjectUnowned) ||
			errors.Is(err, authz.ErrMissingIdentity) || errors.Is(err, authz.ErrInvalidTicket) {
			status = http.StatusForbidden
		}
		fail(c, status, errors.Wrap(err, "登录调试错误"))
		return
	}

	ok(c, debugUrl)
}

var wsUpGrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
	HandshakeTimeout: 1 * time.Hour,
}

func debugBuilder(c *gin.Context) {
	podName := c.Param("podName")
	containerName := c.Param("containerName")

	if podName == "" || containerName == "" {
		fail(c, http.StatusBadRequest, errors.New("podName或containerName名称不能为空"))
		return
	}

	if err := authorizeDebugBuilder(c, podName, containerName); err != nil {
		fail(c, http.StatusForbidden, err)
		return
	}

	//升级原get请求为webSocket协议
	ws, err := wsUpGrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		fail(c, http.StatusInternalServerError, errors.Wrap(err, "登录调试建立与manager的websocket链接错误"))
		return
	}
	defer ws.Close()

	service.DebugBuilder(ws, podName, containerName)
}

func authorizeDebugBuilder(c *gin.Context, podName, containerName string) error {
	caller, err := authz.RequireTenantCaller(c)
	if err != nil {
		return err
	}
	ticket := c.Query(authz.QueryTicket)
	pod, err := loadDebugPod(podName)
	if err != nil || pod == nil {
		return authz.ErrObjectUnowned
	}
	return authz.AuthorizeDebugSession(caller, ticket, podName, containerName, pod, nowFunc())
}

func builderOwnerFromRequest(c *gin.Context, env map[string]string) authz.Owner {
	return authz.CallerFromRequest(c).Owner().Fill(authz.OwnerFromEnvMap(env))
}

func checkBuilderName(c *gin.Context, builderName string) bool {
	if builderName == "" {
		fail(c, http.StatusBadRequest, errors.New("builder名称不能为空"))
		return false
	}

	return true
}
