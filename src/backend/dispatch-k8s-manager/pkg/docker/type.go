package docker

type ImagePullPolicyEnum string

const (
	ImagePullPolicyAlways       ImagePullPolicyEnum = "always"
	ImagePullPolicyIfNotPresent ImagePullPolicyEnum = "if-not-present"
)

func (i ImagePullPolicyEnum) String() string {
	return string(i)
}
