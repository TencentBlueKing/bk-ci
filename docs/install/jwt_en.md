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

---

## JWT Key Rotation Guide (Emergency Response for Key Compromise)

### Background
JWT flow in BK-CI: **Gateway ➡ Microservices ⬅➡ Turbo (if applicable)**
- **Gateway**: Signs JWT tokens using the private key
- **Microservices**: Validates JWT tokens using the public key
- **Turbo**: Validates JWT tokens using the public key

The system supports multiple key configurations, enabling key rotation without service interruption.

### Key Rotation Process

#### Phase 1: Add New Key (Dual-Key Coexistence)

**Objective**: Enable all services to support both old and new keys for smooth transition.

1. **Generate New Key Pair**
   ```bash
   ./scripts/bk-ci-gen-jwt-env.sh
   ```
   Save the output as `NEW_PRIVATE_KEY` and `NEW_PUBLIC_KEY`.

2. **Update Configuration Files in Order**

   **2.1 Update Microservices Configuration (support-files/templates/#etc#ci#common.yml)**
   
   Add new key configuration in `bkci.security.properties` while keeping the old key:
   ```yaml
   bkci:
     security:
       auth-enable: "__BK_CI_JWT_ENABLE__"
       properties:
         - kid: "devops"  # Old key, keep active: true
           public-key: "__BK_CI_JWT_RSA_PUBLIC_KEY__"
           private-key: "__BK_CI_JWT_RSA_PRIVATE_KEY__"
           active: true
         - kid: "devops-new"  # New key, set active: false temporarily
           public-key: "NEW_PUBLIC_KEY"
           private-key: "NEW_PRIVATE_KEY"
           active: false
   ```

   **2.2 Update Turbo Configuration (if applicable)**
   
   Similarly add the new key in Turbo configuration (refer to helm-charts/core/ci/templates/configmap/turbo-configmap.yaml).

3. **Service Restart Order**

   **Important Principle**: Restart validators (microservices, Turbo) first, then restart issuer (gateway)
   
   ```bash
   # Step 1: Restart all microservices (in any order)
   kubectl rollout restart deployment -l app.kubernetes.io/component=microservice
   
   # Step 2: Restart Turbo service (if applicable)
   kubectl rollout restart deployment -l app.kubernetes.io/component=microservice-turbo
   
   # Step 3: Wait for all microservices and Turbo to be ready
   kubectl rollout status deployment -l app.kubernetes.io/component=microservice
   kubectl rollout status deployment -l app.kubernetes.io/component=microservice-turbo
   
   # Now microservices support both old and new public keys for validation,
   # but gateway still uses old private key for signing
   ```

4. **Verify Phase 1 Completion**
   - Confirm all microservices and Turbo are running normally
   - Confirm existing business requests work (gateway still uses old key, microservices can validate)

#### Phase 2: Switch to New Key

**Objective**: Switch signing key to new key while maintaining old key validation capability.

5. **Update Configuration to Enable New Key**

   **5.1 Update Microservices Configuration**
   
   Set new key as active:
   ```yaml
   bkci:
     security:
       properties:
         - kid: "devops"  # Old key, change to active: false
           public-key: "__BK_CI_JWT_RSA_PUBLIC_KEY__"
           private-key: "__BK_CI_JWT_RSA_PRIVATE_KEY__"
           active: false
         - kid: "devops-new"  # New key, change to active: true
           public-key: "NEW_PUBLIC_KEY"
           private-key: "NEW_PRIVATE_KEY"
           active: true
   ```

   **5.2 Update Gateway Configuration (support-files/templates/gateway#core#lua#init.lua)**
   
   Update gateway's private key and kid:
   ```lua
   jwtPrivateKey = "NEW_PRIVATE_KEY",
   jwtKid= "devops-new",
   ```

6. **Service Restart Order**

   **Important Principle**: Restart issuer (gateway) first, then restart validators (microservices)
   
   ```bash
   # Step 1: Restart gateway
   kubectl rollout restart deployment gateway
   kubectl rollout status deployment gateway
   
   # Step 2: Restart all microservices
   kubectl rollout restart deployment -l app.kubernetes.io/component=microservice
   kubectl rollout status deployment -l app.kubernetes.io/component=microservice
   
   # Step 3: Restart Turbo (if applicable)
   kubectl rollout restart deployment -l app.kubernetes.io/component=microservice-turbo
   kubectl rollout status deployment -l app.kubernetes.io/component=microservice-turbo
   ```

7. **Verify Phase 2 Completion**
   - Confirm gateway signs tokens with new key
   - Confirm microservices validate tokens with new key
   - Confirm all business requests work normally

#### Phase 3: Remove Old Key (Optional)

**Objective**: Completely remove the compromised old key.

**Recommended Wait Time**: After Phase 2 completion, wait **24-48 hours** to ensure all requests using old tokens have expired.

8. **Remove Old Key Configuration**

   **8.1 Update Microservices Configuration**
   
   Delete old key configuration:
   ```yaml
   bkci:
     security:
       auth-enable: "__BK_CI_JWT_ENABLE__"
       properties:
         - kid: "devops-new"  # Keep only new key, do not change kid
           public-key: "NEW_PUBLIC_KEY"
           private-key: "NEW_PRIVATE_KEY"
           active: true
   ```

9. **Restart Services**
   ```bash
   # Restart all services in any order
   kubectl rollout restart deployment -l app.kubernetes.io/component=microservice
   kubectl rollout restart deployment -l app.kubernetes.io/component=microservice-turbo
   ```

### Key Considerations

1. **Rollback Plan**
   - If Phase 2 encounters issues, quick rollback:
     - Change old key's `active` back to `true` in microservices configuration
     - Revert gateway configuration to old private key
     - Restart gateway and microservices

2. **Validation Methods**
   - Check gateway logs to confirm JWT signing is normal
   - Check microservices logs to confirm JWT validation is normal
   - Execute business operations to confirm end-to-end flow works
