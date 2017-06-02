FROM maven:3-jdk-8-alpine

ADD ./ /rap

EXPOSE 8080

WORKDIR /rap

RUN mvn clean package cargo:start

ENTRYPOINT ["entrypoint.sh"]