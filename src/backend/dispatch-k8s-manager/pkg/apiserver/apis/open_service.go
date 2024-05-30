package apis

import (
	"disaptch-k8s-manager/pkg/kubeclient"
	"github.com/gin-gonic/gin"
	"github.com/pkg/errors"
	corev1 "k8s.io/api/core/v1"
	"net/http"
)

const (
	servicePrefix = "/services"
)

func initServiceApis(r *gin.RouterGroup) {
	service := r.Group(servicePrefix)
	{
		service.GET("/:serviceName", getService)
		service.POST("", createService)
		service.DELETE("/:serviceName", deleteService)
	}
}

// @Tags  service
// @Summary  获取service详情
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  serviceName  path  string  true  "deployment名称"
// @Success 200 {object} types.Result{data=appsv1.service} "service详情"
// @Router /service/{serviceName} [get]
func getService(c *gin.Context) {
	serviceName := c.Param("serviceName")

	if !checkServiceName(c, serviceName) {
		return
	}

	service, err := kubeclient.GetService(serviceName)
	if err != nil {
		okFail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, service)
}

// @Tags  service
// @Summary  创建service负载资源
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  service  body  corev1.Service  true  "service负载信息"
// @Success 200 {object} ""
// @Router /service [post]
func createService(c *gin.Context) {
	service := &corev1.Service{}

	if err := c.BindJSON(service); err != nil {
		fail(c, http.StatusBadRequest, err)
		return
	}

	serviceInfo, _ := kubeclient.GetService(service.Name)
	if serviceInfo != nil {
		err := kubeclient.UpdateService(service)
		if err != nil {
			fail(c, http.StatusInternalServerError, err)
			return
		}
	} else {
		err := kubeclient.CreateService(service)
		if err != nil {
			fail(c, http.StatusInternalServerError, err)
			return
		}
	}

	ok(c, "")
}

// @Tags  service
// @Summary  删除service
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  serviceName  path  string  true  "service名称"
// @Success 200 {object} types.Result{data=""} ""
// @Router /service/{serviceName} [delete]
func deleteService(c *gin.Context) {
	serviceName := c.Param("serviceName")

	if !checkServiceName(c, serviceName) {
		return
	}

	err := kubeclient.DeleteService(serviceName)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, "")
}

func checkServiceName(c *gin.Context, deploymentName string) bool {
	if deploymentName == "" {
		fail(c, http.StatusBadRequest, errors.New("service名称不能为空"))
		return false
	}

	return true
}
