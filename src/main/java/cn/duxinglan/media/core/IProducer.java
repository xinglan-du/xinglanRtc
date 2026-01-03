package cn.duxinglan.media.core;

import cn.duxinglan.media.protocol.rtcp.RtcpPacket;
import cn.duxinglan.media.protocol.rtp.RtpPacket;

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
public interface IProducer {

    /**
     * 获取主媒体流的同步源标识符 (SSRC)。
     * SSRC 是 RTP 协议中用于区分不同媒体流的唯一标识符。
     *
     * @return 主媒体流的同步源标识符，表示为一个长整型值。
     */
    long getPrimarySsrc();

    /**
     * 获取冗余传输流的同步源标识符 (SSRC)。
     * RTX (Retransmission) 流用于在丢包情况下重传主流中的数据，
     * 其 SSRC 用于标识该冗余流。
     *
     * @return 冗余传输流的同步源标识符，表示为一个长整型值。
     */
    long getRtxSsrc();

    /**
     * 获取CNAME (Canonical Name)，用于标识在会话中唯一的媒体发送者。
     *
     * @return 表示CNAME的字符串值，用于唯一标识媒体会话中的发送方。
     */
    String getCname();

    /**
     * 获取与媒体流相关联的唯一标识符。
     * 该标识符用于区分不同的媒体流。
     *
     * @return 表示媒体流唯一标识符的字符串。
     */
    String getStreamId();


    /**
     * 处理接收到的 RTP 数据包。
     * 该方法用于处理实时媒体流中传输的 RTP 数据包，例如音频或视频数据。
     *
     * @param packet 表示 RTP 数据包的对象，包含媒体流的载荷、时间戳、序列号等信息。
     */
    void onRtpPacket(RtpPacket packet);

    /**
     * 处理接收到的 RTCP 数据包。
     * 该方法用于处理实时控制协议（RTCP）所传输的控制消息，例如统计报告、同步信息等。
     *
     * @param packet 表示 RTCP 数据包的对象。包含版本号、填充位、载荷类型、长度以及
     *               与媒体流相关的其他控制信息（例如同步源标识符和载荷数据）。
     */
    void onRtcpPacket(RtcpPacket packet);


    /**
     * 设置媒体订阅者。
     * 媒体订阅者用于接收 RTP 数据包，并对媒体流进行处理。
     *
     * @param subscriber 表示媒体订阅者的对象，用于处理接收的媒体流数据。
     *                   此对象应实现 {@link IProducerMediaSubscriber} 接口。
     */
    void setMediaSubscriber(IProducerMediaSubscriber subscriber);

    /**
     * 移除当前的媒体订阅者。
     * 该方法用于取消之前设置的媒体订阅者，使其不再接收媒体流数据。
     * 调用此方法后，媒体流的处理可能需要重新分配或停止传递给先前的订阅者。
     */
    void removeMediaSubscriber();


    /**
     * 关闭当前对象并释放其关联的资源。
     * <p>
     * 调用此方法将终止对象的正常操作，例如停止处理媒体数据或发送控制消息，
     * 并将任何已分配的资源释放以避免内存泄漏或其他问题。
     * <p>
     * 注意：调用此方法后，对象可能进入未定义状态，不应再进行其他操作。
     */
    void close();

    /**
     * 判断源时间是否已准备就绪。
     * 此方法通常用于确认与媒体流或同步相关的时间信息是否已初始化和可用。
     *
     * @return 如果源时间已准备就绪，则返回 true；否则返回 false。
     */
    boolean isSourceTimeReady();

    IMediaControl getMediaControl();
}
