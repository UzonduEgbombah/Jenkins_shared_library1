﻿# YouTube Clone App with DevSecOps and Jenkins Shared Library

![](https://github.com/user-attachments/assets/bf4b6196-6eb4-49e4-838a-6472ce53fb1d)


With the following tool set :

- Haproxy (load balancer)

- Kubernetes

- Docker,

- SonarQube, 

- Trivy, 

- OWASP Dependency Check,

- Prometheus, 

- Grafana, 

- Jenkins (and a shared library), 

- Splunk, Rapid API, 

- Slack notifications

With the following tools above I was able to deploy a highly available youtube clone application


### ci

![](https://github.com/user-attachments/assets/7702fc74-2465-4a7b-813b-0e511f0a7fea)





### slack notification

![](https://github.com/user-attachments/assets/fe5baf67-128e-4347-9eea-8b693f0860e8)





### splunk (jenkins ci)

![](https://github.com/user-attachments/assets/a9f78ba4-09a3-409e-8e40-70d24ad38b48)





### sonarqube integration

![](https://github.com/user-attachments/assets/8a605550-4085-496e-93c4-ca2261312496)





### owasp dependency check

![](https://github.com/user-attachments/assets/ae4a8ba4-cad7-42d3-bcda-1103fbdcd726)





### 3 master node and 2 worker node for high availability of kubernetes cluster

![](https://github.com/user-attachments/assets/ca088920-ca41-4a74-8fe4-a6d5c1563485)




### Ha-proxy

To set up a highly available Kubernetes cluster with three master nodes and two worker nodes without using a cloud load balancer, you can use a virtual machine to act as a load balancer for the API server. Here are the detailed steps for setting up such a cluster:

Prerequisites: 

- 3 master nodes

- 2 worker nodes

- 1 load balancer node

All nodes should be running a Linux distribution like Ubuntu


Doc: https://github.com/jaiswaladi246/HA-K8-Cluster.git



![](https://github.com/user-attachments/assets/ab7d24c2-4434-4f61-a502-9ff01af610fb)





### app


![](https://github.com/user-attachments/assets/3e0d8eee-20a0-4f41-9665-fada82c7f1d5)





for full documentation checkout : https://mrcloudbook.com/youtube-clone-app-with-devsecops-and-jenkins-shared-library/#more-1834



## jenkins pipeline


```sh

@Library('Jenkins_shared_library1') _  // name used in Jenkins system for library

def COLOR_MAP = [
    'FAILURE' : 'danger',
    'SUCCESS' : 'good'
]

pipeline {
    agent any
    tools {
        jdk 'jdk17'
        nodejs 'node16'
    }
    environment {
        SCANNER_HOME = tool 'sonar-scanner'
    }
    parameters {
        choice(name: 'action', choices: 'create\ndelete', description: 'Select create or destroy.')
        string(name: 'DOCKER_HUB_USERNAME', defaultValue: 'uzondu', description: 'Docker Hub Username')
        string(name: 'IMAGE_NAME', defaultValue: 'youtube', description: 'Docker Image Name')
    }
    stages {
        stage('clean workspace') {
            steps {
                deleteDir() // This is a standard method for cleaning workspace
            }
        }
        stage('checkout from Git') {
            steps {
                git url: 'https://github.com/UzonduEgbombah/Youtube-Clone-App.git', branch: 'main'
            }
        }
        stage('sonarqube Analysis') {
            when { expression { params.action == 'create' } }
            steps {
                sonarqubeAnalysis()
            }
        }
        stage('sonarqube QualityGate') {
            when { expression { params.action == 'create' } }
            steps {
                script {
                    def credentialsId = 'Sonar-token'
                    qualityGate(credentialsId)
                }
            }
        }
        stage('Npm') {
            when { expression { params.action == 'create' } }
            steps {
                npmInstall()
            }
        }
        stage('OWASP FS SCAN') {
            when { expression { params.action == 'create' } }
            steps {
                dependencyCheck additionalArguments: '--scan ./ --disableYarnAudit --disableNodeAudit', odcInstallation: 'DP-Check'
                dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
            }
        }
        stage('Trivy file scan') {
            when { expression { params.action == 'create' } }
            steps {
                script {
                    trivyFilescan()
                }
            }
        }
        stage('Docker Build') {
            when { expression { params.action == 'create' } }
            steps {
                script {
                    def dockerHubUsername = params.DOCKER_HUB_USERNAME
                    def imageName = params.IMAGE_NAME
                    dockerBuild(dockerHubUsername, imageName)
                }
            }
        }
        stage('Trivy image') {
            when { expression { params.action == 'create' } }
            steps {
                script {
                    trivyImage()
                }
            }
        }
        stage('Run container') {
            when { expression { params.action == 'create' } }
            steps {
                script {
                    runContainer()
                }
            }
        }
        stage('Remove container') {
            when { expression { params.action == 'delete' } }
            steps {
                script {
                    removeContainer()
                }
            }
        }
    }    
    post {
        always {
            echo 'Slack Notifications'
            slackSend (
                channel: '#jenkins-ytb',  
                color: COLOR_MAP[currentBuild.currentResult],
                message: "*${currentBuild.currentResult}:* Job ${env.JOB_NAME} \n build ${env.BUILD_NUMBER} \n More info at: ${env.BUILD_URL}"
            )
        }
    }
}



stage('Kube deploy'){
        when { expression { params.action == 'create'}}    
            steps{
                kubeDeploy()
            }
        }
        stage('kube deleter'){
        when { expression { params.action == 'delete'}}    
            steps{
                kubeDelete()
            }
```
