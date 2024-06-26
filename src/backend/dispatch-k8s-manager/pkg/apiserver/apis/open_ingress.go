package apis

import (
	"disaptch-k8s-manager/pkg/kubeclient"
	"github.com/gin-gonic/gin"
	"github.com/pkg/errors"
	networkv1 "k8s.io/api/networking/v1"
	"net/http"
)

const (
	ingressPrefix = "/namespace/:namespace/ingress"
)

func initIngressApis(r *gin.RouterGroup) {
	ingress := r.Group(ingressPrefix)
	{
		ingress.GET("/:ingressName", getIngress)
		ingress.POST("", createIngress)
		ingress.DELETE("/:ingressName", deleteIngress)
	}
}

// @Tags  ingress
// @Summary  获取ingress详情
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  ingressName  path  string  true  "ingress名称"
// @Success 200 {object} types.Result{data=networkv1.ingress} "ingress详情"
// @Router /ingress/{ingressName} [get]
func getIngress(c *gin.Context) {
	namespace := c.Param("namespace")
	ingressName := c.Param("ingressName")

	if !checkIngressName(c, ingressName) {
		return
	}

	ingress, err := kubeclient.GetIngress(namespace, ingressName)
	if err != nil {
		okFail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, ingress)
}

// @Tags  ingress
// @Summary  创建ingress负载资源
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  ingress  body  networkv1.ingress  true  "ingress负载信息"
// @Success 200 {object} ""
// @Router /ingress [post]
func createIngress(c *gin.Context) {
	namespace := c.Param("namespace")
	ingress := &networkv1.Ingress{}

	if err := c.BindJSON(ingress); err != nil {
		fail(c, http.StatusBadRequest, err)
		return
	}

	ingressInfo, _ := kubeclient.GetIngress(namespace, ingress.Name)
	if ingressInfo != nil {
		err := kubeclient.UpdateIngress(namespace, ingress)
		if err != nil {
			fail(c, http.StatusInternalServerError, err)
			return
		}
	} else {
		err := kubeclient.CreateIngress(namespace, ingress)
		if err != nil {
			fail(c, http.StatusInternalServerError, err)
			return
		}
	}

	ok(c, "")
}

// @Tags  ingress
// @Summary  删除ingress
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  ingressName  path  string  true  "ingress名称"
// @Success 200 {object} types.Result{data=""} ""
// @Router /ingress/{ingressName} [delete]
func deleteIngress(c *gin.Context) {
	namespace := c.Param("namespace")
	ingressName := c.Param("ingressName")

	if !checkIngressName(c, ingressName) {
		return
	}

	err := kubeclient.DeleteIngress(namespace, ingressName)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, "")
}

func checkIngressName(c *gin.Context, deploymentName string) bool {
	if deploymentName == "" {
		fail(c, http.StatusBadRequest, errors.New("ingress名称不能为空"))
		return false
	}

	return true
}
