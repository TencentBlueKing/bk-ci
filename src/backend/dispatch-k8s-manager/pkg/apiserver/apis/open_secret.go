package apis

import (
	"disaptch-k8s-manager/pkg/kubeclient"
	"github.com/gin-gonic/gin"
	"github.com/pkg/errors"
	corev1 "k8s.io/api/core/v1"
	"net/http"
)

const (
	secretPrefix = "/namespace/:namespace/secrets"
)

func initSecretApis(r *gin.RouterGroup) {
	secret := r.Group(secretPrefix)
	{
		secret.GET("/:secretName", getSecret)
		secret.POST("", createSecret)
		secret.DELETE("/:secretName", deleteSecret)
	}
}

// @Tags  secret
// @Summary  获取secret详情
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  secretName  path  string  true  "secret名称"
// @Success 200 {object} types.Result{data=corev1.secret} "secret详情"
// @Router /secret/{secretName} [get]
func getSecret(c *gin.Context) {
	namespace := c.Param("namespace")
	secretName := c.Param("secretName")

	if !checkSecretName(c, secretName) {
		return
	}

	secret, err := kubeclient.GetNativeSecret(namespace, secretName)
	if err != nil {
		okFail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, secret)
}

// @Tags  secret
// @Summary  创建secret负载资源
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  secret  body  corev1.secret  true  "secret信息"
// @Success 200 {object} ""
// @Router /secret [post]
func createSecret(c *gin.Context) {
	namespace := c.Param("namespace")
	secret := &corev1.Secret{}

	if err := c.BindJSON(secret); err != nil {
		fail(c, http.StatusBadRequest, err)
		return
	}

	err := kubeclient.CreateNativeSecret(namespace, secret)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, "")
}

// @Tags  secret
// @Summary  删除secret
// @Accept  json
// @Product  json
// @Param  Devops-Token  header  string  true "凭证信息"
// @Param  secretName  path  string  true  "secret名称"
// @Success 200 {object} types.Result{data=""} ""
// @Router /secret/{secretName} [delete]
func deleteSecret(c *gin.Context) {
	namespace := c.Param("namespace")
	secretName := c.Param("secretName")

	if !checkSecretName(c, secretName) {
		return
	}

	err := kubeclient.DeleteNativeSecret(namespace, secretName)
	if err != nil {
		fail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, "")
}

func checkSecretName(c *gin.Context, secretName string) bool {
	if secretName == "" {
		fail(c, http.StatusBadRequest, errors.New("secret名称不能为空"))
		return false
	}

	return true
}
