package com.rtb.k8gen.ai.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtb.k8gen.model.DeploymentConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

public class K8ConfigGeneratorAgent {

    private final ChatModel model;

    public K8ConfigGeneratorAgent(String apiKey, String modelName) {
        this.model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
//                .temperature(0.2)
                .build();
    }


    public String generateConfigs(DeploymentConfig deploymentConfig, Integer version) throws JsonProcessingException {

        String prompt = generatePrompt(deploymentConfig, version);
        return model.chat(prompt);
    }

    public String generatePrompt(DeploymentConfig deploymentConfig, Integer version) throws JsonProcessingException {

        String inputConfig = new ObjectMapper().writeValueAsString(deploymentConfig);

        if (version == null || version == 1) {
            return """
                    #Role:
                    You are an expert DevOps assistant. Generate production-ready container and Kubernetes manifests for a Spring Boot application. Follow platform best practices for security, observability, resource constraints, and operational concerns.
                    
                    #Objective:
                    From the provided Inputs produce all files required to build, run, and deploy the application:
                    - Dockerfile (multi-stage, cache-friendly)
                    - .dockerignore
                    - docker-compose.yml (optional DB and extra images)
                    - Kubernetes YAML manifests: Namespace, ConfigMap, Secret, Deployment, Service, Ingress, PVC (if DB persists), HPA (optional), PodDisruptionBudget (optional), ServiceAccount & RBAC (optional), NetworkPolicy (optional), initContainer for migrations (optional).
                    
                    #Context:
                    The user will call this prompt with a JSON `inputs` object. The assistant must consume inputs, apply defaults and validation rules, and return a single string containing every file. No extra text or commentary outside the prescribed file markers. If a parameter is absent use safe defaults described below.
                    
                    #Instructions:
                    
                    ##Instruction1: Input validation & normalization
                    - Validate `applicationName` to a DNS-1123 label: lower-case, alphanumerics and `-`. Replace invalid chars with `-`. Trim to 253 chars.
                    - Validate `port` in 1..65535.
                    - Validate `javaVersion` and `gradleVersion` format; if unrecognized fall back to defaults: javaVersion="21", gradleVersion="8.9".
                    - Validate resource values; if absent use: cpuRequest="250m", memoryRequest="512Mi", cpuLimit="500m", memoryLimit="1Gi".
                    - For any secret with null value generate a secure random 32-character base64 string.
                    
                    ##Instruction2: File output format and extraction pattern (MANDATORY)
                    - Return one single code-block string. Inside that string present files in the exact order defined in "Files to produce".
                    - Use this exact file delimiter format so automated parsers can extract files reliably:
                    
                    -----BEGIN_FILE: <path>-----
                    <file content raw, no extra wrapper>
                    -----END_FILE: <path>-----
                    
                    - Extraction regex example (for implementer):
                      `(?ms)^-----BEGIN_FILE:*(?P<filename>.+?)*-----\\n(?P<content>.*?)\\n-----END_FILE:*(?P=filename)*-----$`
                    - No other lines or commentary outside those BEGIN/END blocks.
                    
                    ##Instruction3: Dockerfile (multi-stage)
                    - Use a build stage with official Gradle image matching `gradleVersion` or use `gradle:alpine` with build arguments.
                    - Use a second stage with a small JRE runtime. Prefer Eclipse Temurin or distroless for final stage if javaVersion >= 17. Example base: `eclipse-temurin:{javaVersion}-jdk` for build stage and `eclipse-temurin:{javaVersion}-jre` or `gcr.io/distroless/java` for runtime.
                    - Mount Gradle cache directories as build cache layers to speed CI builds. Use `--mount=type=cache` when using BuildKit.
                    - Copy only necessary files for build to improve Docker cache.
                    - Build an executable fat JAR via `./gradlew bootJar` or `./gradlew assemble` depending on project type.
                    - Create a non-root user and run as that user.
                    - Add HEALTHCHECK that probes the actuator liveness endpoint: default `CMD curl --fail http://localhost:{port}/actuator/health/liveness || exit 1`
                    - Accept build args: `GRADLE_VERSION`, `JAVA_VERSION`, `APP_HOME=/app`, `JAR_FILE`, `BUILD_ARGS`.
                    - Add a short commented block with example build and push commands (comments are allowed inside file content).
                    
                    ##Instruction4: .dockerignore
                    - Exclude Gradle caches, `.git`, `.idea`, `.gradle`, `build`, `target`, local env files, logs and node_modules.
                    - Keep pattern minimal but effective.
                    
                    ##Instruction5: docker-compose.yml
                    - Create services:
                      - `app` service built from Dockerfile with environment variables wired from `configd` and `secrets` (use `.env` for compose or `environment:` with ${VAR} placeholders).
                      - Optional DB service when `includeDatabase` true: postgres image, volumes, healthcheck, env for POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD.
                      - Additional services from `additionalDockerImages`.
                    - Add recommended `depends_on` with healthcheck condition if compose v2.4+ supports it.
                    - Provide an `.env.example` section in a separate file marker if secrets present. (Do not place real secret values in the sample.)
                    
                    ##Instruction6: Kubernetes manifests - general
                    - All resources must use labels: `app: <applicationName>`, `component: backend`.
                    - Use `metadata.annotations` for Prometheus scraping when appropriate.
                    - Use `imagePullPolicy: IfNotPresent` by default. If `imageTag` is `latest` set `Always`.
                    - Use `envFrom` for ConfigMap and Secret injection where appropriate and also show individual env var examples for JDBC URL and JAVA_TOOL_OPTIONS.
                    - Supply `livenessProbe` and `readinessProbe` that target actuator endpoints:
                      - readiness: `/actuator/health/readiness` with initialDelaySeconds 15, periodSeconds 10, failureThreshold 3.
                      - liveness: `/actuator/health/liveness` with initialDelaySeconds 30, periodSeconds 20, failureThreshold 5.
                    - Mount JVM opts through `JAVA_TOOL_OPTIONS` env var. Example memory flags using `-Xms` and `-Xmx` based on memoryRequest/limit.
                    - Provide `startupProbe` when app has long initialization (optional when migrations run).
                    
                    ##Instruction7: Kubernetes Secret
                    - Create `secret.yaml`. Encode values using base64. If any secret value supplied plaintext, encode it.
                    - For docker-compose include secrets in `.env` only if user explicitly wants; otherwise leave placeholder tokens.
                    
                    ##Instruction8: ConfigMap
                    - Create `configmap.yaml` for all `configd` entries. Keep values as plain strings. Add fallback `SPRING_PROFILES_ACTIVE` default to "prod" unless user specifies otherwise.
                    - If a config value length exceeds 1Mi, place in a mounted file instead of ConfigMap data.
                    
                    ##Instruction9: Deployment
                    - Create `deployment.yaml` with:
                      - `replicas` from inputs.
                      - `resources.requests` and `limits` from inputs or defaults.
                      - `readiness`, `liveness`, optional `startupProbe`.
                      - `securityContext` with `runAsNonRoot: true` and `runAsUser`.
                      - `imagePullSecrets` only if `imageRegistry` requires it; otherwise omit.
                      - `envFrom` for ConfigMap and Secret along with explicit `env` for `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
                      - Example `affinity` and `tolerations` minimal templates for production clusters.
                    
                    ##Instruction10: Service
                    - Create ClusterIP `service.yaml` exposing the application on `port` and targeting containerPort.
                    - Add `annotations` if load balancer IP, internal-only, or sessionAffinity required.
                    
                    ##Instruction11: Ingress
                    - Create `ingress.yaml` with host-based routing if `ingressHost` provided.
                    - Include TLS stanza when `tlsSecretName` provided.
                    - Add common ingress controller annotations for NGINX and Traefik as optional blocks; choose which to include based on an `ingressController` input if present, otherwise default NGINX annotation set.
                    
                    ##Instruction12: Persistence for DB
                    - If `includeDatabase` true, create `pvc.yaml` and `postgres-deployment.yaml` or a single StatefulSet depending on `dbPersistenceStrategy` (default: PVC + Deployment).
                    - Use `storageClassName` only if provided; otherwise leave it unset for dynamic provisioning.
                    
                    ##Instruction13: Migrations (initContainer)
                    - If `migrations` provided add an initContainer to the application Deployment that runs the migration image or built jar command before main container starts.
                    - Ensure initContainer uses same network and has correct DB env vars.
                    
                    ##Instruction14: Horizontal Pod Autoscaler
                    - If `enableHPA` true produce `hpa.yaml` using `metrics` on CPU or custom metrics if provided.
                    - Use `minReplicas` and `maxReplicas` from inputs.
                    
                    ##Instruction15: PodDisruptionBudget, RBAC, ServiceAccount
                    - If requested via `extraK8sResources`, emit:
                      - `pdb.yaml` with `minAvailable` or `maxUnavailable`.
                      - `serviceaccount.yaml` and `role.yaml` / `rolebinding.yaml` if necessary for secrets or cluster-level access.
                    - Keep RBAC minimal and least-privilege.
                    
                    ##Instruction16: NetworkPolicy
                    - If requested, generate `networkpolicy.yaml` restricting ingress to known namespaces and egress to DB service and required external endpoints.
                    
                    ##Instruction17: Observability & Logging
                    - Add pod annotations or sidecar template for Prometheus scraping.
                    - Provide mmap-friendly log configuration: ensure Spring Boot logs to stdout/stderr, not files.
                    - Add recommended `terminationGracePeriodSeconds: 30` and `preStop` hook to allow graceful shutdown.
                    
                    ##Instruction18: Image tags & build metadata
                    - Compose image name from `imageRegistry` + `applicationName` + `imageTag`. If `imageTag` not provided use timestamp placeholder `{{BUILD_TIMESTAMP}}`.
                    - Add commented example commands for CI: build, tag, push, kubectl apply.
                    
                    ##Instruction19: Defaults & fallbacks
                    - If `includeDatabase` missing assume true and default to `postgres:16`.
                    - If `ingressHost` missing produce manifests without Ingress.
                    - For missing secrets generate secure random values, but mark them in the docker-compose `.env.example` as generated values (do not leak secrets inside the prompt result).
                    
                    ##Instruction20: Final output constraints
                    - Do not include any explanatory prose outside file markers.
                    - Do not include raw plaintext secrets outside `secret.yaml` base64 values and docker-compose `.env` only when user requested.
                    - Keep YAML valid and compact. Use `apiVersion` and `kind` appropriate for Kubernetes stable releases (use apps/v1 for Deployments, networking.k8s.io/v1 for Ingress).
                    - Order files to be applied safely: namespace -> configmap/secret -> pvc -> serviceaccount/rbac -> service -> deployment -> ingress -> hpa -> pdb.
                    
                    #Files to produce. No text outside. Follow file order strictly.
                    1. Dockerfile
                    2. .dockerignore
                    3. docker-compose.yml
                    4. .env (if secrets exist)
                    5. namespace.yaml
                    6. configmap.yaml
                    7. secret.yaml
                    8. pvc.yaml (if DB persistence)
                    9. serviceaccount.yaml (if requested)
                    10. role.yaml & rolebinding.yaml (if requested)
                    11. service.yaml
                    12. deployment.yaml
                    13. postgres-deployment-or-statefulset.yaml (if includeDatabase)
                    14. ingress.yaml (if ingressHost provided)
                    15. hpa.yaml (if enableHPA)
                    16. pdb.yaml (if requested)
                    17. networkpolicy.yaml (if requested)
                    18. README_AUTOMATION.md (small file with one-line instructions for CI to build/push/apply)
                    
                    #Notes:
                    1. Always return output using the exact BEGIN/END file markers. Parsers will rely on them.
                    2. Keep comments inside produced files to a minimum and only for operational commands (build/push/apply examples).
                    3. Do not attempt to call external systems. Generate manifests only from given inputs and safe defaults.
                    4. Prefer declarative, idempotent manifests. Avoid imperative commands embedded in YAML.
                    5. Recommend using external secret stores (Vault, SealedSecrets, ExternalSecrets) for production. Include an optional commented block showing how to reference external secrets if user provides a `externalSecrets` flag.
                    6. When in doubt about a parameter, apply the default and annotate the generated file (comment) which default was used.
                    7. Ensure all generated YAML is valid for Kubernetes v1.26+ (use stable API groups).
                    8. Provide minimal examples for CI commands as commented lines inside relevant files. Do not output CI scripts outside file markers.
                    9. The assistant must not ask clarification questions. If inputs are ambiguous or missing use defaults described above.
                    10. Output must be machine-extractable. No trailing characters outside final END_FILE marker.
                    
                    Inputs:
                    """
                    + inputConfig + "\n" + """
                    -- End of prompt --
                    """;
        }

        if (version == 2) {

            return """
                    #Role
                    Expert DevOps assistant. Generate production-ready container and Kubernetes manifests for a Spring Boot app with secure defaults, observability, resource limits, and ops best practices.
                    
                    #Objective
                    From JSON `inputs` produce all files to build, run, and deploy:
                    - Dockerfile (multi-stage, cache-friendly)
                    - .dockerignore
                    - docker-compose.yml (+ optional DB/extra images)
                    - Kubernetes YAMLs: Namespace, ConfigMap, Secret, Deployment, Service, Ingress, PVC (if DB), HPA (if enabled), PodDisruptionBudget (if requested), ServiceAccount & RBAC (if requested), NetworkPolicy (if requested), initContainer for migrations (if provided).
                    
                    #Context
                    Consume inputs, apply defaults/validation, and output one string containing all files. No text outside file markers.
                    
                    #Instructions
                    
                    **Validation**
                    - `applicationName`: DNS-1123, lower-case, alphanum + `-`, max 253 chars.
                    - `port`: 1–65535.
                    - `javaVersion`, `gradleVersion`: fall back to java=21, gradle=8.9.
                    - Resources: default cpuReq=250m, memReq=512Mi, cpuLim=500m, memLim=1Gi.
                    - Null secrets → random 32-char base64.
                    
                    **File output format (mandatory)**
                    - Wrap each file with markers:
                    -----BEGIN_FILE: <path>-----
                    <content>
                    -----END_FILE: <path>-----
                    - No text outside these blocks. Files in defined order only.
                    
                    **Dockerfile**
                    - Build stage: official Gradle `{gradleVersion}` (alpine fallback).
                    - Runtime: Eclipse Temurin JRE `{javaVersion}` or distroless if ≥17.
                    - Use BuildKit cache mounts. Copy minimal files. Run `./gradlew bootJar`.
                    - Non-root user. HEALTHCHECK on `/actuator/health/liveness` at `{port}`.
                    - Accept build args. Include short comment with example build/push.
                    
                    **.dockerignore**
                    - Exclude caches, VCS, IDE files, build dirs, env files, logs, node_modules.
                    
                    **docker-compose**
                    - `app` service from Dockerfile. Env from config and secrets.
                    - Optional Postgres if `includeDatabase` true. Add volumes and healthcheck.
                    - Include `additionalDockerImages`.
                    - Add `.env.example` if secrets exist (no real values).
                    
                    **Kubernetes general**
                    - Place under ``.
                    - Labels: `app=<applicationName>`, `component=backend`.
                    - Add Prometheus annotations.
                    - imagePullPolicy: IfNotPresent (Always if tag=latest).
                    - Inject ConfigMap/Secret with `envFrom`. Explicit env vars for JDBC + JAVA_TOOL_OPTIONS.
                    - Probes: readiness `/actuator/health/readiness` (15s delay), liveness `/actuator/health/liveness` (30s delay). Optional startupProbe.
                    - Set JVM memory flags from requests/limits.
                    
                    **Secrets**
                    - `secret.yaml`. Encode all values base64.
                    
                    **ConfigMap**
                    - `configmap.yaml` with config values. Default `SPRING_PROFILES_ACTIVE=prod`. Large values → mounted files.
                    
                    **Deployment**
                    - Replicas from inputs. Use resources/defaults. Add probes.
                    - `securityContext`: runAsNonRoot.
                    - Add imagePullSecrets if registry requires.
                    - Inject env from ConfigMap + Secret, plus explicit DB vars.
                    - Show affinity/tolerations template.
                    
                    **Service**
                    - ClusterIP exposing `{port}`. Optional annotations.
                    
                    **Ingress**
                    - If `ingressHost` provided. Add TLS if `tlsSecretName`. Default NGINX annotations unless controller specified.
                    
                    **Database persistence**
                    - If `includeDatabase`, create `pvc.yaml` and Postgres Deployment or StatefulSet (`dbPersistenceStrategy`, default PVC+Deployment).
                    
                    **Migrations**
                    - If `migrations` provided, add initContainer running migration job.
                    
                    **HPA**
                    - If `enableHPA`, create `hpa.yaml` with CPU/custom metrics. Use `minReplicas`/`maxReplicas`.
                    
                    **PDB, RBAC, SA**
                    - If requested, output `pdb.yaml`, `serviceaccount.yaml`, `role.yaml`, `rolebinding.yaml`. Keep least-privilege.
                    
                    **NetworkPolicy**
                    - If requested, restrict ingress to trusted namespaces, egress to DB and required endpoints.
                    
                    **Observability**
                    - Prometheus scrape annotations. Logs to stdout/stderr.
                    - terminationGracePeriod=30s and preStop hook.
                    
                    **Image & CI**
                    - Image name: `{registry}/{app}:{tag}` (tag default `{{BUILD_TIMESTAMP}}`).
                    - Add commented build/push/kubectl apply examples.
                    
                    **Defaults**
                    - `includeDatabase` default true, Postgres 16.
                    - If `ingressHost` missing, omit Ingress.
                    - Missing secrets → generate base64. `.env.example` shows placeholders.
                    
                    **Constraints**
                    - No prose outside file markers.
                    - No plaintext secrets outside base64 in Secret and placeholders in `.env`.
                    - Valid YAML for k8s v1.26+.
                    - File apply order: namespace → configmap/secret → pvc → rbac/sa → service → deployment → ingress → hpa → pdb.
                    
                    #Files to produce (order)
                    1. Dockerfile
                    2. .dockerignore
                    3. docker-compose.yml
                    4. .env (if secrets)
                    5. namespace.yaml
                    6. configmap.yaml
                    7. secret.yaml
                    8. pvc.yaml (if DB)
                    9. serviceaccount.yaml (if requested)
                    10. role.yaml & rolebinding.yaml (if requested)
                    11. service.yaml
                    12. deployment.yaml
                    13. postgres-deployment-or-statefulset.yaml (if DB)
                    14. ingress.yaml (if ingressHost)
                    15. hpa.yaml (if enabled)
                    16. pdb.yaml (if requested)
                    17. networkpolicy.yaml (if requested)
                    18. README_AUTOMATION.md (1-line CI usage)
                    
                    Inputs:
                    
                    """ +
                    inputConfig + "\n";
        }

        if (version == 3) {

            return """
            #Role
            DevOps assistant. Generate container + Kubernetes manifests for Spring Boot with secure defaults, observability, resource limits.
            #Output format
            Return one code block string. Each file wrapped in markers:
            -----BEGIN_FILE: <path>-----
            <content>
            -----END_FILE: <path>---
            No text outside. Follow file order strictly.
               #Output files
               1. Dockerfile
               2. .dockerignore
               3. docker-compose.yml
               4. .env (if secrets)
               5. namespace.yaml
               6. configmap.yaml
               7. secret.yaml
               8. pvc.yaml (if DB)
               9. serviceaccount.yaml (if requested)
               10. role.yaml & rolebinding.yaml (if requested)
               11. service.yaml
               12. deployment.yaml
               13. postgres.yaml (if DB)
               14. ingress.yaml (if ingressHost)
               15. hpa.yaml (if enabled)
               16. pdb.yaml (if requested)
               17. networkpolicy.yaml (if requested)
               18. README_AUTOMATION.md
            
               #Key rules
               - Validate inputs (name, port, versions, resources). Defaults: java=21, gradle=8.9, cpuReq=250m, memReq=512Mi, cpuLim=500m, memLim=1Gi.
               - Secrets → base64 (random if null).
               - Use multi-stage Dockerfile, cache-friendly, non-root, HEALTHCHECK.
               - .dockerignore excludes caches, VCS, builds, env files.
               - Compose: app + optional Postgres, extra images. `.env.example` only placeholders.
               - K8s: namespace → config/secret → pvc → sa/rbac → service → deployment → ingress → hpa → pdb.
               - Deployment: replicas, probes, resources, envFrom, securityContext.
               - Service: ClusterIP on `{port}`.
               - Ingress: host, TLS if provided.
               - DB: PVC + Postgres deployment by default.
               - Optional: initContainer (migrations), HPA, PDB, SA/RBAC, NetworkPolicy.
               - Logs to stdout/stderr, Prometheus scrape annotations.
               - Image name: `{registry}/{app}:{tag}` (default `{{BUILD_TIMESTAMP}}`).
               - Only minimal comments for CI build/apply.
            Inputs:
            
            """ + inputConfig;
        }

        return "";
    }

}
