# 업로드 폴더 확인 및 문제 해결

## 1. 폴더 생성 확인

PowerShell에서 실행:

```powershell
# 폴더 존재 확인
Test-Path "C:\app-data\uploads"

# 폴더가 없으면 생성
if (-not (Test-Path "C:\app-data\uploads")) {
    New-Item -ItemType Directory -Path "C:\app-data\uploads" -Force
    Write-Host "폴더 생성 완료: C:\app-data\uploads"
} else {
    Write-Host "폴더가 이미 존재합니다: C:\app-data\uploads"
}

# 폴더 내용 확인
Get-ChildItem "C:\app-data\uploads" | Select-Object Name, Length, LastWriteTime
```

## 2. 권한 설정

1. `C:\app-data\uploads` 폴더 우클릭 → 속성
2. 보안 탭 → 편집
3. 다음 계정에 "쓰기" 권한 추가:
   - `IIS_IUSRS` (Nginx 실행 계정)
   - `NETWORK SERVICE` (Spring Boot 실행 계정)
   - 또는 현재 사용자 계정

## 3. Nginx 설정 확인

`nginx-https.conf` 파일에서:

```nginx
location /uploads/ {
    alias C:/app-data/uploads/;  # 슬래시(/) 사용, 끝에 슬래시 필수
    ...
}
```

**중요**: 
- Windows 경로는 슬래시(`/`)로 작성
- `alias` 지시어 끝에 슬래시(`/`) 필수
- 경로는 반드시 존재해야 함

## 4. Nginx 재시작

```bash
# 설정 테스트
nginx -t

# 재시작
nginx -s reload

# 또는 서비스 재시작
net stop nginx
net start nginx
```

## 5. 파일 저장 확인

이미지 업로드 후 다음을 확인:

1. **파일이 실제로 저장되었는지**:
   ```powershell
   Get-ChildItem "C:\app-data\uploads" | Select-Object Name
   ```

2. **Spring Boot 로그 확인**:
   - `이미지 업로드 성공: ...` 메시지 확인
   - `파일 저장 성공: ...` 메시지 확인

3. **브라우저에서 직접 접근**:
   - `https://forum.rjsgud.com/uploads/파일명.jpg` 직접 접근 시도
   - 404가 나오면 Nginx 설정 문제
   - 403이 나오면 권한 문제

## 6. 문제 해결

### 404 Not Found
- 파일이 실제로 저장되었는지 확인
- Nginx `alias` 경로가 올바른지 확인
- Nginx 재시작 확인

### 403 Forbidden
- 폴더 권한 확인
- Nginx 실행 계정에 읽기 권한 부여

### 파일이 저장되지 않음
- Spring Boot 로그 확인
- `C:\app-data\uploads` 폴더 권한 확인
- 디스크 공간 확인

