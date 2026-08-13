package com.pcverse.utils;

import lombok.experimental.UtilityClass;

import java.text.Normalizer;
import java.util.Locale;

@UtilityClass
public class SlugUtils {

    public String generateSlug(String value) {
        if (value == null) return "";

        return Normalizer.normalize(
                        value.strip().toLowerCase(Locale.ROOT),
                        Normalizer.Form.NFD
                )
                .replace("đ", "d")
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

}
