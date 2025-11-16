pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    environment {
        REGISTRY = 'docker.io'
        DOCKERHUB_USER = 'monishan8130'         // your Docker Hub username
        BACKEND_IMAGE = "${DOCKERHUB_USER}/ems-backend"
        FRONTEND_IMAGE = "${DOCKERHUB_USER}/ems-frontend"
        BACKEND_CONTEXT = 'ems-backend/ems-backend'
        FRONTEND_CONTEXT = 'ems-fullstack'
        DOCKERHUB_CRED_ID = 'dockerhub-cred'    // Jenkins credentials ID
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Prepare Tags') {
            steps {
                script {
                    env.SHORT_SHA = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.BACKEND_TAG_LATEST = "${BACKEND_IMAGE}:latest"
                    env.BACKEND_TAG_SHA = "${BACKEND_IMAGE}:${SHORT_SHA}"
                    env.FRONTEND_TAG_LATEST = "${FRONTEND_IMAGE}:latest"
                    env.FRONTEND_TAG_SHA = "${FRONTEND_IMAGE}:${SHORT_SHA}"
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    sh "docker build -t ${BACKEND_TAG_LATEST} -t ${BACKEND_TAG_SHA} ${BACKEND_CONTEXT}"
                    sh "docker build -t ${FRONTEND_TAG_LATEST} -t ${FRONTEND_TAG_SHA} ${FRONTEND_CONTEXT}"
                }
            }
        }

        stage('Login to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: DOCKERHUB_CRED_ID, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                    sh "echo $PASS | docker login -u $USER --password-stdin"
                }
            }
        }

        stage('Push Images') {
            steps {
                script {
                    sh "docker push ${BACKEND_TAG_LATEST}"
                    sh "docker push ${BACKEND_TAG_SHA}"
                    sh "docker push ${FRONTEND_TAG_LATEST}"
                    sh "docker push ${FRONTEND_TAG_SHA}"
                }
            }
        }
    }

    post {
        always {
            sh "docker logout"
        }
    }
}
