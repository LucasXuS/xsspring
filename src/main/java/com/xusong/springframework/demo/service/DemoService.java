package com.xusong.springframework.demo.service;

import com.xusong.springframework.annotation.XSService;

@XSService("demoService")
public interface DemoService {
    String plusDemo(String s);
}
