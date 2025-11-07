package com.barry.bank.financial.tracking_ws.testutils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class TestUtils {

    private TestUtils() {}

    public static void printPrettyJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        Object obj = mapper.readValue(json, Object.class);
        System.out.println(mapper.writeValueAsString(obj));
    }
}
