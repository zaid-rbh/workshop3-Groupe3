pipeline {
    agent any

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

        stage('OWASP Dependency-Check') {
            steps {
                withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
                    sh '''
                        mvn -B org.owasp:dependency-check-maven:check \
                          -DnvdApiKey=$NVD_API_KEY \
                          -DdataDirectory=/var/jenkins_home/dependency-check-data \
                          -Dformats=HTML,JSON \
                          -DfailBuildOnCVSS=11 \
                          -DossindexAnalyzerEnabled=false
                    '''
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/dependency-check-report.*', allowEmptyArchive: true
            echo 'Pipeline terminé'
        }
    }
}