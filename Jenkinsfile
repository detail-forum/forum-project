pipeline {
    agent any

    options {
        timestamps()
    }

    environment {
        // 배포 경로
        DEPLOY_ROOT     = 'C:\\deploy\\forum'
        DEPLOY_BACKEND  = 'C:\\deploy\\forum\\backend'
        DEPLOY_FRONTEND = 'C:\\deploy\\forum\\frontend'

        // NSSM 경로
        NSSM = 'C:\\nssm\\nssm.exe'

        // Next.js 텔레메트리 비활성화
        NEXT_TELEMETRY_DISABLED = '1'
    }

    stages {

        /* =========================
           1. Git Checkout
        ========================= */
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        /* =========================
           2. Frontend Build (Next.js)
        ========================= */
        stage('Frontend Build') {
            steps {
                dir('forum_front') {
                    bat '''
                        echo ===== Frontend Build =====
                        node -v
                        npm -v

                        npm ci
                        npm run build
                    '''
                }
            }
        }

        /* =========================
           3. Backend Build (Spring)
        ========================= */
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

        /* =========================
           4. Copy Artifacts
           - frontend: 전체 소스 + .next + node_modules
           - backend : 실행 JAR (plain JAR 제외)
        ========================= */
        stage('Copy Artifacts') {
            steps {
                bat '''
                    echo ===== Stop Services =====
                    "%NSSM%" stop forum-backend
                    "%NSSM%" stop forum-frontend

                    ping 127.0.0.1 -n 5 > nul

                    echo ===== Copy Artifacts =====
                    if not exist "%DEPLOY_BACKEND%"  mkdir "%DEPLOY_BACKEND%"
                    if not exist "%DEPLOY_FRONTEND%" mkdir "%DEPLOY_FRONTEND%"

                    echo --- Backend JAR ---
                    REM plain JAR 제외하고 실행 가능한 JAR만 복사
                    if exist "forum_server\\build\\libs\\api_practice-0.0.1-SNAPSHOT.jar" (
                        copy /Y "forum_server\\build\\libs\\api_practice-0.0.1-SNAPSHOT.jar" "%DEPLOY_BACKEND%\\app.jar"
                        echo Backend JAR copied successfully
                    ) else (
                        echo [ERROR] Backend JAR file not found!
                        exit /b 1
                    )

                    echo --- Frontend Source ---
                    rmdir /S /Q "%DEPLOY_FRONTEND%"
                    mkdir "%DEPLOY_FRONTEND%"

                    REM 필수 파일 및 디렉토리 복사
                    xcopy /E /I /Y forum_front\\.next "%DEPLOY_FRONTEND%\\.next"
                    xcopy /E /I /Y forum_front\\public "%DEPLOY_FRONTEND%\\public"
                    xcopy /E /I /Y forum_front\\node_modules "%DEPLOY_FRONTEND%\\node_modules"
                    copy /Y forum_front\\package.json "%DEPLOY_FRONTEND%\\package.json"
                    copy /Y forum_front\\package-lock.json "%DEPLOY_FRONTEND%\\package-lock.json" 2>nul
                    if exist forum_front\\next.config.js copy /Y forum_front\\next.config.js "%DEPLOY_FRONTEND%\\next.config.js"
                    if exist forum_front\\tsconfig.json copy /Y forum_front\\tsconfig.json "%DEPLOY_FRONTEND%\\tsconfig.json"
                    xcopy /E /I /Y forum_front\\app "%DEPLOY_FRONTEND%\\app"
                    xcopy /E /I /Y forum_front\\components "%DEPLOY_FRONTEND%\\components"
                    xcopy /E /I /Y forum_front\\services "%DEPLOY_FRONTEND%\\services"
                    xcopy /E /I /Y forum_front\\store "%DEPLOY_FRONTEND%\\store"
                    xcopy /E /I /Y forum_front\\types "%DEPLOY_FRONTEND%\\types"
                    xcopy /E /I /Y forum_front\\utils "%DEPLOY_FRONTEND%\\utils"
                    if exist forum_front\\tailwind.config.js copy /Y forum_front\\tailwind.config.js "%DEPLOY_FRONTEND%\\tailwind.config.js"
                    if exist forum_front\\postcss.config.js copy /Y forum_front\\postcss.config.js "%DEPLOY_FRONTEND%\\postcss.config.js"
                '''
            }
        }

        /* =========================
           5. Restart Services (NSSM)
        ========================= */
        stage('Restart Services (NSSM)') {
            steps {
                bat '''
                    echo ===== Restart Services =====
                    "%NSSM%" restart forum-backend
                    "%NSSM%" start forum-frontend

                    ping 127.0.0.1 -n 6 > nul

                    echo ===== Service Status Check =====
                    netstat -ano | findstr :8081 && echo [OK] Backend(8081) is running || echo [WARN] Backend(8081) not started
                    netstat -ano | findstr :3000 && echo [OK] Frontend(3000) is running || echo [WARN] Frontend(3000) not started
                '''
            }
        }
    }

    post {
        success {
            echo '✅ Build & Deploy SUCCESS'
        }
        failure {
            echo '❌ Build FAILED'
        }
        cleanup {
            cleanWs()
        }
    }
}
