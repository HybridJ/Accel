# ACCEL

ACCEL은 차량 탐색, 브랜드별 영상 콘텐츠, 커뮤니티, 전기차 충전 인프라 정보를 한 곳에서 제공하는 모빌리티 커뮤니티 플랫폼입니다.

기획서: https://canva.link/j3e7wt1mtqwa4fr

![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Vue.js](https://img.shields.io/badge/Vue.js-3.x-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-8.x-646CFF?style=flat-square&logo=vite&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)

---

## 프로젝트 소개

ACCEL은 차량 구매와 이용 과정에서 필요한 탐색 경험을 커뮤니티와 연결하는 서비스입니다.

- 차량 브랜드/모델별 영상 콘텐츠 탐색
- 브랜드별 게시판을 통한 차량 오너 및 관심 사용자 커뮤니티
- 회원가입, 로그인, 사용자 정보 관리
- 전기차 충전 인프라 페이지
- Gemini 기반 AI 호출을 통한 개인 차량 추천 서비스
- Spring Boot 기반 REST API와 Vue 기반 SPA 프론트엔드 분리 구조

---

## 프로젝트 정보

| 항목 | 내용 |
| --- | --- |
| 프로젝트명 | ACCEL |
| 개발 기간 | 2026.05 ~ 2026.06 |
| 서비스 도메인 | 모빌리티, 차량 커뮤니티 |
| 통합 저장소 | `accel` |
| 백엔드 프로젝트 | `accel-back` |
| 프론트엔드 프로젝트 | `accel-front` |
| 배치 프로젝트 | `accel_spring_batch` |

---

## 주요 기능

### 회원

- 회원가입
- 로그인
- 사용자 정보 조회 및 관리

### 차량 영상

- 브랜드/차종 카테고리 기반 영상 쇼룸
- 영상 목록 및 상세 화면
- 영상 데이터 상태 관리

### 브랜드 커뮤니티

- 브랜드별 게시판 화면
- 게시글 상세 화면
- 게시글/댓글 API 연동을 위한 백엔드 도메인 구조

### 전기차 인프라

- 전기차 충전 인프라 화면
- 충전소 데이터 연동을 위한 배치 프로젝트 구성

### AI 차량 추천

- 개인 차량 추천을 위한 AI 설문 화면
- Gemini GMS API 호출을 통한 채팅형 추천 응답 제공
- `/ai/chat` API를 통한 AI 요청/응답 연동

---

## 기술 스택

### Backend

| 기술 | 용도 |
| --- | --- |
| Java 17 | 백엔드 개발 언어 |
| Spring Boot 4.0.6 | 백엔드 애플리케이션 프레임워크 |
| Spring Web MVC | REST API |
| Spring Security | 인증/인가 및 비밀번호 암호화 |
| Spring OAuth2 Resource Server | JWT Bearer 토큰 검증 |
| MyBatis | SQL Mapper |
| MySQL Connector/J | MySQL 연동 |
| Lombok | 반복 코드 감소 |
| Springdoc OpenAPI | Swagger API 문서 |
| RestClient | 외부 AI API 호출 |
| Maven Wrapper | 빌드 및 실행 |

### Frontend

| 기술 | 용도 |
| --- | --- |
| Vue 3 | 프론트엔드 UI 프레임워크 |
| Vite | 개발 서버 및 빌드 도구 |
| Vue Router | 클라이언트 라우팅 |
| Pinia | 전역 상태 관리 |
| Axios | HTTP API 통신 |

---

## 프로젝트 구조

```text
accel
├─ accel-back                    # Spring Boot 백엔드 프로젝트
│  ├─ pom.xml
│  ├─ mvnw, mvnw.cmd
│  └─ src
│     ├─ main
│     │  ├─ java/com/accel/api
│     │  │  ├─ AccelApplication.java
│     │  │  ├─ ai                 # AI 호출 및 추천 응답 도메인
│     │  │  ├─ auth               # 인증/회원가입/로그인 도메인
│     │  │  ├─ board              # 게시글 및 댓글 도메인
│     │  │  ├─ config             # Security, Swagger, CORS 등 설정
│     │  │  ├─ security           # JWT 발급 및 사용자 인증 연동
│     │  │  ├─ user               # 사용자 도메인
│     │  │  └─ video              # 차량/영상 도메인
│     │  └─ resources
│     │     ├─ mapper             # MyBatis Mapper XML
│     │     ├─ static             # 정적 리소스
│     │     ├─ accel_schema.sql        # DB 스키마 초기화 SQL
│     │     ├─ accel_data.sql          # DB 초기 데이터 SQL
│     │     ├─ application.properties  # 기본 실행 설정
│     │     └─ application-init.properties # 새 DB 초기화 프로필 설정
│     └─ test
│        └─ java/com/accel/api
├─ accel-front                   # Vue 프론트엔드 프로젝트
│  ├─ package.json
│  ├─ vite.config.js
│  ├─ index.html
│  └─ src
│     ├─ main.js
│     ├─ App.vue
│     ├─ assets                  # 로고, 영상, 이미지 리소스
│     ├─ components/common       # Header, NavBar, Footer
│     ├─ constants               # 상수 정리
│     ├─ router                  # Vue Router 설정
│     ├─ stores                  # Pinia store
│     └─ views
│        ├─ auth                 # 로그인/회원가입
│        ├─ ai                   # AI 차량 추천 설문
│        ├─ boards               # 브랜드 게시판
│        ├─ ev                   # 전기차 인프라
│        └─ videos               # 영상 쇼룸/카테고리/상세
└─ accel_ev_batch            # 전기차 충전소 데이터 배치 프로젝트 (본 프로젝트로 이식 예정)
```

---

## 실행 방법

### 1. 저장소 받기

```bash
git clone https://lab.ssafy.com/s2ndn1s/accel.git
cd accel
```

### 2. 데이터베이스 준비

로컬 MySQL에 `acceldb` 데이터베이스와 접속 계정을 준비합니다.

```sql
CREATE DATABASE acceldb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

백엔드 설정 파일은 다음 위치에 있습니다.

```text
accel-back/src/main/resources/application.properties
```

로컬 환경에 맞게 아래 값을 확인하거나 수정합니다.

```properties
spring.config.import=optional:file:.env[.properties]

jwt.secret=${JWT_SECRET}
jwt.expiration=1800000

spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}
gms.base-url=https://gms.ssafy.io/gmsapi
gms.key=${GMS_API_KEY}
gemini.gms.model=gemini-3.5-flash
gemini.gms.generate-path=/generativelanguage.googleapis.com/v1beta/models/${gemini.gms.model}:generateContent
youtube.api.key=${YOUTUBE_API_KEY}
```

`accel-back` 디렉터리에 `.env` 파일을 두고 로컬 값을 관리할 수 있습니다.

```properties
JWT_SECRET=your-256-bit-secret-key
DATABASE_URL=jdbc:mysql://localhost:3306/acceldb?serverTimezone=Asia/Seoul
DATABASE_USERNAME=accel
DATABASE_PASSWORD=your-password
GMS_API_KEY=your-gms-api-key
YOUTUBE_API_KEY=your-youtube-api-key
```

기본 실행에서는 DB 초기화가 비활성화되어 있습니다. 새로운 DB를 최초 구성할 때만 `init` 프로필로 실행합니다. 처음 실행하는 환경은 [`FIRST_RUN.md`](FIRST_RUN.md)를 먼저 참고하고, 상세 기준은 [`DB_INITIALIZATION.md`](DB_INITIALIZATION.md)를 참고합니다.

### 3. 백엔드 서버 실행

Windows PowerShell:

```powershell
cd accel-back
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
cd accel-back
./mvnw spring-boot:run
```

백엔드 기본 주소:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

### 4. 프론트엔드 서버 실행

새 터미널에서 실행합니다.

```bash
cd accel-front
npm install
npm run dev
```

Vite 개발 서버 기본 주소:

```text
http://localhost:5173
```

### 5. 빌드 확인

백엔드:

```powershell
cd accel-back
.\mvnw.cmd -DskipTests package
```

프론트엔드:

```bash
cd accel-front
npm run build
```

---

## API 문서

백엔드 서버 실행 후 Swagger UI에서 API 명세를 확인할 수 있습니다.

```text
http://localhost:8080/swagger-ui/index.html
```

주요 도메인 패키지:

| 도메인 | 백엔드 패키지 |
| --- | --- |
| 인증 | `com.accel.api.auth` |
| AI | `com.accel.api.ai` |
| 보안/JWT | `com.accel.api.security`, `com.accel.api.config.SecurityConfig` |
| 사용자 | `com.accel.api.user` |
| 게시판/댓글 | `com.accel.api.board` |
| 영상/차량 | `com.accel.api.video` |

---

## AI 호출 API

백엔드는 GMS 경유 Gemini API를 호출하는 `/ai/chat` 엔드포인트를 제공합니다.

- 설정값은 `accel-back/src/main/resources/application.properties`의 `gms.*`, `gemini.gms.*` 항목을 사용합니다.
- 로컬 실행 시 `accel-back/.env`에 `GMS_API_KEY`를 설정해야 합니다.
- 현재 `/ai/**` 경로는 AI 테스트용으로 인증 없이 접근할 수 있도록 설정되어 있습니다.

### AI 채팅 요청

```http
POST /ai/chat
Content-Type: application/json
```

```json
{
  "message": "출퇴근용 전기차를 추천해줘"
}
```

`message` 대신 `msg` 필드도 사용할 수 있습니다. 요청 본문이 없거나 메시지가 비어 있으면 기본 메시지로 Gemini API를 호출합니다.

### AI 채팅 응답

```json
{
  "answer": "..."
}
```

---

## JWT 인증 방식

백엔드는 Spring Security와 OAuth2 Resource Server 기반의 JWT Bearer 인증을 사용합니다.

- 회원가입 시 비밀번호는 `BCryptPasswordEncoder`로 암호화되어 저장됩니다.
- 로그인은 `AuthenticationManager`가 `userId`와 `password`를 검증합니다.
- 로그인 성공 시 `JwtTokenProvider`가 HS256 방식의 Access Token을 발급합니다.
- JWT에는 `issuer=Accel`, `sub=userId`, `role=ROLE_USER 또는 ROLE_ADMIN`, 만료 시간이 포함됩니다.
- 기본 만료 시간은 `jwt.expiration=1800000`으로 30분입니다.
- `/auth/**`, Swagger, 정적 리소스는 인증 없이 접근할 수 있고, 그 외 API는 JWT가 필요합니다.

### 로그인 요청

```http
POST /auth/login
Content-Type: application/json
```

```json
{
  "userId": "testuser",
  "password": "password123"
}
```

### 로그인 응답

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "username": "testuser",
  "role": "ROLE_USER"
}
```

### 인증 API 호출

로그인 응답의 `accessToken`을 `Authorization` 헤더에 담아 요청합니다.

```http
Authorization: Bearer eyJ...
```

예시:

```bash
curl -H "Authorization: Bearer eyJ..." http://localhost:8080/users
```

---

## ERD

ERD 링크: https://www.erdcloud.com/d/sNEf9ZQPWXhrbwEdo

---

## 팀

Team ACCEL · SSAFY 15기 대전 5반 1팀: 김준호, 나주원
