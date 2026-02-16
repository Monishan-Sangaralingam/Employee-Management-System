pipeline {
    agent any

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    environment {
        BACKEND_DIR = 'ems-backend/ems-backend'
        FRONTEND_DIR = 'ems-fullstack'

        BACKEND_REPO = 'ems-backend'
        FRONTEND_REPO = 'ems-frontend'
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

        stage('Preflight') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'docker version'
                        sh 'docker compose version'
                        sh 'node --version || true'
                        sh 'npm --version || true'
                    } else {
                        bat 'docker version'
                        bat 'docker compose version'
                        bat 'node --version'
                        bat 'npm --version'
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Prepare Tags') {
            steps {
                script {
                    if (isUnix()) {
                        env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    } else {
                        env.GIT_COMMIT_SHORT = bat(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    }

                    env.IMAGE_TAG = env.GIT_COMMIT_SHORT
                    env.DOCKER_SERVER = params.DOCKER_REGISTRY.tokenize('/')[0]

                    env.BACKEND_IMAGE = "${params.DOCKER_REGISTRY}/${env.BACKEND_REPO}:${env.IMAGE_TAG}"
                    env.FRONTEND_IMAGE = "${params.DOCKER_REGISTRY}/${env.FRONTEND_REPO}:${env.IMAGE_TAG}"

                    env.BACKEND_IMAGE_LATEST = "${params.DOCKER_REGISTRY}/${env.BACKEND_REPO}:latest"
                    env.FRONTEND_IMAGE_LATEST = "${params.DOCKER_REGISTRY}/${env.FRONTEND_REPO}:latest"
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
                                    script {
                                        if (isUnix()) {
                                            sh './mvnw -B -DskipTests=false package'
                                        } else {
                                            bat '.\\mvnw.cmd -B -DskipTests=false package'
                                        }
                                    }
                                }
                            }
                        }
                        stage('Run Backend Tests') {
                            steps {
                                dir(env.BACKEND_DIR) {
                                    script {
                                        if (isUnix()) {
                                            sh './mvnw -B test'
                                        } else {
                                            bat '.\\mvnw.cmd -B test'
                                        }
                                    }
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
                                    script {
                                        if (isUnix()) {
                                            sh 'npm ci'
                                            sh 'npm run build'
                                        } else {
                                            bat 'npm ci'
                                            bat 'npm run build'
                                        }
                                    }
                                }
                            }
                        }
                        stage('Frontend Unit Tests') {
                            steps {
                                dir(env.FRONTEND_DIR) {
                                    script {
                                        if (isUnix()) {
                                            sh 'npm test'
                                        } else {
                                            bat 'npm test'
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    withEnv([
                        "DOCKER_REGISTRY=${params.DOCKER_REGISTRY}",
                        "IMAGE_TAG=${env.IMAGE_TAG}",
                    ]) {
                        if (isUnix()) {
                            sh 'docker compose build ems-backend ems-frontend'
                        } else {
                            bat 'docker compose build ems-backend ems-frontend'
                        }
                    }
                }
            }
        }

        stage('Docker Smoke Test (Compose)') {
            steps {
                script {
                    // Required env vars for docker-compose.yml; set CI-safe defaults
                    def mysqlPassword = "ems_ci_pass"
                    def mysqlRootPassword = "ems_ci_root_pass"
                    def jwtSecret = "ci-jwt-secret-change-me-32-bytes-min" + env.GIT_COMMIT_SHORT

                    try {
                        withEnv([
                            "DOCKER_REGISTRY=${params.DOCKER_REGISTRY}",
                            "IMAGE_TAG=${env.IMAGE_TAG}",
                            "MYSQL_PASSWORD=${mysqlPassword}",
                            "MYSQL_ROOT_PASSWORD=${mysqlRootPassword}",
                            "JWT_SECRET=${jwtSecret}",
                        ]) {
                            if (isUnix()) {
                                sh 'docker compose up -d mysql redis ems-backend ems-frontend'

                                // Verify endpoints respond (200/30x for frontend, any HTTP for backend)
                                sh 'for i in $(seq 1 60); do code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/ || true); if [ "$code" != "000" ]; then echo "backend_http=$code"; exit 0; fi; sleep 2; done; echo "Backend did not respond"; exit 1'
                                sh 'curl -s -o /dev/null -w "%{http_code}\n" http://localhost:5173/ | grep -E "^(200|30[0-9])$"'
                            } else {
                                bat 'docker compose up -d mysql redis ems-backend ems-frontend'

                                bat 'powershell -NoProfile -ExecutionPolicy Bypass -Command "$uri=\"http://localhost:8090/\"; $ok=$false; for($i=0;$i -lt 60;$i++){ try{ $r=Invoke-WebRequest -UseBasicParsing -Uri $uri -TimeoutSec 2; Write-Host (\"backend_http=\"+$r.StatusCode); $ok=$true; break } catch { Start-Sleep -Seconds 2 } }; if(-not $ok){ throw \"Backend did not respond\" }"'
                                bat 'powershell -NoProfile -ExecutionPolicy Bypass -Command "$r=Invoke-WebRequest -UseBasicParsing -Uri \"http://localhost:5173/\" -TimeoutSec 10; if($r.StatusCode -lt 200 -or $r.StatusCode -ge 400){ throw \"Frontend did not respond with 2xx/3xx\" }"'
                            }
                        }
                    } finally {
                        if (isUnix()) {
                            sh 'docker compose down -v || true'
                        } else {
                            bat 'docker compose down -v'
                        }
                    }
                }
            }
        }

        stage('Login to Registry') {
            when {
                allOf {
                    not { changeRequest() }
                    anyOf { branch 'main'; branch 'master'; buildingTag() }
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: params.DOCKER_CRED_ID, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    script {
                        if (isUnix()) {
                            sh 'echo $DOCKER_PASS | docker login $DOCKER_SERVER -u $DOCKER_USER --password-stdin'
                        } else {
                            bat 'powershell -NoProfile -ExecutionPolicy Bypass -Command "docker logout | Out-Null; $pass=$env:DOCKER_PASS; $user=$env:DOCKER_USER; $server=$env:DOCKER_SERVER; $pass | docker login $server -u $user --password-stdin"'
                        }
                    }
                }
            }
        }

        stage('Push Images') {
            when {
                allOf {
                    not { changeRequest() }
                    anyOf { branch 'main'; branch 'master'; buildingTag() }
                }
            }
            parallel {
                stage('Push Backend') {
                    steps {
                        script {
                            if (isUnix()) {
                                sh "docker push ${env.BACKEND_IMAGE}"
                            } else {
                                bat "docker push ${env.BACKEND_IMAGE}"
                            }
                        }
                    }
                }
                stage('Push Frontend') {
                    steps {
                        script {
                            if (isUnix()) {
                                sh "docker push ${env.FRONTEND_IMAGE}"
                            } else {
                                bat "docker push ${env.FRONTEND_IMAGE}"
                            }
                        }
                    }
                }
            }
        }

        stage('Tag & Push Latest') {
            when {
                allOf {
                    not { changeRequest() }
                    anyOf { branch 'main'; branch 'master' }
                }
            }
            steps {
                script {
                    if (isUnix()) {
                        sh "docker tag ${env.BACKEND_IMAGE} ${env.BACKEND_IMAGE_LATEST}"
                        sh "docker tag ${env.FRONTEND_IMAGE} ${env.FRONTEND_IMAGE_LATEST}"
                        sh "docker push ${env.BACKEND_IMAGE_LATEST}"
                        sh "docker push ${env.FRONTEND_IMAGE_LATEST}"
                    } else {
                        bat "docker tag ${env.BACKEND_IMAGE} ${env.BACKEND_IMAGE_LATEST}"
                        bat "docker tag ${env.FRONTEND_IMAGE} ${env.FRONTEND_IMAGE_LATEST}"
                        bat "docker push ${env.BACKEND_IMAGE_LATEST}"
                        bat "docker push ${env.FRONTEND_IMAGE_LATEST}"
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
                            sh "ssh -o StrictHostKeyChecking=no ${params.DEPLOY_HOST} 'cd ${params.DEPLOY_PATH} && DOCKER_REGISTRY=${params.DOCKER_REGISTRY} IMAGE_TAG=latest docker compose pull && DOCKER_REGISTRY=${params.DOCKER_REGISTRY} IMAGE_TAG=latest docker compose up -d && docker compose ps'"
                        }
                    } else {
                        withEnv([
                            "DOCKER_REGISTRY=${params.DOCKER_REGISTRY}",
                            'IMAGE_TAG=latest',
                        ]) {
                            if (isUnix()) {
                                sh 'docker compose pull'
                                sh 'docker compose up -d'
                                sh 'docker compose ps'
                            } else {
                                bat 'docker compose pull'
                                bat 'docker compose up -d'
                                bat 'docker compose ps'
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'ems-backend/ems-backend/target/surefire-reports/*.xml'
            script {
                if (isUnix()) {
                    sh 'docker logout || true'
                } else {
                    bat 'docker logout'
                }
            }
        }
    }
}
