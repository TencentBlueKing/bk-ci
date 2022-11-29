package apis

import (
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/logs"
	"disaptch-k8s-manager/pkg/types"
	"fmt"
	"github.com/gin-gonic/gin"
	ut "github.com/go-playground/universal-translator"
	"github.com/go-playground/validator/v10"
	swaggerFiles "github.com/swaggo/files"
	ginSwagger "github.com/swaggo/gin-swagger"
	"net/http"
)

const ApiPrefix = "/api"

var NoAuthUrls = []string{
	// TODO: 等完善鉴权机制后放开，否则可能有安全问题
	// builderPrefix + builderDebugUrl,
}

var Trans ut.Translator

// InitApis 初始化Api
// @title kubernetes-manager api文档
// @version 0.0.1
// @BasePath /api
func InitApis(r *gin.Engine, handlers ...gin.HandlerFunc) {
	if config.Envs.IsDebug {
		r.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler))
	}

	// 只给api接口添加中间件
	apis := r.Group(ApiPrefix, handlers...)
	initJobsApis(apis)
	initBuilderApis(apis)
	initTasksApis(apis)
}

func ok(c *gin.Context, data interface{}) {
	c.JSON(http.StatusOK, &types.Result{
		Data:   data,
		Status: 0,
	})
}

// okFail 请求是成功的但是逻辑错了 TODO: 未来用来放各种错误码
func okFail(c *gin.Context, code int, err error) {
	// 对于所有的500日志都记录下堆栈信息
	if code == http.StatusInternalServerError {
		logs.Error(fmt.Sprintf("request %s error. ", c.Request.RequestURI), err)
	}
	c.JSON(http.StatusOK, &types.Result{
		Data:    nil,
		Status:  code,
		Message: err.Error(),
	})
}

func fail(c *gin.Context, code int, err error) {
	// 对于所有的500日志都记录下堆栈信息
	if code == http.StatusInternalServerError {
		logs.Error(fmt.Sprintf("request %s error. ", c.Request.RequestURI), err)
	}

	// 对于校验报错特殊处理
	errs, ok := err.(validator.ValidationErrors)
	if ok {
		var msg string
		for _, v := range errs.Translate(Trans) {
			msg = msg + v + ". "
		}
		c.JSON(code, &types.Result{
			Data:    nil,
			Status:  code,
			Message: msg,
		})
		return
	}

	c.JSON(code, &types.Result{
		Data:    nil,
		Status:  code,
		Message: err.Error(),
	})
}
