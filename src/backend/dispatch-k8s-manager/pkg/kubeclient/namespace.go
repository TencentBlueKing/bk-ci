package kubeclient

import (
	"context"

	"github.com/pkg/errors"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func GetNamespace(namespace string) (*corev1.Namespace, error) {
	if kubeClient == nil {
		return nil, errors.New("kube client not initialized")
	}
	return kubeClient.CoreV1().Namespaces().Get(context.TODO(), namespace, metav1.GetOptions{})
}
