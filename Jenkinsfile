pipeline {
  agent { 
    docker {
      image "maven:3.9-eclipse-temurin-17" 
      args '-e JAVA_HOME=/opt/java/openjdk/ -u root'
      reuseNode true
    }
  }

  stages {
    
    stage('maven') {
      steps {
                // Clean before build
        sh "mvn -version"
        sh "java -version"
        sh "javac -version"
        sh "printenv"
      }
    }
    
    stage('build') {
      steps {
        sh "mvn -version"

        sh "mvn clean install -debug"
      }
    }
    
    stage("capture") {
      steps {
        junit '**/target/surefire-reports/TEST*.xml'    
      }  
    }
  }
}
