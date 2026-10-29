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

        stage('DAST - ZAP Baseline') {
            steps {
                sh '''
                    JAR=$(ls target/*.jar | grep -v original | head -n 1)
                    nohup java -jar "$JAR" --server.port=8081 > app.log 2>&1 &
                    echo $! > app.pid

                    for i in $(seq 1 40); do
                        curl -s -o /dev/null http://localhost:8081 && break
                        sleep 3
                    done

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
            post {
                always {
                    sh 'kill $(cat app.pid) 2>/dev/null || true'
                }
            }
        }

        stage('OWASP Dependency-Check') {
            steps {
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

    post {
        always {
            archiveArtifacts artifacts: 'zap-reports/*, app.log, target/dependency-check-report.*', allowEmptyArchive: true
            echo 'Pipeline terminé'
        }
    }
}