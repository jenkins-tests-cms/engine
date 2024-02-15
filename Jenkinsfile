pipeline {
  agent { docker "maven:3.9-eclipse-temurin-21" }

  stages {
    
    stage('maven') {
      steps {
        sh "mvn -version"
        sh "java -version"
        sh "ls;pwd"
        sh "echo $WORKSPACE"
        sh "echo $HOME"
        sh "printenv"
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
