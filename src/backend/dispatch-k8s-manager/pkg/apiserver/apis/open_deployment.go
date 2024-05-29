package apis

import (
	"disaptch-k8s-manager/pkg/kubeclient"
	"disaptch-k8s-manager/pkg/logs"
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/pkg/errors"
	appsv1 "k8s.io/api/apps/v1"
	"net/http"
)

const (
	deploymentPrefix = "/deployments"
)

func initDeploymentApis(r *gin.RouterGroup) {
	deployment := r.Group(deploymentPrefix)
	{
		deployment.GET("/:deploymentName", getDeployment)
		deployment.POST("", createDeployment)
		deployment.DELETE("/:deploymentName", deleteDeployment)
	}
}

// @Tags  deployment
// @Summary  获取deployment状态
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  deploymentName  path  string  true  "deployment名称"
// @Success 200 {object} types.Result{data=appsv1.Deployment} "deployment详情"
// @Router /deployment/{deploymentName} [get]
func getDeployment(c *gin.Context) {
	deploymentName := c.Param("deploymentName")

	if !checkDeploymentName(c, deploymentName) {
		return
	}

	deployment, err := kubeclient.GetDeployment(deploymentName)
	if err != nil {
		okFail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, deployment)
}

// @Tags  deployment
// @Summary  创建deployment负载资源
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  deployment  body  appsv1.Deployment  true  "deployment负载信息"
// @Success 200 {object} ""
// @Router /deployment [post]
func createDeployment(c *gin.Context) {
	deployment := &appsv1.Deployment{}

	if err := c.BindJSON(deployment); err != nil {
		fail(c, http.StatusBadRequest, err)
		return
	}

	deploymentInfo, err := kubeclient.GetDeployment(deployment.Name)
	if err == nil || deploymentInfo == nil {
		logs.Info(fmt.Sprintf("Deployment: %s not exist, create.", deployment.Name))
		createErr := kubeclient.CreateNativeDeployment(deployment)
		if createErr != nil {
			fail(c, http.StatusInternalServerError, createErr)
			return
		}
	} else {
		logs.Info(fmt.Sprintf("Deployment: %s exist, update.", deployment.Name))
		updateErr := kubeclient.UpdateNativeDeployment(deployment)
		if updateErr != nil {
			fail(c, http.StatusInternalServerError, updateErr)
			return
		}
	}

	ok(c, "")
}

// @Tags  deployment
// @Summary  删除deployment
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  deploymentName  path  string  true  "deployment名称"
// @Success 200 {object} types.Result{data=""} ""
// @Router /deployment/{deploymentName} [delete]
func deleteDeployment(c *gin.Context) {
	deploymentName := c.Param("deploymentName")

	if !checkDeploymentName(c, deploymentName) {
		return
	}

	err := kubeclient.DeleteDeployment(deploymentName)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, "")
}

func checkDeploymentName(c *gin.Context, deploymentName string) bool {
	if deploymentName == "" {
		fail(c, http.StatusBadRequest, errors.New("deployment名称不能为空"))
		return false
	}

	return true
}
