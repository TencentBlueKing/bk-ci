package kubeclient

import (
	"bytes"
	"context"
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/logs"
	"fmt"
	"github.com/gorilla/websocket"
	"github.com/pkg/errors"
	"io"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/watch"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/rest"
	"net/http"
	"time"
)

func GetPod(podName string) (*corev1.Pod, error) {
	get, err := infs.pod.Pods(config.Config.Kubernetes.NameSpace).Get(
		podName,
	)
	if err != nil {
		return nil, err
	}

	return get, nil
}

func ListPod(workloadCoreLabel string) ([]*corev1.Pod, error) {
	list, err := infs.pod.Pods(config.Config.Kubernetes.NameSpace).List(
		labels.SelectorFromSet(map[string]string{
			config.Config.Dispatch.Label: workloadCoreLabel,
		}),
	)

	if err != nil {
		return nil, err
	}

	return list, nil
}

func LogPod(podName string, containerName string, sinceSeconds *int64) (string, error) {
	req := kubeClient.CoreV1().Pods(config.Config.Kubernetes.NameSpace).GetLogs(podName, &corev1.PodLogOptions{
		Container:    containerName,
		SinceSeconds: sinceSeconds,
	})

	podLogs, err := req.Stream(context.TODO())
	if err != nil {
		return "", errors.Wrap(err, "error in opening podLogs stream")
	}
	defer podLogs.Close()

	buf := new(bytes.Buffer)
	_, err = io.Copy(buf, podLogs)
	if err != nil {
		return "", errors.Wrap(err, "error in copy information from podLogs to buf")
	}
	str := buf.String()

	return str, nil
}

func WatchPod(labelName string) (watch.Interface, error) {
	return kubeClient.CoreV1().Pods(config.Config.Kubernetes.NameSpace).Watch(
		context.TODO(),
		metav1.ListOptions{
			LabelSelector: labelName,
		},
	)
}

var protocols = []string{
	"base64.channel.k8s.io",
}

func WebSocketExecPod(podName string, containerName string) (ws *websocket.Conn, err error) {
	url := kubeClient.CoreV1().RESTClient().Post().
		Resource("pods").
		Name(podName).
		Namespace(config.Config.Kubernetes.NameSpace).
		SubResource("exec").
		VersionedParams(
			&corev1.PodExecOptions{
				Command:   []string{"sh", "-c", "(bash || ash || sh)"},
				Stdin:     true,
				Stdout:    true,
				Stderr:    true,
				TTY:       true,
				Container: containerName,
			},
			scheme.ParameterCodec,
		).URL()

	switch url.Scheme {
	case "https":
		url.Scheme = "wss"
	case "http":
		url.Scheme = "ws"
	default:
		logs.Error(fmt.Sprintf("WebSocketExecPod|%s|%s|unrecognised kube URL scheme in %v. ", podName, containerName, url.Scheme))
		return nil, errors.New("")
	}

	addr := url.String()

	// 组装ws配置
	tlsConfig, err := rest.TLSConfigFor(kubeRestConfig)
	if err != nil {
		logs.Error(fmt.Sprintf("WebSocketExecPod|%s|%s|tlsconfig kube error. ", podName, containerName), err)
		return nil, err
	}
	dialer := &websocket.Dialer{
		Proxy:            http.ProxyFromEnvironment,
		TLSClientConfig:  tlsConfig,
		HandshakeTimeout: 1 * time.Hour,
		Subprotocols:     protocols,
	}

	// 获取ws链接
	ws, _, err = dialer.Dial(addr, nil)
	if err != nil {
		logs.Error(fmt.Sprintf("WebSocketExecPod|%s|%s|dial kube URL %s error. ", podName, containerName, addr), err)
		return nil, err
	}

	return ws, nil
}
