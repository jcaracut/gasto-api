---
description: "Write unit and integration tests for a service, controller, or domain class in the Expense Tracker API"
argument-hint: "Name the class to test, e.g. 'ExpenseService.getMonthlySummary' or 'AuthController'"
agent: "agent"
---
Write comprehensive tests for the following class or method in the Gasto Expense Tracker API:

**Target**: $input

## Instructions

### Identify the Test Type
- If the target is a service or domain class Å® unit test with `@ExtendWith(MockitoExtension.class)`
- If the target is a controller Å® `@WebMvcTest` slice test

### For Service / Domain Tests
```java
@ExtendWith(MockitoExtension.class)
class {TargetClass}Test {
    @Mock ...
    @InjectMocks {TargetClass} subject;

    @BeforeEach void setUp() { ... }
}
```
Cover:
- Happy path (correct inputs, expected output)
- All exception branches (`ResourceNotFoundException`, `BusinessException`, `BadCredentialsException`)
- Cache hit path (when applicable ? return cached value, verify no repo call)
- Cache miss path (when applicable ? fetch, store in cache)
- Kafka event published on create/update (verify `eventProducer` was called)

### For Controller Tests
```java
@WebMvcTest({Controller}.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
```
Cover:
- 200/201/204 happy path
- 400 with missing/invalid request body
- 401 without Authorization header
- 404 when service throws `ResourceNotFoundException`

### Naming Convention
`methodName_condition_expectedOutcome()`

### Read These Files Before Writing
- The target source file
- One existing test file in the same package for style reference

## Output
Complete, compilable test class with all imports ? no placeholders, no TODOs.
