# ACCEL VM Deployment Guide

이 문서는 ACCEL 프로젝트를 Google Cloud Compute Engine VM에 배포하고, 코드 수정 후 다시 빌드/재배포하는 절차를 정리한다.

현재 배포 대상:

| 항목 | 값 |
| --- | --- |
| GCP Project | `accel-500412` |
| VM Name | `accel-vm` |
| Zone | `asia-northeast3-c` |
| External IP | `34.64.234.98` |
| Domain | `accel.ai.kr`, `www.accel.ai.kr` |
| Frontend URL | `https://accel.ai.kr/` |
| API Prefix | `https://accel.ai.kr/api` |
| Backend Service | `accel.service` |
| Backend Path | `/opt/accel/backend/accel-back.jar` |
| Frontend Path | `/var/www/accel` |
| Deploy Script on VM | `/tmp/accel_vm_deploy.sh` |

## 배포 구조

- Nginx가 `80` 포트에서 Vue 정적 파일을 서빙한다.
- 도메인 접속은 HTTP에서 HTTPS로 리다이렉트한다.
- HTTPS 인증서는 Let's Encrypt / Certbot으로 관리한다.
- Nginx가 `/api/*` 요청만 Spring Boot `127.0.0.1:8080`으로 프록시한다.
- Spring Boot는 외부에 직접 노출하지 않고 localhost에만 바인딩한다.
- MySQL은 VM 내부 `127.0.0.1:3306`에서 동작한다.
- 새 DB 구성 시 배포 스크립트는 `java -jar accel-back.jar --spring.profiles.active=init` 흐름을 1회 실행해 스키마, 초기 데이터, EV 초기 배치를 구성한다.
- 일반 재배포 시 기존 DB가 있으면 DB 초기화는 다시 실행하지 않는다.

## 공통 준비 사항

로컬 PC에 아래가 준비되어 있어야 한다.

- Java 17
- Node.js `20.19.0+` 또는 `22.12.0+`
- npm
- Google Cloud CLI
- GCP 로그인 및 VM 접근 권한

로그인 확인:

```bash
gcloud auth list
gcloud config list
```

VM 확인:

```bash
gcloud compute instances list \
  --project=accel-500412 \
  --filter="name=accel-vm"
```

## 도메인 및 HTTPS 구성

현재 도메인:

```text
accel.ai.kr
www.accel.ai.kr
```

DNS A record:

```text
A    @      34.64.234.98
A    www    34.64.234.98
```

인증서 정보:

```text
Certificate Path: /etc/letsencrypt/live/accel.ai.kr/fullchain.pem
Private Key Path: /etc/letsencrypt/live/accel.ai.kr/privkey.pem
```

인증서 상태 확인:

```bash
gcloud compute ssh accel-vm \
  --project=accel-500412 \
  --zone=asia-northeast3-c \
  --command='sudo certbot certificates'
```

자동 갱신 테스트:

```bash
gcloud compute ssh accel-vm \
  --project=accel-500412 \
  --zone=asia-northeast3-c \
  --command='sudo certbot renew --dry-run --no-random-sleep-on-renew'
```

주의: 배포 스크립트가 Nginx 설정을 다시 생성한다. HTTPS 설정을 수동으로 `/etc/nginx/sites-available/accel`에만 고치면 다음 재배포 때 사라질 수 있다. 도메인/HTTPS 관련 변경은 `deploy/accel_vm_deploy.sh`에 반영한다.

## Kakao Maps 설정

`/ev/info`는 Kakao Maps JavaScript SDK를 사용한다. 프론트 빌드 전에 `accel-front/.env`에 아래 값이 있어야 한다.

```text
VITE_KAKAO_MAP_APP_KEY=카카오_JavaScript_키
```

운영 도메인은 Kakao Developers 콘솔의 해당 앱에서 `플랫폼 > Web 플랫폼 > 사이트 도메인`에 등록되어 있어야 한다.

등록 권장 값:

