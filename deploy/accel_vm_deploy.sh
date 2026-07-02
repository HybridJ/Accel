#!/usr/bin/env bash
set -euo pipefail

BUNDLE_PATH="${1:-/tmp/accel-deploy.tgz}"
APP_USER="accel"
APP_HOME="/opt/accel"
BACKEND_DIR="$APP_HOME/backend"
WEB_ROOT="/var/www/accel"
DB_NAME="acceldb"
DB_USER="accel"
FORCE_DB_INIT="${FORCE_DB_INIT:-false}"
PRIMARY_DOMAIN="${PRIMARY_DOMAIN:-accel.ai.kr}"
WWW_DOMAIN="${WWW_DOMAIN:-www.accel.ai.kr}"
SERVER_NAMES="${PRIMARY_DOMAIN} ${WWW_DOMAIN}"
CERT_DIR="/etc/letsencrypt/live/${PRIMARY_DOMAIN}"
ACME_WEBROOT="/var/www/letsencrypt"

run_init_profile_once() {
  local init_log="/tmp/accel-init-$(date +%Y%m%d%H%M%S).log"
  local deadline=$((SECONDS + 900))
  local init_pid

  echo "Running Spring init profile once. Log: ${init_log}"
  sudo systemctl stop accel >/dev/null 2>&1 || true

  sudo -u "$APP_USER" bash -c "
    cd \"$BACKEND_DIR\" &&
    exec /usr/bin/java \
      -Dserver.address=127.0.0.1 \
      -Dserver.port=18080 \
      -jar \"$BACKEND_DIR/accel-back.jar\" \
      --spring.profiles.active=init
  " >"$init_log" 2>&1 &
  init_pid=$!

  while kill -0 "$init_pid" >/dev/null 2>&1; do
    if grep -q "\\[STATION-SAVE\\] ev_charger upsert rows" "$init_log"; then
      break
    fi

    if grep -q "\\[EV-MANUAL-RUNNER\\] stationJob failed" "$init_log"; then
      cat "$init_log"
      sudo pkill -TERM -u "$APP_USER" -f "server.port=18080" >/dev/null 2>&1 || true
      kill "$init_pid" >/dev/null 2>&1 || true
      wait "$init_pid" >/dev/null 2>&1 || true
      echo "Spring init profile failed while running EV station job." >&2
      exit 1
    fi

    if (( SECONDS >= deadline )); then
      tail -n 120 "$init_log" || true
      sudo pkill -TERM -u "$APP_USER" -f "server.port=18080" >/dev/null 2>&1 || true
      kill "$init_pid" >/dev/null 2>&1 || true
      wait "$init_pid" >/dev/null 2>&1 || true
      echo "Timed out waiting for Spring init profile EV station job." >&2
      exit 1
    fi

    sleep 5
  done

  if ! grep -q "\\[STATION-SAVE\\] ev_charger upsert rows" "$init_log"; then
    cat "$init_log"
    wait "$init_pid" >/dev/null 2>&1 || true
    echo "Spring init profile exited before EV station job completed." >&2
    exit 1
  fi

  sudo pkill -TERM -u "$APP_USER" -f "server.port=18080" >/dev/null 2>&1 || true
  kill "$init_pid" >/dev/null 2>&1 || true
  wait "$init_pid" >/dev/null 2>&1 || true

  local ev_station_count
  local ev_charger_count
  ev_station_count="$(sudo mysql -N -B "$DB_NAME" -e "SELECT COUNT(*) FROM ev_station;")"
  ev_charger_count="$(sudo mysql -N -B "$DB_NAME" -e "SELECT COUNT(*) FROM ev_charger;")"

  if [[ "$ev_station_count" -eq 0 || "$ev_charger_count" -eq 0 ]]; then
    tail -n 120 "$init_log" || true
    echo "EV initialization did not populate ev_station/ev_charger." >&2
    exit 1
  fi

  echo "Spring init profile completed: ev_station=${ev_station_count}, ev_charger=${ev_charger_count}"
}

if [[ ! -f "$BUNDLE_PATH" ]]; then
  echo "Bundle not found: $BUNDLE_PATH" >&2
  exit 1
fi

stage_dir="/tmp/accel-release-$(date +%Y%m%d%H%M%S)"
mkdir -p "$stage_dir"

