# Springboot plugin to generate Dockerfile, docker-compose.yml and K8s config all in once.

## Requirements
GEMINI_API_KEY as env varaiables

## How to use/configure this plugin?

 1. In you springboot application buid.gradle add below configuration:
 ```build.gradle

plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.3'
    id 'io.spring.dependency-management' version '1.1.7'
    id "com.rtb.k8gen" version "0.0.1"
}

k8Gen {
    outputDir.set("/k8s")
    jsonConfigFilePath.set("k8-gen-config.json")
    model.set("gemini-2.5-flash")
}
```
