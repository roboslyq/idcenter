/* 
 * 作者：钟勋 (e-mail:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2017-12-02 13:33 创建
 */
package org.antframework.ids.biz.service;

import org.antframework.boot.bekit.AntBekitException;
import org.antframework.common.util.facade.CommonResultCode;
import org.antframework.common.util.facade.Status;
import org.antframework.ids.dal.dao.IderDao;
import org.antframework.ids.dal.dao.ProducerDao;
import org.antframework.ids.dal.entity.Ider;
import org.antframework.ids.dal.entity.Producer;
import org.antframework.ids.facade.order.AddIderOrder;
import org.antframework.ids.facade.result.AddIderResult;
import org.antframework.ids.facade.util.PeriodUtils;
import org.bekit.service.annotation.service.Service;
import org.bekit.service.annotation.service.ServiceExecute;
import org.bekit.service.engine.ServiceContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * 新增id提供者服务
 */
@Service(enableTx = true)
public class AddIderService {
    @Autowired
    private IderDao iderDao;
    @Autowired
    private ProducerDao producerDao;

    @ServiceExecute
    public void execute(ServiceContext<AddIderOrder, AddIderResult> context) {
        AddIderOrder order = context.getOrder();

        Ider ider = iderDao.findLockByIdCode(order.getIdCode());
        if (ider != null) {
            throw new AntBekitException(Status.FAIL, CommonResultCode.INVALID_PARAMETER.getCode(), String.format("id提供者[%s]已存在", order.getIdCode()));
        }
        ider = buildIder(order);
        producerDao.save(buildProducer(ider));
        iderDao.save(ider);
    }

    // 构建id提供者
    private Ider buildIder(AddIderOrder addIderOrder) {
        Ider ider = new Ider();
        BeanUtils.copyProperties(addIderOrder, ider);
        ider.setFactor(1);

        return ider;
    }

    // 构建生产者
    private Producer buildProducer(Ider ider) {
        Producer producer = new Producer();
        producer.setIdCode(ider.getIdCode());
        producer.setIndex(0);
        producer.setCurrentPeriod(PeriodUtils.parse(ider.getPeriodType(), new Date()));
        producer.setCurrentId(0L);

        return producer;
    }
}