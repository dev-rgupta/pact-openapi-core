package com.expediagroup.pact.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class RawHeadersParser {

    private RawHeadersParser() {
    }

    public static List<Param> parseAll(String[] stringHeaderArray) {
        return Arrays.stream(stringHeaderArray)
                .map(stringHeader -> stringHeader.split("=", 2))
                .map(RawHeadersParser::parse)
                .collect(Collectors.toList());
    }

    private static Param parse(String[] stringHeaderArray) {
        return Param.builder()
                .name(stringHeaderArray[0])
                .childNode(stringHeaderArray[1])
                .build();
    }
}
