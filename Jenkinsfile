pipeline {
    agent any

    options {
        timestamps()
    }

    environment {
        DEPLOY_ROOT     = 'C:\\deploy\\forum'
        DEPLOY_BACKEND  = 'C:\\deploy\\forum\\backend'
        DEPLOY_FRONTEND = 'C:\\deploy\\forum\\frontend'
        NSSM            = 'C:\\nssm\\nssm.exe'
        NEXT_TELEMETRY_DISABLED = '1'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Frontend Build') {
            steps {
                dir('forum_front') {
                    bat '''
                        echo ===== Frontend Build =====
                        npm ci
                        npm run build
                    '''
                }
            }
        }

        stage('Backend Build') {
            steps {
                dir('forum_server') {
                    bat '''
                        echo ===== Backend Build =====
                        gradlew.bat clean build -x test
                    '''
                }
            }
        }

        stage('Copy Artifacts') {
            steps {
                bat '''
                    if not exist "%DEPLOY_BACKEND%"  mkdir "%DEPLOY_BACKEND%"
                    if not exist "%DEPLOY_FRONTEND%" mkdir "%DEPLOY_FRONTEND%"

                    echo --- Backend JAR ---
                    for %%f in (forum_server\\build\\libs\\*-SNAPSHOT.jar) do (
                        copy /Y "%%f" "%DEPLOY_BACKEND%\\app.jar"
                    )

                    echo --- Frontend ---
                    if exist forum_front\\.next (
                        xcopy /E /I /Y forum_front\\.next "%DEPLOY_FRONTEND%\\.next"
                    )
                    if exist forum_front\\public (
                        xcopy /E /I /Y forum_front\\public "%DEPLOY_FRONTEND%\\public"
                    )
                    copy /Y forum_front\\package.json "%DEPLOY_FRONTEND%\\package.json"
                '''
            }
        }

        stage('Restart Services (NSSM)') {
            steps {
                bat '''
                    echo ===== Restart NSSM Services =====
                    "%NSSM%" restart forum-backend
                    "%NSSM%" restart forum-frontend

                    REM Jenkins-safe wait
                    ping 127.0.0.1 -n 6 > nul
                '''
            }
        }
    }

    post {
        success {
            echo '✅ Build & Restart SUCCESS'
        }
        failure {
            echo '❌ Build FAILED'
        }
        cleanup {
            cleanWs()
        }
    }
}
