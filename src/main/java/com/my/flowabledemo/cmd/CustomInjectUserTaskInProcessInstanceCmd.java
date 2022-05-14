package com.my.flowabledemo.cmd;

import com.my.flowabledemo.util.Snowflake;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cmd.AbstractDynamicInjectionCmd;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.dynamic.BaseDynamicSubProcessInjectUtil;
import org.flowable.engine.impl.dynamic.DynamicUserTaskBuilder;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.beans.BeanUtils;

/**
 * @author 杨宇帆
 */
public class CustomInjectUserTaskInProcessInstanceCmd extends AbstractDynamicInjectionCmd implements Command<Void> {

    protected String processInstanceId;

    protected DynamicUserTaskBuilder dynamicUserTaskBuilder;

    protected FlowElement currentFlowElemet;


    public CustomInjectUserTaskInProcessInstanceCmd(String processInstanceId,
                                                    DynamicUserTaskBuilder dynamicUserTaskBuilder,
                                                    FlowElement currentFlowElemet) {
        this.processInstanceId = processInstanceId;
        this.dynamicUserTaskBuilder = dynamicUserTaskBuilder;
        this.currentFlowElemet = currentFlowElemet;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        createDerivedProcessDefinitionForProcessInstance(commandContext, processInstanceId);
        return null;
    }

