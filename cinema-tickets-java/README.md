# Cinema Tickets Service

Java 21 implementation of the DWP Cinema Tickets coding exercise.

## Implementation

The `TicketServiceImpl` class implements ticket purchase validation, payment calculation and seat allocation according to the provided business rules.

The solution includes:

- Validation of account IDs, ticket requests and purchase limits.
- Payment and seat calculations for adult, child and infant tickets.
- Descriptive exceptions for invalid purchases.
- Constructor-based dependency injection for testability.
- Immutable `TicketTypeRequest` objects.

The provided `TicketService` interface and third-party services remain unchanged.

## Technologies

- Java 21
- Maven
- JUnit 5
- Mockito

## Build and Test

From the `cinema-tickets-java` directory, run:

```bash 
mvn clean test
```

The unit tests use Mockito to verify payment and seat reservation interactions without invoking the actual third-party implementations.

Tests cover valid purchases, invalid inputs, business-rule constraints, boundary conditions and calculation accuracy.

## Assumptions

- All positive account IDs are valid and have sufficient funds.
- Ticket quantities must be greater than zero.
- At least one adult ticket is required per purchase.
- There is no one-infant-per-adult restriction.
- Third-party payment and seat reservation services are assumed to operate successfully.