# 배포 임시 산출물(/tmp/accel-release-*, 업로드 번들, init 로그)이 누적돼 10GB 디스크를
# 채우는 것을 막는다. 스크립트가 성공하든 실패하든(EXIT) 이번 작업 디렉터리와 번들을 지우고,
# 과거 배포가 남긴 스테이징 디렉터리(1일 경과)·init 로그(7일 경과)도 함께 정리한다.
cleanup() {
  rm -rf "$stage_dir"
  rm -f "$BUNDLE_PATH"
  find /tmp -maxdepth 1 -name 'accel-release-*' -type d -mtime +0 -exec rm -rf {} + 2>/dev/null || true
  find /tmp -maxdepth 1 -name 'accel-init-*.log' -type f -mtime +7 -delete 2>/dev/null || true
}
trap cleanup EXIT

tar -xzf "$BUNDLE_PATH" -C "$stage_dir"

if ! id "$APP_USER" >/dev/null 2>&1; then
  sudo useradd --system --home "$APP_HOME" --shell /usr/sbin/nologin "$APP_USER"
fi

sudo mkdir -p "$BACKEND_DIR" "$WEB_ROOT" "$ACME_WEBROOT" /var/log/accel
sudo install -o "$APP_USER" -g "$APP_USER" -m 0644 \
  "$stage_dir/accel-back/target/accel-back-0.0.1-SNAPSHOT.jar" \
  "$BACKEND_DIR/accel-back.jar"
sudo install -o "$APP_USER" -g "$APP_USER" -m 0600 \
  "$stage_dir/accel-back/.env" \
  "$BACKEND_DIR/.env"
sudo install -o "$APP_USER" -g "$APP_USER" -m 0644 \
  "$stage_dir/accel-back/src/main/resources/accel_schema.sql" \
  "$BACKEND_DIR/accel_schema.sql"
sudo install -o "$APP_USER" -g "$APP_USER" -m 0644 \
  "$stage_dir/accel-back/src/main/resources/accel_data.sql" \
  "$BACKEND_DIR/accel_data.sql"

db_pass="$(openssl rand -base64 32 | tr -dc 'A-Za-z0-9' | head -c 24)"
# .env가 개행으로 끝나지 않으면 보정한다.
# (보정하지 않으면 아래 tee -a 추가분이 마지막 줄에 그대로 이어붙어
#  GCS_BUCKET_NAME 등 마지막 키 값이 깨진다. 예: accel-file-containerSERVER_ADDRESS=...)
if [[ -f "$BACKEND_DIR/.env" && -n "$(sudo tail -c1 "$BACKEND_DIR/.env")" ]]; then
  echo | sudo tee -a "$BACKEND_DIR/.env" >/dev/null
fi
sudo sed -i "s#^DATABASE_URL=.*#DATABASE_URL=jdbc:mysql://localhost:3306/${DB_NAME}?serverTimezone=Asia/Seoul#" "$BACKEND_DIR/.env"
sudo sed -i "s#^DATABASE_USERNAME=.*#DATABASE_USERNAME=${DB_USER}#" "$BACKEND_DIR/.env"
if sudo grep -q '^DATABASE_PASSWORD=' "$BACKEND_DIR/.env"; then
  sudo sed -i "s#^DATABASE_PASSWORD=.*#DATABASE_PASSWORD=${db_pass}#" "$BACKEND_DIR/.env"
else
  printf 'DATABASE_PASSWORD=%s\n' "$db_pass" | sudo tee -a "$BACKEND_DIR/.env" >/dev/null
fi
if sudo grep -q '^SERVER_ADDRESS=' "$BACKEND_DIR/.env"; then
  sudo sed -i 's#^SERVER_ADDRESS=.*#SERVER_ADDRESS=127.0.0.1#' "$BACKEND_DIR/.env"
else
  printf 'SERVER_ADDRESS=127.0.0.1\n' | sudo tee -a "$BACKEND_DIR/.env" >/dev/null
fi
if sudo grep -q '^REFRESH_COOKIE_PATH=' "$BACKEND_DIR/.env"; then
  sudo sed -i 's#^REFRESH_COOKIE_PATH=.*#REFRESH_COOKIE_PATH=/#' "$BACKEND_DIR/.env"
else
  printf 'REFRESH_COOKIE_PATH=/\n' | sudo tee -a "$BACKEND_DIR/.env" >/dev/null
fi
if sudo grep -q '^REFRESH_COOKIE_SECURE=' "$BACKEND_DIR/.env"; then
  sudo sed -i 's#^REFRESH_COOKIE_SECURE=.*#REFRESH_COOKIE_SECURE=true#' "$BACKEND_DIR/.env"
else
  printf 'REFRESH_COOKIE_SECURE=true\n' | sudo tee -a "$BACKEND_DIR/.env" >/dev/null
