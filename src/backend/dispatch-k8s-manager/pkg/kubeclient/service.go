package kubeclient

import (
	"context"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func CreateService(namespace string, service *corev1.Service) error {
	_, err := kubeClient.CoreV1().Services(namespace).Create(
		context.TODO(),
		service,
		metav1.CreateOptions{},
	)

	if err != nil {
		return err
	}
	return nil
}

func UpdateService(namespace string, service *corev1.Service) error {
	_, err := kubeClient.CoreV1().Services(namespace).Update(
		context.TODO(),
		service,
		metav1.UpdateOptions{},
	)

	if err != nil {
		return err
	}
	return nil
}

func DeleteService(namespace string, serviceName string) error {
	return kubeClient.CoreV1().Services(namespace).Delete(
		context.TODO(),
		serviceName,
		metav1.DeleteOptions{},
	)
}

func GetService(namespace string, serviceName string) (*corev1.Service, error) {
	service, err := kubeClient.CoreV1().Services(namespace).Get(
		context.TODO(),
		serviceName,
		metav1.GetOptions{},
	)

	if err != nil {
		return nil, err
	}

	return service, err
}
