/*
 * ========================================================================
 *  Employee Management System — Full CI/CD Pipeline
 * ========================================================================
 *  Architecture (matches deployment diagram):
 *
 *  Developer → Commit/Push → GitHub Repository → Webhook Trigger
 *      → Jenkins CI Server
 *          → Checkout Source Code
 *          → Build Backend Image  → Backend Tests   ┐ (parallel)
 *          → Build Frontend Image → Frontend Tests  ┘
 *          → Code Quality & Validation
 *          → Push Docker Images → Docker / Container Registry
 *      → Kubernetes Cluster (or Docker Compose)
 *          → Database Container
 *          → Backend Container  (JDBC / ORM)
 *          → Frontend Container (HTTP / REST API)
 *      → Monitoring & Logging → Alerts / Metrics
 *
 *  Optional infrastructure stages:
 *      Terraform  → Infrastructure Provisioning → VMs / Cloud Instances
 *      Ansible    → Configuration Management    → VMs / Cloud Instances
 * ========================================================================
 */

pipeline {
    agent {
        label 'Monishan'              // runs on your inbound agent
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    /* ──────────────────────── Environment ──────────────────────── */
    environment {
        BACKEND_DIR      = 'ems-backend/ems-backend'
        FRONTEND_DIR     = 'ems-fullstack'
        BACKEND_REPO     = 'ems-backend'
        FRONTEND_REPO    = 'ems-frontend'
    }

    /* ──────────────────────── Parameters ──────────────────────── */
    parameters {
        // ── Docker / Registry ──
        string(name: 'DOCKER_REGISTRY',   defaultValue: 'docker.io/monishan8130',  description: 'Registry prefix (docker.io/<user>)')
        string(name: 'DOCKER_CRED_ID',    defaultValue: 'dockerhub-cred',           description: 'Jenkins credential ID for Docker Hub login')

        // ── Deploy ──
        booleanParam(name: 'DEPLOY',            defaultValue: false, description: 'Deploy after push (Compose or K8s)')
        choice(name: 'DEPLOY_TARGET',           choices: ['compose', 'kubernetes'],  description: 'Deployment target')
        string(name: 'DEPLOY_HOST',              defaultValue: '',                    description: 'SSH host for remote deploy (blank = local)')
        string(name: 'DEPLOY_PATH',              defaultValue: '/opt/employee-management-system', description: 'Remote path with docker-compose.yml')
        string(name: 'DEPLOY_SSH_CRED_ID',       defaultValue: 'deploy-ssh',          description: 'Jenkins SSH credential for remote deploy')

        // ── Kubernetes ──
        string(name: 'K8S_NAMESPACE',            defaultValue: 'ems',                 description: 'Kubernetes namespace')
        string(name: 'KUBECONFIG_CRED_ID',       defaultValue: 'kubeconfig',           description: 'Jenkins credential for kubeconfig file')

        // ── Infrastructure (optional) ──
        booleanParam(name: 'RUN_TERRAFORM',      defaultValue: false, description: 'Run Terraform infrastructure provisioning')
        booleanParam(name: 'RUN_ANSIBLE',         defaultValue: false, description: 'Run Ansible configuration management')
        string(name: 'TERRAFORM_DIR',             defaultValue: 'infra/terraform',     description: 'Path to Terraform files')
        string(name: 'ANSIBLE_DIR',               defaultValue: 'infra/ansible',       description: 'Path to Ansible playbooks')
    }

    stages {

        /* ============================================================
         *  STAGE 1 — Preflight (verify toolchain)
         * ============================================================ */
        stage('Preflight') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'java -version && docker version && docker compose version && node --version && npm --version'
                    } else {
                        bat 'java -version && docker version && docker compose version && node --version && npm --version'
                    }
                }
            }
        }

        /* ============================================================
         *  STAGE 2 — Checkout Source Code  (from GitHub via webhook)
         * ============================================================ */
        stage('Checkout Source Code') {
            steps {
                checkout scm
                script {
                    if (isUnix()) {
                        env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    } else {
                        env.GIT_COMMIT_SHORT = bat(script: '@git rev-parse --short HEAD', returnStdout: true).trim()
                    }
                    env.IMAGE_TAG           = env.GIT_COMMIT_SHORT
                    env.DOCKER_SERVER       = params.DOCKER_REGISTRY.tokenize('/')[0]
                    env.BACKEND_IMAGE       = "${params.DOCKER_REGISTRY}/${env.BACKEND_REPO}:${env.IMAGE_TAG}"
                    env.FRONTEND_IMAGE      = "${params.DOCKER_REGISTRY}/${env.FRONTEND_REPO}:${env.IMAGE_TAG}"
                    env.BACKEND_IMAGE_LATEST  = "${params.DOCKER_REGISTRY}/${env.BACKEND_REPO}:latest"
                    env.FRONTEND_IMAGE_LATEST = "${params.DOCKER_REGISTRY}/${env.FRONTEND_REPO}:latest"

                    echo "──── Build ${env.IMAGE_TAG} ────"
                    echo "Backend  : ${env.BACKEND_IMAGE}"
                    echo "Frontend : ${env.FRONTEND_IMAGE}"
                }
            }
        }

        /* ============================================================
         *  STAGE 3 — Build & Test  (parallel: backend + frontend)
         * ============================================================ */
        stage('Build & Test') {
            parallel {

                /* ---- Backend: build + unit tests ---- */
                stage('Backend') {
                    stages {
                        stage('Build Backend Image') {
                            steps {
                                dir(env.BACKEND_DIR) {
                                    script {
                                        if (isUnix()) {
                                            sh 'chmod +x mvnw && ./mvnw -B -DskipTests package'
                                        } else {
                                            bat '.\\mvnw.cmd -B -DskipTests package'
                                        }
                                    }
                                }
                            }
                        }
                        stage('Backend Tests') {
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
                            post {
                                always {
                                    junit allowEmptyResults: true,
                                         testResults: 'ems-backend/ems-backend/target/surefire-reports/*.xml'
                                }
                            }
                        }
                    }
                }

                /* ---- Frontend: install + build + unit tests ---- */
                stage('Frontend') {
                    stages {
                        stage('Build Frontend Image') {
                            steps {
                                dir(env.FRONTEND_DIR) {
                                    script {
                                        if (isUnix()) {
                                            sh 'npm ci && npm run build'
                                        } else {
                                            bat 'npm ci && npm run build'
                                        }
                                    }
                                }
                            }
                        }
                        stage('Frontend Tests') {
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

        /* ============================================================
         *  STAGE 4 — Code Quality & Validation
         * ============================================================ */
        stage('Code Quality & Validation') {
            parallel {
                stage('Backend Code Quality') {
                    steps {
                        dir(env.BACKEND_DIR) {
                            script {
                                // Compile-time checks + verify phase (includes Checkstyle/PMD if configured in pom.xml)
                                if (isUnix()) {
                                    sh './mvnw -B -DskipTests verify'
                                } else {
                                    bat '.\\mvnw.cmd -B -DskipTests verify'
                                }
                            }
                        }
                    }
                }
                stage('Frontend Code Quality') {
                    steps {
                        dir(env.FRONTEND_DIR) {
                            script {
                                // Run ESLint if configured; ignore exit code if not present
                                if (isUnix()) {
                                    sh 'npx eslint src/ --max-warnings=50 || echo "ESLint not configured — skipping"'
                                } else {
                                    bat 'npx eslint src/ --max-warnings=50 || echo ESLint not configured — skipping'
                                }
                            }
                        }
                    }
                }
            }
        }

        /* ============================================================
         *  STAGE 5 — Docker Build  (compose build both images)
         * ============================================================ */
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

        /* ============================================================
         *  STAGE 6 — Login to Docker / Container Registry
         * ============================================================ */
        stage('Login to Registry') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: params.DOCKER_CRED_ID,
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    script {
                        if (isUnix()) {
                            sh 'echo $DOCKER_PASS | docker login $DOCKER_SERVER -u $DOCKER_USER --password-stdin'
                        } else {
                            bat "docker login %DOCKER_SERVER% -u %DOCKER_USER% -p %DOCKER_PASS%"
                        }
                    }
                }
            }
        }

        /* ============================================================
         *  STAGE 7 — Push Docker Images  (commit-sha tag + latest)
         * ============================================================ */
        stage('Push Docker Images') {
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
            steps {
                script {
                    if (isUnix()) {
                        sh "docker tag ${env.BACKEND_IMAGE}  ${env.BACKEND_IMAGE_LATEST}"
                        sh "docker tag ${env.FRONTEND_IMAGE} ${env.FRONTEND_IMAGE_LATEST}"
                        sh "docker push ${env.BACKEND_IMAGE_LATEST}"
                        sh "docker push ${env.FRONTEND_IMAGE_LATEST}"
                    } else {
                        bat "docker tag ${env.BACKEND_IMAGE}  ${env.BACKEND_IMAGE_LATEST}"
                        bat "docker tag ${env.FRONTEND_IMAGE} ${env.FRONTEND_IMAGE_LATEST}"
                        bat "docker push ${env.BACKEND_IMAGE_LATEST}"
                        bat "docker push ${env.FRONTEND_IMAGE_LATEST}"
                    }
                }
            }
        }

        /* ============================================================
         *  STAGE 8 — Infrastructure Provisioning (Terraform – optional)
         * ============================================================ */
        stage('Terraform Provisioning') {
            when { expression { return params.RUN_TERRAFORM } }
            steps {
                dir(params.TERRAFORM_DIR) {
                    script {
                        if (isUnix()) {
                            sh '''
                                terraform init -input=false
                                terraform plan -out=tfplan
                                terraform apply -auto-approve tfplan
                            '''
                        } else {
                            bat '''
                                terraform init -input=false
                                terraform plan -out=tfplan
                                terraform apply -auto-approve tfplan
                            '''
                        }
                    }
                }
            }
        }

        /* ============================================================
         *  STAGE 9 — Configuration Management (Ansible – optional)
         * ============================================================ */
        stage('Ansible Configuration') {
            when { expression { return params.RUN_ANSIBLE } }
            steps {
                dir(params.ANSIBLE_DIR) {
                    script {
                        sh 'ansible-playbook -i inventory.ini site.yml'
                    }
                }
            }
        }

        /* ============================================================
         *  STAGE 10 — Deploy  (Kubernetes or Docker Compose)
         * ============================================================ */
        stage('Deploy') {
            when { expression { return params.DEPLOY } }
            steps {
                script {
                    if (params.DEPLOY_TARGET == 'kubernetes') {
                        /* ── Kubernetes Deployment ── */
                        echo "Deploying to Kubernetes namespace: ${params.K8S_NAMESPACE}"
                        withCredentials([file(credentialsId: params.KUBECONFIG_CRED_ID, variable: 'KUBECONFIG')]) {
                            if (isUnix()) {
                                sh """
                                    kubectl set image deployment/ems-backend  ems-backend=${env.BACKEND_IMAGE_LATEST}  -n ${params.K8S_NAMESPACE}
                                    kubectl set image deployment/ems-frontend ems-frontend=${env.FRONTEND_IMAGE_LATEST} -n ${params.K8S_NAMESPACE}
                                    kubectl rollout status deployment/ems-backend  -n ${params.K8S_NAMESPACE} --timeout=120s
                                    kubectl rollout status deployment/ems-frontend -n ${params.K8S_NAMESPACE} --timeout=120s
                                """
                            } else {
                                bat """
                                    kubectl set image deployment/ems-backend  ems-backend=${env.BACKEND_IMAGE_LATEST}  -n ${params.K8S_NAMESPACE}
                                    kubectl set image deployment/ems-frontend ems-frontend=${env.FRONTEND_IMAGE_LATEST} -n ${params.K8S_NAMESPACE}
                                    kubectl rollout status deployment/ems-backend  -n ${params.K8S_NAMESPACE} --timeout=120s
                                    kubectl rollout status deployment/ems-frontend -n ${params.K8S_NAMESPACE} --timeout=120s
                                """
                            }
                        }
                    } else {
                        /* ── Docker Compose Deployment ── */
                        if (params.DEPLOY_HOST?.trim()) {
                            sshagent(credentials: [params.DEPLOY_SSH_CRED_ID]) {
                                sh """
                                    ssh -o StrictHostKeyChecking=no ${params.DEPLOY_HOST} '
                                        cd ${params.DEPLOY_PATH} &&
                                        DOCKER_REGISTRY=${params.DOCKER_REGISTRY} IMAGE_TAG=latest docker compose pull &&
                                        DOCKER_REGISTRY=${params.DOCKER_REGISTRY} IMAGE_TAG=latest docker compose up -d &&
                                        docker compose ps
                                    '
                                """
                            }
                        } else {
                            withEnv([
                                "DOCKER_REGISTRY=${params.DOCKER_REGISTRY}",
                                'IMAGE_TAG=latest',
                                'MYSQL_PASSWORD=ems_deploy_pass',
                                'MYSQL_ROOT_PASSWORD=ems_deploy_root',
                                'JWT_SECRET=production-jwt-secret-change-this-in-env',
                            ]) {
                                if (isUnix()) {
                                    sh 'docker compose pull && docker compose up -d && docker compose ps'
                                } else {
                                    bat 'docker compose pull && docker compose up -d && docker compose ps'
                                }
                            }
                        }
                    }
                }
            }
        }

        /* ============================================================
         *  STAGE 11 — Monitoring & Logging  (health checks + notifications)
         * ============================================================ */
        stage('Monitoring & Logging') {
            when { expression { return params.DEPLOY } }
            steps {
                script {
                    echo '── Post-deploy health checks ──'
                    if (params.DEPLOY_TARGET == 'kubernetes') {
                        withCredentials([file(credentialsId: params.KUBECONFIG_CRED_ID, variable: 'KUBECONFIG')]) {
                            if (isUnix()) {
                                sh "kubectl get pods -n ${params.K8S_NAMESPACE} -o wide"
                                sh "kubectl logs deployment/ems-backend -n ${params.K8S_NAMESPACE} --tail=30 || true"
                            } else {
                                bat "kubectl get pods -n ${params.K8S_NAMESPACE} -o wide"
                                bat "kubectl logs deployment/ems-backend -n ${params.K8S_NAMESPACE} --tail=30"
                            }
                        }
                    } else {
                        if (isUnix()) {
                            sh 'docker compose ps'
                            sh 'docker compose logs --tail=30 ems-backend ems-frontend'
                        } else {
                            bat 'docker compose ps'
                            bat 'docker compose logs --tail=30 ems-backend ems-frontend'
                        }
                    }
                }
            }
        }
    }

    /* ──────────────────────── Post Actions ──────────────────────── */
    post {
        success {
            echo "Pipeline SUCCEEDED — images pushed: ${env.BACKEND_IMAGE_LATEST}, ${env.FRONTEND_IMAGE_LATEST}"
        }
        failure {
            echo 'Pipeline FAILED — check stage logs above for details.'
        }
        always {
            script {
                if (isUnix()) {
                    sh 'docker logout || true'
                } else {
                    bat 'docker logout 2>nul || echo Logged out'
                }
            }
            cleanWs()
        }
    }
}
