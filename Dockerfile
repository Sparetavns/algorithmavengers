FROM cimg/openjdk:8.0.472
VOLUME /tmp
ARG JAR_FILE
EXPOSE 8080
ADD ${JAR_FILE} algorithm-avengers.jar
CMD java $JVM_OPTS  -jar ./algorithm-avengers.jar $PROG_ARGS
