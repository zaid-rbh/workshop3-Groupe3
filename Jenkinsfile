pipeline {
    agent any

    environment {
        IMAGE_NAME = 'opssaad/devsecops'
        IMAGE_TAG = "1.0.${env.BUILD_NUMBER}"
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
                sh 'pip3 install semgrep || true'
                sh 'semgrep scan --config=p/ci --json -o semgrep-report.json'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'semgrep-report.json', allowEmptyArchive: true
                }
            }
        }

        stage('OWASP Dependency-Check') {
            steps {
                script {
                    // Remove -DautoUpdate=false or set it to true
                    sh 'mvn -B org.owasp:dependency-check-maven:13.0.0:check'
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

        // 📊 مرحلة التقارير المرئية المضافة حديثاً لتناسب مسارات Maven OWASP
        stage('Publish Security Reports') {
            steps {
                script {
                    publishHTML([
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'target',
                            reportFiles: 'dependency-check-report.html',
                            reportName: 'OWASP Security Report'
                    ])
                }
            }
        }
    }

    post {
        always {
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
        success {
            echo 'Pipeline finished successfully with visual security reports!'
        }
        failure {
            echo "Pipeline failed - check the stage logs above."
        }
    }
}