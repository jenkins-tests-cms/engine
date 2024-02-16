DOCKER_MAVEN_IMAGE = 'maven:3.9-eclipse-temurin-17'
DOCKER_MAVEN_ARGS = '-v $HOME/.m2/builds/$BRANCH_NAME:/root/.m2 -u 113:120'

pipeline {
  agent { 
    docker {
      image DOCKER_MAVEN_IMAGE
      args DOCKER_MAVEN_ARGS
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
