package cn.duxinglan.media.impl.webrtc;

import cn.duxinglan.media.core.IConsumer;
import cn.duxinglan.media.core.IConsumerMediaSubscriber;
import cn.duxinglan.media.core.IMediaTransport;
import cn.duxinglan.media.core.IProducer;
import cn.duxinglan.media.protocol.rtcp.*;
import cn.duxinglan.media.protocol.rtp.RtpPacket;
import cn.duxinglan.media.protocol.rtp.SenderRtpPacket;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class NodeFlowManager implements IConsumerMediaSubscriber {

    private AtomicBoolean isRunning = new AtomicBoolean(true);

    /**
     * 数据消费者，发送数据给客户端
     */
    private final Map<Long, WebrtcMediaConsumer> rtpMediaConsumer = new ConcurrentHashMap<>();

    /**
     * 数据生产者，接受客户端发送过来的数据
     */
    private final Map<Long, WebrtcMediaProducer> rtpMediaProducer = new ConcurrentHashMap<>();

    /**
     * 数据通道
     */
    private IMediaTransport mediaTransport;


    /**
     * 添加数据通道，传输通道准备完毕后会调用此方法
     *
     * @param mediaTransport 数据通道
     */
    public void onMediaTransport(IMediaTransport mediaTransport) {
        this.mediaTransport = mediaTransport;
    }

    /**
     * 处理接收到的 RTP 数据包，该方法根据数据包的同步源标识符 (SSRC)，
     * 查找到对应的生产者，并调用其 onRtpPacket 方法进行数据处理。
     *
     * @param rtpPacket 表示 RTP 数据包的对象，包含媒体流的载荷、时间戳、序列号等信息。
     *                  通过该参数传递需要处理的实时媒体流数据。
     */
    public void onRtpPacket(RtpPacket rtpPacket) {
        rtpMediaProducer.get(rtpPacket.getSsrc()).onRtpPacket(rtpPacket);
    }

    /**
     * 处理接收到的 RTCP 数据包列表。
     * 根据提供的同步源标识符 (SSRC)，查找对应的生产者，并调用其 onRtcpPacket 方法处理数据包。
     *
     * @param rtcpPackets   表示 RTCP 数据包的列表，每个数据包包含控制协议相关的元数据信息，
     *                      比如统计报告、同步信息等。
     * @param remoteAddress
     */
    public void onRtcpPacket(List<RtcpPacket> rtcpPackets, InetSocketAddress remoteAddress) {
        for (RtcpPacket rtcpPacket : rtcpPackets) {
            long ssrc = 0;
            switch (rtcpPacket) {
                case SenderReportRtcpPacket senderReportRtcpPacket -> ssrc = senderReportRtcpPacket.getSsrc();
                case SdesRtcpPacket sdesRtcpPacket -> {
                    log.debug("不需要处理");
                    continue;
                }
                case PsFbRtcpPacket psFbRtcpPacket -> {
                    if (psFbRtcpPacket.getFmt() != 1) {
                        log.debug("fmt为{}，不处理请求关键帧的接口", psFbRtcpPacket.getFmt());
                        continue;
                    }
                    ssrc = psFbRtcpPacket.getMediaSsrc();
                    log.debug("接收到申请关键帧{}", psFbRtcpPacket);
                }
                case ReceiverReportRtcpPacket receiverReportRtcpPacket -> {
                    log.debug("暂时不处理接受处理");
                }
                case null -> {
                    log.warn("接收到null类型的rtcp");
                    continue;
                }
                default -> {
                    log.info("未处理类型:{},ssrc:{}", rtcpPacket.getPayloadType(), ssrc);
                    continue;
                }
            }
            if (rtpMediaProducer.containsKey(ssrc)) {
                rtpMediaProducer.get(ssrc).onRtcpPacket(rtcpPacket);
            } else if (rtpMediaConsumer.containsKey(ssrc)) {
                rtpMediaConsumer.get(ssrc).onRtcpPacket(rtcpPacket, remoteAddress);
            }

        }


    }

    public void onRtcpPacket(Long ssrc, RtcpPacket rtcpPacket) {
        if (rtpMediaProducer.containsKey(ssrc)) {
            rtpMediaProducer.get(ssrc).onRtcpPacket(rtcpPacket);
        }
    }

    public void addRtpMediaProducer(WebrtcMediaProducer mediaProducer) {
        rtpMediaProducer.put(mediaProducer.getPrimarySsrc(), mediaProducer);
        rtpMediaProducer.put(mediaProducer.getRtxSsrc(), mediaProducer);
    }

    public WebrtcMediaProducer removeRtpMediaProducer(long primarySsrc, long rtxSsrc) {
        WebrtcMediaProducer remove = rtpMediaProducer.remove(primarySsrc);
        if (remove != null) {
            remove.close();
        }

        WebrtcMediaProducer remove1 = rtpMediaProducer.remove(rtxSsrc);
        if (remove1 != null) {
            remove1.close();
        }
        return remove != null ? remove : remove1;
    }

    public void addRtpMediaConsumer(WebrtcMediaConsumer consumer) {
        consumer.setMediaSubscriber(this);
        rtpMediaConsumer.put(consumer.getPrimarySsrc(), consumer);
        rtpMediaConsumer.put(consumer.getRtxSsrc(), consumer);
    }

    public WebrtcMediaConsumer removeRtpMediaConsumer(IConsumer consumer) {
        WebrtcMediaConsumer remove = rtpMediaConsumer.remove(consumer.getPrimarySsrc());
        rtpMediaConsumer.remove(consumer.getRtxSsrc());
        return remove;
    }

    public IProducer getMediaProducer(long primaryMediaStream) {
        return rtpMediaProducer.get(primaryMediaStream);
    }

    public void sendReadyPackets(long nowNs) {
        for (IConsumer consumer : rtpMediaConsumer.values()) {
            int batch = 0;
            int maxBatch = 2; // 每轮每个 Consumer 最多发2包
            SenderRtpPacket senderRtpPacket;
            while (batch < maxBatch && (senderRtpPacket = consumer.pollReady(nowNs)) != null) {
                mediaTransport.sendRtpPacket(senderRtpPacket);
                batch++;
            }
        }
    }


    public void sendReadyRtcpPackets(long nowNs) {
        for (Map.Entry<Long, WebrtcMediaConsumer> longWebrtcMediaConsumerEntry : rtpMediaConsumer.entrySet()) {
            Long key = longWebrtcMediaConsumerEntry.getKey();
            WebrtcMediaConsumer webrtcMediaConsumer = longWebrtcMediaConsumerEntry.getValue();
            if (key != webrtcMediaConsumer.getPrimarySsrc()) {
                continue;
            }
            if (webrtcMediaConsumer.getTimeState() != WebrtcMediaConsumer.TimeState.RUNNING) {
                continue;
            }
            List<RtcpPacket> rtcpPackets = new ArrayList<>();
            SenderReportRtcpPacket senderReportRtcpPacket = webrtcMediaConsumer.buildSenderReportRtcpPacket(nowNs);
            SdesRtcpPacket sdesRtcpPacket = webrtcMediaConsumer.buildSdesRtcpPacket();
            if (senderReportRtcpPacket != null) {
                rtcpPackets.add(senderReportRtcpPacket);
            }
            if (sdesRtcpPacket != null) {
                rtcpPackets.add(sdesRtcpPacket);
            }

            if (!rtcpPackets.isEmpty()) {
                mediaTransport.sendRtcpPackets(rtcpPackets);
            }
        }

        for (Map.Entry<Long, WebrtcMediaProducer> longWebrtcMediaProducerEntry : rtpMediaProducer.entrySet()) {
            Long key = longWebrtcMediaProducerEntry.getKey();
            WebrtcMediaProducer webrtcMediaProducer = longWebrtcMediaProducerEntry.getValue();
            if (key != webrtcMediaProducer.getPrimarySsrc()) {
//                log.info("子流 跳过");
                continue;
            }


            List<RtcpPacket> rtcpPackets = new ArrayList<>();
            ReceiverReportRtcpPacket receiverReportRtcpPacket = webrtcMediaProducer.consumeReceiverReport(nowNs);
            if (receiverReportRtcpPacket != null) {
                rtcpPackets.add(receiverReportRtcpPacket);
            }

            PsFbRtcpPacket psFbRtcpPacket = webrtcMediaProducer.consumePli();
            if (psFbRtcpPacket != null) {
                rtcpPackets.add(psFbRtcpPacket);
            }
            if (!rtcpPackets.isEmpty()) {
                mediaTransport.sendRtcpPackets(rtcpPackets);
            }
        }


    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public void close() {
        isRunning.set(true);
        rtpMediaProducer.values().forEach(WebrtcMediaProducer::close);
        rtpMediaConsumer.values().forEach(WebrtcMediaConsumer::close);
    }


}
