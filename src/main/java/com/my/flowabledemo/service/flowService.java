package com.my.flowabledemo.service;

import java.io.InputStream;

/**
 * @author 杨宇帆
 */
public interface flowService {
    /**
     * 导入model
     * @param inputStream
     */
    void createModel(InputStream inputStream) throws Exception;
}
