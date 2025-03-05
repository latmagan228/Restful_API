# Simple RESTful API Task Manager

This project demonstrates a simple RESTful API for task management using Java's built-in HttpServer.

## Components

- **TaskServer.java**: Implements a simple HTTP server that handles CRUD operations for tasks


## API Endpoints

- `GET /tasks`: List all tasks
- `POST /tasks`: Create a new task
- `PUT /tasks/{id}`: Update a task by ID
- `DELETE /tasks/{id}`: Delete a task by ID

## How to Run

# Compile everything
mvn compile

# Run the server (from the project root directory)
java -jar target/restapi-1.0-SNAPSHOT.jar

## Using Postman to test the project

This project uses JWT tokens for authentification:

First, get a token by logging in:
-Method: POST
-URL: http://localhost:8081/auth/login
-Body:
  {
       "username": "admin",
       "password": "password123"
   }

For all other requests (GET, POST, PUT, DELETE), you need to:
-Add the Authorization header
-Set its value to: Bearer YOUR_TOKEN (replace YOUR_TOKEN with the token you received from login)





