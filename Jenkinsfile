pipeline {
  agent { docker "maven:3.9-eclipse-temurin-21" }

  stages {
    
    stage('maven') {
      steps {
        sh "mvn -version"
        sh "java -version"
        sh "ls -alh;pwd"
        sh "printenv"
        sh "ls -alh /usr/share/maven"
        sh "ls -alh /usr/share/maven-repo"
        sh "whoami"
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
