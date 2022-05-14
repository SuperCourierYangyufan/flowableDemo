package com.my.flowabledemo;

import com.my.flowabledemo.cmd.CustomInjectUserTaskInProcessInstanceCmd;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.dynamic.DynamicUserTaskBuilder;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
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
                                             .addClasspathResource("测试后加签.bpmn20.xml")
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
                                                       .startProcessInstanceByKey("testAddFlow", params);
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
            List<IdentityLink> identityLinkList = processEngine.getTaskService()
                                                               .getIdentityLinksForTask(task.getId());
            for (IdentityLink identityLink : identityLinkList) {
                log.info(identityLink.getGroupId());
                log.info(identityLink.getUserId());
            }
            log.info("================================================");
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

    /**
     * 串行回退 若 并行网关某线某节点还是回退至并行网关内 == 串行
     */
    @Test
    void fun7() {
        //串行==排他网关==多人会签
        processEngine.getRuntimeService()
                     .createChangeActivityStateBuilder()
                     .processInstanceId("流程实例id")
                     //节点跳转
                     .moveActivityIdTo("当前节点id", "目标节点id")
                     //节点跳转-根据Execution
                     .moveExecutionToActivityId(
                         processEngine.getTaskService().createTaskQuery().taskId("任务id").singleResult()
                                      .getExecutionId(), "目标节点id")
                     .changeState();
    }

    /**
     * 并行回退
     */
    @Test
    void fun8() {
        // 并行网关两条分支线内,某节点回退至并行网关之前,需要并行网关两条分支全部回退至并行网关前
        List<String> oldActIds = new ArrayList<String>() {{
            add("分支线1某节点");
            add("分支线2某节点");
        }};
        processEngine.getRuntimeService()
                     .createChangeActivityStateBuilder()
                     .processInstanceId("流程实例id")
                     .moveActivityIdsToSingleActivityId(oldActIds, "目标节点id");

        // 并行网关后,回退到并行网关内,需要回退到并行多条线上
        processEngine.getRuntimeService()
                     .createChangeActivityStateBuilder()
                     .processInstanceId("流程实例id")
                     .moveSingleActivityIdToActivityIds("当前id", oldActIds);
    }

    /**
     * 多人会签
     */
    @Test
    void fun9() {
        //需要XML设置[sequential:true串行,false并行,Loop cardinality:循环次数,Collection:循环集合Or类,
        //Element variable:循环集合循环出来的对象key,Completion condition:结束表达式Or类]
        //多实例,自带参数 nrOfInstances:实例总数 nrOfActiveInstances:未完成的实例 nrOfCompletedInstances:已完成实例
    }

    /**
     * 动态加签测试
     */
    @Test
    void fun10() {
        String processInstaceId = "73bdad85-d2ca-11ec-aac1-00ff2a9c3e4d";
        Task task = processEngine.getTaskService().createTaskQuery().processInstanceId(processInstaceId).singleResult();
        String processDefinitionId = "";
        DynamicUserTaskBuilder dynamicUserTaskBuilder = new DynamicUserTaskBuilder();
        dynamicUserTaskBuilder.setId("act_2");
        dynamicUserTaskBuilder.setName("测试节点2");
        dynamicUserTaskBuilder.setAssignee("yyf");
        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(task.getProcessDefinitionId());
        Process process = bpmnModel.getProcesses().get(0);
        processEngine.getManagementService().executeCommand(
            new CustomInjectUserTaskInProcessInstanceCmd(processInstaceId, dynamicUserTaskBuilder,
                                                         process.getFlowElement(task.getId())));
    }


    /**
     * 删除运行的实例
     */
    @Test
    void fun11() {
        processEngine.getRuntimeService().deleteProcessInstance("73bdad85-d2ca-11ec-aac1-00ff2a9c3e4d", "删除实例");
    }

    /**
     * 签收
     */
    @Test
    void fun12() {
        Task task = processEngine.getTaskService().createTaskQuery()
                                 .processInstanceId("73bdad85-d2ca-11ec-aac1-00ff2a9c3e4d").singleResult();
        processEngine.getTaskService().claim(task.getId(), "user1");
    }
}
