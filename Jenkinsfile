pipeline {
    agent any

    tools {
        maven 'Maven 3'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                dir('loco/loco') {
                    // Build and run tests
                    sh 'mvn clean package -DskipTests=false'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir('loco/loco') {
                    withSonarQubeEnv('SonarQube') {
                        sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=loco-analyzer'
                    }
                }
            }
        }

        stage("Quality Gate") {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Package App') {
            steps {
                dir('loco/loco') {
                    sh '''
                    # Clean previous output
                    rm -rf output
                    mkdir -p target/libs
                    cp target/loco-1.0-SNAPSHOT.jar target/libs/

                    # Download standalone JDK 21 (with jmods) since system JDK is missing them
                    if [ ! -d "jdk-21" ]; then
                         curl -L "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk" -o jdk.tar.gz
                         tar -xzf jdk.tar.gz
                         mv jdk-21* jdk-21
                         rm jdk.tar.gz
                    fi

                    # Run JPackage for Linux using the downloaded JDK
                    ./jdk-21/bin/jpackage --name Loco \
                             --input target/libs \
                             --main-jar loco-1.0-SNAPSHOT.jar \
                             --main-class com.myapp.loco.Launcher \
                             --type app-image \
                             --module-path jdk-21/jmods \
                             --add-modules java.base,java.sql,java.rmi,java.management,java.logging,java.xml,java.naming,java.net.http,java.desktop,jdk.unsupported,jdk.crypto.ec,jdk.management.jfr \
                             --dest output

                    # Fix duplicate JavaFX jars for Linux runtime
                    cp target/libs/javafx-*-linux.jar output/Loco/lib/app/
                    if [ -f output/Loco/lib/app/javafx-base-17.0.2.jar ]; then rm output/Loco/lib/app/javafx-base-17.0.2.jar; fi
                    if [ -f output/Loco/lib/app/javafx-controls-17.0.2.jar ]; then rm output/Loco/lib/app/javafx-controls-17.0.2.jar; fi
                    if [ -f output/Loco/lib/app/javafx-fxml-17.0.2.jar ]; then rm output/Loco/lib/app/javafx-fxml-17.0.2.jar; fi
                    if [ -f output/Loco/lib/app/javafx-graphics-17.0.2.jar ]; then rm output/Loco/lib/app/javafx-graphics-17.0.2.jar; fi

                    # Compress
                    tar -czf loco-admin-linux.tar.gz -C output Loco
                    '''
                }
            }
        }
    }

    post {
        success {
            dir('loco/loco') {
                archiveArtifacts artifacts: 'loco-admin-linux.tar.gz', fingerprint: true
            }
        }
    }
}
