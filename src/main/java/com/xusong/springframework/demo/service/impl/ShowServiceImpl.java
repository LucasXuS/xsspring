package com.xusong.springframework.demo.service.impl;

import com.xusong.springframework.demo.service.ShowService;

/**
 * @author <a href="mailto:xusong@gtmap.cn">xusong</a>
 * @version 1.0, ${date}
 * @description: ${todo}
 */
public class ShowServiceImpl implements ShowService {
    @Override
    public String show() {
        return "test show";
    }
}
