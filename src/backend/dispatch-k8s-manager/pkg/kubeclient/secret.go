package kubeclient

import (
	"context"
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/types"
	"encoding/base64"
	"encoding/json"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func CreateDockerRegistry(dockerSecret *DockerSecret) (*corev1.Secret, error) {
	if dockerSecret == nil {
		return nil, nil
	}

	dockerConfigJSONContent, err := HandleDockerCfgJSONContent(dockerSecret.Registries)
	if err != nil {
		return nil, err
	}

	secret := &corev1.Secret{
		TypeMeta: metav1.TypeMeta{
			APIVersion: corev1.SchemeGroupVersion.String(),
			Kind:       "Secret",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      dockerSecret.Name,
			Namespace: config.Config.Kubernetes.NameSpace,
			Labels:    dockerSecret.Labels,
		},
		Type: corev1.SecretTypeDockerConfigJson,
		Data: map[string][]byte{corev1.DockerConfigJsonKey: dockerConfigJSONContent},
	}

	s, err := kubeClient.CoreV1().
		Secrets(config.Config.Kubernetes.NameSpace).
		Create(context.TODO(), secret, metav1.CreateOptions{})
	if err != nil {
		return nil, err
	}

	return s, nil
}

// DockerConfigJSON represents a local docker auth config file
// for pulling images.
type DockerConfigJSON struct {
	Auths DockerConfig `json:"auths" datapolicy:"token"`
	// +optional
	HttpHeaders map[string]string `json:"HttpHeaders,omitempty" datapolicy:"token"`
}

// DockerConfig represents the config file used by the docker CLI.
// This config that represents the credentials that should be used
// when pulling images from specific image repositories.
type DockerConfig map[string]DockerConfigEntry

// DockerConfigEntry holds the user information that grant the access to docker registry
type DockerConfigEntry struct {
	Username string `json:"username,omitempty"`
	Password string `json:"password,omitempty" datapolicy:"password"`
	Email    string `json:"email,omitempty"`
	Auth     string `json:"auth,omitempty" datapolicy:"token"`
}

// HandleDockerCfgJSONContent serializes a ~/.docker/config.json file
func HandleDockerCfgJSONContent(registries []types.Registry) ([]byte, error) {
	auths := map[string]DockerConfigEntry{}
	for _, regis := range registries {
		auths[regis.Server] = DockerConfigEntry{
			Auth: encodeDockerConfigFieldAuth(regis.UserName, regis.Password),
		}
	}
	dockerConfigJSON := DockerConfigJSON{
		Auths: auths,
	}

	return json.Marshal(dockerConfigJSON)
}

// encodeDockerConfigFieldAuth returns base64 encoding of the username and password string
func encodeDockerConfigFieldAuth(username, password string) string {
	fieldValue := username + ":" + password
	return base64.StdEncoding.EncodeToString([]byte(fieldValue))
}

func ListSecret(workloadCoreLabel string) ([]corev1.Secret, error) {
	l, err := kubeClient.CoreV1().Secrets(config.Config.Kubernetes.NameSpace).List(context.TODO(), metav1.ListOptions{
		LabelSelector: config.Config.Dispatch.Label + "=" + workloadCoreLabel,
	})
	if err != nil {
		return nil, err
	}
	return l.Items, nil
}

func DeleteSecret(secretName string) error {
	if err := kubeClient.CoreV1().
		Secrets(config.Config.Kubernetes.NameSpace).
		Delete(context.TODO(), secretName, metav1.DeleteOptions{}); err != nil {
		return err
	}

	return nil
}
