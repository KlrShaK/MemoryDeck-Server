# Memory Deck

## Introduction

Memory Deck aims to help elderly individuals and those with memory loss retain valuable information through interactive flashcards. This is the backend of Memory Deck — a useful tool designed to help users (especially the elderly or those with memory challenges) retain information through interactive decks of flashcards. The app emphasizes simplicity and clarity, enhancing its usability for its target audience.
<br><br>
## Technologies Used

- Java
- Google Cloud
- OpenAI API
- Unittest
- Springboot
- H2 DB
<br><br>
## High-Level Components

The backend is structured into modular RESTful components, each responsible for a core part of the Memory Deck system. These components follow a layered architecture with **Controllers** (handling HTTP requests), **Services** (business logic), and **Repositories** (database access).

### Flashcard Controller & Service
- **Files**: [FlashcardController.java](https://github.com/KlrShaK/MemoryDeck-Server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/controller/FlashcardController.java), [FlashcardService.java](https://github.com/KlrShaK/MemoryDeck-Server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/FlashcardService.java)
- **Role**: Manages creation, retrieval, updating, and deletion of decks and flashcards.
- **Logic**: Handles all operations related to deck and flashcard entities. Each flashcard is associated with a deck, and the logic ensures proper grouping and consistency. This component supports the core content structure for the quizzes and plays a key role in user-driven content creation and memory training.

### Quiz Controller & Service
- **Files**: [QuizController.java](https://github.com/KlrShaK/MemoryDeck-Server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/controller/QuizController.java), [QuizService.java](https://github.com/KlrShaK/MemoryDeck-Server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/QuizService.java)
- **Role**: Generates quizzes from flashcards within a deck.
- **Logic**: Randomizes questions, tracks user input, and supports interactive quiz experiences. Includes invitation logic and ensures real-time notification between users. Results can be used for statistics and performance tracking.

### Statistics Controller & Service
- **Files**: [StatisticsController.java](https://github.com/KlrShaK/MemoryDeck-Server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/controller/StatisticsController.java), [StatisticsService.java](https://github.com/KlrShaK/MemoryDeck-Server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/StatisticsService.java)
- **Role**: Tracks and reports user progress and performance.
- **Logic**: Calculates scores, monitors correct/incorrect answers, and provides data for feedback and therapy effectiveness.

### User Controller & Service
- **Files**: [UserController.java](https://github.com/KlrShaK/MemoryDeck-Server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/controller/UserController.java), [UserService.java](https://github.com/KlrShaK/MemoryDeck-Server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/UserService.java)
- **Role**: Handles user registration, authentication, and profile management.
- **Logic**: Verifies unique usernames, manages user tokens, and stores metadata such as birthday or associated decks.
<br><br>
## Launch & Deployment

To get the backend server up and running locally, follow these steps:

### Prerequisites

- Java 17+
- Gradle (we use the Gradle Wrapper, so no need to install Gradle separately)
- OpenAI API key
- Google Cloud credentials

### Running the Application

#### 1. Clone the repository:

```
git clone https://github.com/yourusername/MemoryDeck-Server.git
cd MemoryDeck-Server
```

#### 2. Add secrets:

You will need to configure the following secrets as environment variables or in your GitHub repo if running CI/CD:
- ```OPENAI_API_KEY```: Your OpenAI key for generating content or interactions.
- ```GOOGLE_APPLICATION_CREDENTIALS```: Path to your Google Cloud credentials JSON file (used for storage or other GCP integrations).

#### 3. Build the project:

```
./gradlew build
```

#### 4. Run the server:

- Option 1: Via command line:
```
./gradlew bootRun
```
- Option 2: Launch directly in Visual Studio Code using the Spring Boot extension.

#### 5. Running tests:
```
./gradlew test
```

### Building with Gradle
You can use the local Gradle Wrapper to build the application.
-   macOS: `./gradlew`
-   Linux: `./gradlew`
-   Windows: `./gradlew.bat`

### Deployment
For deployment (Google Cloud), make sure your environment has access to the required secrets listed above and configure your application.properties or application.yml accordingly. The project can be accessed through the URL: [https://sopra-fs25-group-40-client.vercel.app/](https://sopra-fs25-group-40-client.vercel.app/)
<br><br>

## Roadmap

Here are some possible features new contributors could add (includes the optional user story we did not implement):
- **Search and Filter Decks**: Implement a search bar or filter tags to allow users to easily find specific decks.
- **Flashcard Enhancements**: Support rich content in flashcards, such as audio clips or formatted text.
- **Scheduled Test Reminders**: Implement a scheduling and automated email reminder system that will notify a user to take their daily test at a previously scheduled reminder time.
<br><br>
## License
This project is licensed under the MIT License. See the [LICENSE](https://choosealicense.com/licenses/mit/) file for details.
<br><br>
## Authors and acknowledgement

This project has come to life thanks to the members of Group 40 of the SoPra module: Melih Serin (melih.serin@uzh.ch), Sarah Nabulsi (sarahosama.nabulsi@uzh.ch), Nicola Luder (nicola.luder@uzh.ch), Shaurya Kishore Panwar (shauryakishore.panwar@uzh.ch), and Leyla Khasiyeva (leyla.khasiyeva@uzh.ch). If you have any questions or comments, you can reach out to us at any of the mentioned email addresses.

<br><br>
