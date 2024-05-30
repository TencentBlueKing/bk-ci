package kubeclient

import (
	"context"
	"disaptch-k8s-manager/pkg/config"
	networkv1 "k8s.io/api/networking/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func CreateIngress(ingress *networkv1.Ingress) error {
	_, err := kubeClient.NetworkingV1().Ingresses(config.Config.Kubernetes.NameSpace).Create(
		context.TODO(),
		ingress,
		metav1.CreateOptions{},
	)

	if err != nil {
		return err
	}
	return nil
}

func UpdateIngress(ingress *networkv1.Ingress) error {
	_, err := kubeClient.NetworkingV1().Ingresses(config.Config.Kubernetes.NameSpace).Update(
		context.TODO(),
		ingress,
		metav1.UpdateOptions{},
	)

	if err != nil {
		return err
	}
	return nil
}

func DeleteIngress(ingressName string) error {
	return kubeClient.NetworkingV1().Ingresses(config.Config.Kubernetes.NameSpace).Delete(
		context.TODO(),
		ingressName,
		metav1.DeleteOptions{},
	)
}

func GetIngress(ingressName string) (*networkv1.Ingress, error) {
	ingress, err := kubeClient.NetworkingV1().Ingresses(config.Config.Kubernetes.NameSpace).Get(
		context.TODO(),
		ingressName,
		metav1.GetOptions{},
	)

	if err != nil {
		return nil, err
	}

	return ingress, err
}
