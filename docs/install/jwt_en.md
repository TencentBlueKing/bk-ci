## JWT Configuration Guide

### 1. Generate JWT Key Pair
Run the following script to generate the RSA key pair required for JWT:
```bash
./scripts/bk-ci-gen-jwt-env.sh
```

### 2. Configure values.yaml
In the `values.yaml` file, perform the following configurations:
- Enable JWT validation: Set `bkCiJwtEnable` to `true`.
- Fill in the generated keys: Enter the private and public keys generated in the previous step into `bkCiJwtRsaPrivateKey` and `bkCiJwtRsaPublicKey`, respectively.

### 3. Deploy or Update the Service
- **Initial Deployment**: Run the Helm deployment command directly.
- **Service Update**: After running `helm upgrade`, restart all Deployments.
  **Note**: The restart process will cause a brief service interruption, so plan the update time accordingly.
