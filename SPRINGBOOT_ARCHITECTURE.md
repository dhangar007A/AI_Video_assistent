# ☕ Java Spring Boot Backend Architecture & Engineering Guide
### Tailored for MoveInSync / WorkInSync SDE-Intern (Java Backend)

---

## 🎯 Executive Summary

This document details the architectural design, concurrency patterns, database modeling, and testing strategy for the **Java Spring Boot 3 Backend** (`server-springboot/`) of the AI Video Assistant platform.

In high-throughput enterprise platforms like **MoveInSync** (fleet tracking, employee routing) and **WorkInSync** (workplace management), backend services must satisfy:
1. **Zero Thread Starvation**: Decoupling long-running compute tasks from HTTP request threads.
2. **Real-time Event Dissemination**: Efficient, unidirectional server-to-client streaming without the protocol overhead of bidirectional WebSockets.
3. **Resilient Error Propagation**: Clean domain-level exceptions translated into standardized REST contracts.
4. **Comprehensive Test Coverage**: Robust automated unit and integration tests.

---

## 🏗️ Layered Architecture & Component Breakdown

```
com.aivideoassistant
├── config/
│   ├── AsyncConfig.java          # ThreadPoolTaskExecutor for async background execution
│   ├── MongoConfig.java          # Enables MongoDB auditing (@CreatedDate, @LastModifiedDate)
│   ├── OpenApiConfig.java        # Swagger / OpenAPI 3 API documentation
│   └── WebConfig.java            # CORS mappings and static resource handlers
├── controller/
│   ├── ChatController.java       # RAG Q&A endpoints (/api/chat)
│   ├── HealthController.java     # Service health check (/api/health)
│   └── MeetingController.java    # Meeting lifecycle & SSE streaming (/api/meetings)
├── dto/
│   ├── ChatRequest.java          # Validated question input
│   ├── ChatResponse.java         # RAG answer and conversation history
│   ├── CreateMeetingRequest.java # Validated URL submission
│   ├── ErrorResponse.java        # RFC 7807-compliant standard error payload
│   ├── MeetingSummaryResponse.java # Lightweight projection for list views
│   └── StartPipelineResponse.java # Pipeline initialization response
├── exception/
│   ├── BadRequestException.java  # HTTP 400
│   ├── GlobalExceptionHandler.java # @RestControllerAdvice centralized error interceptor
│   └── ResourceNotFoundException.java # HTTP 404
├── model/
│   ├── ChatMessage.java          # Embedded document for chat history
│   ├── Meeting.java              # Primary MongoDB document entity
│   └── PipelineEvent.java        # Deserialized Python event payload
├── repository/
│   └── MeetingRepository.java    # Spring Data Mongo repository with derived query methods
└── service/
    ├── ChatService.java          # Interface for RAG conversational search
    ├── FileStorageService.java   # Interface for multipart media uploads
    ├── MeetingService.java       # Interface for meeting business logic & vector DB cleanup
    ├── MeetingSseService.java    # Interface for thread-safe SSE event broadcasting
    ├── PipelineExecutionService.java # Interface for async AI process execution
    └── impl/
        ├── ChatServiceImpl.java
        ├── FileStorageServiceImpl.java
        ├── MeetingServiceImpl.java
        ├── MeetingSseServiceImpl.java
        └── PipelineExecutionServiceImpl.java
```

---

## ⚡ Concurrency & Multithreading Model

### 1. ThreadPoolTaskExecutor (Async Task Offloading)
In `AsyncConfig.java`:
```java
@Bean(name = "pipelineTaskExecutor")
public Executor pipelineTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("PipelineAsync-");
    executor.initialize();
    return executor;
}
```
- **Why this matters**: A meeting transcription and summarization pipeline takes between 15 seconds to several minutes depending on media length.
- If executed synchronously on Tomcat's HTTP worker threads (`http-nio-5000-exec-*`), a few concurrent requests would exhaust the thread pool, causing HTTP connection timeouts for all other users.
- By annotating `runPipelineAsync` with `@Async("pipelineTaskExecutor")`, the HTTP thread immediately returns `201 CREATED` with the `meetingId`, and execution continues on a dedicated worker pool.

### 2. Thread-Safe Server-Sent Events (`MeetingSseService`)
`MeetingSseServiceImpl` maintains active client connections using concurrent data structures:
```java
private final Map<String, List<SseEmitter>> sseClients = new ConcurrentHashMap<>();
```
- Each meeting ID maps to a `CopyOnWriteArrayList<SseEmitter>`.
- **Thread Safety**: Multiple clients (or multiple tabs) can observe the same meeting without risking `ConcurrentModificationException` during broadcast iterations.
- **Resource Leak Prevention**: Every `SseEmitter` registers `onCompletion`, `onTimeout`, and `onError` handlers that systematically prune stale or disconnected emitters.

### 3. Non-Blocking Process Management with `ProcessBuilder`
In `PipelineExecutionServiceImpl`:
- Spawns the Python AI process (`api_bridge.py`) in the background.
- Employs dedicated worker threads for reading `stdout` and `stderr` concurrently, preventing deadlocks caused by OS buffer saturation.
- Parses structured JSON lines (`progress`, `result`, `error`) and invokes the SSE broadcast manager.

