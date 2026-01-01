# WebSocket 404 오류 해결 가이드

## 문제 증상
```
GET https://forum.rjsgud.com/ws/info?t=1767276377621 404 (Not Found)
```

## 원인
SockJS가 연결을 시도할 때 `/ws/info` 엔드포인트를 호출하는데, Nginx 리버스 프록시에서 이 경로를 백엔드로 프록시하지 않고 있습니다.

## 해결 방법

### 1. Nginx 설정 확인

현재 Nginx 설정 파일 위치 확인:
```bash
# Windows (NSSM 사용 시)
# 보통 C:\nginx\conf\nginx.conf 또는 설치 경로에 있음

# Linux
nginx -t  # 설정 파일 경로 확인
```

### 2. Nginx 설정 수정

`nginx.conf` 또는 사이트 설정 파일에 다음을 추가:

```nginx
# /ws 경로는 /api보다 먼저 위치해야 함 (더 구체적인 경로 우선)
location /ws {
    proxy_pass http://localhost:8081;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # WebSocket 타임아웃 설정
    proxy_read_timeout 86400;
    proxy_send_timeout 86400;
    proxy_connect_timeout 60;
    
    # 버퍼링 비활성화
    proxy_buffering off;
}
```

**중요**: `/ws` 경로 설정이 `/api` 경로 설정보다 **위에** 있어야 합니다. Nginx는 더 구체적인 경로를 먼저 매칭합니다.

### 3. Nginx 설정 테스트 및 재시작

```bash
# 설정 파일 문법 검사
nginx -t

# 오류가 없으면 재시작
nginx -s reload

# 또는 Windows에서 NSSM 사용 시
# 서비스 재시작
```

### 4. 백엔드 서버 확인

백엔드가 8081 포트에서 실행 중인지 확인:
```bash
# Windows
netstat -ano | findstr :8081

# Linux
netstat -tlnp | grep 8081
# 또는
ss -tlnp | grep 8081
```

### 5. 방화벽 확인

8081 포트가 열려있는지 확인 (로컬 연결이므로 보통 문제 없음)

### 6. 백엔드 로그 확인

백엔드 애플리케이션 로그에서 WebSocket 연결 시도가 들어오는지 확인:
```bash
# 로그 파일 확인 또는 콘솔 출력 확인
```

## 설정 예시 (전체)

```nginx
server {
    listen 80;
    server_name forum.rjsgud.com;

    # 1. WebSocket 설정 (가장 먼저)
    location /ws {
        proxy_pass http://localhost:8081;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 86400;
        proxy_send_timeout 86400;
        proxy_buffering off;
    }

    # 2. API 요청
    location /api {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 3. 프론트엔드
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 테스트

설정 후 브라우저에서 다음을 확인:
1. 개발자 도구 → Network 탭
2. `/ws/info` 요청이 200 OK를 반환하는지 확인
3. WebSocket 연결이 성공하는지 확인

## 추가 디버깅

만약 여전히 문제가 발생하면:

1. **직접 백엔드 연결 테스트**:
   ```bash
   curl http://localhost:8081/ws/info
   ```
   - 성공하면: Nginx 설정 문제
   - 실패하면: 백엔드 설정 문제

2. **Nginx 액세스 로그 확인**:
   ```bash
   tail -f /var/log/nginx/access.log
   ```

3. **Nginx 에러 로그 확인**:
   ```bash
   tail -f /var/log/nginx/error.log
   ```