fi
if sudo grep -q '^REFRESH_COOKIE_SAME_SITE=' "$BACKEND_DIR/.env"; then
  sudo sed -i 's#^REFRESH_COOKIE_SAME_SITE=.*#REFRESH_COOKIE_SAME_SITE=Lax#' "$BACKEND_DIR/.env"
else
  printf 'REFRESH_COOKIE_SAME_SITE=Lax\n' | sudo tee -a "$BACKEND_DIR/.env" >/dev/null
fi
sudo chown "$APP_USER:$APP_USER" "$BACKEND_DIR/.env"
sudo chmod 600 "$BACKEND_DIR/.env"

sudo mysql <<SQL
CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${db_pass}';
ALTER USER '${DB_USER}'@'localhost' IDENTIFIED BY '${db_pass}';
CREATE USER IF NOT EXISTS '${DB_USER}'@'127.0.0.1' IDENTIFIED BY '${db_pass}';
ALTER USER '${DB_USER}'@'127.0.0.1' IDENTIFIED BY '${db_pass}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'127.0.0.1';
FLUSH PRIVILEGES;
-- binlog가 기본 30일치 쌓여 디스크를 채우지 않도록 3일로 제한(재기동 후에도 유지: SET PERSIST).
-- expire 설정만으로 오래된 binlog가 자동 purge 되며, binlog 비활성 환경에서도 에러 없이 통과한다.
SET PERSIST binlog_expire_logs_seconds = 259200;
SET PERSIST binlog_expire_logs_auto_purge = ON;
SQL

table_count="$(sudo mysql -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}' AND table_type='BASE TABLE';")"
if [[ "$FORCE_DB_INIT" == "true" || "$table_count" -eq 0 ]]; then
  run_init_profile_once
fi

if command -v rsync >/dev/null 2>&1; then
  sudo rsync -a --delete "$stage_dir/accel-front/dist/" "$WEB_ROOT/"
else
  sudo find "$WEB_ROOT" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
  sudo cp -a "$stage_dir/accel-front/dist/." "$WEB_ROOT/"
fi
sudo chown -R www-data:www-data "$WEB_ROOT"

# --- 디스크 고갈 재발 방지 -------------------------------------------------
# 1) journald: JVM 크래시 루프 시 로그가 디스크의 10%까지 차는 것을 200MB로 제한한다.
sudo mkdir -p /etc/systemd/journald.conf.d
sudo tee /etc/systemd/journald.conf.d/00-accel-size.conf >/dev/null <<'JOURNALD'
[Journal]
SystemMaxUse=200M
SystemKeepFree=500M
# 앱 로그가 journald와 /var/log/syslog 양쪽에 중복 적재돼 syslog가 폭증하는 것을 막는다.
# (로그는 journald 하나로 일원화: journalctl -u accel 로 확인)
ForwardToSyslog=no
JOURNALD
sudo systemctl restart systemd-journald