---

## 💾 Spring Data MongoDB Modeling

### Document Entity: `Meeting`
- Stored in the `meetings` collection.
- Dual ID serialization: Uses `@Id private String id` along with `@JsonProperty("_id")` to ensure 100% interoperability with existing frontend clients expecting MongoDB's native `_id` convention.
- **Embedded Document vs. Reference**: `chatHistory` is embedded directly within `Meeting` as a `List<ChatMessage>` because:
  1. A chat history is strictly scoped to its parent meeting.
  2. Reading meeting details and chat messages in a single atomic database read minimizes network roundtrips.
- **Data Projection**: `MeetingSummaryResponse` projects the document into a lightweight DTO omitting large text columns (`transcript`, `chatHistory`) when querying `/api/meetings`, drastically cutting network bandwidth.

---

## 🛡️ Exception Handling & API Contracts

`GlobalExceptionHandler` uses `@RestControllerAdvice` to translate Java exceptions into consistent, predictable JSON structures:

```json
{
  "error": "Meeting not found",
  "status": 404,
  "timestamp": "2026-09-24T00:15:09Z"
}
```

Handled exceptions:
- `ResourceNotFoundException` -> `404 Not Found`
- `BadRequestException` -> `400 Bad Request`
- `MethodArgumentNotValidException` -> `400 Bad Request` (Bean validation errors aggregated)
- `MaxUploadSizeExceededException` -> `400 Bad Request` (File upload size ceiling)
- `Exception` -> `500 Internal Server Error`

---

## 🧪 Testing & Quality Assurance

The test suite leverages **JUnit 5** and **Mockito** with isolated test slices:

1. **Unit Testing (`MeetingServiceTest`)**:
   - Tests business logic in isolation using `@ExtendWith(MockitoExtension.class)`.
   - Validates creation, validation failures, projection mapping, and vector database cleanup.
2. **Controller Slice Testing (`MeetingControllerTest`, `ChatControllerTest`, `HealthControllerTest`)**:
   - Uses `@WebMvcTest` with `MockMvc`.
   - Verifies HTTP status codes (`201 CREATED`, `200 OK`, `400 BAD_REQUEST`, `404 NOT_FOUND`), request body deserialization, and JSONPath assertions.

Run tests:
```powershell
cd server-springboot
.\mvnw.cmd test
```

---

## 🎤 Interview Guide: MoveInSync / WorkInSync SDE-Intern

Use these talking points during technical interview discussions:

### 1. "Can you tell us about a Java Spring Boot backend project you developed?"
> *"I designed and implemented an enterprise Spring Boot 3 backend for an AI Meeting Intelligence platform. The system handles media ingestion, coordinates long-running AI pipelines (Whisper, Mistral, ChromaDB RAG), and streams real-time status updates to clients using Server-Sent Events. I designed it following Clean Architecture principles, with custom thread pools for asynchronous process orchestration, Spring Data MongoDB for persistence, and full unit test coverage using JUnit 5 and Mockito."*

### 2. "Why did you use Server-Sent Events (SSE) instead of WebSockets?"
> *"Our AI pipeline requires unidirectional updates from the server to the client (downloading -> transcribing -> summarizing -> RAG ready). WebSockets provide full-duplex communication but introduce connection state complexity, custom heartbeat frames, and proxy traversal overhead. SSE runs over standard HTTP/1.1 or HTTP/2, supports automatic browser reconnection natively via the EventSource API, and is much simpler to scale and monitor in a Spring Boot environment using `SseEmitter`."*

### 3. "How did you prevent long-running AI tasks from freezing Tomcat?"
> *"Tomcat assigns a worker thread from its bounded connector pool to each incoming HTTP request. To prevent long AI tasks from tying up these threads, I implemented a decoupled architecture: the controller immediately returns a `201 CREATED` response with the task ID, while the actual process execution is dispatched to a custom `ThreadPoolTaskExecutor` using `@Async`. The client then connects to an SSE endpoint where progress events are pushed as they occur."*

### 4. "How did you ensure thread safety when broadcasting progress to multiple clients?"
> *"I maintained a registry using `ConcurrentHashMap` where each meeting ID maps to a `CopyOnWriteArrayList<SseEmitter>`. `CopyOnWriteArrayList` creates a snapshot of the array on modification, making iteration during event broadcast completely thread-safe without manual locks. Furthermore, I registered `onCompletion`, `onTimeout`, and `onError` lifecycle hooks on each emitter to cleanly prune disconnected clients and avoid memory leaks."*

### 5. "How is vector database storage managed when a meeting is deleted?"
> *"Each meeting is allocated an isolated ChromaDB vector index in a separate directory (`vector_db_<meetingId>`). In `MeetingServiceImpl.deleteMeeting()`, after deleting the MongoDB entity, I implemented a directory cleanup utility that traverses the directory tree using Java NIO `Files.walk()` in reverse topological order, deleting all indexed vector files to reclaim disk space."*
