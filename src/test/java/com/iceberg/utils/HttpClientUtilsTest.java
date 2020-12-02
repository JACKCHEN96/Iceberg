package com.iceberg.utils;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HttpClientUtilsTest {

    @Test
    public void doGetTest(){
        String URL = "https://example.com";
        Integer status = HttpClientUtils.doGet(URL);
        Assert.assertEquals("200", String.valueOf(status));
    }

    @Test
    public void doPostTest(){
        String URL = "https://example.com";
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("key", "value");
        String result = HttpClientUtils.doPost(URL, paramMap);
        assertNotNull(result);
    }
}
