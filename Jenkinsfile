pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                dir('loco/loco') {
                    sh 'chmod +x mvnw'
                    sh './mvnw clean package -DskipTests'
                }
            }
        }

        stage('Code Quality (SonarQube)') {
            steps {
                dir('loco/loco') {
                    withSonarQubeEnv('SonarQube') {
                        sh '''
                        ./mvnw clean verify sonar:sonar \
                          -Dsonar.projectKey=loco-analyzer \
                          -Dsonar.projectName="LoCo Analyzer"
                        '''
                    }
                }
            }
        }

        stage('Package App') {
            steps {
                dir('loco/loco') {
                    sh '''
                    # Clean previous output
                    rm -rf output

                    # Run JPackage
                    jpackage --name Loco \
                             --input target/libs \
                             --main-jar loco-1.0-SNAPSHOT.jar \
                             --main-class com.myapp.loco.MainApp \
                             --type app-image \
                             --dest output \
                             --java-options "--module-path \\$APPDIR/lib/app --add-modules javafx.controls,javafx.fxml"

                    # Fix duplicate JavaFX jars for Linux runtime
                    cp target/libs/javafx-*-linux.jar output/Loco/lib/app/
                    rm -f output/Loco/lib/app/javafx-base-17.0.2.jar
                    rm -f output/Loco/lib/app/javafx-controls-17.0.2.jar
                    rm -f output/Loco/lib/app/javafx-fxml-17.0.2.jar
                    rm -f output/Loco/lib/app/javafx-graphics-17.0.2.jar

                    # Compress to tar.gz
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
