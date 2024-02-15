pipeline {
  agent { docker "maven:3.9-eclipse-temurin-21" }

  stages {
    
    stage('maven') {
      steps {
        sh "mvn -version"
        sh "java -version"
        sh "mkdir -p $HOME/.m2"
        sh "ls"
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
