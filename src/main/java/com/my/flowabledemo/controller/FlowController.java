package com.my.flowabledemo.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author 杨宇帆
 */
@Controller
@Slf4j
public class FlowController {
    @Autowired
    private HistoryService historyService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ProcessEngine processEngine;

    @GetMapping(value = "/view")
    public void diagramView(String processInstanceId, HttpServletResponse httpServletResponse) {
        // 获得当前活动的节点
        String processDefinitionId;
        // 如果流程已经结束，则得到结束节点
        if (this.isFinished(processInstanceId)) {
            HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery()
                                                       .processInstanceId(processInstanceId).singleResult();
            processDefinitionId = pi.getProcessDefinitionId();
        } else {
            // 如果流程没有结束，则取当前活动节点
            // 根据流程实例ID获得当前处于活动状态的ActivityId合集
            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId)
                                               .singleResult();
            processDefinitionId = pi.getProcessDefinitionId();
        }
        List<String> highLightedActivities = new ArrayList<>();

        // 获得活动的节点
        List<HistoricActivityInstance> highLightedActivityList = historyService.createHistoricActivityInstanceQuery()
                                                                               .processInstanceId(processInstanceId)
                                                                               .orderByHistoricActivityInstanceStartTime()
                                                                               .asc().list();
        List<String> highLightedFlows = new ArrayList<>();

        for (HistoricActivityInstance tempActivity : highLightedActivityList) {
            String activityId = tempActivity.getActivityId();
            highLightedActivities.add(activityId);
            if ("sequenceFlow".equals(tempActivity.getActivityType())) {
                highLightedFlows.add(tempActivity.getActivityId());
            }
        }

        // 获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        ProcessEngineConfiguration engConf = processEngine.getProcessEngineConfiguration();

        ProcessDiagramGenerator diagramGenerator = engConf.getProcessDiagramGenerator();
//		ProcessDiagramGenerator diagramGenerator = new CustomProcessDiagramGenerator();
        InputStream in = diagramGenerator
            .generateDiagram(bpmnModel, "bmp", highLightedActivities, highLightedFlows, engConf.getActivityFontName(),
                             engConf.getLabelFontName(), engConf.getAnnotationFontName(), engConf.getClassLoader(), 1.0,
                             true);
        OutputStream out = null;
        byte[] buf = new byte[1024];
        int length;
        try {
            out = httpServletResponse.getOutputStream();
            while ((length = in.read(buf)) != -1) {
                out.write(buf, 0, length);
            }
        } catch (IOException e) {
            log.error("操作异常", e);
        } finally {
            IoUtil.closeSilently(out);
            IoUtil.closeSilently(in);
        }
    }

    private boolean isFinished(String processInstanceId) {
        return historyService.createHistoricProcessInstanceQuery().finished()
                             .processInstanceId(processInstanceId).count() > 0;
    }
}