    @Override
    protected void updateBpmnProcess(CommandContext commandContext, Process process,
                                     BpmnModel bpmnModel, ProcessDefinitionEntity originalProcessDefinitionEntity,
                                     DeploymentEntity newDeploymentEntity) {

        List<StartEvent> startEvents = process.findFlowElementsOfType(StartEvent.class);
        StartEvent initialStartEvent = null;
        for (StartEvent startEvent : startEvents) {
            if (startEvent.getEventDefinitions().size() == 0) {
                initialStartEvent = startEvent;
                break;

            } else if (initialStartEvent == null) {
                initialStartEvent = startEvent;
            }
        }
        if (currentFlowElemet != null) {

            UserTask userTask = new UserTask();
            BeanUtils.copyProperties(currentFlowElemet, userTask);
            if (dynamicUserTaskBuilder.getId() != null) {
                userTask.setId(dynamicUserTaskBuilder.getId());
            } else {
                userTask.setId(dynamicUserTaskBuilder.nextTaskId(process.getFlowElementMap()));
            }
            dynamicUserTaskBuilder.setDynamicTaskId(userTask.getId());

            userTask.setName(dynamicUserTaskBuilder.getName());
            userTask.setAssignee(dynamicUserTaskBuilder.getAssignee());

            UserTask currentFlowElemet = (UserTask) this.currentFlowElemet;
            SequenceFlow sequenceFlow = null;

            List<SequenceFlow> outgoingFlows = new ArrayList<>();
            for (SequenceFlow sequenceFlow1 : currentFlowElemet.getOutgoingFlows()) {
                sequenceFlow = new SequenceFlow(userTask.getId(), sequenceFlow1.getTargetRef());
                sequenceFlow.setSkipExpression(sequenceFlow1.getSkipExpression());
                sequenceFlow.setConditionExpression(sequenceFlow1.getConditionExpression());
                sequenceFlow.setExtensionElements(sequenceFlow1.getExtensionElements());
                sequenceFlow.setExecutionListeners(sequenceFlow1.getExecutionListeners());
                sequenceFlow.setName(sequenceFlow1.getName());
                sequenceFlow.setId("seq_" + new Snowflake().nextId());
                outgoingFlows.add(sequenceFlow);
                //删除原先节点的出线
                process.removeFlowElement(sequenceFlow1.getId());
                process.addFlowElement(sequenceFlow);
            }

            List<SequenceFlow> incomingFlows = new ArrayList<>();
            SequenceFlow incomingFlow = new SequenceFlow(currentFlowElemet.getId(), userTask.getId());
            // 可以设置唯一编号，这里通过雪花算法设置
            incomingFlow.setId("seq_" + new Snowflake().nextId());
            incomingFlows.add(incomingFlow);

            process.addFlowElement(incomingFlow);
            userTask.setOutgoingFlows(outgoingFlows);
            userTask.setIncomingFlows(incomingFlows);
            process.addFlowElement(userTask);

            //新增坐标 点
            GraphicInfo elementGraphicInfo = bpmnModel.getGraphicInfo(currentFlowElemet.getId());
            if (elementGraphicInfo != null) {
                double yDiff = 0;
                double xDiff = 80;
                if (elementGraphicInfo.getY() < 173) {
                    yDiff = 173 - elementGraphicInfo.getY();
                    elementGraphicInfo.setY(173);
                }

                Map<String, GraphicInfo> locationMap = bpmnModel.getLocationMap();
                for (String locationId : locationMap.keySet()) {
                    if (initialStartEvent.getId().equals(locationId)) {
                        continue;
                    }

                    GraphicInfo locationGraphicInfo = locationMap.get(locationId);
                    locationGraphicInfo.setX(locationGraphicInfo.getX() + xDiff);
                    locationGraphicInfo.setY(locationGraphicInfo.getY() + yDiff);
                }

                Map<String, List<GraphicInfo>> flowLocationMap = bpmnModel.getFlowLocationMap();
                for (String flowId : flowLocationMap.keySet()) {
//                    if (flowFromStart.getId().equals(flowId)) {
//                        continue;
//                    }

                    List<GraphicInfo> flowGraphicInfoList = flowLocationMap.get(flowId);
                    for (GraphicInfo flowGraphicInfo : flowGraphicInfoList) {
                        flowGraphicInfo.setX(flowGraphicInfo.getX() + xDiff);
                        flowGraphicInfo.setY(flowGraphicInfo.getY() + yDiff);

                    }
                }

				/* 以下代码 可以替换以下步骤,推荐使用这种
				 步骤一： 引入 自动排版jar
				<dependency>
            		<groupId>org.flowable</groupId>
            		<artifactId>flowable-bpmn-layout</artifactId>
            		<version>6.4.1</version>
       			 </dependency>
       			 步骤二 调用自动排版方法：
       			         new BpmnAutoLayout(bpmnModel).execute();
				*/

                /* 手动绘制节点 */
                GraphicInfo newTaskGraphicInfo = new GraphicInfo(elementGraphicInfo.getX() + 185,
                                                                 elementGraphicInfo.getY() - 163, 80, 100);
                bpmnModel.addGraphicInfo(userTask.getId(), newTaskGraphicInfo);

                bpmnModel.addFlowGraphicInfoList(userTask.getId(), createWayPoints(elementGraphicInfo.getX() + 95,
                                                                                   elementGraphicInfo.getY() - 5,
                                                                                   elementGraphicInfo.getX() + 95,
                                                                                   elementGraphicInfo.getY() - 123,
                                                                                   elementGraphicInfo.getX() + 185,
                                                                                   elementGraphicInfo.getY() - 123));

                List<SequenceFlow> addFlows = new ArrayList<>();
                addFlows.addAll(outgoingFlows);
                addFlows.addAll(incomingFlows);

                /* 绘制连线 */
                for (SequenceFlow sequenceFlow1 : addFlows) {
                    bpmnModel.addFlowGraphicInfoList(sequenceFlow1.getId(),
                                                     createWayPoints(elementGraphicInfo.getX() + 30,
                                                                     elementGraphicInfo.getY() + 15,
                                                                     elementGraphicInfo.getX() + 75,
                                                                     elementGraphicInfo.getY() + 15));
                }
            }

        } else {

            ParallelGateway parallelGateway = new ParallelGateway();
            parallelGateway.setId(dynamicUserTaskBuilder.nextForkGatewayId(process.getFlowElementMap()));
            process.addFlowElement(parallelGateway);

            UserTask userTask = new UserTask();
            if (dynamicUserTaskBuilder.getId() != null) {
                userTask.setId(dynamicUserTaskBuilder.getId());
            } else {
                userTask.setId(dynamicUserTaskBuilder.nextTaskId(process.getFlowElementMap()));
            }
            dynamicUserTaskBuilder.setDynamicTaskId(userTask.getId());

            userTask.setName(dynamicUserTaskBuilder.getName());
            userTask.setAssignee(dynamicUserTaskBuilder.getAssignee());
            process.addFlowElement(userTask);

            EndEvent endEvent = new EndEvent();
            endEvent.setId(dynamicUserTaskBuilder.nextEndEventId(process.getFlowElementMap()));
            process.addFlowElement(endEvent);

            SequenceFlow flowToUserTask = new SequenceFlow(parallelGateway.getId(), userTask.getId());
            flowToUserTask.setId(dynamicUserTaskBuilder.nextFlowId(process.getFlowElementMap()));
            process.addFlowElement(flowToUserTask);

            SequenceFlow flowFromUserTask = new SequenceFlow(userTask.getId(), endEvent.getId());
            flowFromUserTask.setId(dynamicUserTaskBuilder.nextFlowId(process.getFlowElementMap()));
            process.addFlowElement(flowFromUserTask);

            SequenceFlow initialFlow = initialStartEvent.getOutgoingFlows().get(0);
            initialFlow.setSourceRef(parallelGateway.getId());

            SequenceFlow flowFromStart = new SequenceFlow(initialStartEvent.getId(), parallelGateway.getId());
            flowFromStart.setId(dynamicUserTaskBuilder.nextFlowId(process.getFlowElementMap()));
            process.addFlowElement(flowFromStart);
            //跳整节点的布局
            GraphicInfo elementGraphicInfo = bpmnModel.getGraphicInfo(initialStartEvent.getId());
            if (elementGraphicInfo != null) {
                double yDiff = 0;
                double xDiff = 80;
                if (elementGraphicInfo.getY() < 173) {
                    yDiff = 173 - elementGraphicInfo.getY();
                    elementGraphicInfo.setY(173);
                }

                Map<String, GraphicInfo> locationMap = bpmnModel.getLocationMap();
                for (String locationId : locationMap.keySet()) {
                    if (initialStartEvent.getId().equals(locationId)) {
                        continue;
                    }

                    GraphicInfo locationGraphicInfo = locationMap.get(locationId);
                    locationGraphicInfo.setX(locationGraphicInfo.getX() + xDiff);
                    locationGraphicInfo.setY(locationGraphicInfo.getY() + yDiff);
                }

                Map<String, List<GraphicInfo>> flowLocationMap = bpmnModel.getFlowLocationMap();
                for (String flowId : flowLocationMap.keySet()) {
                    if (flowFromStart.getId().equals(flowId)) {
                        continue;
                    }

                    List<GraphicInfo> flowGraphicInfoList = flowLocationMap.get(flowId);
                    for (GraphicInfo flowGraphicInfo : flowGraphicInfoList) {
                        flowGraphicInfo.setX(flowGraphicInfo.getX() + xDiff);
                        flowGraphicInfo.setY(flowGraphicInfo.getY() + yDiff);
                    }
                }

                GraphicInfo forkGraphicInfo = new GraphicInfo(elementGraphicInfo.getX() + 75,
                                                              elementGraphicInfo.getY() - 5, 40, 40);
                bpmnModel.addGraphicInfo(parallelGateway.getId(), forkGraphicInfo);

                bpmnModel.addFlowGraphicInfoList(flowFromStart.getId(), createWayPoints(elementGraphicInfo.getX() + 30,
                                                                                        elementGraphicInfo.getY() + 15,
                                                                                        elementGraphicInfo.getX() + 75,
                                                                                        elementGraphicInfo.getY()
                                                                                            + 15));

                GraphicInfo newTaskGraphicInfo = new GraphicInfo(elementGraphicInfo.getX() + 185,
                                                                 elementGraphicInfo.getY() - 163, 80, 100);
                bpmnModel.addGraphicInfo(userTask.getId(), newTaskGraphicInfo);

                bpmnModel.addFlowGraphicInfoList(flowToUserTask.getId(), createWayPoints(elementGraphicInfo.getX() + 95,
                                                                                         elementGraphicInfo.getY() - 5,
                                                                                         elementGraphicInfo.getX() + 95,
                                                                                         elementGraphicInfo.getY()
                                                                                             - 123,
                                                                                         elementGraphicInfo.getX()
                                                                                             + 185,
                                                                                         elementGraphicInfo.getY()
                                                                                             - 123));

                GraphicInfo endGraphicInfo = new GraphicInfo(elementGraphicInfo.getX() + 335,
                                                             elementGraphicInfo.getY() - 137, 28, 28);
                bpmnModel.addGraphicInfo(endEvent.getId(), endGraphicInfo);

                bpmnModel.addFlowGraphicInfoList(flowFromUserTask.getId(),
                                                 createWayPoints(elementGraphicInfo.getX() + 285,
                                                                 elementGraphicInfo.getY() - 123,
                                                                 elementGraphicInfo.getX() + 335,
                                                                 elementGraphicInfo.getY() - 123));
            }
        }

        BaseDynamicSubProcessInjectUtil
            .processFlowElements(commandContext, process, bpmnModel, originalProcessDefinitionEntity,
                                 newDeploymentEntity);
    }

