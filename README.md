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
* act_re_deployment 部署后,部署定义的信息
* act_ge_bytearray 部署后,部署的xml信息
* act_re_procdef 部署后,流程的记录信息
* act_ru_variable 流程变量
* act_ru_task 流程部署信息,总览,完成数据后就没了
* act_ru_execution 流程运行消息,详细,完成数据后就没了
* act_hi_actinst 流程运行历史记录,详情,完成数据后添加
* act_hi_identitylink 流程运行的处理人