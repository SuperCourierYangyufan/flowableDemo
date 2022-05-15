# Flowable

### ProcessEngine
* RepositoryService
    - 资源管理,流程部署
* RuntimeService
    - 流程运行
* TaskService
    - 任务处理
* HistoryService
    - 历史信息


### 表
* act_re_deployment 一次流程部署操作会生成一条数据
* act_ge_bytearray 部署后,部署的xml信息
* act_re_procdef 一次流程部署操作,有几条流程定义,就会几条记录
* act_ru_variable 流程变量
* act_ru_task 流程部署信息,每条串行线一条数据,总览,完成数据后就没了
* act_ru_execution 运行时流程执行实例表,完成数据后就没了
* act_ru_actinst 流程运行详情
* act_ru_event_subscr 运行时事件
* act_ru_identitylink 运行时用户关系
* act_hi_actinst 流程运行历史记录,详情,完成数据后添加
* act_hi_identitylink 流程运行的处理人

### 事件
* XML定义好对应的事件类后,需要类实现JavaDelegate.public void execute(DelegateExecution execution),使用springUtils
* 定时器
    - 启动,可以定时器自己启动事件,循环参数生效,可以多次
    - 中间捕获,等到前面审批完成,达到中间捕获定时器节点时,等待设置的时间后,才会到达下一个审批(指定循环,也只会执行一次)
    - 边界,等待多久没有执行,进行其他流程(指定循环,也只会执行一次)
* 消息
    * 需要先在全局消息定义中定义消息,然后对消息节点设置消息引用
    * 类型
        - 启动:processEngine.getRuntimeService().startProcessInstanceByMessage("消息的name,并非id")
        - 中间:只有接受特定消息后才能继续流程processEngine.getRuntimeService().messageEventReceived(消息name,executionId);
        - 边界: 通过消息走其他流程 processEngine.getRuntimeService().messageEventReceived(消息name,executionId);
* 错误
    * 启动,只能用事件子流程(一个页面,一个主流程,一个事件子流程),需要定义错误引用id
        ``` 
            //定义xml中需要在<process>同一层级增加
            <error id="错误引用id" errorCode="code定义"></error>
            //代码中
            throw new BpmnError(errorCode)
        ```
    * 边界,操作和启动一样,当抛出code异常后会走对应边界的异常的流程
* 信号
     * 需要先在全局消息定义中定义信号,id,name,scope(全局,流程实例)
     * processEngine.getRuntimeService().signalEventReceived("定义信号的name",executionId);
     * 类型
        - 启动,信号引用设置,正常部署流程,再释放信号才会正常进行流程