    @Override
    protected void updateExecutions(CommandContext commandContext, ProcessDefinitionEntity processDefinitionEntity,
                                    ExecutionEntity processInstance, List<ExecutionEntity> childExecutions) {

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        List<ExecutionEntity> oldExecution = executionEntityManager
            .findChildExecutionsByProcessInstanceId(processInstance.getProcessInstanceId());
        ExecutionEntity execution = executionEntityManager.createChildExecution(processInstance);
        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionEntity.getId());

        org.flowable.task.service.TaskService taskService = CommandContextUtil.getTaskService(commandContext);
        List<TaskEntity> taskEntities = taskService.findTasksByProcessInstanceId(processInstanceId);
        // 删除当前活动任务
        for (TaskEntity taskEntity : taskEntities) {
            taskEntity.getIdentityLinks().stream().forEach(identityLinkEntity -> {
                if (identityLinkEntity.isGroup()) {
                    taskEntity.deleteGroupIdentityLink(identityLinkEntity.getGroupId(), "candidate");
                } else {
                    taskEntity.deleteUserIdentityLink(identityLinkEntity.getUserId(), "participant");
                }
            });
            if (taskEntity.getTaskDefinitionKey().equals(currentFlowElemet.getId())) {

                taskService.deleteTask(taskEntity, false);
            }
        }
        //设置活动后的节点
        UserTask userTask = (UserTask) bpmnModel.getProcessById(processDefinitionEntity.getKey())
                                                .getFlowElement(dynamicUserTaskBuilder.getId());
        execution.setCurrentFlowElement(userTask);
        Context.getAgenda().planContinueProcessOperation(execution);
    }
}
