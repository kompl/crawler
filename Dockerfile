# Используем базовый образ с JDK 17
FROM eclipse-temurin:17-jdk-jammy AS build

# Устанавливаем рабочую директорию
WORKDIR /app

# Устанавливаем SBT
RUN apt-get update && \
    apt-get install -y curl && \
    curl -L -o sbt.deb https://repo.scala-sbt.org/scalasbt/debian/sbt-1.8.2.deb && \
    dpkg -i sbt.deb && \
    rm sbt.deb && \
    apt-get clean

# Копируем файлы проекта
COPY . .

# Собираем проект и создаем JAR-файл с помощью sbt-assembly
RUN sbt 'set assembly / assemblyOutputPath := new File("/app/target/crawler.jar")' assembly

# Создаем финальный образ
FROM eclipse-temurin:17-jre-jammy

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR-файл из предыдущего этапа
COPY --from=build /app/target/crawler.jar /app/crawler.jar
RUN   mkdir -p /app/config
COPY ./config/main.yml.default /app/config/main.yml

# Указываем команду для запуска приложения
CMD ["java", "-jar", "/app/crawler.jar"]
