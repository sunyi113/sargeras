package org.mltds.sargeras.api;

import java.util.Date;

public class SagaContextBase {

    protected Long id;

    protected String appName;
    protected String bizName;
    protected String bizId;

    protected SagaStatus status;

    protected String currentTxName;
    protected String preExecutedTxName;
    protected String preCompensatedTxName;

    /**
     * 每次执行的id, 伴随着 Context 对象的生命周期，不需要存储<br/>
     * 比如第一次执行，triggerId 为 A ，返回处理中后挂起，过段时间第二次执行的时候为 B 。<br/>
     */
    protected String triggerId;
    /**
     * 触发执行次数，首次执行记为1，每次轮询重试+1
     */
    protected int triggerCount;
    /**
     * 当遇到处理中的情况时，期望下一次轮询重试的触发时间。
     */
    protected Date nextTriggerTime;

    /**
     * 创建时间
     */
    protected Date createTime;

    /**
     * 过期时间，过期时间，创建时间加业务超时时间
     */
    protected Date expireTime;

    /**
     * 最新修改时间
     */
    protected Date modifyTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getBizName() {
        return bizName;
    }

    public void setBizName(String bizName) {
        this.bizName = bizName;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public void setStatus(SagaStatus status) {
        this.status = status;
    }

    public String getCurrentTxName() {
        return currentTxName;
    }

    public void setCurrentTxName(String currentTxName) {
        this.currentTxName = currentTxName;
    }

    public String getPreExecutedTxName() {
        return preExecutedTxName;
    }

    public void setPreExecutedTxName(String preExecutedTxName) {
        this.preExecutedTxName = preExecutedTxName;
    }

    public String getPreCompensatedTxName() {
        return preCompensatedTxName;
    }

    public void setPreCompensatedTxName(String preCompensatedTxName) {
        this.preCompensatedTxName = preCompensatedTxName;
    }

    public String getTriggerId() {
        return triggerId;
    }

    public void setTriggerId(String triggerId) {
        this.triggerId = triggerId;
    }

    public int getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(int triggerCount) {
        this.triggerCount = triggerCount;
    }

    public Date getNextTriggerTime() {
        return nextTriggerTime;
    }

    public void setNextTriggerTime(Date nextTriggerTime) {
        this.nextTriggerTime = nextTriggerTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public Date getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Date modifyTime) {
        this.modifyTime = modifyTime;
    }

}