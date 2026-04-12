# Stardust (Java Port)

Stardust is a minimal discussion forum built with Java and Spring Boot.

This application was ported from an earlier Python/Flask project called CircusCircus. The goal of this repo is to keep the original forum concept while implementing it using a standard Spring stack (MVC, templates, data repositories, and security).

## What It Includes

- user accounts and authentication
- subforums and posts
- comments on posts
- server-rendered pages using templates
- starter data initialization on first run

## Tech Stack

- Java 17+
- Spring Boot
- Spring MVC + Thymeleaf templates
- Spring Data JPA
- Spring Security
- Maven

## Running the App

From the `java/` directory:

```bash
mvn spring-boot:run
```

Or build and run the jar:

```bash
mvn clean package
java -jar target/Stardust-0.0.1-SNAPSHOT.jar
```

By default, the app runs on:

`http://localhost:8080`

## Notes on the Port

- The original Python project structure and setup instructions no longer apply in this Java module.
- Data and configuration are managed through Spring Boot settings in `src/main/resources/application.properties`.
- Startup data (such as default subforums) is initialized in the Java configuration layer.

## Suggested Next Improvements

- richer post formatting (markdown support)
- reactions (like/dislike)
- direct messaging
- user profile/settings pages
- media embedding (image/video links)
- moderation tools and role-based controls

to run it- cd java
mvn spring-boot:run

curl -s -b cookies.txt http://localhost:5000/viewpost?post=1 | grep -o "action_react" | head -1

curl -s -o /dev/null -w "%{http_code}" -X POST \
  -b cookies.txt -c cookies.txt \
  -d "postId=1&type=LIKE" \
  http://localhost:5000/action_react

Reaction.java
→
ReactionRepository
→
ForumService
→
ForumController
→
viewpost.html

lsof -ti :5000 | xargs kill -9 2>/dev/null && mvn spring-boot:run