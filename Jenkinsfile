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
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
                                sh '''
                            mvn -B org.owasp:dependency-check-maven:check \
                              -DnvdApiKey=$NVD_API_KEY \
                              -DdataDirectory=/var/jenkins_home/dependency-check-data \
                              -Dformats=HTML,JSON \
                              -DfailBuildOnCVSS=7 \
                              -DossindexAnalyzerEnabled=false
                        '''
                            }
                        }
                    }
                }
            }

            post {
                always {
                    archiveArtifacts artifacts: 'semgrep-report.txt, zap-reports/*, app.log, target/dependency-check-report.*', allowEmptyArchive: true
                    echo 'Pipeline terminé'
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
                        sh 'printf \'%s\' "$DOCKERHUB_PASSWORD" | docker login --username "$DOCKERHUB_USERNAME" --password-stdin'
                        sh "docker push ${IMAGE_NAME}:${IMAGE_TAG}"
                        sh "docker push ${IMAGE_NAME}:latest"
                    }
                }
            }
        }

        stage('Publish Security Reports') {
            steps {
                script {
                    publishHTML([
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'reports/owasp',
                            reportFiles: 'dependency-check-report.html',
                            reportName: 'OWASP Security Report'
                    ])
                }
            }
        }
    post {
        always {
            sh 'docker logout || true'
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
        success {
            echo 'Pipeline finished successfully with security checks and reports!'
        }
        failure {
            echo "Pipeline failed - check the stage logs above."
        }
    }
