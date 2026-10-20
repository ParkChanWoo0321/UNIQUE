# 또롱모 백엔드 다이어그램

애플리케이션 코드 `73102f4`를 확인해 작성한 포트폴리오용 도식입니다. README에는 PNG를 삽입하고, 확대·수정용 SVG와 재생성용 Java 소스를 함께 보관합니다. 도식의 내용은 코드에서 자동 추출하지 않으므로 구현이 바뀌면 렌더러의 설명·필드·관계도 함께 갱신합니다.

| 파일 | 내용 | 코드 근거 |
| --- | --- | --- |
| [architecture.png](architecture.png) / [SVG](architecture.svg) | 도메인 계층, 쿠키 식별, Cloud SQL 연결 | [build.gradle](../../build.gradle), [application.yml](../../src/main/resources/application.yml), 각 도메인의 Controller·Service·Repository |
| [erd.png](erd.png) / [SVG](erd.svg) | 엔티티 5개의 전체 필드, 키, 관계 다중성 | [Participant](../../src/main/java/hsu/unique/participant/entity/Participant.java), [DailyParticipation](../../src/main/java/hsu/unique/participant/entity/DailyParticipation.java), [Submission](../../src/main/java/hsu/unique/game/entity/Submission.java), [BonusEvent](../../src/main/java/hsu/unique/operation/entity/BonusEvent.java), [GameResult](../../src/main/java/hsu/unique/game/entity/GameResult.java) |
| [entry-sequence.png](entry-sequence.png) / [SVG](entry-sequence.svg) | 최초 응모의 검증, 잠금 순서, 저장, 커밋 및 실패 경계 | [GameController.createEntry](../../src/main/java/hsu/unique/game/controller/GameController.java), [GameService.createEntry](../../src/main/java/hsu/unique/game/service/GameService.java), [ParticipantRepository](../../src/main/java/hsu/unique/participant/repository/ParticipantRepository.java), [DailyParticipationRepository](../../src/main/java/hsu/unique/participant/repository/DailyParticipationRepository.java), [GlobalExceptionHandler](../../src/main/java/hsu/unique/common/exception/GlobalExceptionHandler.java) |

## 표기 범위

- 아키텍처는 코드의 의존성과 배포 구성을 나타냅니다. 현재 클라우드 리소스의 존재·가동 상태를 검증한 그림은 아닙니다.
- ERD 필드 오른쪽은 Java 타입입니다. `Date`는 `LocalDate`의 축약이며 실제 DB 타입·전체 DDL을 나타내지 않습니다.
- `participants`의 회색 필드는 호환용입니다. 현재 날짜별 참여 정보는 `daily_participations`가 기준입니다.
- 외래키 관계는 실선으로 표시하고 양 끝에 해당 개체의 다중성을 적었습니다. `game_results`의 당첨 참가자·응모는 선택 관계입니다.
- `submissions`와 `daily_participations` 사이의 직접 FK, `bonus_events`에서 다른 테이블로 향하는 FK는 없습니다.
- 인덱스는 핵심 응모 조회 경로를 표시했습니다. 전체 인덱스 정의는 각 엔티티의 `@Table`을 확인합니다.
- 시퀀스의 `Repository / DB`는 호출 경로를 간결하게 보여주기 위해 합친 레인입니다. 서비스 레인에는 Spring 트랜잭션 프록시의 커밋 처리를 포함했습니다.
- 비관적 잠금은 참가자 행 → 날짜별 참여 행 순서입니다. 행사 종료와 진행 중 응모를 함께 직렬화하는 행사일 단위 잠금은 현재 없습니다.

## 재생성

저장소 루트에서 JDK 17로 실행합니다. 외부 Java 라이브러리는 필요하지 않습니다. PNG의 한글 렌더링에는 맑은 고딕이 필요하며, 다른 OS에서는 `DiagramRenderer.java`의 `FONT`와 SVG `font-family`를 설치된 한글 폰트로 바꿉니다.

```powershell
& 'C:\Program Files\Java\jdk-17\bin\javac.exe' -encoding UTF-8 -d build\diagram-renderer docs\diagrams\DiagramRenderer.java
& 'C:\Program Files\Java\jdk-17\bin\java.exe' '-Djava.awt.headless=true' -cp build\diagram-renderer DiagramRenderer docs\diagrams
```

PNG는 도식 좌표의 2배 해상도로 생성합니다. SVG와 PNG는 같은 좌표·문구로 생성하며, 컴파일 산출물은 Git에서 제외된 `build/diagram-renderer`에 둡니다.
