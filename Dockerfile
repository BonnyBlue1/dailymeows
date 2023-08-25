FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.5_8_1.9.3_3.3.0 as stage0
WORKDIR /dailymeows-build

COPY ./build.sbt ./
COPY ./project ./project

COPY ./src ./src

RUN sbt stage
RUN sbt docker:stage

FROM openjdk:17 as mainstage
USER root
WORKDIR /opt/docker
COPY --from=stage0 /dailymeows-build/target/docker/stage/2/opt/docker /opt/docker
COPY --from=stage0 /dailymeows-build/target/docker/stage/4/opt/docker /opt/docker
ENTRYPOINT [ "/opt/docker/bin/dailymeows" ]