package cn.duxinglan.media.impl.webrtc;

import cn.duxinglan.media.core.IConsumer;
import cn.duxinglan.media.core.IMediaNode;
import cn.duxinglan.media.core.IProducer;
import cn.duxinglan.media.core.IProducerMediaSubscriber;
import cn.duxinglan.media.protocol.rtp.TimerRtpPacket;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * 版权所有 (c) 2025 www.duxinglan.cn
 * <p>
 * 项目名称：xinglanRtc
 * <p>
 * 本文件属于 xinglanRtc 项目的一部分。
 * <p>
 * 本软件依据 XinglanRtc 非商业许可证（XNCL）授权，仅限个人非商业使用。
 * 禁止任何形式的商业用途，包括但不限于：收费安装、收费部署、
 * 收费运维、收费技术支持等行为。
 * <p>
 * 详情请参阅项目根目录下的 LICENSE 文件。
 **/
@Slf4j
public class GlobalIProducerMediaRouter implements IProducerMediaSubscriber {

    /**
     * IMediaNodeMediaProducerListener 的实例，用于监听媒体节点中的生产者相关事件。
     * 该变量允许外部对象注册回调，以便在生产者被添加到媒体路由器时执行特定操作。
     * <p>
     * 主要用于：
     * 1. 处理生产者添加事件。
     * 2. 提供媒体节点与生产者之间的交互支持。
     * <p>
     * 关联方法：
     * - {@link #setMediaNodeMediaProducerListener(IMediaNodeMediaProducerListener)}：设置监听器。
     * - {@link #addProducer(IProducer)}：触发监听器中的 onAddMediaProducer 方法。
     */
    private IMediaNodeMediaProducerListener mediaNodeMediaProducerListener;

    /**
     * 生产者与消费者之间的映射关系表。
     * 该变量使用线程安全的 {@code ConcurrentHashMap} 实现，用于维护生产者与其关联消费者列表之间的关系。
     * <p>
     * 每个生产者（{@link IProducer}）作为映射表的键，与之关联的消费者列表
     * （{@link IConsumer} 的实例列表）作为值。
     * <p>
     * 功能：
     * 1. 支持动态添加和移除生产者及消费者。
     * 2. 用于实现生产者和消费者之间的事件通知机制，例如 RTP 包和 RTCP 包的传递。
     * <p>
     * 应用场景：
     * - 媒体路由器需要跟踪每个生产者和其对应的消费者集合。
     * - 允许生产者向多个消费者分发媒体数据流。
     * <p>
     * 数据结构：
     * 1. 键：生产者 {@link IProducer} 对象，表示媒体流的发送者。
     * 2. 值：与生产者相关联的 {@code List<IConsumer>}，用于存储接收者（消费者）的列表。
     */
    private final Map<IProducer, List<IConsumer>> producerConsumerMap = new ConcurrentHashMap<>();


    private final IMediaNode mediaNode;

    public GlobalIProducerMediaRouter(IMediaNode mediaNode) {
        this.mediaNode = mediaNode;
    }


    /**
     * 将指定的生产者添加到生产者-消费者映射表中。
     * 如果生产者尚未在映射表中存在，则初始化与其关联的消费者列表。
     *
     * @param producer 要添加的生产者对象
     */
    public void addProducer(IProducer producer) {
        producer.setMediaSubscriber(this);
        producerConsumerMap.putIfAbsent(producer, new ArrayList<>());
        mediaNodeMediaProducerListener.onAddMediaProducer(mediaNode,this, producer);
    }


    /**
     * 从生产者-消费者映射表中移除指定的生产者。
     * 此方法会首先取消该生产者的媒体订阅者，然后将其从映射表中清除。
     *
     * @param producer 要移除的生产者对象。
     */
    public void removeProducer(IProducer producer) {
        producer.removeMediaSubscriber();
        producerConsumerMap.remove(producer);
    }

