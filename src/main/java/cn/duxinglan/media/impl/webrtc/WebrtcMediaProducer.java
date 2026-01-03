package cn.duxinglan.media.impl.webrtc;

import cn.duxinglan.media.core.IMediaControl;
import cn.duxinglan.media.core.IProducer;
import cn.duxinglan.media.core.IProducerMediaSubscriber;
import cn.duxinglan.media.protocol.rtcp.*;
import cn.duxinglan.media.protocol.rtp.RtpPacket;
import cn.duxinglan.media.protocol.rtp.RtpTimeState;
import cn.duxinglan.media.protocol.rtp.TimerRtpPacket;
import lombok.extern.slf4j.Slf4j;

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
public class WebrtcMediaProducer implements IProducer, IMediaControl {

    private final long primarySsrc;

    private final long rtxSsrc;

    private final String cname;

    private final String streamId;

    //TODO 临时写法 需要从sdp传入
    private final int clockRate = 90000;

    private IProducerMediaSubscriber producerMediaSubscriber;

    private final RtpTimeState rtpTimeState = new RtpTimeState();

    private final AtomicBoolean requestKeyframes = new AtomicBoolean(false);

    private int maxSeq = 0;


    private long lastRtpTs = -1;
    private long lastArrivalRtpTime = -1;
    private long jitter = 0;

    public WebrtcMediaProducer(long primarySsrc, long rtxSsrc, String cname, String streamId) {
        log.info("生产者初始化：primarySsrc:{},rtxSsrc:{},cname:{},streamId:{}", primarySsrc, rtxSsrc, cname, streamId);
        this.primarySsrc = primarySsrc;
        this.rtxSsrc = rtxSsrc;
        this.cname = cname;
        this.streamId = streamId;
    }


    @Override
    public long getPrimarySsrc() {
        return primarySsrc;
    }

    @Override
    public long getRtxSsrc() {
        return rtxSsrc;
    }

    @Override
    public String getCname() {
        return cname;
    }

    @Override
    public String getStreamId() {
        return streamId;
    }


    @Override
    public void onRtpPacket(RtpPacket packet) {
        //这里最好从接受层传入，目前先在这里进行处理
        long arrivalTimeNs = System.nanoTime();
        long arrivalRtpTime =
                arrivalTimeNs * clockRate / 1_000_000_000L;

        if (lastRtpTs >= 0) {
            long sendDiff = packet.getTimestamp() - lastRtpTs;
            long recvDiff = arrivalRtpTime - lastArrivalRtpTime;
            long d = recvDiff - sendDiff;

            jitter += (long) ((Math.abs(d) - jitter) / 16.0);
        }

        lastRtpTs = packet.getTimestamp();
        lastArrivalRtpTime = arrivalRtpTime;

        TimerRtpPacket timerRtpPacket;
        if (producerMediaSubscriber != null) {
            if (rtpTimeState.isHasSr()) {
                long sourceTimeNs =
                        rtpTimeState.getBaseNtpNs()
                        + (packet.getTimestamp() - rtpTimeState.getBaseRtpTs())
                          * 1_000_000_000L / clockRate;
                timerRtpPacket = new TimerRtpPacket(sourceTimeNs, packet);
            } else {
                timerRtpPacket = new TimerRtpPacket(null, packet);
            }
            //TODO 这里需要处理回绕 暂时不做处理
            maxSeq = packet.getSequenceNumber();
            producerMediaSubscriber.onRtpPacket(this, timerRtpPacket);
        }
    }

    @Override
    public void onRtcpPacket(RtcpPacket packet) {
        if (packet instanceof SenderReportRtcpPacket senderReportRtcpPacket) {
            this.rtpTimeState.updateFromSr(senderReportRtcpPacket.getNtpSec(), senderReportRtcpPacket.getNtpFrac(), senderReportRtcpPacket.getRtpTimestamp());
            this.producerMediaSubscriber.onSourceTimeReady(this);
//            log.info("当前的数据：{}", this.rtpTimeState);
        }
    }


    @Override
    public void setMediaSubscriber(IProducerMediaSubscriber subscriber) {
        this.producerMediaSubscriber = subscriber;
    }

    @Override
    public void removeMediaSubscriber() {
        this.producerMediaSubscriber = null;
    }


    @Override
    public void close() {

    }

    @Override
    public boolean isSourceTimeReady() {
        return this.rtpTimeState.isHasSr();
    }

    @Override
    public void onPli() {
        log.info("有人请求当前生产者的关键帧：{},当前的引用{}", primarySsrc, this);
        if (requestKeyframes.compareAndSet(false, true)) {
            requestKeyframes.set(true);
        }
    }

    @Override
    public IMediaControl getMediaControl() {
        return this;
    }

    public ReceiverReportRtcpPacket consumeReceiverReport(long nowNs) {
        ReceiverReportRtcpPacket receiverReportRtcpPacket = new ReceiverReportRtcpPacket();
        receiverReportRtcpPacket.setVersion((byte) 2);
        receiverReportRtcpPacket.setPadding((byte) 0);
        receiverReportRtcpPacket.setRc(1);
        receiverReportRtcpPacket.setLength(7);
        receiverReportRtcpPacket.setPayloadType(RtcpPayloadType.RECEIVER_REPORT.value);
        receiverReportRtcpPacket.setSsrc(1);
        ReceiverReportBlock receiverReportBlock = new ReceiverReportBlock();
        receiverReportBlock.setSourceSsrc(primarySsrc);
        receiverReportBlock.setFractionLost(0);
        receiverReportBlock.setLost(0);
        receiverReportBlock.setExtHighestSeq(maxSeq);
        receiverReportBlock.setCycles(0);
        receiverReportBlock.setJitter(jitter);
        receiverReportBlock.setLsr(0);
        receiverReportBlock.setDlsr(0);
        receiverReportBlock.setDelaySeconds(0);
        receiverReportRtcpPacket.addReceiverReportBlock(receiverReportBlock);

        return receiverReportRtcpPacket;
    }

    public PsFbRtcpPacket consumePli() {
        if (requestKeyframes.compareAndSet(true, false)) {
            PsFbRtcpPacket psFbRtcpPacket = new PsFbRtcpPacket();
            psFbRtcpPacket.setVersion((byte) 2);
            psFbRtcpPacket.setPadding((byte) 0);
            psFbRtcpPacket.setFmt(1);
            psFbRtcpPacket.setPayloadType(RtcpPayloadType.PSFB.value);
            psFbRtcpPacket.setLength(2);
            psFbRtcpPacket.setSenderSsrc(1);
            psFbRtcpPacket.setMediaSsrc(primarySsrc);
            return psFbRtcpPacket;
        }
        return null;

    }
}
