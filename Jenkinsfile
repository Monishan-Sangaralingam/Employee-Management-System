pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    environment {
        BACKEND_DIR = 'ems-backend/ems-backend'
        FRONTEND_DIR = 'ems-fullstack'
    }

    parameters {
        string(name: 'DOCKER_REGISTRY', defaultValue: 'docker.io/monishan8130', description: 'Registry/namespace prefix, e.g. docker.io/<user> or ghcr.io/<org>')
        string(name: 'DOCKER_CRED_ID', defaultValue: 'dockerhub-cred', description: 'Jenkins credentialsId for registry login (username/password)')

        booleanParam(name: 'DEPLOY', defaultValue: false, description: 'If true, run docker compose pull && up -d')
        string(name: 'DEPLOY_HOST', defaultValue: '', description: 'Optional SSH host (user@host). If empty, deploy runs on the Jenkins agent.')
        string(name: 'DEPLOY_PATH', defaultValue: '/opt/employee-management-system', description: 'Path on deploy host containing docker-compose.yml')
        string(name: 'DEPLOY_SSH_CRED_ID', defaultValue: 'deploy-ssh', description: 'Jenkins SSH credentialsId (used when DEPLOY_HOST is set)')
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
                    env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.DOCKER_SERVER = params.DOCKER_REGISTRY.tokenize('/')[0]
                    env.BACKEND_IMAGE = "${params.DOCKER_REGISTRY}/ems-backend:${env.GIT_COMMIT_SHORT}"
                    env.FRONTEND_IMAGE = "${params.DOCKER_REGISTRY}/ems-frontend:${env.GIT_COMMIT_SHORT}"
                }
            }
        }

        stage('Build & Test') {
            parallel {
                stage('Backend') {
                    stages {
                        stage('Build Backend') {
                            steps {
                                dir(env.BACKEND_DIR) {
                                    sh './mvnw -B -DskipTests=false package'
                                }
                            }
                        }
                        stage('Run Backend Tests') {
                            steps {
                                dir(env.BACKEND_DIR) {
                                    sh './mvnw -B test'
                                }
                            }
                        }
                    }
                }

                stage('Frontend') {
                    stages {
                        stage('Build Frontend') {
                            steps {
                                dir(env.FRONTEND_DIR) {
                                    sh 'npm ci'
                                    sh 'npm run build'
                                }
                            }
                        }
                        stage('Frontend Unit Tests') {
                            steps {
                                dir(env.FRONTEND_DIR) {
                                    sh 'npm test'
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Docker Build & Tag') {
            parallel {
                stage('Docker Build Backend') {
                    steps {
                        sh "docker build -t ${env.BACKEND_IMAGE} ${env.BACKEND_DIR}"
                    }
                }
                stage('Docker Build Frontend') {
                    steps {
                        sh "docker build -t ${env.FRONTEND_IMAGE} ${env.FRONTEND_DIR}"
                    }
                }
            }
        }

        stage('Login to Registry') {
            steps {
                withCredentials([usernamePassword(credentialsId: params.DOCKER_CRED_ID, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh 'echo $DOCKER_PASS | docker login $DOCKER_SERVER -u $DOCKER_USER --password-stdin'
                }
            }
        }

        stage('Push Images') {
            parallel {
                stage('Push Backend') {
                    steps {
                        sh "docker push ${env.BACKEND_IMAGE}"
                    }
                }
                stage('Push Frontend') {
                    steps {
                        sh "docker push ${env.FRONTEND_IMAGE}"
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                expression { return params.DEPLOY }
            }
            steps {
                script {
                    if (params.DEPLOY_HOST?.trim()) {
                        sshagent(credentials: [params.DEPLOY_SSH_CRED_ID]) {
                            sh "ssh -o StrictHostKeyChecking=no ${params.DEPLOY_HOST} 'cd ${params.DEPLOY_PATH} && docker compose pull && docker compose up -d && docker compose ps'"
                        }
                    } else {
                        sh 'docker compose pull'
                        sh 'docker compose up -d'
                        sh 'docker compose ps'
                    }
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'ems-backend/ems-backend/target/surefire-reports/*.xml'
            sh 'docker logout || true'
        }
    }
}
