# Marketplace Service

## Overview
The Marketplace Service is responsible for managing carbon credit listings, auctions, shopping cart functionality, and price recommendations in the Carbon Credit Marketplace platform.

## Features
- **Listing Management**: Create, update, and manage fixed-price and auction listings
- **Advanced Search**: Elasticsearch-powered search with filters for region, price, amount, and more
- **Real-time Auctions**: WebSocket-enabled real-time bidding with auto-extension logic
- **AI Price Recommendations**: Historical data-based price suggestions using machine learning
- **Shopping Cart**: 30-minute reservation system for fixed-price listings
- **Market Analytics**: Price history tracking and trend analysis

## Technology Stack
- **Java 17**
- **Spring Boot 3.x**
- **Spring Data JPA** (PostgreSQL)
- **Spring Data Elasticsearch**
- **Spring WebSocket** (STOMP)
- **Spring Security** (JWT)
- **Redis** (Caching)
- **RabbitMQ** (Event messaging)
- **Lombok**
- **MapStruct**
- **OpenAPI/Swagger**

## Architecture

### Database Schema
```sql
-- Main Listings Table
CREATE TABLE listings (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL,
    credit_id UUID NOT NULL,
    listing_type VARCHAR(20) NOT NULL,
    credit_amount_tons DECIMAL(10,3) NOT NULL,
    available_amount_tons DECIMAL(10,3) NOT NULL,
    price_per_ton_vnd DECIMAL(15,2),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_seller FOREIGN KEY (seller_id) REFERENCES users(id)
);

-- Bids Table
CREATE TABLE bids (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL,
    bidder_id UUID NOT NULL,
    bid_amount_per_ton DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_listing FOREIGN KEY (listing_id) REFERENCES listings(id)
);

-- Cart Items Table
CREATE TABLE cart_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    listing_id UUID NOT NULL,
    quantity_tons DECIMAL(10,3) NOT NULL,
    price_per_ton_vnd DECIMAL(15,2) NOT NULL,
    reservation_expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

-- Transactions Table
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    transaction_code VARCHAR(50) UNIQUE NOT NULL,
    listing_id UUID NOT NULL,
    buyer_id UUID NOT NULL,
    seller_id UUID NOT NULL,
    credit_amount_tons DECIMAL(10,3) NOT NULL,
    total_amount_vnd DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Price History Table
CREATE TABLE price_history (
    id UUID PRIMARY KEY,
    date DATE NOT NULL,
    region VARCHAR(100),
    avg_price_per_ton DECIMAL(15,2),
    min_price_per_ton DECIMAL(15,2),
    max_price_per_ton DECIMAL(15,2),
    transaction_count INTEGER,
    created_at TIMESTAMP NOT NULL
);
```

## API Endpoints

### Marketplace Listings
- `GET /api/marketplace/listings` - Search listings
- `GET /api/marketplace/listings/{id}` - Get listing details
- `POST /api/marketplace/listings` - Create listing (Seller only)
- `PUT /api/marketplace/listings/{id}` - Update listing (Seller only)
- `DELETE /api/marketplace/listings/{id}` - Cancel listing (Seller only)
- `GET /api/marketplace/my-listings` - Get user's listings
- `GET /api/marketplace/trending` - Get trending listings

### Auctions
- `POST /api/marketplace/auctions/bid` - Place bid
- `GET /api/marketplace/auctions/{id}/bids` - Get bid history
- `GET /api/marketplace/auctions/my-bids` - Get user's bids

### Shopping Cart
- `POST /api/marketplace/cart/items` - Add to cart
- `GET /api/marketplace/cart` - Get cart
- `PUT /api/marketplace/cart/items/{id}` - Update cart item
- `DELETE /api/marketplace/cart/items/{id}` - Remove from cart
- `DELETE /api/marketplace/cart` - Clear cart
- `POST /api/marketplace/cart/reserve` - Reserve cart items

### Price Recommendations
- `GET /api/marketplace/price-recommendation` - Get price recommendation

### WebSocket Endpoints
- `/ws` - WebSocket connection endpoint
- `/topic/auction/{listingId}` - Subscribe to auction updates
- `/queue/notifications` - User-specific notifications

## Configuration

### Application Properties
```yaml
server:
  port: 8084

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/marketplace_db
    username: postgres
    password: postgres
    
  elasticsearch:
    uris: http://localhost:9200
    
  redis:
    host: localhost
    port: 6379
    
  rabbitmq:
    host: localhost
    port: 5672
    
marketplace:
  listing:
    auction:
      min-increment-vnd: 50000
      auto-extend-minutes: 10
      auto-extend-window-minutes: 5
    cart:
      reservation-minutes: 30
  fees:
    platform-percentage: 5
```

## Running the Service

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Elasticsearch 8.x
- Redis 6+
- RabbitMQ 3.x

### Development
```bash
# Install dependencies
mvn clean install

# Run the application
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring.profiles.active=dev
```

### Docker
```bash
# Build Docker image
docker build -t marketplace-service .

# Run container
docker run -p 8084:8084 \
  -e DB_HOST=postgres \
  -e ELASTICSEARCH_URI=http://elasticsearch:9200 \
  -e REDIS_HOST=redis \
  marketplace-service
```

### Docker Compose
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f marketplace-service
```

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### API Testing
```bash
# Health check
curl http://localhost:8084/actuator/health

# Search listings
curl http://localhost:8084/api/marketplace/listings?region=HCMC

# Create listing (requires auth token)
curl -X POST http://localhost:8084/api/marketplace/listings \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "creditId": "uuid",
    "listingType": "FIXED",
    "creditAmountTons": 1.5,
    "pricePerTonVnd": 2500000,
    "region": "HCMC",
    "description": "High-quality carbon credits"
  }'
```

## WebSocket Testing
```javascript
// Connect to WebSocket
const socket = new SockJS('http://localhost:8084/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // Subscribe to auction updates
    stompClient.subscribe('/topic/auction/listing-id', function(update) {
        console.log('Bid update:', JSON.parse(update.body));
    });
});

// Place bid
stompClient.send('/app/bid', {}, JSON.stringify({
    listingId: 'listing-id',
    bidAmountPerTon: 2600000
}));
```

## Monitoring

### Metrics Endpoints
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics
- `/actuator/health` - Health status

### Key Metrics
- `listing.created.count` - Number of listings created
- `bid.placed.count` - Number of bids placed
- `cart.checkout.success` - Successful checkouts
- `price.recommendation.requests` - Price recommendation requests
- `elasticsearch.search.latency` - Search latency

## Security

### Authentication
- JWT-based authentication
- Role-based access control (SELLER, BUYER, ADMIN)
- Secure WebSocket connections

### Data Protection
- Input validation on all endpoints
- SQL injection prevention
- XSS protection
- Rate limiting on critical endpoints

## Troubleshooting

### Common Issues

1. **Elasticsearch connection issues**
   - Check Elasticsearch is running: `curl http://localhost:9200`
   - Verify cluster health: `curl http://localhost:9200/_cluster/health`

2. **WebSocket connection failures**
   - Check CORS configuration
   - Verify WebSocket endpoint is accessible
   - Check firewall/proxy settings

3. **Price recommendation not working**
   - Ensure sufficient historical data exists
   - Check Redis cache is running
   - Verify calculation service is responsive

## API Documentation
Swagger UI is available at: `http://localhost:8084/swagger-ui.html`

## Contact
For issues or questions, contact the platform team.

## License
Copyright © 2025 Carbon Credit Marketplace. All rights reserved.
