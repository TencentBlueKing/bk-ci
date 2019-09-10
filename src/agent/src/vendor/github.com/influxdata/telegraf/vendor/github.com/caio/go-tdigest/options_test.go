package tdigest

import "testing"

func TestDefaults(t *testing.T) {
	digest, err := New()

	if err != nil {
		t.Errorf("Creating a default TDigest should never error out. Got %s", err)
	}

	if digest.compression != 100 {
		t.Errorf("The default compression should be 100")
	}
}

func TestCompression(t *testing.T) {
	digest, _ := New(Compression(40))
	if digest.compression != 40 {
		t.Errorf("The compression option should change the new digest compression")
	}

	digest, err := New(Compression(0))
	if err == nil || digest != nil {
		t.Errorf("Trying to create a digest with bad compression should give an error")
	}
}

func TestRandomNumberGenerator(t *testing.T) {
	const numTests = 100

	// Create two digests with unshared rngs seeded with
	// the same seed
	t1, _ := New(RandomNumberGenerator(newLocalRNG(0xDEADBEE)))
	t2, _ := New(LocalRandomNumberGenerator(0xDEADBEE))

	// So that they should emit the same values when called
	// at the same frequency
	for i := 0; i < numTests; i++ {
		if t1.rng.Float32() != t2.rng.Float32() ||
			t1.rng.Intn(10) != t2.rng.Intn(10) {
			t.Errorf("r1 and r2 should be distinct RNGs returning the same values")
		}
	}
}
