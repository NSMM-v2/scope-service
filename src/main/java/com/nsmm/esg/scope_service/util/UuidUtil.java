package com.nsmm.esg.scope_service.util;

import java.util.UUID;

/**
 * UUID 관련 유틸리티 클래스
 */
public class UuidUtil {

    /**
     * 문자열이 유효한 UUID 형식인지 검증
     * 
     * @param str 검증할 문자열
     * @return UUID 형식이면 true, 아니면 false
     */
    public static boolean isValidUUID(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}