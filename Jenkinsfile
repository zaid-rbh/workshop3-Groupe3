pipeline {
    agent any

    environment {
        IMAGE_NAME       = 'opssaad/devsecops'
        IMAGE_TAG        = "1.0.${BUILD_NUMBER}"
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
                    sh '''
                       semgrep scan --config=p/ci --json -o semgrep-report.json || true
                   '''
                    archiveArtifacts artifacts: 'semgrep-report.json', allowEmptyArchive: true
                }
            }
        }


        stage('OWASP Dependency-Check') {
            steps {
                script {
                    def mavenHome = tool 'Maven3'
                    try {
                        withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
                            sh "${mavenHome}/bin/mvn -B org.owasp:dependency-check-maven:13.0.0:check"
                        }
                    } finally {
                        archiveArtifacts artifacts: 'target/dependency-check-report.html,target/dependency-check-report.json', allowEmptyArchive: true
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
            when {
                not {
                    changeRequest()
                }
            }
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
            echo 'Pipeline finished successfully.'
        }
        failure {
            echo "Pipeline failed - check the stage logs above."
        }
    }
}