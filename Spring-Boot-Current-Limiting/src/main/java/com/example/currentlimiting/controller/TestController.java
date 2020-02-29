package com.example.currentlimiting.controller;

import com.example.currentlimiting.annotation.Limit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author dengzhiming
 * @date 2020/2/29 12:39
 */
@RestController
public class TestController {

    @Limit(name="测试",key = "key",period = 60,count = 3)
    @GetMapping("/test")
    public String test(){
        return "test";
    }
}
