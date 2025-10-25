# Carbon Credit Marketplace for EV Owners

A comprehensive platform that enables electric vehicle owners to convert their CO2 savings into tradable carbon credits.

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Maven 3.8+
- PostgreSQL 14+
- Redis 7+

### Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/carbon-credit-marketplace.git
   cd carbon-credit-marketplace
   ```

2. **Start infrastructure services**
   ```bash
   docker-compose up -d postgres redis elasticsearch rabbitmq
   ```

3. **Start backend services**
   ```bash
   cd backend
   # Start Eureka Server
   cd eureka-server && mvn spring-boot:run
   
   # Start Config Server (in new terminal)
   cd ../config-server && mvn spring-boot:run
   
   # Start API Gateway (in new terminal)
   cd ../api-gateway && mvn spring-boot:run
   
   # Start User Service (in new terminal)
   cd ../user-service && mvn spring-boot:run
   ```

4. **Start frontend**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

5. **Access the applications**
   - Frontend: http://localhost:3000
   - API Gateway: http://localhost:8080
   - Eureka Dashboard: http://localhost:8761
   - RabbitMQ Management: http://localhost:15672 (admin/admin)

## 📁 Project Structure

```
carbon-credit-marketplace/
├── backend/                    # Java Spring Boot microservices
│   ├── user-service/          # User management & authentication
│   ├── vehicle-service/       # Vehicle registration & sync
│   ├── trip-service/          # Trip data management
│   ├── carbon-credit-service/ # CO2 calculation & credits
│   ├── verification-service/  # CVA verification workflow
│   ├── marketplace-service/   # Marketplace & listings
│   ├── transaction-service/   # Transaction orchestration
│   ├── payment-service/       # Payment processing
│   ├── certificate-service/   # Certificate generation
│   ├── notification-service/  # Notifications
│   ├── admin-service/         # Admin dashboard
│   ├── analytics-service/     # Analytics & reporting
│   ├── common/                # Shared utilities & DTOs
│   ├── config-server/         # Centralized configuration
│   ├── eureka-server/         # Service discovery
│   └── api-gateway/           # API Gateway
│
├── frontend/                   # React + Next.js application
│   ├── src/
│   │   ├── components/        # Reusable UI components
│   │   ├── pages/            # Next.js pages
│   │   ├── services/         # API client services
│   │   ├── hooks/            # Custom React hooks
│   │   ├── store/            # Redux store
│   │   ├── utils/            # Utility functions
│   │   └── styles/           # CSS/SCSS styles
│   └── public/               # Static assets
│
├── infrastructure/            # Infrastructure configurations
│   ├── kubernetes/           # K8s manifests
│   ├── terraform/            # Infrastructure as Code
│   └── scripts/              # Deployment scripts
│
├── project-docs/             # Project documentation
│   ├── README.md            # Project overview
│   ├── REQUIREMENTS.md      # Requirements specification
│   ├── USER_STORIES.md      # User stories
│   ├── API_SPECS.md         # API documentation
│   ├── DATABASE_SCHEMA.md   # Database design
│   └── ARCHITECTURE.md      # System architecture
│
└── docker-compose.yml        # Docker services configuration
```

## 🛠️ Technology Stack

### Backend
- **Language**: Java 17+
- **Framework**: Spring Boot 3.x, Spring Cloud
- **Database**: PostgreSQL 14
- **Cache**: Redis 7
- **Search**: Elasticsearch 8
- **Message Queue**: RabbitMQ
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway

### Frontend
- **Framework**: React 18 + Next.js 14
- **Language**: TypeScript
- **State Management**: Redux Toolkit
- **UI Library**: Material-UI + Tailwind CSS
- **HTTP Client**: Axios
- **Real-time**: Socket.io

### Infrastructure
- **Container**: Docker
- **Orchestration**: Kubernetes
- **Cloud**: AWS
- **CI/CD**: GitHub Actions
- **Monitoring**: Prometheus + Grafana

## 📚 Documentation

- [Project Overview](project-docs/README.md)
- [Requirements](project-docs/REQUIREMENTS.md)
- [User Stories](project-docs/USER_STORIES.md)
- [API Documentation](project-docs/API_SPECS.md)
- [Database Schema](project-docs/DATABASE_SCHEMA.md)
- [System Architecture](project-docs/ARCHITECTURE.md)

## 🧪 Testing

### Backend Tests
```bash
cd backend/user-service
mvn test
```

### Frontend Tests
```bash
cd frontend
npm test
```

### End-to-End Tests
```bash
cd e2e
npm run test:e2e
```

## 🚀 Deployment

### Using Docker Compose (Development)
```bash
docker-compose up -d
```

### Using Kubernetes (Production)
```bash
kubectl apply -f infrastructure/kubernetes/
```

## 📈 Monitoring

- **Application Metrics**: http://localhost:8080/actuator/metrics
- **Health Check**: http://localhost:8080/actuator/health
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Team

- **Project Manager**: [Name]
- **Tech Lead**: [Name]
- **Backend Team**: [Names]
- **Frontend Team**: [Names]
- **DevOps Team**: [Names]

## 📞 Support

For support, email support@carbonmarketplace.vn or join our Slack channel.

---

**Version**: 1.0.0  
**Last Updated**: October 25, 2025
