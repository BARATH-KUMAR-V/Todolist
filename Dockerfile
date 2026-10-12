FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY TodoApp.java .
RUN javac --add-modules jdk.httpserver TodoApp.java
EXPOSE 8000
CMD ["java","--add-modules","jdk.httpserver","-cp","/app","TodoApp"]