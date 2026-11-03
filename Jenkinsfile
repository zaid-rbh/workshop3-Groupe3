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

        stage('Security Scans') {
            parallel {
                stage('Semgrep SAST Scan') {
                    steps {
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
                            def mavenHome = tool 'Maven3'
                            withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
                                sh "\"${mavenHome}/bin/mvn\" -B org.owasp:dependency-check-maven:13.0.0:check"
                            }
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'target/dependency-check-report.html,target/dependency-check-report.json', allowEmptyArchive: true
                            publishHTML([
                                    allowMissing: true,
                                    alwaysLinkToLastBuild: true,
                                    keepAll: true,
                                    reportDir: 'target',
                                    reportFiles: 'dependency-check-report.html',
                                    reportName: 'OWASP Dependency-Check'
                            ])
                        }
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    def scannerHome = tool 'SonarQubeScanner'
                    withSonarQubeEnv('SonarQube') {
                        withEnv(["SONAR_SCANNER_HOME=${scannerHome}"]) {
                            sh '''
                                export SONAR_TOKEN="$SONAR_AUTH_TOKEN"
                                "$SONAR_SCANNER_HOME/bin/sonar-scanner" \
                                -Dsonar.projectKey=devsecops \
                                -Dsonar.projectName=DevSecOps-VulnerableShop \
                                -Dsonar.sources=src/main/java \
                                -Dsonar.java.binaries=target/classes
                            '''
                        }
                    }
                }
            }
        }

        stage('Quality Gate Check') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "❌ Pipeline failed: Quality Gate failed because security issues were found!"
                        } else {
                            echo "✅ Quality Gate passed successfully! Security is clean."
                        }
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
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_PASSWORD')]) {
                        sh '''
                            set +x
                            export DOCKER_CONFIG="$(mktemp -d)"
                            trap 'docker logout >/dev/null 2>&1 || true; rm -rf "$DOCKER_CONFIG"' EXIT
                            printf '%s' "$DOCKERHUB_PASSWORD" | docker login --username "$DOCKERHUB_USERNAME" --password-stdin
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
            echo 'Pipeline finished successfully with security checks and reports!'
        }
        failure {
            echo "Pipeline failed - check the stage logs above."
        }
    }
}