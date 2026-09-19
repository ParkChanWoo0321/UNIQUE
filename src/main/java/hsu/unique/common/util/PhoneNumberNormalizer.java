package hsu.unique.common.util;

import hsu.unique.common.exception.BusinessException;
import hsu.unique.common.exception.ErrorCode;

public final class PhoneNumberNormalizer {

    private PhoneNumberNormalizer() {
    }

    public static String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            throw new BusinessException(ErrorCode.PHONE_NUMBER_INVALID);
        }
        String normalized = phoneNumber.replace("-", "");
        if (!normalized.matches("^010\\d{8}$")) {
            throw new BusinessException(ErrorCode.PHONE_NUMBER_INVALID);
        }
        return normalized;
    }
}
