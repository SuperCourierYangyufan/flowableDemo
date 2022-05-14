package com.my.flowabledemo.service.Impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.my.flowabledemo.service.flowService;
import com.sun.xml.internal.ws.util.xml.XmlUtil;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.validation.ProcessValidator;
import org.flowable.validation.ProcessValidatorFactory;
import org.flowable.validation.ValidationError;
import org.springframework.stereotype.Service;

/**
 * @author 杨宇帆
 */
@Slf4j
@Service
public class flowServiceImpl implements flowService {
    protected BpmnXMLConverter bpmnXmlConverter = new BpmnXMLConverter();

    @Override
    public void createModel(InputStream inputStream) throws Exception{
        XMLInputFactory xif = XmlUtil.createSafeXmlInputFactory();
        InputStreamReader xmlIn = new InputStreamReader(inputStream, "UTF-8");
        XMLStreamReader xtr = xif.createXMLStreamReader(xmlIn);
        BpmnModel bpmnModel = bpmnXmlConverter.convertToBpmnModel(xtr);
        //模板验证
        ProcessValidator validator = new ProcessValidatorFactory().createDefaultProcessValidator();
        List<ValidationError> errors = validator.validate(bpmnModel);
        if (CollectionUtils.isNotEmpty(errors)) {
            StringBuffer es = new StringBuffer();
            errors.forEach(ve -> es.append(ve.toString()).append("/n"));
            return new ReturnVo(ReturnCode.SUCCESS,"模板验证失败，原因: " + es.toString());
        }
        String fileName = bpmnModel.getMainProcess().getName();
        if (CollectionUtils.isEmpty(bpmnModel.getProcesses())) {
            return new ReturnVo(ReturnCode.SUCCESS,"No process found in definition " + fileName);
        }
        if (bpmnModel.getLocationMap().size() == 0) {
            BpmnAutoLayout bpmnLayout = new BpmnAutoLayout(bpmnModel);
            bpmnLayout.execute();
        }
        ObjectNode modelNode = bpmnJsonConverter.convertToJson(bpmnModel);
        org.flowable.bpmn.model.Process process = bpmnModel.getMainProcess();
        String name = process.getId();
        if (StringUtils.isNotEmpty(process.getName())) {
            name = process.getName();
        }
        String description = process.getDocumentation();
        User createdBy = SecurityUtils.getCurrentUserObject();
        //查询是否已经存在流程模板
        Model newModel = new Model();
        List<Model> models = modelRepository.findByKeyAndType(process.getId(), AbstractModel.MODEL_TYPE_BPMN);
        if (CollectionUtils.isNotEmpty(models)) {
            Model updateModel = models.get(0);
            newModel.setId(updateModel.getId());
        }
        newModel.setName(name);
        newModel.setKey(process.getId());
        newModel.setModelType(AbstractModel.MODEL_TYPE_BPMN);
        newModel.setCreated(Calendar.getInstance().getTime());
        newModel.setCreatedBy(createdBy.getId());
        newModel.setDescription(description);
        newModel.setModelEditorJson(modelNode.toString());
        newModel.setLastUpdated(Calendar.getInstance().getTime());
        newModel.setLastUpdatedBy(createdBy.getId());
        modelService.createModel(newModel, SecurityUtils.getCurrentUserObject());
    }
}
