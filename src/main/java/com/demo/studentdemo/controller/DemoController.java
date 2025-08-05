package com.demo.studentdemo.controller;

import com.demo.studentdemo.entity.Student;
import com.demo.studentdemo.service.DemoService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/demo")
public class DemoController {
    @Autowired
    private DemoService demoService;

    @SneakyThrows
    @GetMapping("/runExample")
    public ResponseEntity<String> runExample(){
        demoService.runExample();
        return ResponseEntity.ok("OK");
    }

    @SneakyThrows
    @GetMapping("/runExampleAsync")
    public ResponseEntity<String> runExampleAsync(){
        demoService.runExampleAsync();
        return ResponseEntity.ok("OK");
    }
}


