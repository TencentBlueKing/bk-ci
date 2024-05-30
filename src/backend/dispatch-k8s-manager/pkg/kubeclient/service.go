package kubeclient

import (
	"context"
	"disaptch-k8s-manager/pkg/config"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func CreateService(service *corev1.Service) error {
	_, err := kubeClient.CoreV1().Services(config.Config.Kubernetes.NameSpace).Create(
		context.TODO(),
		service,
		metav1.CreateOptions{},
	)

	if err != nil {
		return err
	}
	return nil
}

func UpdateService(service *corev1.Service) error {
	_, err := kubeClient.CoreV1().Services(config.Config.Kubernetes.NameSpace).Update(
		context.TODO(),
		service,
		metav1.UpdateOptions{},
	)

	if err != nil {
		return err
	}
	return nil
}

func DeleteService(serviceName string) error {
	return kubeClient.CoreV1().Services(config.Config.Kubernetes.NameSpace).Delete(
		context.TODO(),
		serviceName,
		metav1.DeleteOptions{},
	)
}

func GetService(serviceName string) (*corev1.Service, error) {
	service, err := kubeClient.CoreV1().Services(config.Config.Kubernetes.NameSpace).Get(
		context.TODO(),
		serviceName,
		metav1.GetOptions{},
	)

	if err != nil {
		return nil, err
	}

	return service, err
}
