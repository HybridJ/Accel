-- 외래키 체크를 잠시 끄고 기존 테이블을 안전하게 삭제
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS comment_like;
DROP TABLE IF EXISTS article_like;
DROP TABLE IF EXISTS video;
DROP TABLE IF EXISTS vehicle_type;
DROP TABLE IF EXISTS vehicle;
DROP TABLE IF EXISTS brand;
DROP TABLE IF EXISTS comment;
DROP TABLE IF EXISTS article_img;
DROP TABLE IF EXISTS article;
DROP TABLE IF EXISTS revoked_access_token;
DROP TABLE IF EXISTS refresh_token;
DROP TABLE IF EXISTS user;
SET FOREIGN_KEY_CHECKS = 1;

-- ==========================================================
-- 1. 테이블 생성 (DDL)
-- ==========================================================

-- 1. 유저 정보
CREATE TABLE user(
    user_id VARCHAR(50),
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL, 
    name VARCHAR(25) NOT NULL,
    nickname VARCHAR(25) NOT NULL,
    age INT NOT NULL,
    email VARCHAR(50),
    profile_img VARCHAR(100) DEFAULT 'profile_default.png',
    PRIMARY KEY(user_id)
);

CREATE TABLE refresh_token(
    token_hash VARCHAR(100) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(token_hash),
    FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
);

CREATE TABLE revoked_access_token(
    jwt_id VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(jwt_id)
);

-- 2. 차량 브랜드 
CREATE TABLE brand(
    brand_id INT AUTO_INCREMENT,
    brand_name VARCHAR(30),
    brand_name_ko VARCHAR(30),
    brand_slug VARCHAR(30) NOT NULL UNIQUE,
    is_domestic BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (brand_id)
);

-- 3. 차량 정보
CREATE TABLE vehicle(
    vehicle_id   INT,
    vehicle_name VARCHAR(50),
    vehicle_name_ko VARCHAR(50),
    brand_id     INT NOT NULL,
    brand_name   VARCHAR(30),
    brand_name_ko VARCHAR(30),
    category     VARCHAR(20) NOT NULL,
    segment      VARCHAR(30),
    body_type    VARCHAR(100),
    seating      INT,
    min_price    BIGINT,
    max_price    BIGINT,
    description  TEXT,
    summary      VARCHAR(500),
    PRIMARY KEY (vehicle_id),
    FOREIGN KEY (brand_id) REFERENCES brand (brand_id)
);

-- 4. 차량 연료 타입 (1:N, fuel_types 배열 정규화)
CREATE TABLE vehicle_type(
    vehicle_id INT NOT NULL AUTO_INCREMENT,
    fuel_type  VARCHAR(30) NOT NULL,
    PRIMARY KEY (vehicle_id, fuel_type),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle (vehicle_id) ON DELETE CASCADE
);

-- 5. 동영상 정보 (차량 1대당 여러 영상, display_order가 낮을수록 우선순위 높음)
CREATE TABLE video(
    video_id     INT NOT NULL AUTO_INCREMENT,
    vehicle_id   INT NOT NULL,
    display_order INT NOT NULL,
    url          VARCHAR(200),
    channel_name VARCHAR(100),
    title        VARCHAR(300),
    PRIMARY KEY (video_id),
    UNIQUE KEY uq_video_vehicle_order (vehicle_id, display_order),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle (vehicle_id) ON DELETE CASCADE
);

-- 5. 게시글
CREATE TABLE article(
    article_id INT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    test_drive_available BOOLEAN NOT NULL,
    view_cnt INT NOT NULL DEFAULT 0,
    board_name VARCHAR(30),
    brand_id INT NOT NULL,
    PRIMARY KEY (article_id),
    FOREIGN KEY (user_id) REFERENCES user (user_id),
    FOREIGN KEY (brand_id) REFERENCES brand (brand_id)
);

-- 6. 댓글
CREATE TABLE article_img(
    article_img_id INT NOT NULL AUTO_INCREMENT,
    article_id INT NOT NULL,
    image_url TEXT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (article_img_id),
    FOREIGN KEY (article_id) REFERENCES article (article_id) ON DELETE CASCADE
);

CREATE TABLE comment(
    comment_id INT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    article_id INT NOT NULL,
    PRIMARY KEY (comment_id),
    FOREIGN KEY (user_id) REFERENCES user (user_id),
    FOREIGN KEY (article_id) REFERENCES article (article_id) ON DELETE CASCADE
);

-- 7. 게시글 좋아요 매핑
CREATE TABLE article_like(
    article_id INT NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    PRIMARY KEY(article_id, user_id),
    FOREIGN KEY (article_id) REFERENCES article (article_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user (user_id)
);

-- 8. 댓글 좋아요 매핑
CREATE TABLE comment_like(
    comment_id INT NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    PRIMARY KEY (comment_id, user_id),
    FOREIGN KEY (comment_id) REFERENCES comment (comment_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user (user_id)
);

-- 9. 전기차 충전소 정보 (KEPCO API 수집)
CREATE TABLE IF NOT EXISTS ev_station (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cs_id VARCHAR(50) NOT NULL COMMENT '충전소ID',
    cs_nm VARCHAR(200) COMMENT '충전소명칭',
    addr VARCHAR(500) COMMENT '충전소주소',
    lat VARCHAR(30) COMMENT '위도',
    longi VARCHAR(30) COMMENT '경도',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '최초 수집일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '갱신일시',
    UNIQUE KEY uk_ev_station_cs (cs_id)
);

-- 10. 전기차 충전기 정보 (KEPCO API 수집)
CREATE TABLE IF NOT EXISTS ev_charger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cs_id VARCHAR(50) NOT NULL COMMENT '충전소ID',
    cp_id VARCHAR(50) NOT NULL COMMENT '충전기ID',
    cp_nm VARCHAR(200) COMMENT '충전기명칭',
    charge_tp VARCHAR(10) COMMENT '충전기타입',
    cp_tp VARCHAR(10) COMMENT '충전방식코드',
    cp_stat VARCHAR(10) COMMENT '충전기 상태코드',
    stat_update_datetime VARCHAR(30) COMMENT '충전기 상태 갱신 시각',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '최초 수집일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '갱신일시',
    UNIQUE KEY uk_ev_charger_cs_cp (cs_id, cp_id)
);
