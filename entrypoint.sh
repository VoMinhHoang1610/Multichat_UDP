#!/bin/sh
set -e

# Tu dong lay PORT do Cloud (Render/Railway/Koyeb) cung cap hoac mac dinh 8088
TARGET_PORT="${PORT:-8088}"
echo "=========================================================="
echo "Khoi dong UDP Multichat Enterprise tren cong: $TARGET_PORT"
echo "=========================================================="

sed -i "s/port=\"8080\"/port=\"$TARGET_PORT\"/g" /usr/local/tomcat/conf/server.xml
sed -i "s/port=\"8088\"/port=\"$TARGET_PORT\"/g" /usr/local/tomcat/conf/server.xml

exec catalina.sh run
