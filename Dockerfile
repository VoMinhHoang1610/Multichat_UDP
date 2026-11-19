# Su dung image Apache Tomcat 10.1 chinh thuc voi JDK 21
FROM tomcat:10.1-jdk21-temurin-jammy

WORKDIR /usr/local/tomcat

# Xoa cac ung dung mac dinh cua Tomcat de giai phong bo nho
RUN rm -rf webapps/*

# Sao chep ma nguon va thu muc web vao container
COPY src ./src
COPY web ./web
COPY entrypoint.sh ./entrypoint.sh

# Chuyen quyen thuc thi cho entrypoint script
RUN chmod +x ./entrypoint.sh

# Bien dich ma nguon Java vao web/WEB-INF/classes
RUN mkdir -p web/WEB-INF/classes && \
    javac -encoding UTF-8 -cp "lib/*" -d web/WEB-INF/classes src/multichat_udp/*.java src/multichat_udp/servlet/*.java

# Dua toan bo thu muc web lam ung dung goc (ROOT)
RUN cp -r web webapps/ROOT

# Mo cong mac dinh (Web & UDP Multicast)
EXPOSE 8088
EXPOSE 8888/udp
EXPOSE 8889/udp

# Khoi chay container voi script thich ung cong dong tren Cloud
ENTRYPOINT ["/usr/local/tomcat/entrypoint.sh"]
