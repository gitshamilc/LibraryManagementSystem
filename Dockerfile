# Use a lightweight OpenJDK base image
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy the entire project into the Docker container
COPY . .

# Compile the Java application
RUN javac -cp "lib/*" -d out src/*.java src/core/*.java src/models/*.java src/dao/*.java src/controllers/*.java src/services/*.java

# Run the compiled application
CMD ["java", "-cp", "out:lib/*", "Main"]
