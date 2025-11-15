// Jenkins Declarative Pipeline for building & pushing Docker images to GHCR
// Prerequisites on the Jenkins agent/controller:
// - Docker CLI + daemon available
// - Jenkins credential (Username with password) containing GHCR username and PAT with read/write:packages
//   Set the ID of that credential below via GHCR_CREDENTIALS_ID.

pipeline {
	agent any

	options {
		timestamps()
		ansiColor('xterm')
		buildDiscarder(logRotator(numToKeepStr: '20'))
		disableConcurrentBuilds()
	}

	environment {
		REGISTRY              = 'ghcr.io'
		IMAGE_OWNER           = 'monishan-sangaralingam'  // change if pushing to another org/user
		BACKEND_IMAGE_NAME    = 'employee-management-system-backend'
		FRONTEND_IMAGE_NAME   = 'employee-management-system-frontend'
		BACKEND_CONTEXT       = 'ems-backend/ems-backend'
		FRONTEND_CONTEXT      = 'ems-fullstack'
		GHCR_CREDENTIALS_ID   = 'ghcr-credentials'        // Jenkins creds ID (username = GHCR user, password = PAT)
	}

	stages {
		stage('Checkout') {
			steps {
				checkout scm
				script {
					echo "Branch: ${env.BRANCH_NAME ?: 'unknown'}"
				}
			}
		}

		stage('Resolve metadata') {
			steps {
				script {
					env.OWNER_LC = env.IMAGE_OWNER.toLowerCase()
					env.SHORT_SHA = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()

					env.BACKEND_TAG_LATEST = "${env.REGISTRY}/${env.OWNER_LC}/${env.BACKEND_IMAGE_NAME}:latest"
					env.BACKEND_TAG_SHA    = "${env.REGISTRY}/${env.OWNER_LC}/${env.BACKEND_IMAGE_NAME}:sha-${env.SHORT_SHA}"
					env.FRONTEND_TAG_LATEST = "${env.REGISTRY}/${env.OWNER_LC}/${env.FRONTEND_IMAGE_NAME}:latest"
					env.FRONTEND_TAG_SHA    = "${env.REGISTRY}/${env.OWNER_LC}/${env.FRONTEND_IMAGE_NAME}:sha-${env.SHORT_SHA}"

					echo "Backend tags:  ${env.BACKEND_TAG_LATEST}, ${env.BACKEND_TAG_SHA}"
					echo "Frontend tags: ${env.FRONTEND_TAG_LATEST}, ${env.FRONTEND_TAG_SHA}"
				}
			}
		}

		stage('Sanity: Docker available') {
			steps {
				script {
					if (isUnix()) {
						sh 'docker version'
						sh 'docker info | head -n 50 || true'
					} else {
						bat 'docker version'
						bat 'docker info'
					}
				}
			}
		}

		stage('Build images') {
			steps {
				script {
					if (isUnix()) {
						sh "docker build -t ${env.BACKEND_TAG_LATEST} -t ${env.BACKEND_TAG_SHA} -f ${env.BACKEND_CONTEXT}/Dockerfile ${env.BACKEND_CONTEXT}"
						sh "docker build -t ${env.FRONTEND_TAG_LATEST} -t ${env.FRONTEND_TAG_SHA} -f ${env.FRONTEND_CONTEXT}/Dockerfile ${env.FRONTEND_CONTEXT}"
					} else {
						bat "docker build -t ${env.BACKEND_TAG_LATEST} -t ${env.BACKEND_TAG_SHA} -f ${env.BACKEND_CONTEXT}\\Dockerfile ${env.BACKEND_CONTEXT}"
						bat "docker build -t ${env.FRONTEND_TAG_LATEST} -t ${env.FRONTEND_TAG_SHA} -f ${env.FRONTEND_CONTEXT}\\Dockerfile ${env.FRONTEND_CONTEXT}"
					}
				}
			}
		}

		stage('Login to GHCR') {
			steps {
				withCredentials([usernamePassword(credentialsId: env.GHCR_CREDENTIALS_ID, usernameVariable: 'GHCR_USER', passwordVariable: 'GHCR_PAT')]) {
					script {
						if (isUnix()) {
							sh "echo \"$GHCR_PAT\" | docker login ${env.REGISTRY} -u \"$GHCR_USER\" --password-stdin"
						} else {
							bat "echo %GHCR_PAT% | docker login ${env.REGISTRY} -u %GHCR_USER% --password-stdin"
						}
					}
				}
			}
		}

		stage('Push images') {
			steps {
				script {
					if (isUnix()) {
						sh "docker push ${env.BACKEND_TAG_LATEST}"
						sh "docker push ${env.BACKEND_TAG_SHA}"
						sh "docker push ${env.FRONTEND_TAG_LATEST}"
						sh "docker push ${env.FRONTEND_TAG_SHA}"
					} else {
						bat "docker push ${env.BACKEND_TAG_LATEST}"
						bat "docker push ${env.BACKEND_TAG_SHA}"
						bat "docker push ${env.FRONTEND_TAG_LATEST}"
						bat "docker push ${env.FRONTEND_TAG_SHA}"
					}
				}
			}
		}
	}

	post {
		success {
			echo "Images pushed: ${env.BACKEND_TAG_LATEST}, ${env.BACKEND_TAG_SHA}, ${env.FRONTEND_TAG_LATEST}, ${env.FRONTEND_TAG_SHA}"
		}
		failure {
			echo 'Build failed. Check previous logs for errors.'
		}
		always {
			script {
				// Avoid leaving auth state on shared agents
				if (isUnix()) {
					sh 'docker logout ${REGISTRY} || true'
				} else {
					bat 'docker logout ${REGISTRY}'
				}
			}
		}
	}
}

