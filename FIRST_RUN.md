# 최초 실행 가이드

새로운 환경에서 ACCEL 백엔드를 처음 실행할 때는 DB와 MySQL 계정을 먼저 준비한 뒤, `init` 프로필을 최초 1회만 명시적으로 실행한다.

중요: `init` 프로필은 자동으로 실행되지 않는다. 처음 구성할 때 사람이 한 번 켜는 초기화 스위치다.

## 1. DB와 계정 생성

MySQL 관리자 계정으로 접속해서 `acceldb`와 애플리케이션 계정을 만든다.

```sql
DROP DATABASE IF EXISTS acceldb;
CREATE DATABASE acceldb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'accel'@'%' IDENTIFIED BY '비밀번호';
GRANT ALL PRIVILEGES ON acceldb.* TO 'accel'@'%';
FLUSH PRIVILEGES;
```

로컬 PC에서만 접속한다면 `'accel'@'localhost'`로 만들어도 된다. 배포 서버, Docker, 원격 DB처럼 접속 위치가 달라질 수 있으면 `'accel'@'%'`가 빠르게 테스트하기 쉽다.

## 2. `.env` 준비

`accel-back/.env` 파일을 만들고 접속 정보와 API 키를 채운다.

```properties
JWT_SECRET=충분히_긴_랜덤_문자열

DATABASE_URL=jdbc:mysql://localhost:3306/acceldb?serverTimezone=Asia/Seoul
DATABASE_USERNAME=accel
DATABASE_PASSWORD=비밀번호

OPENAI_API_KEY=발급받은_OPENAI_API_KEY
OPENAI_CHAT_MODEL=gpt-5-mini
YOUTUBE_API_KEY=발급받은_YOUTUBE_API_KEY
KEPCO_API_KEY=발급받은_KEPCO_API_KEY

GCS_BUCKET_NAME=
```

`KEPCO_API_KEY`가 없으면 EV 충전소/충전기 초기 배치가 실패한다.

## 3. 최초 1회 초기화 실행

개발 환경에서 Maven으로 실행할 때:

```powershell
cd accel-back
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=init"
```

jar로 실행할 때:

```powershell
java -jar accel-back.jar --spring.profiles.active=init
```

이 실행에서 처리되는 일:

1. `accel_schema.sql`로 테이블 생성
2. `accel_data.sql`로 기본 데이터 삽입
3. `ev_station` 또는 `ev_charger`가 비어 있으면 `stationJob` 1회 실행
4. KEPCO API 데이터를 `ev_station`, `ev_charger`에 UPSERT

초기화가 끝나면 서버를 종료한다.

## 4. 이후 일반 실행

초기화 이후에는 `init` 프로필 없이 실행한다.

개발 환경:

```powershell
cd accel-back
.\mvnw.cmd spring-boot:run
```

jar 실행:

```powershell
java -jar accel-back.jar
```

일반 실행에서는 `spring.sql.init.mode=never`가 적용되므로 `accel_schema.sql`, `accel_data.sql`이 다시 실행되지 않는다.

## 주의

운영 데이터가 생긴 뒤에는 `init` 프로필을 다시 사용하지 않는다. `accel_schema.sql`에는 `DROP TABLE IF EXISTS` 구문이 있어 기존 데이터가 삭제될 수 있다.

요약:

```text
처음 1회:
DB/계정 생성 -> .env 준비 -> init 프로필로 실행

그 이후:
일반 실행만 사용
```
