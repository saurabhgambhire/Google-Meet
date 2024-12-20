# Google Meet Integration with Spring Boot

This project demonstrates how to integrate Google Meet with a Spring Boot application using OAuth2 for authentication.


### Prerequisites

- Java 23
- Maven
- Google Cloud account with OAuth2 credentials

## Cloning the Repository

To clone the repository, run the following command:

```sh
git clone https://github.com/saurabhgambhire/Google-Meet.git
```

```sh
cd Google-Meet 
```


### Configuration

Update the `src/main/resources/application.yml` file with your Google OAuth2 credentials:

```yml
google:
  oauth:
    client-id: YOUR_CLIENT_ID
    client-secret: YOUR_CLIENT_SECRET
    redirect-uri: http://localhost:8083/api/master/google-meet/create-space
    token-url: https://oauth2.googleapis.com/token
```

### Building the Project
To build the project, run the following command:

```cmd
 ./mvnw clean install
```

### Running the Application
To run the application, use the following command:

```cmd
 ./mvnw spring-boot:run 
```

### API Endpoints
- GET /api/master/google-meet/auth: Generates an authorization URL for Google Meet.
- GET /api/master/google-meet/create-space: Creates a Google Meet space using the provided authorization code and state.

### Author
- Saurabh Gambhire
