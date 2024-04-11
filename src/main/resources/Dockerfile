FROM tomcat:9-jdk17
LABEL authors="tipil"

ENV JAVA_OPTS "$JAVA_OPTS --add-opens=java.base/java.lang=ALL-UNNAMED -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8001"
ADD api.war /usr/local/tomcat/webapps/api.war
EXPOSE 8080/tcp
EXPOSE 8001/tcp
CMD ["catalina.sh", "run"]