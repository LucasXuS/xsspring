package com.xusong.springframework.demo.service.impl;


import com.xusong.springframework.demo.service.DemoService;


public class DemoServiceImpl implements DemoService {
    @Override
    public String plusDemo(String s) {
        return s + " plus demo";
    }
}
