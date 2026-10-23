> 로고 사진 넣는곳

# 또롱모 | 대학 축제 숫자 응모 서비스 백엔드

**비회원 참여부터 응모 데이터 관리, 당첨자 집계까지 구현한 Spring Boot 백엔드 프로젝트입니다.**

2026년 2학기 대학 축제에서 **멋쟁이사자처럼 14기 동아리 부스를 운영하며 사용한 서비스**입니다. 방문자는 부스의 QR 코드로 접속해 숫자를 응모했고, 당첨자에게는 현장에서 상품을 지급했습니다.

참가자는 `0000~9999` 중 서로 다른 숫자 3개를 제출합니다. **전체 응모에서 정확히 한 번 나온 숫자 중 가장 작은 숫자**가 당첨되며, 운영자가 보너스 단계를 열면 최대 4회의 추가 응모가 가능합니다. 여러 날 진행되는 행사에 맞춰 참여 상태와 당첨 결과를 날짜별로 관리합니다.

| 항목 | 내용 |
| --- | --- |
| 백엔드 구현 범위 | 참가자 식별, REST API, 데이터 모델링, 응모 트랜잭션, 당첨 집계, 통합 테스트, 배포 구성 |
| 주요 기술 | Java 17 · Spring Boot · Spring Data JPA · MySQL |
| 배포 환경 | Google Cloud Run · Cloud SQL for MySQL |
| 서비스 이용 흐름 | 부스 QR 접속 → 숫자 응모 → 당첨 결과 확인 → 부스에서 상품 수령 |

> 서비스 소개 사진 넣는곳

## 백엔드 핵심 구현

| 요구사항 | 설계와 구현 |
| --- | --- |
| 회원가입 없이 참여하고, 다음 행사일에도 다시 응모할 수 있어야 한다 | 난수 토큰과 HttpOnly 쿠키로 참가자를 식별하고 날짜별 참여 상태를 별도 엔티티로 관리 |
| 응모 기록과 사용 기회가 일치해야 한다 | 상태 변경과 응모 저장을 하나의 트랜잭션으로 처리하고 비관적 잠금·DB UNIQUE 제약 적용 |
| 보너스 지급 이후 방문한 참가자도 열린 기회를 받아야 한다 | 날짜별 보너스 단계 이력을 저장하고 신규 참여 초기화에 반영 |
| 날짜별 결과와 행사 전체 결과를 각각 구할 수 있어야 한다 | SQL 집계로 당첨자를 선정하고, 날짜별 결과 확정과 읽기 전용 통합 재계산 분리 |

