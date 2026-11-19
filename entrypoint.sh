#!/bin/sh
set -e

# Tu dong lay PORT do Cloud (Render/Railway/Koyeb) cung cap hoac mac dinh 8088
TARGET_PORT="${PORT:-8088}"
echo "=========================================================="
echo "Khoi dong UDP Multichat Enterprise tren cong: $TARGET_PORT"
echo "=========================================================="

# 1. Doi cong HTTP connector sang cong do Cloud chi dinh
sed -i "s/port=\"8080\"/port=\"$TARGET_PORT\"/g" /usr/local/tomcat/conf/server.xml
sed -i "s/port=\"8088\"/port=\"$TARGET_PORT\"/g" /usr/local/tomcat/conf/server.xml

# 2. Tat cong shutdown 8005 (dat port="-1") de tranh health check cua Cloud gay ra warning
sed -i 's/<Server port="8005"/<Server port="-1"/g' /usr/local/tomcat/conf/server.xml
sed -i 's/<Server port="[0-9]*"/<Server port="-1"/g' /usr/local/tomcat/conf/server.xml

exec catalina.sh run
