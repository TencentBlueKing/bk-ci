package authz

import (
	"net/http"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestIdentitySignatureRoundTrip(t *testing.T) {
	h := http.Header{}
	h.Set(HeaderUserID, "alice")
	h.Set(HeaderProjectID, "proj-a")
	now := time.Unix(1_700_000_000, 0)
	AttachIdentitySignature(h, now)
	caller := Caller{UserID: "alice", ProjectID: "proj-a"}
	assert.NoError(t, VerifyIdentitySignature(h, caller, now))
	assert.NoError(t, VerifyIdentitySignature(h, caller, now.Add(4*time.Minute)))
	assert.ErrorIs(t, VerifyIdentitySignature(h, caller, now.Add(6*time.Minute)), ErrUntrustedIdentity)

	h.Set(HeaderIdentitySig, "tampered")
	assert.ErrorIs(t, VerifyIdentitySignature(h, caller, now), ErrUntrustedIdentity)
}

func TestCallerFromHeaderDropsUnsignedAndBadSig(t *testing.T) {
	h := http.Header{}
	h.Set(HeaderUserID, "alice")
	h.Set(HeaderProjectID, "proj-a")
	assert.True(t, CallerFromHeader(h).IsEmpty())

	AttachIdentitySignature(h, time.Now())
	got := CallerFromHeader(h)
	assert.Equal(t, "alice", got.UserID)
	assert.Equal(t, "proj-a", got.ProjectID)

	h.Set(HeaderIdentitySig, "bad")
	assert.True(t, CallerFromHeader(h).IsEmpty())
}

func TestAttachIdentitySignatureSkipsEmptyCaller(t *testing.T) {
	h := http.Header{}
	AttachIdentitySignature(h, time.Now())
	assert.Empty(t, h.Get(HeaderIdentitySig))
}
