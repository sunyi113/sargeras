package org.mltds.sargeras.repository.rdbms;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mltds.sargeras.api.SagaApplication;
import org.mltds.sargeras.api.SagaContextBase;
import org.mltds.sargeras.api.SagaStatus;
import org.mltds.sargeras.exception.SagaException;
import org.mltds.sargeras.repository.Repository;
import org.mltds.sargeras.repository.rdbms.mapper.ContextInfoMapper;
import org.mltds.sargeras.repository.rdbms.mapper.ContextLockMapper;
import org.mltds.sargeras.repository.rdbms.mapper.ContextMapper;
import org.mltds.sargeras.repository.rdbms.model.ContextDO;
import org.mltds.sargeras.repository.rdbms.model.ContextInfoDO;
import org.mltds.sargeras.repository.rdbms.model.ContextLockDO;
import org.mltds.sargeras.serialize.Serialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sunyi
 */
public class RdbmsRepository implements Repository {

    private static final Logger logger = LoggerFactory.getLogger(RdbmsRepository.class);

    private SqlSessionFactory sqlSessionFactory;

    private ContextMapper contextMapper;
    private ContextInfoMapper contextInfoMapper;
    private ContextLockMapper contextLockMapper;

    private Serialize serialize = SagaApplication.getSerialize();

    @Override
    @Transaction
    public long saveContextAndLock(SagaContextBase context, int lockTimeout) {

        ContextDO contextDO = sagaContextToContextDO(context);

        Calendar c = Calendar.getInstance();

        contextDO.setCreateTime(c.getTime());
        contextDO.setModifyTime(c.getTime());

        contextMapper.insert(contextDO);

        Long id = contextDO.getId();

        lock(id, context.getTriggerId(), lockTimeout);

        return id;

    }

    @Override
    public SagaContextBase loadContext(long contextId) {

        ContextDO contextDO = contextMapper.selectById(contextId);
        if (contextDO == null) {
            throw new SagaException("查找Context失败，ID: " + contextId);
        }

        try {
            return contextDOToSagaContext(contextDO);
        } catch (ClassNotFoundException e) {
            throw new SagaException("重新构建SagaContext失败，ID：" + contextId, e);
        }

    }

    @Override
    public SagaContextBase loadContext(String appName, String bizName, String bizId) {

        ContextDO contextDO = contextMapper.selectByBiz(appName, bizName, bizId);
        if (contextDO == null) {
            throw new SagaException("查找Context失败，appName: " + appName + "，bizName：" + bizName + "，bizId：" + bizId);
        }

        try {
            return contextDOToSagaContext(contextDO);
        } catch (ClassNotFoundException e) {
            throw new SagaException("重新构建SagaContext失败，ID：" + contextDO.getId(), e);
        }

    }

    @Override
    public void saveContextStatus(long contextId, SagaStatus status) {
        ContextDO contextDO = new ContextDO();
        contextDO.setId(contextId);
        contextDO.setStatus(status);
        contextDO.setModifyTime(new Date());
        contextMapper.updateById(contextDO);
    }

    @Override
    public void saveCurrentTx(long contextId, String cls) {
        ContextDO contextDO = new ContextDO();
        contextDO.setId(contextId);
        contextDO.setCurrentTxName(cls);
        contextDO.setModifyTime(new Date());
        contextMapper.updateById(contextDO);
    }

    @Override
    public void savePreExecutedTx(long contextId, String cls) {
        ContextDO contextDO = new ContextDO();
        contextDO.setId(contextId);
        contextDO.setPreExecutedTxName(cls);
        contextDO.setModifyTime(new Date());
        contextMapper.updateById(contextDO);
    }

    @Override
    public void savePreCompensatedTx(long contextId, String cls) {
        ContextDO contextDO = new ContextDO();
        contextDO.setId(contextId);
        contextDO.setPreCompensatedTxName(cls);
        contextDO.setModifyTime(new Date());
        contextMapper.updateById(contextDO);
    }

    @Override
    public void incrementTriggerCount(long contextId) {
        contextMapper.incrementTriggerCount(contextId, new Date());
    }

    @Override
    public void saveNextTriggerTime(long contextId, Date nextTriggerTime) {
        ContextDO contextDO = new ContextDO();
        contextDO.setId(contextId);
        contextDO.setNextTriggerTime(nextTriggerTime);
        contextDO.setModifyTime(new Date());
        contextMapper.updateById(contextDO);
    }

