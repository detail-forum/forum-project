# SSL 인증서 발급 및 HTTPS 설정 가이드

## Windows 환경에서 Let's Encrypt 인증서 발급

### 1. win-acme 설치 및 실행

1. **win-acme 다운로드**
   - https://www.win-acme.com/ 에서 최신 버전 다운로드
   - 또는 PowerShell에서:
   ```powershell
   Invoke-WebRequest -Uri "https://github.com/win-acme/win-acme/releases/latest/download/win-acme.zip" -OutFile "win-acme.zip"
   Expand-Archive -Path "win-acme.zip" -DestinationPath "C:\win-acme"
   ```

2. **인증서 발급**
   ```powershell
   cd C:\win-acme
   .\wacs.exe
   ```
   
   - 대화형 모드에서:
     - `N` (새 인증서 생성)
     - `2` (Nginx 선택)
     - 도메인 입력: `forum.rjsgud.com`
     - 인증 방법: `1` (HTTP 파일 유효성 검사)
     - 인증서 저장 경로: `C:\certificates\forum.rjsgud.com` (또는 원하는 경로)

3. **자동 갱신 설정**
   - win-acme는 자동으로 Windows 작업 스케줄러에 갱신 작업을 등록합니다.

### 2. 인증서 파일 위치

발급된 인증서는 다음 위치에 저장됩니다:
- **인증서 파일**: `C:\certificates\forum.rjsgud.com\forum.rjsgud.com-crt.pem`
- **개인 키**: `C:\certificates\forum.rjsgud.com\forum.rjsgud.com-key.pem`
- **체인 파일**: `C:\certificates\forum.rjsgud.com\forum.rjsgud.com-chain.pem`

### 3. Nginx 설정

`nginx.conf` 파일을 다음과 같이 설정하세요:

```nginx
worker_processes  1;

events {
    worker_connections  1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;

    sendfile        on;
    keepalive_timeout  65;

    # HTTP to HTTPS 리다이렉트
    server {
        listen 80;
        server_name forum.rjsgud.com;
        
        # Let's Encrypt 인증을 위한 경로 (win-acme가 사용)
        location /.well-known/acme-challenge/ {
            root C:/certificates;
        }
        
        # 모든 HTTP 요청을 HTTPS로 리다이렉트
        location / {
            return 301 https://$server_name$request_uri;
        }
    }

    # HTTPS 서버
    server {
        listen 443 ssl;
        server_name forum.rjsgud.com;

        # SSL 인증서 설정
        ssl_certificate      C:/certificates/forum.rjsgud.com/forum.rjsgud.com-crt.pem;
        ssl_certificate_key  C:/certificates/forum.rjsgud.com/forum.rjsgud.com-key.pem;
        ssl_trusted_certificate C:/certificates/forum.rjsgud.com/forum.rjsgud.com-chain.pem;

        # SSL 설정
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;
        ssl_prefer_server_ciphers on;
        ssl_session_cache shared:SSL:10m;
        ssl_session_timeout 10m;

        # 프론트엔드(Next.js: 3000)
        location / {
            proxy_pass http://127.0.0.1:3000;
            proxy_http_version 1.1;

            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;

            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
        }

        # 백엔드(Spring: 8081)
        location /api/ {
            proxy_pass http://127.0.0.1:8081/;  # /api/ 제거하고 전달
            proxy_http_version 1.1;

            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
```

### 4. 방화벽 설정

Windows 방화벽에서 포트 443(HTTPS)을 열어야 합니다:

```powershell
# 관리자 권한으로 실행
New-NetFirewallRule -DisplayName "HTTPS" -Direction Inbound -LocalPort 443 -Protocol TCP -Action Allow
```

### 5. DNS 설정 확인

도메인 `forum.rjsgud.com`이 서버 IP `211.110.30.142`로 올바르게 설정되어 있는지 확인하세요.

## 참고사항

- 인증서는 90일마다 자동 갱신됩니다.
- win-acme는 Windows 작업 스케줄러에 자동 갱신 작업을 등록합니다.
- 인증서 갱신 후 Nginx를 재시작해야 합니다.

