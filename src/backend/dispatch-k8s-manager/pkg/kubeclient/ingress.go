package kubeclient

import (
	"context"
	networkv1 "k8s.io/api/networking/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func CreateIngress(namespace string, ingress *networkv1.Ingress) error {
	_, err := kubeClient.NetworkingV1().Ingresses(namespace).Create(
		context.TODO(),
		ingress,
		metav1.CreateOptions{},
	)

	if err != nil {
		return err
	}
	return nil
}

func UpdateIngress(namespace string, ingress *networkv1.Ingress) error {
	_, err := kubeClient.NetworkingV1().Ingresses(namespace).Update(
		context.TODO(),
		ingress,
		metav1.UpdateOptions{},
	)

	if err != nil {
		return err
	}
	return nil
}

func DeleteIngress(namespace string, ingressName string) error {
	return kubeClient.NetworkingV1().Ingresses(namespace).Delete(
		context.TODO(),
		ingressName,
		metav1.DeleteOptions{},
	)
}

func GetIngress(namespace string, ingressName string) (*networkv1.Ingress, error) {
	ingress, err := kubeClient.NetworkingV1().Ingresses(namespace).Get(
		context.TODO(),
		ingressName,
		metav1.GetOptions{},
	)

	if err != nil {
		return nil, err
	}

	return ingress, err
}
