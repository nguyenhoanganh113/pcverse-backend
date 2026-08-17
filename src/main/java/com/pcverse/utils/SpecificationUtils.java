package com.pcverse.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SpecificationUtils {

    public static String escapeLikePattern(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

}
