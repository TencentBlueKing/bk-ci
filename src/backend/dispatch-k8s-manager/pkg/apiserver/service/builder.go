package service

import (
	"context"
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/db/mysql"
	"disaptch-k8s-manager/pkg/kubeclient"
	"disaptch-k8s-manager/pkg/logs"
	"disaptch-k8s-manager/pkg/task"
	"disaptch-k8s-manager/pkg/types"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/gorilla/websocket"
	corev1 "k8s.io/api/core/v1"
	"sync"
	"time"
)

func GetBuilderStatus(workloadName string) (*BuilderStatus, error) {
	// 先查询deployment有可能deployment存在但是pod为空证明是停止等待启动
	depList, err := kubeclient.ListDeployment(workloadName)
	if err != nil {
		return nil, err
	}
	if len(depList) == 0 {
		return &BuilderStatus{
			Status:  BuilderNotExist,
			Message: "",
		}, nil
	}

	depStatus := depList[0].Status
	if depStatus.Replicas == 0 {
		return &BuilderStatus{
			Status:  BuilderReadyToRun,
			Message: "",
		}, nil
	}

	podList, err := kubeclient.ListPod(workloadName)
	if err != nil {
		return nil, err
	}
	if len(podList) == 0 {
		return &BuilderStatus{
			Status:  BuilderUnknown,
			Message: fmt.Sprintf("builder pod with %s is null", workloadName),
		}, nil
	}

	podStatus := podList[0].Status
	switch podStatus.Phase {
	case corev1.PodPending:
		return &BuilderStatus{Status: BuilderPending, Message: ""}, nil
	case corev1.PodRunning:
		if len(podList[0].Spec.Containers) > 0 && podStatus.ContainerStatuses[0].State.Running != nil {
			return &BuilderStatus{Status: BuilderRunning, Message: ""}, nil
		} else {
			return &BuilderStatus{
				Status:  BuilderUnknown,
				Message: "pod status running but container not",
			}, nil
		}
	case corev1.PodSucceeded:
		return &BuilderStatus{Status: BuilderSucceeded, Message: ""}, nil
	case corev1.PodFailed:
		{
			var message string
			if len(podStatus.ContainerStatuses) > 0 {
				containerState := podStatus.ContainerStatuses[0].State
				if containerState.Terminated != nil {
					if containerState.Terminated.ExitCode != 0 {
						message = fmt.Sprintf("%s|%s(Exit code %d)",
							containerState.Terminated.Message,
							containerState.Terminated.Reason,
							containerState.Terminated.ExitCode,
						)
					}
				}
			} else {
				message = podStatus.Message + "|" + podStatus.Reason
			}
			return &BuilderStatus{Status: BuilderFailed, Message: message}, nil
		}
	}

	return &BuilderStatus{
		Status:  BuilderUnknown,
		Message: "pod status unknown",
	}, nil
}

func StopBuilder(builderName string) (taskId string, err error) {
	taskId = generateTaskId()

	labels := getDispatchLabel(builderName, taskId, types.TaskActionStop, types.BuilderTaskLabel)

	data, err := json.Marshal([]map[string]interface{}{
		{
			"op":    "replace",
			"path":  "/metadata/labels",
			"value": labels,
		},
		{
			"op":    "replace",
			"path":  "/spec/template/metadata/labels",
			"value": labels,
		},
		{
			"op":    "replace",
			"path":  "/spec/replicas",
			"value": 0,
		},
	})

	if err = mysql.InsertTask(types.Task{
		TaskId:     taskId,
		TaskKey:    builderName,
		TaskBelong: types.TaskBelongBuilder,
		Action:     types.TaskActionStop,
		Status:     types.TaskWaiting,
		Message:    nil,
		ActionTime: time.Now(),
		UpdateTime: time.Now(),
	}); err != nil {
		return "", err
	}

	go task.DoStopBuilder(taskId, builderName, data)

	return taskId, nil
}

func DeleteBuilder(builderName string) (taskId string, err error) {
	taskId = generateTaskId()

	if err := mysql.InsertTask(types.Task{
		TaskId:     taskId,
		TaskKey:    builderName,
		TaskBelong: types.TaskBelongBuilder,
		Action:     types.TaskActionDelete,
		Status:     types.TaskWaiting,
		Message:    nil,
		ActionTime: time.Now(),
		UpdateTime: time.Now(),
	}); err != nil {
		return "", err
	}

	go task.DoDeleteBuilder(taskId, builderName)

	return taskId, nil
}