```text
https://accel.ai.kr
https://www.accel.ai.kr
http://localhost:5173
http://127.0.0.1:5173
```

외부 IP로도 `/ev/info`를 계속 테스트할 계획이면 아래도 추가한다.

```text
http://34.64.234.98
```

도메인이 등록되지 않으면 Kakao SDK 응답이 `401 AccessDeniedError`와 `domain mismatched` 메시지로 실패한다. 이 경우 코드 재배포가 아니라 Kakao Developers 콘솔 설정을 먼저 수정한다.

## macOS 기준: 최초 배포

프로젝트 경로:

```bash
cd /Users/wony/Desktop/accel
```

백엔드 빌드:

```bash
cd /Users/wony/Desktop/accel/accel-back
./mvnw package -DskipTests -Dmaven.test.skip=true
```

프론트 빌드:

```bash
cd /Users/wony/Desktop/accel/accel-front
VITE_API_BASE_URL=/api npm run build
```

배포 번들 생성:

```bash
cd /Users/wony/Desktop/accel
tar -czf /private/tmp/accel-deploy.tgz \
  accel-back/target/accel-back-0.0.1-SNAPSHOT.jar \
  accel-back/.env \
  accel-back/src/main/resources/accel_schema.sql \
  accel-back/src/main/resources/accel_data.sql \
  accel-front/dist
```

배포 스크립트와 번들 전송:

```bash
gcloud compute scp deploy/accel_vm_deploy.sh accel-vm:/tmp/accel_vm_deploy.sh \
  --project=accel-500412 \
  --zone=asia-northeast3-c

gcloud compute scp /private/tmp/accel-deploy.tgz accel-vm:/tmp/accel-deploy.tgz \
  --project=accel-500412 \
  --zone=asia-northeast3-c
```

배포 실행:

```bash
gcloud compute ssh accel-vm \
  --project=accel-500412 \
  --zone=asia-northeast3-c \
  --command='bash /tmp/accel_vm_deploy.sh /tmp/accel-deploy.tgz'
```

새 DB라면 이 과정에서 `init` 프로필이 1회 실행되고, 이후 일반 `accel.service`가 기동된다.

## macOS 기준: 수정 후 빌드 및 VM 재배포

일반 코드 수정 후에는 아래 순서로 재배포한다.

```bash
cd /Users/wony/Desktop/accel/accel-back
./mvnw package -DskipTests -Dmaven.test.skip=true

cd /Users/wony/Desktop/accel/accel-front
VITE_API_BASE_URL=/api npm run build

cd /Users/wony/Desktop/accel
tar -czf /private/tmp/accel-deploy.tgz \
  accel-back/target/accel-back-0.0.1-SNAPSHOT.jar \
  accel-back/.env \
  accel-back/src/main/resources/accel_schema.sql \
  accel-back/src/main/resources/accel_data.sql \
  accel-front/dist

gcloud compute scp deploy/accel_vm_deploy.sh accel-vm:/tmp/accel_vm_deploy.sh \
  --project=accel-500412 \
  --zone=asia-northeast3-c

gcloud compute scp /private/tmp/accel-deploy.tgz accel-vm:/tmp/accel-deploy.tgz \
  --project=accel-500412 \
  --zone=asia-northeast3-c

gcloud compute ssh accel-vm \
  --project=accel-500412 \
  --zone=asia-northeast3-c \
  --command='bash /tmp/accel_vm_deploy.sh /tmp/accel-deploy.tgz'
```

## Windows PowerShell 기준: 최초 배포

아래 예시는 프로젝트가 `C:\Users\<USER>\Desktop\accel`에 있다고 가정한다. 실제 경로에 맞게 `$ProjectRoot`만 수정한다.

```powershell
$ProjectRoot = "C:\Users\<USER>\Desktop\accel"
$Bundle = "$env:TEMP\accel-deploy.tgz"
```

백엔드 빌드:

```powershell
cd "$ProjectRoot\accel-back"
.\mvnw.cmd package -DskipTests -Dmaven.test.skip=true
```

