package kubeclient

import (
	"disaptch-k8s-manager/pkg/types"
	corev1 "k8s.io/api/core/v1"
)

type Deployment struct {
	Name        string
	Labels      map[string]string
	MatchLabels map[string]string
	Replicas    *int32
	Pod         Pod
}

type DeploymentStart struct {
	Name     string
	Replicas *int32
	Labels   map[string]string
	Env      []corev1.EnvVar
	Command  []string
}

type Job struct {
	Name                  string
	NodeName              string
	ActiveDeadlineSeconds *int64
	BackOffLimit          *int32
	Pod                   Pod
}

type Pod struct {
	Labels          map[string]string
	Annotations     map[string]string
	Volumes         []corev1.Volume
	Containers      []Container
	RestartPolicy   corev1.RestartPolicy
	NodeMatches     []NodeMatch
	Tolerations     []corev1.Toleration
	PullImageSecret *DockerSecret
}

type NodeMatch struct {
	Key      string
	Operator corev1.NodeSelectorOperator
	Values   []string
}

type Container struct {
	Image        string
	Resources    corev1.ResourceRequirements
	Env          []corev1.EnvVar
	Command      []string
	VolumeMounts []corev1.VolumeMount
	Args         []string
}

type DockerSecret struct {
	Name       string
	Labels     map[string]string
	Registries []types.Registry
}
