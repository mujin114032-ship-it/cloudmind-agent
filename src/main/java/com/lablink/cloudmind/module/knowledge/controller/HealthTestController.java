package com.lablink.cloudmind.module.knowledge.controller;

import com.lablink.cloudmind.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthTestController {

    @GetMapping("/api/test/ping")
    public Result<String> ping() {
        return Result.success("cloudmind-agent is running");
    }
}