func DebugBuilderUrl(urlPerfix string, builderName string) (url string, err error) {
	podName, containerName, err := getPodAndContainerName(builderName)
	if err != nil {
		return "", err
	}
	if podName == "" || containerName == "" {
		return "", fmt.Errorf("登录调试容器 %s 不存在", builderName)
	}

	return fmt.Sprintf("ws://%s%s/%s/%s", config.Config.Gateway.Url, urlPerfix, podName, containerName), nil
}

const defaultCols = 144
const defaultRows = 24

// DebugBuilder
// websocket报文内容的第一个字节，用来表示“频道”
// 0 标准输入
// 1 标准输出
// 2 标准错误
// 3 服务端异常信息
// 4 terminal窗口大小调整resize
func DebugBuilder(ws *websocket.Conn, podName string, containerName string) {
	// 获取kubernetes ws
	kubeWs, err := kubeclient.WebSocketExecPod(podName, containerName)
	if err != nil {
		logs.Error(fmt.Sprintf("WebSocketExecPod|%s|%s| client kube ws error. ", podName, containerName), err)
		_ = ws.WriteMessage(websocket.CloseMessage, []byte("登录调试建立与kubernetes的websocket链接错误, 请联系管理员"))
		return
	}
	defer kubeWs.Close()

	// 建立链接后立刻发送重置窗口
	windowMsg := "4" + base64.StdEncoding.EncodeToString(
		[]byte(fmt.Sprintf("{\"Width\":%d,\"Height\":%d}", defaultCols, defaultRows)),
	)
	if err = kubeWs.WriteMessage(websocket.TextMessage, []byte(windowMsg)); err != nil {
		logs.Error(fmt.Sprintf("WebSocketExecPod|%s|%s| write kube window message error. ", podName, containerName), err)
		_ = ws.WriteMessage(websocket.CloseMessage, []byte("写入kube websocket信息失败，请联系管理员"))
		return
	}

	wg := sync.WaitGroup{}
	wg.Add(2)
	ctx, cancel := context.WithCancel(context.Background())

	go func() {
		defer wg.Done()
		defer cancel()

		for {
			select {
			case <-ctx.Done():
				break
			default:
			}

			// 将写回的数据返回
			rt, reply, err := kubeWs.ReadMessage()
			if rt == websocket.CloseMessage {
				return
			}
			if err != nil {
				logs.Error(fmt.Sprintf("WebSocketExecPod|%s|%s| read kube ws message error. ", podName, containerName), err)
				_ = ws.WriteMessage(websocket.CloseMessage, []byte("读取kube websocket信息失败，请联系管理员"))
				return
			}
			if rt != websocket.TextMessage && rt != websocket.BinaryMessage {
				continue
			}

			//写入client数据
			if err = ws.WriteMessage(websocket.TextMessage, reply); err != nil {
				logs.Error(fmt.Sprintf("WebSocketExecPod|%s|%s| write ws message error. ", podName, containerName), err)
				_ = ws.WriteMessage(websocket.CloseMessage, []byte("写入websocket信息失败，请联系管理员"))
				return
			}
		}
	}()

	go func() {
		defer wg.Done()
		defer cancel()

		for {
			select {
			case <-ctx.Done():
				break
			default:
			}

			//读取client的数据
			mt, message, err := ws.ReadMessage()
			if mt == websocket.CloseMessage {
				return
			}
			if err != nil {
				logs.Error(fmt.Sprintf("WebSocketExecPod|%s|%s| read ws message error. ", podName, containerName), err)
				_ = ws.WriteMessage(websocket.CloseMessage, []byte("读取用户websocket信息失败，请联系管理员"))
				return
			}
			if mt != websocket.TextMessage {
				continue
			}

			// 读取的数据写入kube的websocket
			if err = kubeWs.WriteMessage(websocket.TextMessage, message); err != nil {
				logs.Error(fmt.Sprintf("WebSocketExecPod|%s|%s| write kube ws message error. ", podName, containerName), err)
				_ = ws.WriteMessage(websocket.CloseMessage, []byte("写入kube websocket信息失败，请联系管理员"))
				return
			}
		}
	}()

	wg.Wait()
}