    @Override
    public void saveContextInfo(long contextId, String key, Object info) {
        ContextInfoDO contextInfoDO = contextInfoMapper.selectByKey(contextId, key);
        if (contextInfoDO == null) {
            contextInfoDO = new ContextInfoDO();
            contextInfoDO.setContextId(contextId);
            contextInfoDO.setKey(key);
            String infoStr = serialize.write(info);
            contextInfoDO.setInfo(infoStr);
            Date now = new Date();
            contextInfoDO.setCreateTime(now);
            contextInfoDO.setModifyTime(now);

            contextInfoMapper.insert(contextInfoDO);
        } else {

            String infoStr = serialize.write(info);
            contextInfoDO.setKey(infoStr);
            contextInfoDO.setModifyTime(new Date());

            contextInfoMapper.updateById(contextInfoDO);
        }
    }

    @Override
    public <T> T loadContextInfo(long contextId, String key, Class<T> cls) {

        ContextInfoDO contextInfoDO = contextInfoMapper.selectByKey(contextId, key);
        if (contextInfoDO == null || contextInfoDO.getInfo() == null) {
            return null;
        }
        return serialize.read(contextInfoDO.getInfo(), cls);
    }

    @Override
    public boolean lock(long id, String reqId, int timeoutSec) {

        try {
            ContextLockDO contextLockDO = contextLockMapper.select(id);
            if (contextLockDO == null) {
                contextLockDO = newLock(id, reqId, timeoutSec);
                contextLockMapper.insert(contextLockDO);
                return true;
            } else {
                Calendar c = Calendar.getInstance();
                boolean after = c.getTime().after(contextLockDO.getExpireTime());
                if (after) {
                    int delete = contextLockMapper.delete(id, contextLockDO.getReqId());
                    if (delete <= 0) {
                        return false;
                    } else {
                        contextLockDO = newLock(id, reqId, timeoutSec);
                        contextLockMapper.insert(contextLockDO);
                        return true;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    private ContextLockDO newLock(long id, String reqId, int timeoutSec) {
        ContextLockDO contextLockDO = new ContextLockDO();
        contextLockDO.setContextId(id);
        contextLockDO.setReqId(reqId);

        Calendar c = Calendar.getInstance();
        contextLockDO.setCreateTime(c.getTime());

        c.add(Calendar.SECOND, timeoutSec);
        contextLockDO.setExpireTime(c.getTime());

        return contextLockDO;
    }

    @Override
    public boolean unlock(long id, String reqId) {

        try {
            ContextLockDO contextLockDO = contextLockMapper.select(id);
            if (contextLockDO == null) {
                return false;
            } else {
                if (contextLockDO.getReqId().equals(reqId)) {
                    int delete = contextLockMapper.delete(id, reqId);
                    return delete > 0;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            logger.warn("操作数据库释放锁失败ContextId:{},ReqId:{}", new Object[] { id, reqId }, e);
            return false;
        }
    }

    @Override
    public List<Long> findNeedRetryContextList(int limit) {
        return contextMapper.findNeedRetryContextList(new Date(), limit);
    }

    private ContextDO sagaContextToContextDO(SagaContextBase sagaContextBase) {
        try {
            ContextDO contextDO = new ContextDO();
            BeanUtils.copyProperties(contextDO, sagaContextBase);
            return contextDO;
        } catch (Exception e) {
            throw new SagaException("SagaContextBase转换为ContextDO失败，BizID:" + sagaContextBase.getBizId(), e);
        }
    }

    private SagaContextBase contextDOToSagaContext(ContextDO contextDO) throws ClassNotFoundException {
        try {
            SagaContextBase sagaContextBase = new SagaContextBase();
            BeanUtils.copyProperties(sagaContextBase, contextDO);
            return sagaContextBase;
        } catch (Exception e) {
            throw new SagaException("ContextDO转换为SagaContextBase失败，BizID:" + contextDO.getBizId(), e);
        }
    }

    void setContextMapper(ContextMapper contextMapper) {
        this.contextMapper = contextMapper;
    }

    void setContextInfoMapper(ContextInfoMapper contextInfoMapper) {
        this.contextInfoMapper = contextInfoMapper;
    }

    void setContextLockMapper(ContextLockMapper contextLockMapper) {
        this.contextLockMapper = contextLockMapper;
    }

}
