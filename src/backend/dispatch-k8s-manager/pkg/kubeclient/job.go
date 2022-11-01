package kubeclient

import (
	"context"
	"disaptch-k8s-manager/pkg/config"
	batchv1 "k8s.io/api/batch/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func CreateJob(job *Job) error {

	var containers []corev1.Container
	for _, con := range job.Pod.Containers {
		containers = append(containers, corev1.Container{
			Name:         job.Name,
			Image:        con.Image,
			Resources:    con.Resources,
			Env:          con.Env,
			Command:      con.Command,
			VolumeMounts: con.VolumeMounts,
			Args:         con.Args,
		})
	}

	var ImagePullSecrets []corev1.LocalObjectReference
	if job.Pod.PullImageSecret != nil {
		ImagePullSecrets = []corev1.LocalObjectReference{{Name: job.Pod.PullImageSecret.Name}}
	}

	_, err := kubeClient.BatchV1().Jobs(config.Config.Kubernetes.NameSpace).Create(
		context.TODO(),
		&batchv1.Job{
			TypeMeta: metav1.TypeMeta{
				Kind:       "Job",
				APIVersion: "batch/v1",
			},
			ObjectMeta: metav1.ObjectMeta{
				Name:      job.Name,
				Namespace: config.Config.Kubernetes.NameSpace,
			},
			Spec: batchv1.JobSpec{
				BackoffLimit: job.BackOffLimit,
				Template: corev1.PodTemplateSpec{
					ObjectMeta: metav1.ObjectMeta{Labels: job.Pod.Labels},
					Spec: corev1.PodSpec{
						Volumes:          job.Pod.Volumes,
						NodeName:         job.NodeName,
						RestartPolicy:    job.Pod.RestartPolicy,
						Containers:       containers,
						ImagePullSecrets: ImagePullSecrets,
					},
				},
				ActiveDeadlineSeconds: job.ActiveDeadlineSeconds,
			},
		},
		metav1.CreateOptions{},
	)
	if err != nil {
		return err
	}

	return nil
}

func DeleteJob(jobName string) error {
	backGround := metav1.DeletePropagationBackground
	return kubeClient.BatchV1().Jobs(config.Config.Kubernetes.NameSpace).Delete(
		context.TODO(),
		jobName,
		metav1.DeleteOptions{
			PropagationPolicy: &backGround,
		},
	)
}
