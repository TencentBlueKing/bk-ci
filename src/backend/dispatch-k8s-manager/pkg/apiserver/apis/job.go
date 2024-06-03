package apis

import (
	"disaptch-k8s-manager/pkg/apiserver/service"
	"github.com/gin-gonic/gin"
	"github.com/pkg/errors"
	"net/http"
	"strconv"
)

func initJobsApis(r *gin.RouterGroup) {
	jobs := r.Group("/jobs")
	{
		jobs.POST("", createJob)
		jobs.GET("/:jobName/status", getJobStatus)
		jobs.GET("/:jobName/log", getJobLogs)
		jobs.DELETE("/:jobName", deleteJob)
		jobs.POST("/buildAndPushImage", buildAndPushImage)
	}
}

// @Tags  job
// @Summary  创建JOB
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  builder  body  service.Job  true  "JOB信息"
// @Success 200 {object} types.Result{data=service.TaskId} "任务ID"
// @Router /jobs [post]
func createJob(c *gin.Context) {
	job := &service.Job{}

	if err := c.BindJSON(job); err != nil {
		fail(c, http.StatusBadRequest, err)
		return
	}

	taskId, err := service.CreateJob(job)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, service.TaskId{TaskId: taskId})
}

// @Tags  job
// @Summary  获取JOB状态
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  jobName  path  string  true  "JOB名称"
// @Success 200 {object} types.Result{data=service.JobStatus} "JOB状态"
// @Router /jobs/{jobName}/status [get]
func getJobStatus(c *gin.Context) {
	jobName := c.Param("jobName")

	if !checkJobName(c, jobName) {
		return
	}

	status, err := service.GetJobStatus(jobName)
	if err != nil {
		okFail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, status)
}

// @Tags  job
// @Summary  获取JOB日志
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  jobName  path  string  true  "JOB名称"
// @Param sinceTime query string false "开始时间"
// @Success 200 {object} types.Result{data=string} "JOB日志"
// @Router /jobs/{jobName}/log [get]
func getJobLogs(c *gin.Context) {
	jobName := c.Param("jobName")

	if !checkJobName(c, jobName) {
		return
	}

	sinceTimeStr := c.Query("sinceTime")
	var sinceTime *int64 = nil
	if sinceTimeStr != "" {
		sinceTimeData, err := strconv.ParseInt(sinceTimeStr, 10, 64)
		if err != nil {
			fail(c, http.StatusBadRequest, errors.Wrap(err, "param sinceTime format error"))
			return
		}
		sinceTime = &sinceTimeData
	}

	log, err := service.GetJobLogs(jobName, sinceTime)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, log)
}

// @Tags  job
// @Summary  删除JOB
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  jobName  path  string  true  "JOB名称"
// @Success 200 {object} types.Result{data=service.TaskId} "任务ID"
// @Router /jobs/{jobName} [delete]
func deleteJob(c *gin.Context) {
	jobName := c.Param("jobName")

	if !checkJobName(c, jobName) {
		return
	}

	taskId, err := service.DeleteJob(jobName)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, service.TaskId{TaskId: taskId})
}

func checkJobName(c *gin.Context, jobName string) bool {
	if jobName == "" {
		fail(c, http.StatusBadRequest, errors.New("job名称不能为空"))
		return false
	}

	return true
}

// @Tags  job
// @Summary  构建并推送镜像
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  builder  body  service.BuildAndPushImageInfo  true  "构建并推送镜像信息"
// @Success 200 {object} types.Result{data=service.TaskId} "任务ID"
// @Router /jobs/buildAndPushImage [post]
func buildAndPushImage(c *gin.Context) {
	info := &service.BuildAndPushImageInfo{}

	if err := c.BindJSON(info); err != nil {
		fail(c, http.StatusBadRequest, err)
		return
	}

	taskId, err := service.BuildAndPushImage(info)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, service.TaskId{TaskId: taskId})
}
