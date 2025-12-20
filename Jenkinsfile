pipeline {
  agent any

  options {
    skipDefaultCheckout(true)  // ✅ 중복 checkout 방지
    timestamps()
  }

  environment {
    // 배포 경로
    DEPLOY_ROOT = 'C:\\deploy\\forum'
    DEPLOY_BACKEND_DIR = 'C:\\deploy\\forum\\backend'
    DEPLOY_FRONT_DIR   = 'C:\\deploy\\forum\\frontend'

    // Nginx 경로
    NGINX_HOME = 'C:\\Nginx\\nginx-1.28.0'

    // (선택) Next telemetry 끄기
    NEXT_TELEMETRY_DISABLED = '1'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('환경 설정') {
      steps {
        bat '''
          node --version
          npm --version
          java -version
          cd forum_server
          gradlew.bat --version
        '''
      }
    }

    stage('프론트엔드 빌드') {
      steps {
        dir('forum_front') {
          bat '''
            call npm ci
            call npm run build
          '''
        }
      }
    }

    stage('백엔드 빌드') {
      steps {
        dir('forum_server') {
          bat '''
            call gradlew.bat clean build -x test
          '''
        }
      }
    }

    stage('테스트 실행') {
      steps {
        dir('forum_server') {
          bat '''
            call gradlew.bat test
          '''
        }
      }
      post {
        always {
          junit 'forum_server/build/test-results/test/*.xml'
        }
      }
    }

    stage('아티팩트 복사') {
      steps {
        bat """
          if not exist "${DEPLOY_BACKEND_DIR}" mkdir "${DEPLOY_BACKEND_DIR}"
          if not exist "${DEPLOY_FRONT_DIR}" mkdir "${DEPLOY_FRONT_DIR}"

          REM ✅ 백엔드 jar 복사 (최신 jar 1개를 app.jar로 통일)
          for %%F in (forum_server\\build\\libs\\*.jar) do copy /Y "%%F" "${DEPLOY_BACKEND_DIR}\\app.jar"

          REM ✅ 프론트 빌드 결과 복사 (Next는 서버 실행이 필요해서 .next도 복사)
          REM 필요하면 node_modules는 제외하고, package.json/next.config.js 등도 같이 복사 권장
          xcopy /E /I /Y "forum_front\\.next" "${DEPLOY_FRONT_DIR}\\.next"
          xcopy /E /I /Y "forum_front\\public" "${DEPLOY_FRONT_DIR}\\public"
          copy /Y "forum_front\\package.json" "${DEPLOY_FRONT_DIR}\\package.json"
        """
      }
    }

    stage('배포') {
      when {
        expression {
          // ✅ Multibranch(BRANCH_NAME) / 일반 Pipeline(GIT_BRANCH) 둘 다 대응
          def b = (env.BRANCH_NAME ?: env.GIT_BRANCH ?: '')
          return b.contains('main') || b.contains('master')
        }
      }
      steps {
        bat """
          echo ===== 서비스 재시작(예시) =====

          REM ✅ 백엔드 서비스 재시작 (서비스명은 실제 등록한 이름으로 변경)
          REM net stop forum-backend
          REM net start forum-backend

          REM ✅ 프론트 서비스 재시작 (서비스명은 실제 등록한 이름으로 변경)
          REM net stop forum-frontend
          REM net start forum-frontend

          echo ===== Nginx 설정 테스트 & 리로드 =====
          "${NGINX_HOME}\\nginx.exe" -t
          "${NGINX_HOME}\\nginx.exe" -s reload
        """
      }
    }
  }

  post {
    success { echo '✅ 빌드 성공!' }
    failure { echo '❌ 빌드 실패!' }
    cleanup { cleanWs() }
  }
}