    /**
     * 将消费者添加到指定生产者的生产者-消费者映射表中。
     * 如果生产者已经准备好源时间，将触发消费者的源时间准备就绪回调。
     *
     * @param producer 要添加消费者的生产者对象
     * @param consumer 要添加的消费者对象
     */
    public void addConsumer(IProducer producer, IConsumer consumer) {
        producerConsumerMap.get(producer).add(consumer);
        log.info("绑定关键帧请求事件,生产者:{},消费者:{}", producer.getPrimarySsrc(), consumer.getPrimarySsrc());
        consumer.setMediaControl(producer.getMediaControl());
        if (producer.isSourceTimeReady()) {
            consumer.onSourceTimeReady();
        }
    }

    /**
     * 从生产者-消费者映射表中移除指定的消费者对象。
     * 如果指定的生产者存在于映射表中，则从其关联的消费者列表中移除所提供的消费者对象。
     *
     * @param producer 要移除消费者的生产者对象
     * @param consumer 要从生产者关联列表中移除的消费者对象
     */
    public void removeConsumer(IProducer producer, IConsumer consumer) {
        log.info("移除一个消费者，消费的是:{}", producer.getPrimarySsrc());
        producerConsumerMap.get(producer).remove(consumer);
        consumer.removeMediaControl();
    }


    @Override
    public void onSourceTimeReady(IProducer producer) {
        producerConsumerMap.get(producer).forEach(IConsumer::onSourceTimeReady);
    }

    @Override
    public void onRtpPacket(IProducer producer, TimerRtpPacket timerRtpPacket) {
        producerConsumerMap.get(producer).forEach(consumer -> consumer.onRtpPacket(timerRtpPacket));
    }


    /**
     * 设置当前的媒体节点生产者监听器，并为生产者-消费者映射表中的每个生产者触发监听器的添加生产者事件。
     *
     * @param mediaNodeMediaProducerListener 要设置的媒体节点生产者监听器。
     */
    public void setMediaNodeMediaProducerListener(IMediaNodeMediaProducerListener mediaNodeMediaProducerListener) {
        this.mediaNodeMediaProducerListener = mediaNodeMediaProducerListener;
        producerConsumerMap.forEach((key, _) -> mediaNodeMediaProducerListener.onAddMediaProducer(mediaNode,this, key));
    }

    /**
     * 获取所有生产者的集合。
     * 该方法从生产者-消费者映射表中提取出所有的生产者对象，返回它们的集合。
     *
     * @return 包含所有生产者对象的集合。集合中的每个元素均为实现 IProducer 接口的实例。
     */
    public Set<IProducer> getProducers() {
        return producerConsumerMap.keySet();
    }

    public void close() {
        producerConsumerMap.clear();
    }

    public List<IConsumer> getConsumer() {
        return producerConsumerMap.values().stream().flatMap(Collection::stream).toList();
    }

    /**
     * IMediaNodeMediaProducerListener 接口提供了一个用于监听媒体节点新增生产者事件的回调方法。
     * 实现此接口的类可以通过此方法对媒体生产者的加入操作作出响应，例如创建消费者并更新路由信息。
     */
    public interface IMediaNodeMediaProducerListener {

        /**
         * 当媒体节点新增生产者时触发的回调方法。
         * 实现此方法的类可以在生产者加入时执行特定逻辑，比如为生产者创建消费者、
         * 更新路由信息或其他业务操作。
         *
         * @param globalMediaRouter 全局媒体路由器对象，负责管理生产者与消费者的连接和交互。
         *                          提供对生产者与消费者映射表的操作能力。
         * @param producer          新增的媒体生产者对象，表示加入媒体节点的生产者实例。
         *                          该对象可能包含媒体流的元数据、控制消息的发送逻辑等功能。
         */
        void onAddMediaProducer(IMediaNode mediaNode, GlobalIProducerMediaRouter globalMediaRouter, IProducer producer);

    }


}