프론트 빌드:

```powershell
cd "$ProjectRoot\accel-front"
$env:VITE_API_BASE_URL = "/api"
npm run build
Remove-Item Env:\VITE_API_BASE_URL
```

배포 번들 생성:

```powershell
cd $ProjectRoot
tar -czf $Bundle `
  accel-back/target/accel-back-0.0.1-SNAPSHOT.jar `
  accel-back/.env `
  accel-back/src/main/resources/accel_schema.sql `
  accel-back/src/main/resources/accel_data.sql `
  accel-front/dist
```

배포 스크립트와 번들 전송:

```powershell
gcloud compute scp deploy/accel_vm_deploy.sh accel-vm:/tmp/accel_vm_deploy.sh `
  --project=accel-500412 `
  --zone=asia-northeast3-c

gcloud compute scp $Bundle accel-vm:/tmp/accel-deploy.tgz `
  --project=accel-500412 `
  --zone=asia-northeast3-c
```

배포 실행:

```powershell
gcloud compute ssh accel-vm `
  --project=accel-500412 `
  --zone=asia-northeast3-c `
  --command "bash /tmp/accel_vm_deploy.sh /tmp/accel-deploy.tgz"
```

새 DB라면 이 과정에서 `init` 프로필이 1회 실행되고, 이후 일반 `accel.service`가 기동된다.

## Windows PowerShell 기준: 수정 후 빌드 및 VM 재배포

일반 코드 수정 후에는 아래 순서로 재배포한다.

```powershell
$ProjectRoot = "C:\Users\<USER>\Desktop\accel"
$Bundle = "$env:TEMP\accel-deploy.tgz"

cd "$ProjectRoot\accel-back"
.\mvnw.cmd package -DskipTests -Dmaven.test.skip=true

cd "$ProjectRoot\accel-front"
$env:VITE_API_BASE_URL = "/api"
npm run build
Remove-Item Env:\VITE_API_BASE_URL

cd $ProjectRoot
tar -czf $Bundle `
  accel-back/target/accel-back-0.0.1-SNAPSHOT.jar `
  accel-back/.env `
  accel-back/src/main/resources/accel_schema.sql `
  accel-back/src/main/resources/accel_data.sql `
  accel-front/dist

gcloud compute scp deploy/accel_vm_deploy.sh accel-vm:/tmp/accel_vm_deploy.sh `
  --project=accel-500412 `
  --zone=asia-northeast3-c

gcloud compute scp $Bundle accel-vm:/tmp/accel-deploy.tgz `
  --project=accel-500412 `
  --zone=asia-northeast3-c

gcloud compute ssh accel-vm `
  --project=accel-500412 `
  --zone=asia-northeast3-c `
  --command "bash /tmp/accel_vm_deploy.sh /tmp/accel-deploy.tgz"
```

## 배포 후 검증

macOS:

```bash
curl -I https://accel.ai.kr/
curl https://accel.ai.kr/api/videos/categories
curl "https://accel.ai.kr/api/ev/nearest?lat=37.5012743&longi=127.039585"
```

Windows PowerShell:

```powershell
curl.exe -I https://accel.ai.kr/
curl.exe https://accel.ai.kr/api/videos/categories
curl.exe "https://accel.ai.kr/api/ev/nearest?lat=37.5012743&longi=127.039585"
```

VM 내부 서비스 확인:

```bash
gcloud compute ssh accel-vm \
  --project=accel-500412 \
  --zone=asia-northeast3-c \
  --command='systemctl is-active accel && systemctl is-active nginx && systemctl is-active mysql'
```

DB row count 확인:

```bash
gcloud compute ssh accel-vm \
  --project=accel-500412 \
  --zone=asia-northeast3-c \
  --command='sudo mysql -N -B acceldb -e "SELECT \"brand\", COUNT(*) FROM brand UNION ALL SELECT \"vehicle\", COUNT(*) FROM vehicle UNION ALL SELECT \"video\", COUNT(*) FROM video UNION ALL SELECT \"article\", COUNT(*) FROM article UNION ALL SELECT \"ev_station\", COUNT(*) FROM ev_station UNION ALL SELECT \"ev_charger\", COUNT(*) FROM ev_charger;"'
```

