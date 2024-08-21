package apis

import (
	"disaptch-k8s-manager/pkg/apiserver/service"
	"net/http"

	"github.com/gin-gonic/gin"
)

const (
	dockerPrefix = "/docker"
)

func initDockerApis(r *gin.RouterGroup) {
	docker := r.Group(dockerPrefix)
	{
		docker.POST("/inspect", dockerInspect)
	}
}

// @Tags  docker
// @Summary  docker inspect命令(同时会pull)
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  info  body  service.DockerInspectInfo  true  "构建机信息"
// @Success 200 {object} types.Result{data=service.TaskId} "任务ID"
// @Router /docker/inspect [post]
func dockerInspect(c *gin.Context) {
	info := &service.DockerInspectInfo{}

	if err := c.BindJSON(info); err != nil {
		fail(c, http.StatusBadRequest, err)
		return
	}

	taskId, err := service.DockerInspect(info)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, service.TaskId{TaskId: taskId})
}
