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
        sh "mvn -version"
        sh "java -version"
        sh "javac -version"
        sh "ls -alh;pwd"
        sh "printenv"
        sh "ls -alh /usr/share/maven"
        sh "mvn help:evaluate -Dexpression=settings.localRepository"
        sh "wget https://raw.githubusercontent.com/craftercms/craftercms/support/4.1.x/pom.xml -O pom-craftercms.xml"
        sh "wget https://raw.githubusercontent.com/craftercms/craftercms/support/4.1.x/pom-spring-boot.xml"
        sh "mvn -DskipTests -f pom-craftercms.xml clean install"
        sh "mvn -DskipTests -f pom-spring-boot.xml clean install"
        sh "mvn help:evaluate -Dexpression=project.repositories"
      }
    }
    
    stage('build') {
      steps {
        sh "mvn -version"

        sh "mvn clean install -X"
      }
    }
    
    stage("capture") {
      steps {
        junit '**/target/surefire-reports/TEST*.xml'    
      }  
    }
  }
}
