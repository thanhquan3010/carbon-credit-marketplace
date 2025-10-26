# User Service Test Suite

This directory contains comprehensive tests for the user-service following best practices and AAA pattern (Arrange, Act, Assert).

## Test Structure

### Unit Tests (`src/test/java/com/carbonmarketplace/userservice/service/`)
- **UserServiceTest.java**: Tests for UserService covering:
  - User CRUD operations
  - Profile management
  - Email/Phone verification
  - Two-factor authentication
  - Account status management
  - Edge cases and negative scenarios

- **AuthServiceTest.java**: Tests for AuthService covering:
  - User registration
  - Login with credentials
  - Two-factor authentication flow
  - Token refresh mechanism
  - Account locking after failed attempts
  - Password management (reset, change)
  - Logout functionality

- **KycServiceTest.java**: Tests for KycService covering:
  - KYC document submission
  - File validation (size, type)
  - Document approval workflow
  - Document rejection workflow
  - Multi-level KYC support
  - Edge cases and error handling

### Controller Tests (`src/test/java/com/carbonmarketplace/userservice/controller/`)
- **UserControllerTest.java**: Integration tests for UserController endpoints
- **AuthControllerTest.java**: Integration tests for AuthController endpoints

### Integration Tests (`src/test/java/com/carbonmarketplace/userservice/integration/`)
- **UserFlowIntegrationTest.java**: Complete user flows testing:
  - Registration → Email Verification → Login → Profile Management → Logout
  - Token refresh and session management
  - Password management flows
  - Two-factor authentication setup
  - Account lifecycle management

- **KycFlowIntegrationTest.java**: Complete KYC flows testing:
  - Document submission with validation
  - Multi-level KYC progression
  - Approval and rejection workflows
  - Document expiration handling
  - Complete journey from submission to approval

## Test Configuration

### Test Profile (`src/test/resources/application-test.yml`)
- Uses H2 in-memory database for fast test execution
- Mocked external services (Email, S3)
- Test-specific JWT configuration
- Disabled service discovery for isolated testing

## Running Tests

### Run all tests
```bash
cd backend/user-service
mvn test
```

### Run specific test class
```bash
mvn test -Dtest=UserServiceTest
```

### Run tests with coverage
```bash
mvn test jacoco:report
```

## Test Coverage Goals

The test suite aims for:
- **>80% code coverage** for all service classes
- **100% coverage** for critical paths (authentication, authorization)
- **Edge case coverage** for all validation logic
- **Integration coverage** for complete user workflows

## Test Patterns Used

### AAA Pattern
All tests follow the Arrange-Act-Assert pattern:
```java
@Test
void testMethod() {
    // Arrange - Setup test data and mocks
    when(repository.findById(id)).thenReturn(Optional.of(entity));
    
    // Act - Execute the method under test
    Result result = service.methodUnderTest(params);
    
    // Assert - Verify the outcome
    assertNotNull(result);
    verify(repository, times(1)).findById(id);
}
```

### Test Naming Convention
Tests use descriptive names following the pattern:
`methodName_shouldExpectedBehavior_whenCondition`

Examples:
- `getUserById_ShouldReturnUser_WhenUserExists`
- `login_ShouldThrowException_WhenAccountLocked`

### Mocking Strategy
- **@Mock**: For dependencies
- **@InjectMocks**: For the class under test
- **@MockBean**: For Spring beans in integration tests
- **Mockito**: For behavior verification

## Key Test Scenarios

### Positive Test Cases
- ✅ Successful operations with valid data
- ✅ Complete workflows from start to finish
- ✅ Correct data transformations
- ✅ Proper authorization checks

### Negative Test Cases
- ✅ Invalid input validation
- ✅ Missing required fields
- ✅ Duplicate data conflicts
- ✅ Unauthorized access attempts
- ✅ Resource not found scenarios

### Edge Cases
- ✅ Boundary value testing
- ✅ Null and empty values
- ✅ Maximum limits (file size, attempts, etc.)
- ✅ Concurrent operations
- ✅ State transitions

## Test Data Management

### Test Users
- Created using builders for flexibility
- Unique identifiers to avoid conflicts
- Realistic data for integration tests
- Automatic cleanup after tests

### Test Files
- MockMultipartFile for file upload testing
- Various file types and sizes for validation
- Mocked S3 service to avoid actual uploads

## Continuous Integration

These tests are designed to run in CI/CD pipelines:
- Fast execution (in-memory database)
- No external dependencies required
- Isolated test execution
- Repeatable and deterministic results

## Troubleshooting

### Common Issues

1. **Test fails with "User not found"**
   - Ensure test data is properly set up in @BeforeEach
   - Check transaction boundaries in integration tests

2. **Mock not working as expected**
   - Verify the mock setup matches the actual method call
   - Check argument matchers (any(), eq(), etc.)

3. **Integration test fails**
   - Ensure H2 dependency is in pom.xml
   - Check application-test.yml configuration
   - Verify @ActiveProfiles("test") is present

## Future Enhancements

- [ ] Performance testing for high-load scenarios
- [ ] Security testing with OWASP test cases
- [ ] Contract testing with Pact
- [ ] Mutation testing with PIT
- [ ] Load testing with JMeter/Gatling

