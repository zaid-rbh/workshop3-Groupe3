pipeline {
    agent any

    environment {
        IMAGE_NAME       = 'opssaad/devsecops'
        IMAGE_TAG        = "${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                script {
                    def mavenHome = tool 'Maven3'
                    sh "${mavenHome}/bin/mvn -B clean package"
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    def scannerHome = tool 'SonarQubeScanner'
                    withSonarQubeEnv('SonarQube') {
                        sh """
                            \"${scannerHome}/bin/sonar-scanner\" \\
                            -Dsonar.projectKey=devsecops \\
                            -Dsonar.projectName=DevSecOps-VulnerableShop \\
                            -Dsonar.sources=src/main/java \\
                            -Dsonar.java.binaries=target/classes
                        """
                    }
                }
            }
        }
        stage('Semgrep SAST Scan') {
            steps {
                script {
                    try {
                        sh 'semgrep scan --config=p/ci --json -o semgrep-report.json'
                    } finally {
                        archiveArtifacts artifacts: 'semgrep-report.json', allowEmptyArchive: true
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest ."
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
                        sh '''
                            set +x
                            export DOCKER_CONFIG="$(mktemp -d)"
                            trap 'docker logout >/dev/null 2>&1 || true; rm -rf "$DOCKER_CONFIG"' EXIT
                            printf '%s' "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USER" --password-stdin
                            docker push "$IMAGE_NAME:$IMAGE_TAG"
                            docker push "$IMAGE_NAME:latest"
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
        success {
            echo "Pipeline finished successfully: ${IMAGE_NAME}:${IMAGE_TAG} pushed to Docker Hub"
        }
        failure {
            echo "Pipeline failed - check the stage logs above."
        }
    }
}