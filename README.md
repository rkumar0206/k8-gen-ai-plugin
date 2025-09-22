# Gradle plugin to generate Dockerfile, docker-compose.yml and K8s config all in once for your springboot application.

### Note: 
    AI used for generating all the files. 
    
## What is sent to the AI?
- Information you provide using the json file below.
- gradle version
- java version
- env variables defined in springboot properties file.
        

## Requirements
GEMINI_API_KEY as env variables. <b>(API KEY is not stored anywhere in the code.)<b>

## How to use/configure this plugin?

1. In you springboot application's `build.gradle` add below configuration:
 ```build.gradle

plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.3'
    id 'io.spring.dependency-management' version '1.1.7'
    id "io.github.rkumar0206.k8gen" version "0.0.1"
}

k8Gen {
    outputDir.set("/k8s")
    jsonConfigFilePath.set("k8-gen-config.json")
    model.set("gemini-2.5-flash")
}
```
2. Create `k8-gen-config.json` file in root directory of your project.
- Use below json to generate files according to your need

```json

{
  "gradleVersion": "8.9",
  "javaVersion": "21",
  "additionalDockerImages": [
    {
      "name": "redis",
      "image": "redis:7.2",
      "role": "cache",
      "ports": ["6379:6379"]
    }
  ],
  "applicationName": "utility-service",
  "dbUsername": "utility_user",
  "dbPassword": "S3cur3P@ssw0rd",
  "dbName": "utilitydb",
  "port": 8080,
  "replicas": 3,
  "namespace": "production",
  "imageRegistry": "registry.example.com/team",
  "imageTag": "1.0.0",
  "enableHPA": true,
  "hpaMinReplicas": 2,
  "hpaMaxReplicas": 6,
  "cpuRequest": "250m",
  "memoryRequest": "512Mi",
  "cpuLimit": "500m",
  "memoryLimit": "1Gi",
  "ingressHost": "utility.example.com",
  "tlsSecretName": "utility-tls",
  "includeDatabase": true,
  "migrations": {
    "tool": "flyway",
    "image": "flyway/flyway:9.22",
    "args": [
      "-url=jdbc:postgresql://postgres:5432/utilitydb",
      "-user=utility_user",
      "-password=S3cur3P@ssw0rd",
      "migrate"
    ]
  },
  "secrets": {
    "JWT_SECRET": null,
    "API_KEY": "12345-ABCDE"
  },
  "configd": {
    "SPRING_PROFILES_ACTIVE": "prod",
    "LOGGING_LEVEL_ROOT": "INFO"
  },
  "extraK8sResources": ["pdb", "networkPolicy", "rbac", "serviceAccount"]
}
```