# 1-2) Google Cloud Ops Agent 자체 로그(/var/log/google-cloud-ops-agent)가 무한 증가해
#      디스크를 채우므로 logrotate로 상한을 둔다(copytruncate: 에이전트는 그대로 기록 지속).
sudo tee /etc/logrotate.d/accel-ops-agent >/dev/null <<'LOGROTATE'
/var/log/google-cloud-ops-agent/*.log /var/log/google-cloud-ops-agent/subagents/*.log {
    daily
    rotate 3
    size 50M
    compress
    missingok
    notifempty
    copytruncate
}
LOGROTATE

# 2) Spring Batch 메타데이터: chargerStatusJob이 5분마다 실행돼 무한 누적되므로
#    7일 경과분을 매일 03:30에 자동 삭제한다(자식 테이블부터 FK 순서대로).
sudo tee /opt/accel/prune-batch-metadata.sh >/dev/null <<PRUNE
#!/usr/bin/env bash
set -euo pipefail
mysql "${DB_NAME}" <<'SQL'
DELETE sec FROM BATCH_STEP_EXECUTION_CONTEXT sec
  JOIN BATCH_STEP_EXECUTION se ON sec.STEP_EXECUTION_ID = se.STEP_EXECUTION_ID
  JOIN BATCH_JOB_EXECUTION je ON se.JOB_EXECUTION_ID = je.JOB_EXECUTION_ID
  WHERE je.CREATE_TIME < NOW() - INTERVAL 7 DAY;
DELETE se FROM BATCH_STEP_EXECUTION se
  JOIN BATCH_JOB_EXECUTION je ON se.JOB_EXECUTION_ID = je.JOB_EXECUTION_ID
  WHERE je.CREATE_TIME < NOW() - INTERVAL 7 DAY;
DELETE jec FROM BATCH_JOB_EXECUTION_CONTEXT jec
  JOIN BATCH_JOB_EXECUTION je ON jec.JOB_EXECUTION_ID = je.JOB_EXECUTION_ID
  WHERE je.CREATE_TIME < NOW() - INTERVAL 7 DAY;
DELETE jep FROM BATCH_JOB_EXECUTION_PARAMS jep
  JOIN BATCH_JOB_EXECUTION je ON jep.JOB_EXECUTION_ID = je.JOB_EXECUTION_ID
  WHERE je.CREATE_TIME < NOW() - INTERVAL 7 DAY;
DELETE je FROM BATCH_JOB_EXECUTION je
  WHERE je.CREATE_TIME < NOW() - INTERVAL 7 DAY;
DELETE ji FROM BATCH_JOB_INSTANCE ji
  WHERE NOT EXISTS (SELECT 1 FROM BATCH_JOB_EXECUTION je WHERE je.JOB_INSTANCE_ID = ji.JOB_INSTANCE_ID);
SQL
PRUNE
sudo chmod 0755 /opt/accel/prune-batch-metadata.sh
sudo tee /etc/cron.d/accel-prune-batch >/dev/null <<'CRON'
30 3 * * * root /opt/accel/prune-batch-metadata.sh >/dev/null 2>&1
CRON
# --------------------------------------------------------------------------

sudo tee /etc/systemd/system/accel.service >/dev/null <<SERVICE
[Unit]
Description=ACCEL Spring Boot API
After=network-online.target mysql.service
Wants=network-online.target

[Service]
User=${APP_USER}
Group=${APP_USER}
WorkingDirectory=${BACKEND_DIR}
EnvironmentFile=${BACKEND_DIR}/.env
ExecStart=/usr/bin/java -Dserver.address=127.0.0.1 -jar ${BACKEND_DIR}/accel-back.jar
Restart=always
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
SERVICE

if sudo test -f "${CERT_DIR}/fullchain.pem" && sudo test -f "${CERT_DIR}/privkey.pem"; then
  sudo tee /etc/nginx/sites-available/accel >/dev/null <<NGINX
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name ${SERVER_NAMES} _;

    location ^~ /.well-known/acme-challenge/ {
        root ${ACME_WEBROOT};
        default_type "text/plain";
    }

    if (\$host = ${PRIMARY_DOMAIN}) {
        return 301 https://\$host\$request_uri;
    }

    if (\$host = ${WWW_DOMAIN}) {
        return 301 https://\$host\$request_uri;
    }

    root ${WEB_ROOT};
    index index.html;
    client_max_body_size 50m;

    location /assets/ {
        try_files \$uri =404;
        access_log off;
        expires 30d;
    }

    location /api/ {
        rewrite ^/api/(.*)\$ /\$1 break;
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 120s;
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name ${SERVER_NAMES};

    root ${WEB_ROOT};
    index index.html;
    client_max_body_size 50m;

    ssl_certificate ${CERT_DIR}/fullchain.pem;
    ssl_certificate_key ${CERT_DIR}/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers off;

    location ^~ /.well-known/acme-challenge/ {
        root ${ACME_WEBROOT};
        default_type "text/plain";
    }

    location /assets/ {
        try_files \$uri =404;
        access_log off;
        expires 30d;
    }

    location /api/ {
        rewrite ^/api/(.*)\$ /\$1 break;
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 120s;
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }
}
NGINX
else
  sudo tee /etc/nginx/sites-available/accel >/dev/null <<NGINX
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name ${SERVER_NAMES} _;

    root ${WEB_ROOT};
    index index.html;
    client_max_body_size 50m;

    location ^~ /.well-known/acme-challenge/ {
        root ${ACME_WEBROOT};
        default_type "text/plain";
    }

    location /assets/ {
        try_files \$uri =404;
        access_log off;
        expires 30d;
    }

    location /api/ {
        rewrite ^/api/(.*)\$ /\$1 break;
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 120s;
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }
}
NGINX
fi

sudo ln -sfn /etc/nginx/sites-available/accel /etc/nginx/sites-enabled/accel
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t

sudo systemctl daemon-reload
sudo systemctl enable accel >/dev/null
sudo systemctl restart accel
sudo systemctl restart nginx

for _ in {1..30}; do
  if curl -fsS http://127.0.0.1:8080/videos/categories >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

sudo systemctl --no-pager --full status accel | sed -n '1,25p'
curl -fsS http://127.0.0.1/api/videos/categories >/dev/null
curl -fsS http://127.0.0.1/ >/dev/null

echo "ACCEL_DEPLOY_OK"
