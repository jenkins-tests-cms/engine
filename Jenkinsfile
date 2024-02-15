pipeline {
  agent { 
    docker {
      image "maven:3.9-eclipse-temurin-17" 
      args '-e JAVA_HOME=/opt/java/openjdk/ -u root'
    }
  }

  stages {
    
    stage('maven') {
      steps {
                // Clean before build
        cleanWs()
        // We need to explicitly checkout from SCM here
        checkout scm
        echo "Building ${env.JOB_NAME}..."
        sh "mvn -version"
        sh "java -version"
        sh "javac -version"
        sh "printenv"
        sh "wget https://raw.githubusercontent.com/craftercms/craftercms/support/4.1.x/pom.xml -O pom-craftercms.xml"
        sh "wget https://raw.githubusercontent.com/craftercms/craftercms/support/4.1.x/pom-spring-boot.xml"
        sh "mvn -DskipTests -f pom-craftercms.xml clean install"
        sh "mvn -DskipTests -f pom-spring-boot.xml clean install"
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
