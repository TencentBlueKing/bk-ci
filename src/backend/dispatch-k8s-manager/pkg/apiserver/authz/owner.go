package authz

import (
	"regexp"
	"strings"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

var invalidLabelChar = regexp.MustCompile(`[^A-Za-z0-9._-]`)

// AuthorizeObject 校验调用方是否对目标对象具备属主/租户权限。默认拒绝无属主对象。
// 仅用于 debug/terminal 与 namespace 资源（deployment/secret）硬拒绝面。
func AuthorizeObject(caller Caller, owner Owner) error {
	if caller.IsEmpty() {
		return ErrMissingIdentity
	}
	if owner.IsEmpty() {
		return ErrObjectUnowned
	}
	if owner.TenantID != "" && caller.TenantID != "" && owner.TenantID != caller.TenantID {
		return ErrForbidden
	}
	if owner.ProjectID != "" && caller.ProjectID != "" && owner.ProjectID != caller.ProjectID {
		return ErrForbidden
	}
	matched := (owner.TenantID != "" && caller.TenantID == owner.TenantID) ||
		(owner.ProjectID != "" && caller.ProjectID == owner.ProjectID) ||
		(owner.UserID != "" && caller.UserID == owner.UserID)
	if !matched {
		return ErrForbidden
	}
	return nil
}

// AuthorizeObjectIfOwned 分级策略：无属主或调用方未带身份则放行。
// 用于 getBuilderStatus/stop/delete，避免灰度期无属主的在跑构建被 default-deny 打挂。
// 新创建的 builder 已打属主 label，有自称身份时仍做匹配。
func AuthorizeObjectIfOwned(caller Caller, owner Owner) error {
	if owner.IsEmpty() || caller.IsEmpty() {
		return nil
	}
	return AuthorizeObject(caller, owner)
}

func OwnerFromMetadata(meta metav1.ObjectMeta) Owner {
	return OwnerFromMap(meta.Labels).Fill(OwnerFromMap(meta.Annotations))
}

func OwnerFromMap(m map[string]string) Owner {
	if m == nil {
		return Owner{}
	}
	return Owner{
		UserID:    firstNonEmpty(m[LabelUserID], m["userId"], m["user-id"]),
		ProjectID: firstNonEmpty(m[LabelProjectID], m["projectId"], m["project-id"]),
		TenantID:  firstNonEmpty(m[LabelTenantID], m["tenantId"], m["tenant-id"]),
	}
}

func OwnerFromEnvMap(env map[string]string) Owner {
	if env == nil {
		return Owner{}
	}
	return Owner{
		UserID:    firstNonEmpty(env[EnvUserID]),
		ProjectID: firstNonEmpty(env[EnvProjectID]),
		TenantID:  firstNonEmpty(env[EnvTenantID]),
	}
}

func OwnerFromPod(pod *corev1.Pod) Owner {
	if pod == nil {
		return Owner{}
	}
	owner := OwnerFromMetadata(pod.ObjectMeta)
	for _, c := range pod.Spec.Containers {
		owner = owner.Fill(ownerFromEnvVars(c.Env))
	}
	return owner
}

func ownerFromEnvVars(env []corev1.EnvVar) Owner {
	m := map[string]string{}
	for _, e := range env {
		if e.Value != "" {
			m[e.Name] = e.Value
		}
	}
	return OwnerFromEnvMap(m)
}

func ApplyOwnerLabels(dst map[string]string, owner Owner) map[string]string {
	if dst == nil {
		dst = map[string]string{}
	}
	if owner.UserID != "" {
		dst[LabelUserID] = SanitizeLabelValue(owner.UserID)
	}
	if owner.ProjectID != "" {
		dst[LabelProjectID] = SanitizeLabelValue(owner.ProjectID)
	}
	if owner.TenantID != "" {
		dst[LabelTenantID] = SanitizeLabelValue(owner.TenantID)
	}
	return dst
}

func ApplyOwnerAnnotations(dst map[string]string, owner Owner) map[string]string {
	if dst == nil {
		dst = map[string]string{}
	}
	if owner.UserID != "" {
		dst[LabelUserID] = owner.UserID
	}
	if owner.ProjectID != "" {
		dst[LabelProjectID] = owner.ProjectID
	}
	if owner.TenantID != "" {
		dst[LabelTenantID] = owner.TenantID
	}
	return dst
}

// SanitizeLabelValue 将属主字段收敛为合法 Kubernetes label value。
func SanitizeLabelValue(v string) string {
	v = strings.TrimSpace(v)
	v = invalidLabelChar.ReplaceAllString(v, "-")
	v = strings.Trim(v, "-._")
	if len(v) > 63 {
		v = v[:63]
		v = strings.Trim(v, "-._")
	}
	return v
}
