FROM docker.io/eclipse-temurin:22-jre-ubi9-minimal

WORKDIR /opt/app

ADD target/pushover-notifier.jar /opt/app/

ENTRYPOINT ["java", "-jar", "pushover-notifier.jar"]
