pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    environment {
        DOCKER_HOST = 'tcp://host.docker.internal:2375'
    }

    tools {
        maven 'Maven3'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean verify'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=workshop2 -Dsonar.projectName=workshop2 -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.token=$SONAR_AUTH_TOKEN'
                }
            }
        }

        stage('Semgrep Scan') {
            steps {
                sh 'semgrep scan --config p/java --config p/owasp-top-ten --metrics=off --text --output semgrep-report.txt .'
                sh 'cat semgrep-report.txt'
            }
        }

        stage('DAST - ZAP Baseline') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    sh '''
                        if [ -f app.pid ]; then kill $(cat app.pid) 2>/dev/null || true; fi
                        pkill -f devsecops-project || true
                        sleep 2

                        JAR=$(ls target/*.jar | grep -v original | head -n 1)
                        nohup java -jar "$JAR" --server.port=8081 --server.address=0.0.0.0 > app.log 2>&1 &
                        echo $! > app.pid

                        for i in $(seq 1 40); do
                            curl -s -o /dev/null http://jenkins:8081 && break
                            sleep 3
                        done
                        tail -n 20 app.log

                        docker rm -f zap-baseline || true
                        docker run --name zap-baseline --network zaid --user root \
                            ghcr.io/zaproxy/zaproxy:stable \
                            bash -c "mkdir -p /zap/wrk && zap-baseline.py -t http://jenkins:8081 -r zap-baseline.html -J zap-baseline.json -I" || true

                        mkdir -p zap-reports
                        docker cp zap-baseline:/zap/wrk/zap-baseline.html zap-reports/
                        docker cp zap-baseline:/zap/wrk/zap-baseline.json zap-reports/
                        docker rm zap-baseline
                    '''
                }
            }
            post {
                always {
                    sh 'kill $(cat app.pid) 2>/dev/null || true'
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