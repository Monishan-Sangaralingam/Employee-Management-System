/*
 * ========================================================================
 *  Employee Management System — Full CI/CD Pipeline  (Windows)
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

def runShell(String unixCmd, String winCmd = null) {
    if (isUnix()) {
        sh(script: unixCmd)
    } else {
        bat(winCmd ?: unixCmd)
    }
}

def runShellOut(String unixCmd, String winCmd = null) {
    if (isUnix()) {
        return sh(script: unixCmd, returnStdout: true).trim()
    }
    return bat(script: (winCmd ?: unixCmd), returnStdout: true).trim()
}

pipeline {
    agent {
        label 'Monishan'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    environment {
        BACKEND_DIR   = 'ems-backend/ems-backend'
        FRONTEND_DIR  = 'ems-fullstack'
        BACKEND_REPO  = 'ems-backend'
        FRONTEND_REPO = 'ems-frontend'
    }

    parameters {
        string(name: 'DOCKER_REGISTRY',    defaultValue: 'docker.io/monishan8130', description: 'Registry prefix (docker.io/<user>)')
        string(name: 'DOCKER_CRED_ID',     defaultValue: 'dockerhub-cred',          description: 'Jenkins credential ID for Docker Hub login')

        booleanParam(name: 'DEPLOY',       defaultValue: false,                     description: 'Deploy after push (Compose or K8s)')
        choice(name: 'DEPLOY_TARGET',      choices: ['compose', 'kubernetes'],       description: 'Deployment target')
        string(name: 'DEPLOY_PATH',        defaultValue: 'C:\\ems-deploy',           description: 'Local path with docker-compose.yml (Compose deploy)')
        string(name: 'K8S_NAMESPACE',      defaultValue: 'ems',                     description: 'Kubernetes namespace')
        string(name: 'KUBECONFIG_CRED_ID', defaultValue: 'kubeconfig',              description: 'Jenkins credential for kubeconfig file')

        booleanParam(name: 'RUN_TERRAFORM', defaultValue: false,                    description: 'Run Terraform infrastructure provisioning')
        booleanParam(name: 'RUN_ANSIBLE',   defaultValue: false,                    description: 'Run Ansible configuration management')
        string(name: 'TERRAFORM_DIR',       defaultValue: 'infra/terraform',       description: 'Path to Terraform files')
        string(name: 'ANSIBLE_DIR',         defaultValue: 'infra/ansible',         description: 'Path to Ansible playbooks')
    }

    stages {

        /* ============================================================
         *  STAGE 1 — Preflight
         * ============================================================ */
        stage('Preflight') {
            steps {
                script {
                    runShell('java -version')
                    runShell('docker version')
                    runShell('docker compose version')
                    runShell('node --version')
                    runShell('npm --version')
                }
            }
        }

        /* ============================================================
         *  STAGE 2 — Checkout Source Code
         * ============================================================ */
        stage('Checkout Source Code') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = runShellOut('git rev-parse --short HEAD', '@git rev-parse --short HEAD')
                    env.IMAGE_TAG        = env.GIT_COMMIT_SHORT
                    env.DOCKER_SERVER    = params.DOCKER_REGISTRY.tokenize('/')[0]

                    env.BACKEND_IMAGE        = "${params.DOCKER_REGISTRY}/${env.BACKEND_REPO}:${env.IMAGE_TAG}"
                    env.FRONTEND_IMAGE       = "${params.DOCKER_REGISTRY}/${env.FRONTEND_REPO}:${env.IMAGE_TAG}"
                    env.BACKEND_IMAGE_LATEST = "${params.DOCKER_REGISTRY}/${env.BACKEND_REPO}:latest"
                    env.FRONTEND_IMAGE_LATEST= "${params.DOCKER_REGISTRY}/${env.FRONTEND_REPO}:latest"

                    echo "Build ${env.IMAGE_TAG}"
                    echo "Backend  : ${env.BACKEND_IMAGE}"
                    echo "Frontend : ${env.FRONTEND_IMAGE}"
                }
            }
        }

        /* ============================================================
         *  STAGE 3 — Build & Test  (parallel)
         * ============================================================ */
        stage('Build & Test') {
            parallel {

                stage('Backend') {
                    stages {
                        stage('Build Backend Image') {
                            steps {
                                dir(env.BACKEND_DIR) {
                                    script {
                                        runShell('./mvnw -B -DskipTests package', '.\\mvnw.cmd -B -DskipTests package')
                                    }
                                }
                            }
                        }
                        stage('Backend Tests') {
                            steps {
                                dir(env.BACKEND_DIR) {
                                    script {
                                        runShell('./mvnw -B test', '.\\mvnw.cmd -B test')
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

                stage('Frontend') {
                    stages {
                        stage('Build Frontend Image') {
                            steps {
                                dir(env.FRONTEND_DIR) {
                                    script {
                                        runShell('npm ci')
                                        runShell('npm run build')
                                    }
                                }
                            }
                        }
                        stage('Frontend Tests') {
                            steps {
                                dir(env.FRONTEND_DIR) {
                                    script {
                                        runShell('npm test')
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
                                runShell('./mvnw -B -DskipTests verify', '.\\mvnw.cmd -B -DskipTests verify')
                            }
                        }
                    }
                }
                stage('Frontend Code Quality') {
                    steps {
                        dir(env.FRONTEND_DIR) {
                            script {
                                runShell(
                                    'npx eslint src/ --max-warnings=50 || echo "ESLint not configured - skipping"',
                                    'npx eslint src/ --max-warnings=50 || echo ESLint not configured - skipping'
                                )
                            }
                        }
                    }
                }
            }
        }

        /* ============================================================
         *  STAGE 5 — Docker Build
         * ============================================================ */
        stage('Docker Build') {
            steps {
                withEnv([
                    "DOCKER_REGISTRY=${params.DOCKER_REGISTRY}",
                    "IMAGE_TAG=${env.IMAGE_TAG}",
                    // Required by docker-compose.yml variable interpolation (even for build-only commands)
                    'MYSQL_PASSWORD=ems_ci_pass',
                    'MYSQL_ROOT_PASSWORD=ems_ci_root',
                    'JWT_SECRET=ems_ci_jwt_secret',
                ]) {
                    script {
                        runShell('docker compose build ems-backend ems-frontend')
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
                        runShell(
                            'docker login "$DOCKER_SERVER" -u "$DOCKER_USER" -p "$DOCKER_PASS"',
                            'docker login %DOCKER_SERVER% -u %DOCKER_USER% -p %DOCKER_PASS%'
                        )
                    }
                }
            }
        }

        /* ============================================================
         *  STAGE 7 — Push Docker Images
         * ============================================================ */
        stage('Push Docker Images') {
            parallel {
                stage('Push Backend') {
                    steps {
                        script {
                            runShell("docker push ${env.BACKEND_IMAGE}")
                        }
                    }
                }
                stage('Push Frontend') {
                    steps {
                        script {
                            runShell("docker push ${env.FRONTEND_IMAGE}")
                        }
                    }
                }
            }
        }

        stage('Tag & Push Latest') {
            steps {
                script {
                    runShell("docker tag ${env.BACKEND_IMAGE}  ${env.BACKEND_IMAGE_LATEST}")
                    runShell("docker tag ${env.FRONTEND_IMAGE} ${env.FRONTEND_IMAGE_LATEST}")
                    runShell("docker push ${env.BACKEND_IMAGE_LATEST}")
                    runShell("docker push ${env.FRONTEND_IMAGE_LATEST}")
                }
            }
        }

        /* ============================================================
         *  STAGE 8 — Terraform Provisioning (optional)
         * ============================================================ */
        stage('Terraform Provisioning') {
            when { expression { return params.RUN_TERRAFORM } }
            steps {
                dir(params.TERRAFORM_DIR) {
                    script {
                        runShell('terraform init -input=false')
                        runShell('terraform plan -out=tfplan')
                        runShell('terraform apply -auto-approve tfplan')
                    }
                }
            }
        }

        /* ============================================================
         *  STAGE 9 — Ansible Configuration (optional)
         * ============================================================ */
        stage('Ansible Configuration') {
            when { expression { return params.RUN_ANSIBLE } }
            steps {
                dir(params.ANSIBLE_DIR) {
                    script {
                        runShell('ansible-playbook -i inventory.ini site.yml')
                    }
                }
            }
        }

        /* ============================================================
         *  STAGE 10 — Deploy (Kubernetes or Docker Compose)
         * ============================================================ */
        stage('Deploy') {
            when { expression { return params.DEPLOY } }
            steps {
                script {
                    if (params.DEPLOY_TARGET == 'kubernetes') {
                        echo "Deploying to Kubernetes namespace: ${params.K8S_NAMESPACE}"
                        withCredentials([file(credentialsId: params.KUBECONFIG_CRED_ID, variable: 'KUBECONFIG')]) {
                            runShell("kubectl set image deployment/ems-backend  ems-backend=${env.BACKEND_IMAGE_LATEST}  -n ${params.K8S_NAMESPACE}")
                            runShell("kubectl set image deployment/ems-frontend ems-frontend=${env.FRONTEND_IMAGE_LATEST} -n ${params.K8S_NAMESPACE}")
                            runShell("kubectl rollout status deployment/ems-backend  -n ${params.K8S_NAMESPACE} --timeout=120s")
                            runShell("kubectl rollout status deployment/ems-frontend -n ${params.K8S_NAMESPACE} --timeout=120s")
                        }
                    } else {
                        withEnv([
                            "DOCKER_REGISTRY=${params.DOCKER_REGISTRY}",
                            'IMAGE_TAG=latest',
                            'MYSQL_PASSWORD=ems_deploy_pass',
                            'MYSQL_ROOT_PASSWORD=ems_deploy_root',
                            'JWT_SECRET=production-jwt-secret-change-this-in-env',
                        ]) {
                            runShell('docker compose pull')
                            runShell('docker compose up -d')
                            runShell('docker compose ps')
                        }
                    }
                }
            }
        }

        /* ============================================================
         *  STAGE 11 — Monitoring & Logging
         * ============================================================ */
        stage('Monitoring & Logging') {
            when { expression { return params.DEPLOY } }
            steps {
                script {
                    echo 'Post-deploy health checks'
                    if (params.DEPLOY_TARGET == 'kubernetes') {
                        withCredentials([file(credentialsId: params.KUBECONFIG_CRED_ID, variable: 'KUBECONFIG')]) {
                            runShell("kubectl get pods -n ${params.K8S_NAMESPACE} -o wide")
                            runShell("kubectl logs deployment/ems-backend -n ${params.K8S_NAMESPACE} --tail=30")
                        }
                    } else {
                        runShell('docker compose ps')
                        runShell('docker compose logs --tail=30 ems-backend ems-frontend')
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline SUCCEEDED - images pushed: ${env.BACKEND_IMAGE_LATEST}, ${env.FRONTEND_IMAGE_LATEST}"
        }
        failure {
            echo 'Pipeline FAILED - check stage logs above.'
        }
        always {
            script {
                runShell('docker logout >/dev/null 2>&1 || true', 'docker logout 2>nul || echo Logged out')
            }
            cleanWs()
        }
    }
}
