package com.xusong.springframework.demo.service.impl;

import com.xusong.springframework.annotation.XSService;
import com.xusong.springframework.demo.service.Demo2Service;

@XSService("demo2Service")
public class Demo2ServiceImpl implements Demo2Service {
    @Override
    public String plusDemo2(String s) {
        return s + " and plus demo2";
    }
}
