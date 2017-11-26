/* 
 * 作者：钟勋 (e-mail:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2017-11-25 20:03 创建
 */
package org.antframework.ids.biz.service;

import org.antframework.boot.bekit.AntBekitException;
import org.antframework.common.util.facade.CommonResultCode;
import org.antframework.common.util.facade.Status;
import org.antframework.ids.biz.util.ProducerUtils;
import org.antframework.ids.dal.dao.IderDao;
import org.antframework.ids.dal.dao.ProducerDao;
import org.antframework.ids.dal.entity.Ider;
import org.antframework.ids.dal.entity.Producer;
import org.antframework.ids.facade.order.ModifyIderProducerNumberOrder;
import org.antframework.ids.facade.result.ModifyIderProducerNumberResult;
import org.bekit.service.annotation.service.Service;
import org.bekit.service.annotation.service.ServiceExecute;
import org.bekit.service.engine.ServiceContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 修改id提供者的生产者数量服务
 */
@Service(enableTx = true)
public class ModifyIderProducerNumberService {
    @Autowired
    private IderDao iderDao;
    @Autowired
    private ProducerDao producerDao;

    @ServiceExecute
    public void execute(ServiceContext<ModifyIderProducerNumberOrder, ModifyIderProducerNumberResult> context) {
        ModifyIderProducerNumberOrder order = context.getOrder();

        Ider ider = iderDao.findLockByIdCode(order.getIdCode());
        if (ider == null) {
            throw new AntBekitException(Status.FAIL, CommonResultCode.INVALID_PARAMETER.getCode(), String.format("id提供者[%s]不存在", order.getIdCode()));
        }
        if (Math.max(order.getNewProducerNumber(), ider.getProducerNumber()) % Math.min(order.getNewProducerNumber(), ider.getProducerNumber()) != 0) {
            throw new AntBekitException(Status.FAIL, CommonResultCode.INVALID_PARAMETER.getCode(), String.format("生产者数量要么成倍增加要么成倍减少，id提供者[%s]当前生产者数量[%s]，期望数量[%s]不符合要求", order.getIdCode(), ider.getProducerNumber(), order.getNewProducerNumber()));
        }

        if (order.getNewProducerNumber() > ider.getProducerNumber()) {
            addProducers(ider, order.getNewProducerNumber());
        } else if (order.getNewProducerNumber() < ider.getProducerNumber()) {
            deleteProducers(ider, order.getNewProducerNumber());
        }

        ider.setProducerNumber(order.getNewProducerNumber());
        iderDao.save(ider);
    }

    // 添加id生产者
    private void addProducers(Ider ider, int newProducerNumber) {
        List<Producer> producers = producerDao.findLockByIdCodeOrderByIndexAsc(ider.getIdCode());
        for (int i = ider.getProducerNumber(); i < newProducerNumber; i++) {
            Producer producer = buildProducer(producers.get(i % ider.getProducerNumber()), i, ider);
            producerDao.save(producer);
        }
    }

    // 构建id生产者
    private Producer buildProducer(Producer sourceProducer, int index, Ider ider) {
        Producer producer = new Producer();
        producer.setIdCode(sourceProducer.getIdCode());
        producer.setIndex(index);
        producer.setCurrentPeriod(sourceProducer.getCurrentPeriod());
        producer.setCurrentId(sourceProducer.getCurrentId());
        ProducerUtils.produce(producer, ider, index - sourceProducer.getIndex());

        return producer;
    }

    // 删除id生产者
    private void deleteProducers(Ider ider, int newProducerNumber) {
        List<Producer> producers = producerDao.findLockByIdCodeOrderByIndexAsc(ider.getIdCode());
        for (int i = newProducerNumber; i < ider.getProducerNumber(); i++) {
            Producer deletingProducer = producers.get(i);
            updateProducer(producers.get(i % ider.getProducerNumber()), deletingProducer);
            producerDao.delete(deletingProducer);
        }
    }

    // 更新id生产者
    private void updateProducer(Producer producer, Producer deletingProducer) {
        if (new ProducerUtils.ProducerComparator().compare(producer, deletingProducer) < 0) {
            producer.setCurrentPeriod(deletingProducer.getCurrentPeriod());
            producer.setCurrentId(deletingProducer.getCurrentId());
            producerDao.save(producer);
        }
    }
}
