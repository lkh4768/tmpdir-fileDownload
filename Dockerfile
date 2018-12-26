FROM java:8
ARG PACKAGE_NAME
ARG PACKAGE_VERSION
ENV PACKAGE_NAME ${PACKAGE_NAME}
ENV PACKAGE_VERSION ${PACKAGE_VERSION}

COPY build/libs/${PACKAGE_NAME}-${PACKAGE_VERSION}.jar /
COPY src/main/resources/*.properties /
COPY script/docker-entrypoint.sh /
RUN mkdir -p /app/bin /app/config /applog /storage \
	&& mv /${PACKAGE_NAME}-${PACKAGE_VERSION}.jar /app/bin/ \
	&& chmod +x /docker-entrypoint.sh

VOLUME ["/app/config", "/applog", "/storage"]
EXPOSE 6001

ENTRYPOINT ["/docker-entrypoint.sh"]

CMD /usr/bin/java -jar /app/bin/${PACKAGE_NAME}-${PACKAGE_VERSION}.jar --spring.config.location=file:/app/config/
