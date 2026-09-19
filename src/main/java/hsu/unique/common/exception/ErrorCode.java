package hsu.unique.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참가자 정보를 찾을 수 없습니다."),
    PARTICIPATION_NOT_INITIALIZED(HttpStatus.CONFLICT, "먼저 해당 날짜의 참가자 초기화를 진행해 주세요."),
    EVENT_DATE_INVALID(HttpStatus.BAD_REQUEST, "운영하지 않는 행사 날짜입니다."),
    INVALID_NUMBER(HttpStatus.BAD_REQUEST, "숫자는 0부터 9999 사이여야 합니다."),
    INITIAL_ENTRY_REQUIRES_THREE_NUMBERS(HttpStatus.BAD_REQUEST, "최초 응모에는 서로 다른 숫자 3개가 필요합니다."),
    DUPLICATE_NUMBERS(HttpStatus.BAD_REQUEST, "서로 다른 숫자 3개를 선택해 주세요."),
    DUPLICATE_NUMBER_FOR_PARTICIPANT(HttpStatus.CONFLICT, "오늘 이미 선택한 번호입니다."),
    ENTRY_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "해당 날짜의 최초 응모가 이미 완료되었습니다."),
    INITIAL_ENTRY_REQUIRED(HttpStatus.CONFLICT, "먼저 숫자 3개와 전화번호로 최초 응모를 완료해 주세요."),
    NO_ATTEMPTS_LEFT(HttpStatus.CONFLICT, "사용 가능한 참여 기회가 없습니다."),
    PHONE_NUMBER_INVALID(HttpStatus.BAD_REQUEST, "올바른 대한민국 휴대전화번호를 입력해 주세요."),
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, "해당 날짜에 이미 응모한 전화번호입니다."),
    PRIVACY_AGREEMENT_REQUIRED(HttpStatus.BAD_REQUEST, "개인정보 수집 및 이용 동의가 필요합니다."),
    BONUS_EVENT_ALREADY_EXECUTED(HttpStatus.CONFLICT, "추가 기회 이벤트가 이미 실행되었습니다."),
    BONUS_STAGE_LIMIT_REACHED(HttpStatus.CONFLICT, "추가 기회 4단계가 모두 열렸습니다."),
    GAME_NOT_OPEN(HttpStatus.CONFLICT, "현재는 응모 가능한 시간이 아닙니다."),
    GAME_CLOSED(HttpStatus.CONFLICT, "이미 결과가 확정된 게임입니다."),
    GAME_RESULT_ALREADY_CALCULATED(HttpStatus.CONFLICT, "게임 결과가 이미 계산되었습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
