package com.sirp.common.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class DateUtils {

    private DateUtils(){}

    public static LocalDateTime now() {

        return LocalDateTime.now(ZoneOffset.UTC);

    }

}
