# Notification Service

Multi-channel notification service for Carbon Credit Marketplace with support for Email, SMS, Push, and In-App notifications.

## Features

### Core Features
- **Multi-Channel Support**: Email, SMS, Push Notifications, In-App Notifications
- **Template Management**: Dynamic template system with variable substitution
- **Async Processing**: RabbitMQ integration for asynchronous notification delivery
- **Delivery Tracking**: Track notification status and delivery confirmation
- **Retry Logic**: Automatic retry with exponential backoff for failed notifications
- **Rate Limiting**: Prevent notification spam with configurable rate limits
- **User Preferences**: Respect user notification preferences and quiet hours
- **Real-time Updates**: WebSocket support for instant in-app notifications
- **Bulk Notifications**: Send notifications to multiple users efficiently

### Technical Features
- **Priority Queues**: Different priority levels for time-sensitive notifications
- **Idempotency**: Prevent duplicate notifications with idempotency keys
- **Template Engines**: Support for FreeMarker and Thymeleaf templates
- **Caching**: Redis caching for templates and rate limiting
- **Monitoring**: Health checks and metrics endpoints
- **Audit Trail**: Complete notification history and delivery logs

## Architecture

### Components

1. **RabbitMQ Queues**
   - Email Queue (Priority: 5)
   - SMS Queue (Priority: 10)
   - Push Queue (Priority: 7)
   - In-App Queue (Priority: 3)

2. **Service Providers**
   - **Email**: SendGrid, SMTP
   - **SMS**: Twilio
   - **Push**: Firebase Cloud Messaging
   - **In-App**: WebSocket/STOMP

3. **Data Storage**
   - **PostgreSQL**: Notification history, templates, preferences
   - **Redis**: Rate limiting, caching

## API Endpoints

### Notification Management
- `POST /api/v1/notifications/send` - Send single notification
- `POST /api/v1/notifications/send-bulk` - Send bulk notifications
- `GET /api/v1/notifications/user/{userId}` - Get user notifications
- `GET /api/v1/notifications/user/{userId}/unread` - Get unread notifications
- `GET /api/v1/notifications/user/{userId}/unread-count` - Get unread count
- `PATCH /api/v1/notifications/{notificationId}/read` - Mark as read
- `PATCH /api/v1/notifications/user/{userId}/read-all` - Mark all as read

### Preferences
- `GET /api/v1/notifications/preferences/{userId}` - Get preferences
- `PUT /api/v1/notifications/preferences/{userId}` - Update preferences

### Admin
- `GET /api/v1/notifications/stats/rate-limits/{userId}` - Get rate limit stats
- `POST /api/v1/notifications/admin/reset-rate-limits/{userId}` - Reset rate limits

### WebSocket
- `/ws/notifications` - WebSocket endpoint for real-time notifications

## Notification Types

```java
USER_REGISTERED, KYC_SUBMITTED, KYC_APPROVED, KYC_REJECTED,
PASSWORD_RESET, EMAIL_VERIFICATION, TWO_FACTOR_AUTH,
VEHICLE_ADDED, VEHICLE_VERIFIED, TRIP_SYNCED, TRIP_ANOMALY_DETECTED,
CREDITS_EARNED, VERIFICATION_REQUESTED, VERIFICATION_APPROVED, VERIFICATION_REJECTED,
LISTING_CREATED, LISTING_SOLD, LISTING_EXPIRED,
BID_PLACED, BID_WON, BID_OUTBID, AUCTION_ENDING_SOON,
PAYMENT_RECEIVED, PAYMENT_SENT, PAYMENT_FAILED, WITHDRAWAL_COMPLETED,
SYSTEM_MAINTENANCE, FEATURE_UPDATE, POLICY_UPDATE, PROMOTIONAL
```

## Configuration

### Environment Variables

```yaml
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/carbon_credit_db
spring.datasource.username=ccm_user
spring.datasource.password=ccm_password

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Redis
spring.redis.host=localhost
spring.redis.port=6379

# SendGrid
SENDGRID_ENABLED=true
SENDGRID_API_KEY=your-api-key

# Twilio
TWILIO_ENABLED=true
TWILIO_ACCOUNT_SID=your-account-sid
TWILIO_AUTH_TOKEN=your-auth-token
TWILIO_FROM_NUMBER=+1234567890

# Firebase
FIREBASE_ENABLED=true
FIREBASE_PROJECT_ID=your-project-id
```

## Running the Service

### Local Development

```bash
# Install dependencies
mvn clean install

# Run the service
mvn spring-boot:run

# Or using Java
java -jar target/notification-service-1.0.0-SNAPSHOT.jar
```

### Docker

```bash
# Build image
docker build -t notification-service .

# Run container
docker run -p 8084:8084 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/carbon_credit_db \
  -e SPRING_RABBITMQ_HOST=host.docker.internal \
  -e SPRING_REDIS_HOST=host.docker.internal \
  notification-service
```

### Docker Compose

```yaml
notification-service:
  build: ./backend/notification-service
  ports:
    - "8084:8084"
  environment:
    - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/carbon_credit_db
    - SPRING_RABBITMQ_HOST=rabbitmq
    - SPRING_REDIS_HOST=redis
  depends_on:
    - postgres
    - rabbitmq
    - redis
```

## Testing

### Send Test Notification

```bash
curl -X POST http://localhost:8084/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "type": "USER_REGISTERED",
    "title": "Welcome to Carbon Credit Marketplace",
    "message": "Your account has been created successfully",
    "channels": ["EMAIL", "IN_APP"]
  }'
```

### WebSocket Test (JavaScript)

```javascript
const socket = new SockJS('http://localhost:8084/ws/notifications');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function() {
    // Subscribe to user notifications
    stompClient.subscribe('/queue/notifications/user-id', function(notification) {
        console.log('Received:', JSON.parse(notification.body));
    });
    
    // Subscribe to marketplace events
    stompClient.subscribe('/topic/marketplace.listing_created', function(event) {
        console.log('New listing:', JSON.parse(event.body));
    });
});
```

## Monitoring

- Health Check: `http://localhost:8084/api/v1/notifications/health`
- Metrics: `http://localhost:8084/actuator/metrics`
- Prometheus: `http://localhost:8084/actuator/prometheus`

## Dependencies

- Spring Boot 3.1.0
- Spring Security
- Spring Data JPA
- Spring AMQP (RabbitMQ)
- Spring WebSocket
- PostgreSQL
- Redis
- SendGrid SDK
- Twilio SDK
- Firebase Admin SDK
- FreeMarker & Thymeleaf
- Lombok
- OpenAPI 3.0
