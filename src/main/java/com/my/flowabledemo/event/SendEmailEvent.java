package com.my.flowabledemo.event;

import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author 杨宇帆
 */
@Log4j2
public class SendEmailEvent implements JavaDelegate {

    /**
     * 无法spring互通,用SpringUtils工具类
     *
     * @param execution
     */
    @Override
    public void execute(DelegateExecution execution) {
        log.info("==================发送邮件拉!========================");
        Set<String> params = execution.getVariableNames();
        for (String param : params) {
            Object value = execution.getVariable(param);
            log.info("key:{},value:{}", param, value);
        }
    }
}
