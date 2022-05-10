package com.my.flowabledemo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class FlowableDemoApplicationTests {
    @Autowired
    private ProcessEngine processEngine;

    /**
     * 简单部署流程
     */
    @Test
    void fun1() {
        Deployment deployment = processEngine.getRepositoryService()
                                             .createDeployment()
                                             .addClasspathResource("test.bpmn20.xml")
                                             .name("测试流程")
                                             .deploy();
        log.info("deployment:id" + deployment.getId());
        log.info("deployment:name" + deployment.getName());
    }

    /**
     * 定义的查询,删除
     */
    @Test
    void fun2() {
        Deployment deployment = processEngine.getRepositoryService()
                                             .createDeploymentQuery()
                                             .deploymentId("c9a45460-d055-11ec-994b-00ff2a9c3e4d")
                                             .singleResult();
        log.info(deployment.getParentDeploymentId());
        log.info(deployment.getId());

        //删除,如果部署的流程启动了,不允许删除
        //若为(ID,Boolean)的构造,当传入true,表示流程启动了也删除,包括下方的任务,一起删除
        processEngine.getRepositoryService()
                     .deleteDeployment(deployment.getParentDeploymentId(), true);
    }

    /**
     * 启动流程
     */
    @Test
    void fun3() {
        //变量
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("name", "测试人员");
        }};
        //启动流程,绑定变量
        ProcessInstance processInstance = processEngine.getRuntimeService()
                                                       .startProcessInstanceByKey("testRequest", params);
        log.info("流程id:" + processInstance.getProcessDefinitionId());
        log.info("流程部署id:" + processInstance.getDeploymentId());
        log.info("流程活跃id:" + processInstance.getActivityId());
        log.info("流程实例id:" + processInstance.getId());
    }

    /**
     * 查询任务
     */
    @Test
    void fun4() {
        List<Task> tasks = processEngine.getTaskService()
                                        .createTaskQuery()
                                        .processDefinitionKey("testRequest")
                                        .taskAssignee("yyf")
                                        .list();
        for (Task task : tasks) {
            log.info(task.getAssignee());
            log.info(task.getId());
            log.info(task.getProcessDefinitionId());
            log.info(task.getName());
        }
    }

    /**
     * 处理任务
     */
    @Test
    void fun5() {
        Task task = processEngine.getTaskService()
                                 .createTaskQuery()
                                 .taskAssignee("yyf")
                                 .singleResult();

        //变量
        Map<String, Object> params = new HashMap<String, Object>() {{
            //设置false 网关false流程,发送邮件
            put("approved", false);
        }};
        //完成
        processEngine.getTaskService().complete(task.getId(), params);
    }

    /**
     * 查询历史
     */
    @Test
    void fun6() {
        List<HistoricActivityInstance> list = processEngine.getHistoryService()
                                                           .createHistoricActivityInstanceQuery()
//                                                           .taskAssignee("yyf")
                                                           .processInstanceId("bcf86ed1-d058-11ec-bd5c-00ff2a9c3e4d")
                                                           .processDefinitionId(
                                                               "testRequest:1:ac470e00-d058-11ec-94ba-00ff2a9c3e4d")
                                                           //已完成
                                                           .finished()
                                                           .orderByHistoricActivityInstanceEndTime()
                                                           .asc()
                                                           .list();
        for (HistoricActivityInstance histor : list) {
            log.info(histor.getActivityId());
            log.info(histor.getActivityName());
            log.info(histor.getAssignee());
            log.info(histor.getTaskId());
            log.info(histor.getDurationInMillis().toString());
            log.info("==================================");
        }
    }
}
