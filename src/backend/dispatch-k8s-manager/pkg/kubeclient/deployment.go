package kubeclient

import (
	"context"
	"disaptch-k8s-manager/pkg/config"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/watch"
)

func CreateDeployment(dep *Deployment) error {

	var containers []corev1.Container
	for _, con := range dep.Pod.Containers {
		containers = append(containers, corev1.Container{
			Name:         dep.Name,
			Image:        con.Image,
			Resources:    con.Resources,
			Env:          con.Env,
			Command:      con.Command,
			VolumeMounts: con.VolumeMounts,
		})
	}

	var affinity *corev1.Affinity
	if len(dep.Pod.NodeMatches) > 0 {
		var matches []corev1.NodeSelectorRequirement
		for _, mat := range dep.Pod.NodeMatches {
			matches = append(matches, corev1.NodeSelectorRequirement{
				Key:      mat.Key,
				Operator: mat.Operator,
				Values:   mat.Values,
			})
		}
		affinity = &corev1.Affinity{
			NodeAffinity: &corev1.NodeAffinity{
				RequiredDuringSchedulingIgnoredDuringExecution: &corev1.NodeSelector{
					NodeSelectorTerms: []corev1.NodeSelectorTerm{
						{
							MatchExpressions: matches,
						},
					},
				},
			},
		}
	}

	var ImagePullSecrets []corev1.LocalObjectReference
	if dep.Pod.PullImageSecret != nil {
		ImagePullSecrets = []corev1.LocalObjectReference{{Name: dep.Pod.PullImageSecret.Name}}
	}

	_, err := kubeClient.AppsV1().Deployments(config.Config.Kubernetes.NameSpace).Create(
		context.TODO(),
		&appsv1.Deployment{
			TypeMeta: metav1.TypeMeta{
				Kind:       "Deployment",
				APIVersion: "apps/v1",
			},
			ObjectMeta: metav1.ObjectMeta{
				Name:      dep.Name,
				Namespace: config.Config.Kubernetes.NameSpace,
				Labels:    dep.Labels,
			},
			Spec: appsv1.DeploymentSpec{
				Replicas: dep.Replicas,
				Selector: &metav1.LabelSelector{
					MatchLabels: dep.MatchLabels,
				},
				Template: corev1.PodTemplateSpec{
					ObjectMeta: metav1.ObjectMeta{
						Labels:      dep.Pod.Labels,
						Annotations: dep.Pod.Annotations,
					},
					Spec: corev1.PodSpec{
						Volumes:          dep.Pod.Volumes,
						Containers:       containers,
						ImagePullSecrets: ImagePullSecrets,
						Affinity:         affinity,
						Tolerations:      dep.Pod.Tolerations,
					},
				},
			},
		},
		metav1.CreateOptions{},
	)

	if err != nil {
		return err
	}
	return nil
}

func PatchDeployment(deploymentName string, jsonPatch []byte) error {
	_, err := kubeClient.AppsV1().Deployments(config.Config.Kubernetes.NameSpace).Patch(
		context.TODO(),
		deploymentName,
		types.JSONPatchType,
		jsonPatch,
		metav1.PatchOptions{},
	)

	if err != nil {
		return err
	}
	return nil
}

func DeleteDeployment(deploymentName string) error {
	return kubeClient.AppsV1().Deployments(config.Config.Kubernetes.NameSpace).Delete(
		context.TODO(),
		deploymentName,
		metav1.DeleteOptions{},
	)
}

func WatchDeployment(labelName string) (watch.Interface, error) {
	return kubeClient.AppsV1().Deployments(config.Config.Kubernetes.NameSpace).Watch(
		context.TODO(),
		metav1.ListOptions{
			LabelSelector: labelName,
		},
	)
}

func ListDeployment(workloadCoreLabel string) ([]*appsv1.Deployment, error) {
	list, err := infs.deployment.Deployments(config.Config.Kubernetes.NameSpace).List(
		labels.SelectorFromSet(map[string]string{
			config.Config.Dispatch.Label: workloadCoreLabel,
		}),
	)

	if err != nil {
		return nil, err
	}

	return list, nil
}