[아키텍처](#시스템-아키텍처) · [ERD](#데이터-모델과-제약조건) · [설계와 문제 해결](#설계와-문제-해결) · [테스트](#테스트와-검증) · [실행 방법](#로컬-실행)

## 기술 스택

| 구분 | 기술 및 적용 목적 |
| --- | --- |
| 언어·프레임워크 | Java 17, Spring Boot 4.1.1, Spring Web MVC — REST API 구현 |
| 영속성 | Spring Data JPA, Hibernate, MySQL — 엔티티 매핑, 트랜잭션, 행 잠금, SQL 집계 |
| 요청 검증·명세 | Jakarta Bean Validation, Springdoc OpenAPI 3.1.1 — 입력 검증 및 Swagger UI |
| 테스트 | JUnit Jupiter, Spring Boot Test, MockMvc, H2 MySQL 모드 — HTTP 요청부터 DB까지 통합 검증 |
| 빌드·배포 | Gradle Wrapper 9.7.1, Cloud Run, Cloud SQL Connector — 빌드와 MySQL 연결 |
| 상태 확인 | Spring Boot Actuator — health 및 liveness/readiness 제공 |

## 시스템 아키텍처

참가자·게임·운영 도메인을 하나의 Spring Boot 애플리케이션으로 구성했습니다. Controller는 요청과 응답을, Service는 업무 규칙과 트랜잭션을, Repository는 조회·잠금·집계를 담당합니다.

![또롱모 백엔드 시스템 아키텍처](docs/diagrams/architecture.png)

[아키텍처 SVG 확대 보기](docs/diagrams/architecture.svg)

| 패키지 | 책임 |
| --- | --- |
| `participant` | 참가자 토큰·쿠키 발급, 날짜별 참여 초기화, 내 응모 내역 조회 |
| `game` | 최초·추가 응모, 진행 상태 및 날짜별 당첨 결과 조회 |
| `operation` | 보너스 단계 개방, 날짜별 결과 확정, 전체 행사일 통합 재계산 |
| `common` | 행사일 검증, 공통 오류 응답, CORS, OpenAPI, 번호·전화번호 변환 |

조회에는 `@Transactional(readOnly = true)`, 데이터 변경에는 `@Transactional`을 적용했습니다. `open-in-view=false`로 설정하고 서비스의 트랜잭션 안에서 연관 데이터를 조회해 응답 DTO를 구성합니다.

아키텍처는 저장소의 애플리케이션 구조와 Cloud Run·Cloud SQL 배포 구성을 나타냅니다. 현재 클라우드 리소스의 가동 상태를 표시한 그림은 아닙니다.

## 데이터 모델과 제약조건

참가자의 식별 정보와 날짜별 참여 상태를 분리했습니다. 같은 참가자가 여러 행사일에 참여하더라도 전화번호·개인정보 동의·사용 기회·응모 완료 여부를 날짜별로 관리할 수 있습니다.

![또롱모 데이터베이스 ERD](docs/diagrams/erd.png)

[ERD SVG 확대 보기](docs/diagrams/erd.svg) · 엔티티 5개의 필드와 실제 외래키 관계를 표시했습니다.

| 테이블 | 역할 | 주요 제약·인덱스 |
| --- | --- | --- |
| `participants` | 브라우저의 참가자 식별 | `participant_token` UNIQUE |
| `daily_participations` | 날짜별 참여 상태·기회·연락처·동의 | `(participant_id, event_date)`, `(event_date, phone_number)` 각각 UNIQUE |
| `submissions` | 응모 숫자·행사일·제출 시각 | `(event_date, number_value)`, `(participant_id, event_date)` 인덱스 |
| `bonus_events` | 날짜별 보너스 단계와 실행 이력 | `(event_date, stage)`, `event_key` 각각 UNIQUE |
| `game_results` | 날짜별 확정 결과와 당첨 응모·참가자 | `event_date`, `result_key`, `winning_submission_id` 각각 UNIQUE |

`daily_participations`와 `submissions`는 각각 참가자를 참조하며 두 테이블 사이에 직접 외래키는 없습니다. `game_results`는 당첨 응모와 참가자를 참조하고, `bonus_events`는 행사일과 단계로 이력을 구분합니다.

전화번호는 하이픈을 제거해 저장합니다. 응모 숫자는 정수로 저장하고 API에서는 `"0037"`처럼 네 자리 문자열로 반환합니다. ERD의 회색 필드는 이전 스키마 호환용이며, 현재 참여 상태는 `daily_participations`를 기준으로 처리합니다.

관련 코드: [Participant](src/main/java/hsu/unique/participant/entity/Participant.java) · [DailyParticipation](src/main/java/hsu/unique/participant/entity/DailyParticipation.java) · [Submission](src/main/java/hsu/unique/game/entity/Submission.java) · [BonusEvent](src/main/java/hsu/unique/operation/entity/BonusEvent.java) · [GameResult](src/main/java/hsu/unique/game/entity/GameResult.java)

## 설계와 문제 해결

### 1. 비회원 식별과 날짜별 참여 상태 분리

부스 방문자가 회원가입 없이 바로 응모하면서도 같은 브라우저에서 자신의 응모 내역을 다시 확인할 수 있어야 했습니다. 또한 첫째 날의 참여가 둘째 날의 기본 기회를 소모해서는 안 됩니다.

`SecureRandom`의 32바이트 난수를 URL-safe Base64 토큰으로 만들고 `participant_token` HttpOnly 쿠키로 발급했습니다. 유효한 쿠키가 있으면 기존 참가자를 재사용하며, `(participant_id, event_date)`로 해당 날짜의 참여 정보를 찾거나 생성합니다.

그 결과 하나의 참가자 식별 정보를 유지하면서 행사일마다 기본 3회와 별도의 응모 내역을 관리할 수 있습니다. 같은 날짜의 전화번호 중복은 제한하되, 다른 날짜의 재참여는 허용합니다. 쿠키는 브라우저를 식별하는 수단이며 전화번호 소유권 인증은 구현하지 않았습니다.

관련 코드: [ParticipantService](src/main/java/hsu/unique/participant/service/ParticipantService.java) · [ParticipantTokenGenerator](src/main/java/hsu/unique/participant/service/ParticipantTokenGenerator.java) · [ParticipantCookieManager](src/main/java/hsu/unique/participant/service/ParticipantCookieManager.java)

### 2. 응모 저장과 기회 차감의 일관성

최초 응모는 숫자 3개, 전화번호, 개인정보 동의, 사용 기회를 함께 변경합니다. 일부만 저장되면 기회는 소모됐지만 응모가 누락되는 상태가 생길 수 있습니다. 중복 요청이 같은 남은 기회를 읽는 상황도 고려해야 합니다.

`GameService.createEntry()`에서 다음 과정을 하나의 트랜잭션으로 처리했습니다.

1. 행사일·응모 가능 상태와 숫자 3개·동의를 검증합니다.
2. 참가자 행 → 날짜별 참여 행 순서로 `PESSIMISTIC_WRITE` 잠금을 획득합니다.
3. 최초 응모 완료 여부와 같은 날짜의 전화번호 중복을 확인합니다.
4. 연락처·동의 시각·사용 기회·완료 상태를 변경하고 응모 3건을 저장합니다.
5. 트랜잭션 커밋 후 성공 응답을 반환하며, 처리 중 예외가 발생하면 함께 롤백합니다.

**잠금과 UNIQUE 제약의 역할을 구분했습니다.** 같은 참가자의 겹치는 응모 요청은 행 잠금으로 상태 확인과 변경을 순서대로 처리합니다. 서로 다른 참가자가 같은 전화번호로 요청하는 경우는 사전 중복 조회와 `(event_date, phone_number)` UNIQUE 제약으로 제한합니다.

추가 응모에서도 동일한 잠금 순서를 사용합니다. 최초 응모 완료 여부, 남은 기회, 당일 본인이 제출한 번호와의 중복을 확인하고 응모 저장과 기회 1회 차감을 함께 처리합니다.

![또롱모 최초 응모 시퀀스 다이어그램](docs/diagrams/entry-sequence.png)

[응모 시퀀스 SVG 확대 보기](docs/diagrams/entry-sequence.svg) · `flush`와 최종 커밋을 구분하고, 실제 검증·잠금·저장 순서를 표시했습니다.

현재 잠금 범위는 참가자와 날짜별 참여 행입니다. 결과 확정과 진행 중인 응모 사이의 경합은 행사일 단위 제어가 추가로 필요한 영역입니다.

관련 코드: [GameService.createEntry / submitBonusNumber](src/main/java/hsu/unique/game/service/GameService.java) · [ParticipantRepository](src/main/java/hsu/unique/participant/repository/ParticipantRepository.java) · [DailyParticipationRepository](src/main/java/hsu/unique/participant/repository/DailyParticipationRepository.java)

### 3. 보너스 이력을 활용한 참여 기회 관리

보너스 지급 시점의 참가자만 갱신하면, 이후 방문한 참가자는 이미 열린 추가 기회를 받지 못합니다. 이를 처리하기 위해 보너스 지급을 날짜별 단계 이력으로 저장했습니다.

보너스 API는 해당 날짜의 참여 행을 잠근 뒤 다음 단계 이력을 저장하고 기회를 1회씩 추가합니다. 두 작업은 하나의 트랜잭션으로 처리합니다. 이후 신규 참가자가 초기화하면 저장된 단계 수를 읽어 추가 기회에 반영합니다.

예를 들어 2단계까지 열린 뒤 참여한 사람은 **기본 3회 + 추가 2회 = 총 5회**를 받습니다. 최대 4단계까지만 열 수 있으며 `(event_date, stage)` UNIQUE 제약으로 같은 단계의 중복 저장을 제한합니다.

별개의 정상 호출은 다음 단계를 여는 방식이므로 요청 재시도에 대한 멱등성을 보장하는 API는 아닙니다. 보너스 지급과 신규 초기화가 동시에 실행되는 경계도 후속 동시성 검증 대상으로 구분했습니다.

관련 코드: [OperationService.executeBonus](src/main/java/hsu/unique/operation/service/OperationService.java) · [ParticipantService.initialize](src/main/java/hsu/unique/participant/service/ParticipantService.java)

### 4. SQL 집계로 당첨 조건 표현

당첨 규칙은 **제출 횟수가 정확히 1회인 숫자 중 최솟값**입니다. 응모 목록 전체를 애플리케이션에 적재하지 않고 DB에서 집계와 정렬을 수행하도록 구현했습니다.

```sql
SELECT number_value
FROM submissions
WHERE event_date = :eventDate
GROUP BY number_value
HAVING COUNT(*) = 1
ORDER BY number_value ASC
LIMIT 1;
```

A가 `[1, 2, 8]`, B가 `[1, 2, 3]`을 제출했다면 `1`과 `2`는 중복으로 제외되고 `3`이 당첨됩니다. 응모가 없거나 모든 숫자가 중복이면 당첨자가 없는 결과를 반환합니다.

날짜별 종료 API는 집계 결과를 `game_results`에 저장합니다. 이미 확정된 날짜의 재확정 요청과 이후 새로 들어오는 응모 요청은 거절합니다. 조회 조건에 맞춰 `(event_date, number_value)` 복합 인덱스를 정의했으며, 성능 개선율은 별도로 측정하지 않았습니다.

관련 코드: [SubmissionRepository.findSmallestUniqueNumber](src/main/java/hsu/unique/game/repository/SubmissionRepository.java) · [OperationService.finishGame](src/main/java/hsu/unique/operation/service/OperationService.java)

### 5. 기존 결과를 보존하는 행사 전체 재계산

여러 행사일을 합쳐 당첨자 1명을 선정할 때는 날짜별 당첨자만 비교할 수 없습니다. 하루에는 고유했던 숫자가 다른 날에도 제출됐다면 전체 집계에서는 제외해야 하기 때문입니다.

`EVENT_DATES`에 포함된 전체 응모를 다시 집계하는 재계산 API를 구현했습니다. 같은 참가자가 다른 날짜에 제출한 같은 숫자도 중복으로 계산합니다. 당첨자의 연락처는 **당첨 응모가 발생한 날짜의 참여 정보**에서 조회합니다.

| 구분 | 날짜별 결과 확정 | 행사 전체 재계산 |
| --- | --- | --- |
| API | `POST /api/operations/finish` | `POST /api/operations/recalculate` |
| 집계 범위 | 선택한 행사일 | 설정된 전체 행사일 |
| 저장·상태 변경 | 날짜별 결과 저장, 이후 해당 날짜 응모 차단 | 기존 응모·결과·마감 상태 변경 없음 |
| 반복 호출 | 이미 확정된 경우 거절 | 호출 시점의 응모로 다시 계산 |

재계산 서비스는 `@Transactional(readOnly = true)`로 실행하며 저장·수정 메서드를 호출하지 않습니다. 따라서 날짜별 확정 결과를 유지하면서 전체 행사 결과를 별도로 확인할 수 있습니다. 행사일 밖의 응모와 날짜가 없는 기존 데이터는 집계에서 제외합니다.

관련 코드: [SubmissionRepository.findOverallWinningSubmission](src/main/java/hsu/unique/game/repository/SubmissionRepository.java) · [OperationService.recalculateOverallResult](src/main/java/hsu/unique/operation/service/OperationService.java)

## API 설계

| 구분 | Method | Endpoint | 기능 |
| --- | --- | --- | --- |
| 참가자 | `POST` | `/api/participants/initialize` | 쿠키 식별 및 날짜별 참여 초기화 |
| 참가자 | `GET` | `/api/participants/me` | 내 응모 번호·남은 기회 조회 |
| 게임 | `GET` | `/api/game/status` | 참여 완료 인원·보너스 단계·진행 상태 조회 |
| 게임 | `POST` | `/api/game/entries` | 숫자 3개·전화번호·동의로 최초 응모 |
| 게임 | `POST` | `/api/game/submissions` | 추가 기회로 숫자 1개 제출 |
| 게임 | `GET` | `/api/game/result` | 날짜별 당첨 번호·본인 당첨 여부 조회 |
| 운영 | `POST` | `/api/operations/bonus` | 선택 날짜의 다음 보너스 단계 개방 |
| 운영 | `POST` | `/api/operations/finish` | 선택 날짜의 결과 계산·확정 |
| 운영 | `POST` | `/api/operations/recalculate` | 전체 행사일의 당첨자 재계산 |

조회·초기화·보너스·종료 API의 `eventDate` 쿼리 파라미터를 생략하면 설정된 첫 행사일을 사용합니다. 최초·추가 응모는 JSON 본문에 날짜가 필수이며, 통합 재계산은 날짜나 본문을 받지 않습니다.

참가자용 결과 응답에는 당첨 번호와 본인의 당첨 여부만 제공하고 연락처는 제외했습니다. 종료·재계산 응답에는 경품 지급을 위한 당첨자 연락처가 포함됩니다. **현재 운영 API에 별도 인증·인가가 없으므로 접근 제어는 우선 개선 과제입니다.**

`@RestControllerAdvice`에서 오류 응답을 `code`, `message` 형식으로 통일했습니다. 입력 오류는 `400`, 참가자 식별 실패는 `404`, 중복 응모·기회 소진 등 상태 충돌은 `409`, 예상하지 못한 예외는 `500`으로 구분합니다.

```json
{
  "code": "DUPLICATE_PHONE_NUMBER",
  "message": "해당 날짜에 이미 응모한 전화번호입니다."
}
```

관련 코드: [CreateEntryRequest](src/main/java/hsu/unique/game/dto/CreateEntryRequest.java) · [GlobalExceptionHandler](src/main/java/hsu/unique/common/exception/GlobalExceptionHandler.java) · [ErrorCode](src/main/java/hsu/unique/common/exception/ErrorCode.java)

## 테스트와 검증

`@SpringBootTest`와 MockMvc를 사용해 HTTP 요청 → 서비스 → Repository → DB를 연결한 통합 테스트를 작성했습니다. H2를 MySQL 모드로 실행하고 각 테스트 전에 데이터를 정리합니다.

**2026년 10월 23일 Java 17 환경에서 통합 테스트 14개 통과, 실패·오류·건너뜀 0개, Gradle 빌드 성공을 확인했습니다.**

| 검증 영역 | 테스트 시나리오 |
| --- | --- |
| 참가자·날짜 분리 | 쿠키 발급 속성, 기존 쿠키 재사용, 날짜별 독립 참여 상태 |
| 최초 응모 | 숫자 3개·전화번호 저장, 번호 형식 변환, 잘못된 입력 및 당일 전화번호 중복 거절 |
| 보너스·추가 응모 | 단계별 지급, 늦은 참가자 기회 반영, 4단계 상한, 기회 부족·본인 번호 중복 거절 |
| 날짜별 결과 | 날짜별 독립 집계, 본인 당첨 여부, 참가자 응답에서 연락처 제외 |
| 통합 재계산 | 날짜 간 중복 제외, 당첨일 연락처 조회, 반복 계산 시 기존 데이터 보존, 새 응모 반영 |
| 경계·연동 | 응모 없음, 모든 숫자 중복, 행사일 밖·날짜 없는 데이터 제외, CORS·OpenAPI·미지원 날짜 |

검증 명령: `./gradlew test build --rerun-tasks --no-daemon` — Windows에서는 `gradlew.bat`을 사용합니다.

현재 테스트는 기능 시나리오를 검증하며, 실제 MySQL에서의 동시 요청·잠금 경합이나 부하 테스트를 대체하지 않습니다.

테스트 코드: [UniqueApplicationTests](src/test/java/hsu/unique/UniqueApplicationTests.java)

## 배포와 운영 구성

Cloud Run에서 애플리케이션을 실행하고 Cloud SQL for MySQL에 연결하는 구성을 사용했습니다. DB 접속 정보, 행사 일정, 쿠키와 CORS 정책은 환경변수로 분리했습니다.

| 운영 항목 | 구성 |
| --- | --- |
| DB 연결 | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, HikariCP 연결 수 설정 |
| 서버·상태 확인 | `PORT`, 프록시 전달 헤더 처리, graceful shutdown, Actuator health 및 probe |
| 쿠키·CORS | HttpOnly 쿠키, Secure·SameSite·만료 기간 설정, 명시한 출처의 credentials 허용 |
| 행사 운영 | 행사일·시간대·시작·마감·결과 안내 시각, 응모 시간 강제 여부 설정 |

진행 상태는 시간과 결과 저장 여부에 따라 `UPCOMING`, `OPEN`, `COUNTING`, `RESULT_OPEN`으로 반환합니다. `EVENT_RESULT_AT`은 안내용 값이며 실제 결과 확정은 종료 API가 수행합니다. 시간 제한을 적용하는 `EVENT_ENFORCE_SCHEDULE`의 기본값은 `false`입니다.

관련 설정: [application.yml](src/main/resources/application.yml) · [build.gradle](build.gradle) · [.gcloudignore](.gcloudignore)

## 개선 과제

구현 범위와 검증 결과를 바탕으로 다음 보완 항목을 정리했습니다.

- **운영 API 보호**: 보너스 지급·결과 확정·연락처 조회에 인증과 권한 검증을 적용하고 쿠키 기반 변경 요청의 CSRF 대응을 보완합니다.
- **행사 단위 동시성 검증**: 결과 확정과 진행 중 응모, 보너스 지급과 신규 초기화 사이의 경합을 검증하고 행사일 단위 처리 순서를 제어합니다.
- **스키마 변경 관리**: `ddl-auto=update`를 버전 기반 마이그레이션으로 전환하고 기존 호환 필드와 nullable 행사일 데이터를 정리합니다.
- **검증 확대**: 실제 MySQL 기반 동시 요청·시간 경계 테스트, 실행 계획 분석과 부하 측정을 추가하고 CI에서 반복 실행합니다.

## 로컬 실행

JDK 17과 MySQL, 접속 가능한 `unique` 데이터베이스가 필요합니다. 로컬 DB 계정에는 스키마 생성·변경 권한이 있어야 합니다.

<details>
<summary>환경변수와 실행 명령 보기</summary>

아래 PowerShell 예시의 DB 계정·비밀번호는 본인의 로컬 환경 값으로 변경합니다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DB_URL = 'jdbc:mysql://localhost:3306/unique?serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
$env:DB_USERNAME = '로컬_DB_계정'
$env:DB_PASSWORD = '로컬_DB_비밀번호'
$env:EVENT_DATES = '2026-09-21,2026-09-22'
$env:EVENT_ENFORCE_SCHEDULE = 'false'
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:5173'

.\gradlew.bat bootRun
```

테스트는 H2를 사용하므로 별도 MySQL 서버 없이 실행할 수 있습니다.

```powershell
.\gradlew.bat test build
```

macOS/Linux에서는 JDK 17과 동일한 환경변수를 설정한 뒤 `./gradlew bootRun`, `./gradlew test build`를 사용합니다.

| 항목 | 로컬 주소 |
| --- | --- |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI | `http://localhost:8080/v3/api-docs` |
| Health | `http://localhost:8080/actuator/health` |
| 게임 상태 예시 | `http://localhost:8080/api/game/status?eventDate=2026-09-21` |

테스트 보고서는 `build/reports/tests/test/index.html`에서 확인할 수 있습니다. 다른 행사일로 실행할 때는 `EVENT_DATES`와 API 요청의 날짜를 함께 변경합니다.

</details>
