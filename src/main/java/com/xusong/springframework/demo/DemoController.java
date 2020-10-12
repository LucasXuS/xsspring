package com.xusong.springframework.demo;

import com.xusong.springframework.annotation.XSAutowired;
import com.xusong.springframework.annotation.XSController;
import com.xusong.springframework.annotation.XSRequestMapping;
import com.xusong.springframework.demo.service.Demo2Service;
import com.xusong.springframework.demo.service.DemoService;
import com.xusong.springframework.demo.service.ShowService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@XSController
@XSRequestMapping("/demo")
public class DemoController {

    @XSAutowired
    private DemoService demoService;

    @XSAutowired
    private Demo2Service demo2Service;

    @XSAutowired
    private ShowService showService;


    @XSRequestMapping("/index")
    public void index(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String s = request.getParameter("param");
        s = demoService.plusDemo(s);
        s = demo2Service.plusDemo2(s);
        response.getWriter().write("Welcome to XSSpring framework! The test print is: " + s);
    }

    @XSRequestMapping("/show")
    public void showMore(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.getWriter().write(showService.show());
    }
}
