pipeline {
  agent { 
    docker "maven:3.9-eclipse-temurin-21" 
    args '-u root'
  }

  stages {
    
    stage('maven') {
      steps {
        sh "mvn -version"
        sh "java -version"
        sh "ls -alh;pwd"
        sh "printenv"
        sh "ls -alh /usr/share/maven"
        sh "mvn help:evaluate -Dexpression=settings.localRepository"
      }
    }
    
    stage('build') {
      steps {
        sh "mvn clean install"
      }
    }
    
    stage("capture") {
      steps {
        junit '**/target/surefire-reports/TEST*.xml'    
      }  
    }
  }
}
