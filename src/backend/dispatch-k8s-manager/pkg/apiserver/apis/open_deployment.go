package apis

import (
	"disaptch-k8s-manager/pkg/kubeclient"
	"disaptch-k8s-manager/pkg/logs"
	"fmt"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/pkg/errors"
	appsv1 "k8s.io/api/apps/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

var getNativeDeployment = kubeclient.GetNativeDeployment
var createNativeDeployment = kubeclient.CreateNativeDeployment
var updateNativeDeployment = kubeclient.UpdateNativeDeployment
var deleteNativeDeployment = kubeclient.DeleteNativeDeployment

const (
	deploymentPrefix = "/namespace/:namespace/deployments"
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
	namespace := c.Param("namespace")
	deploymentName := c.Param("deploymentName")

	if denySystemNamespace(c, namespace) {
		return
	}
	if !checkDeploymentParams(c, deploymentName) {
		return
	}

	deployment, err := getNativeDeployment(namespace, deploymentName)
	if err != nil {
		okFail(c, http.StatusInternalServerError, err)
		return
	}
	meta := metav1.ObjectMeta{}
	if deployment != nil {
		meta = deployment.ObjectMeta
	}
	if !authorizeNamespaceRead(c, namespace, meta) {
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
	namespace := c.Param("namespace")
	if denySystemNamespace(c, namespace) {
		return
	}

	deployment := &appsv1.Deployment{}

	if err := c.BindJSON(deployment); err != nil {
		fail(c, http.StatusBadRequest, err)
		return
	}

	existingMeta := metav1.ObjectMeta{}
	deploymentInfo, _ := getNativeDeployment(namespace, deployment.Name)
	if deploymentInfo != nil {
		existingMeta = deploymentInfo.ObjectMeta
	}
	caller, allowed := authorizeNamespaceWrite(c, namespace, existingMeta)
	if !allowed {
		return
	}
	stampObjectOwner(&deployment.ObjectMeta, caller.Owner())

	if deploymentInfo != nil {
		logs.Info(fmt.Sprintf("Deployment: %s exist, update.", deployment.Name))
		updateErr := updateNativeDeployment(namespace, deployment)
		if updateErr != nil {
			fail(c, http.StatusInternalServerError, updateErr)
			return
		}
	} else {
		logs.Info(fmt.Sprintf("Deployment: %s not exist, create.", deployment.Name))
		createErr := createNativeDeployment(namespace, deployment)
		if createErr != nil {
			fail(c, http.StatusInternalServerError, createErr)
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
	namespace := c.Param("namespace")
	deploymentName := c.Param("deploymentName")

	if denySystemNamespace(c, namespace) {
		return
	}
	if !checkDeploymentParams(c, deploymentName) {
		return
	}

	existing, _ := getNativeDeployment(namespace, deploymentName)
	meta := metav1.ObjectMeta{}
	if existing != nil {
		meta = existing.ObjectMeta
	}
	if !authorizeNamespaceRead(c, namespace, meta) {
		return
	}

	err := deleteNativeDeployment(namespace, deploymentName)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, "")
}

func checkDeploymentParams(c *gin.Context, deploymentName string) bool {
	if deploymentName == "" {
		fail(c, http.StatusBadRequest, errors.New("deployment名称不能为空"))
		return false
	}

	return true
}
