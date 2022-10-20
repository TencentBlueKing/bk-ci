package apis

import (
	"disaptch-k8s-manager/pkg/apiserver/service"
	"github.com/gin-gonic/gin"
	"github.com/pkg/errors"
	"net/http"
)

func initTasksApis(r *gin.RouterGroup) {
	jobs := r.Group("/tasks")
	{
		jobs.GET("/:taskId/status", getTaskStatus)
	}
}

// @Tags  task
// @Summary  获取任务状态
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  taskId  path  string  true  "任务ID"
// @Success 200 {object} types.Result{data=service.TaskStatus} "任务状态"
// @Router /tasks/{taskId}/status [get]
func getTaskStatus(c *gin.Context) {
	taskId := c.Param("taskId")

	if !checkTaskId(c, taskId) {
		return
	}

	status, err := service.GetTaskStatus(taskId)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, status)
}

func checkTaskId(c *gin.Context, jobName string) bool {
	if jobName == "" {
		fail(c, http.StatusBadRequest, errors.New("task id不能为空"))
		return false
	}

	return true
}
