package com.my.flowabledemo;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
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
        log.info("deployment:id"+deployment.getParentDeploymentId());
        log.info("deployment:name"+deployment.getName());
    }

    /**
     * 定义的查询,删除
     */
    @Test
    void fun2(){
        Deployment deployment = processEngine.getRepositoryService()
                                             .createDeploymentQuery()
                                             .deploymentId("4cbc5b46-cf92-11ec-be22-00ff2a9c3e4d")
                                             .singleResult();
        log.info(deployment.getParentDeploymentId());
        log.info(deployment.getId());

        //删除,如果部署的流程启动了,不允许删除
        //若为(ID,Boolean)的构造,当传入true,表示流程启动了也删除,包括下方的任务,一起删除
        processEngine.getRepositoryService()
                     .deleteDeployment(deployment.getParentDeploymentId());
    }

    /**
     * 启动流程
     */
    @Test
    void fun3(){
        //变量
        Map<String,Object> params = new HashMap<String,Object>(){{
            put("name","测试人员");
//            //网关判断变量
//            put("approved",true);
        }};
        //启动流程,绑定变量
        ProcessInstance processInstance = processEngine.getRuntimeService()
                                                       .startProcessInstanceByKey("testRequest", params);
        log.info("流程id:"+processInstance.getProcessDefinitionId());
        log.info("流程部署id:"+processInstance.getDeploymentId());
        log.info("流程活跃id:"+processInstance.getActivityId());
        log.info("流程实例id:"+processInstance.getId());
    }

}