정상 예시:

```text
brand       31
vehicle     212
video       905
article     620
ev_station  4520
ev_charger  9753
```

## DB 초기화가 필요한 경우

일반 재배포는 DB를 초기화하지 않는다. 기존 데이터가 유지된다.

아래 명령은 DB를 강제로 초기화한다.

주의: `accel_schema.sql`에는 `DROP TABLE IF EXISTS`가 있으므로 운영 데이터가 삭제된다. 정말 초기화가 필요할 때만 사용한다.

macOS:

```bash
gcloud compute ssh accel-vm \
  --project=accel-500412 \
  --zone=asia-northeast3-c \
  --command='FORCE_DB_INIT=true bash /tmp/accel_vm_deploy.sh /tmp/accel-deploy.tgz'
```

Windows PowerShell:

```powershell
gcloud compute ssh accel-vm `
  --project=accel-500412 `
  --zone=asia-northeast3-c `
  --command "FORCE_DB_INIT=true bash /tmp/accel_vm_deploy.sh /tmp/accel-deploy.tgz"
```

## 상황별 수정 방법

### 프론트만 수정한 경우

프론트만 다시 빌드해도 되지만, 배포 번들은 현재 스크립트 형식에 맞춰 백엔드 JAR도 함께 포함한다.

가장 중요한 값:

```bash
VITE_API_BASE_URL=/api
```

이 값을 빼면 브라우저가 `localhost:8080` 또는 잘못된 API 주소를 호출할 수 있다.

### 백엔드만 수정한 경우

백엔드 JAR를 다시 빌드하고 전체 번들을 다시 배포한다.

```bash
cd accel-back
./mvnw package -DskipTests -Dmaven.test.skip=true
```

Windows는 `.\mvnw.cmd`를 사용한다.

### 환경변수만 수정한 경우

API 키, JWT secret, DB 접속 정보 등만 바꾸는 경우 VM에서 `.env`를 수정한 뒤 서비스만 재시작할 수 있다.

```bash
gcloud compute ssh accel-vm \
  --project=accel-500412 \
  --zone=asia-northeast3-c
```

VM 접속 후:

```bash
sudo nano /opt/accel/backend/.env
sudo systemctl restart accel
sudo systemctl status accel --no-pager
```

### DB 스키마를 수정한 경우

일반 재배포만으로는 기존 DB 스키마가 자동 변경되지 않는다.

권장 순서:

1. 별도 migration SQL 작성
2. VM에서 `sudo mysql acceldb < migration.sql` 실행
3. 백엔드 재배포
4. API 검증

초기 데이터까지 완전히 다시 만들 때만 `FORCE_DB_INIT=true`를 사용한다.

## 주의 사항

- `/tmp/accel_vm_deploy.sh`는 VM의 임시 경로에 있으므로, 재부팅이나 정리 작업 후 사라질 수 있다. 재배포 때마다 `deploy/accel_vm_deploy.sh`를 다시 전송하는 것을 권장한다.
- `accel-back/.env`는 배포 번들에 포함되지만 git에는 커밋하지 않는다.
- 배포 스크립트는 VM에서 DB 비밀번호를 새로 생성하고 `/opt/accel/backend/.env`에 반영한다.
- Spring Boot는 `127.0.0.1:8080`에만 바인딩된다. 외부에서는 Nginx의 `/api/*`로만 접근한다.
- 외부에서 `http://34.64.234.98:8080`은 열리지 않는 것이 정상이다.
- 도메인은 `https://accel.ai.kr`, `https://www.accel.ai.kr`를 사용한다.
- `init` 프로필은 최초 DB 구성 또는 강제 초기화 때만 사용한다.
