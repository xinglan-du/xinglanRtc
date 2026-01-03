package cn.duxinglan.media.core;

import cn.duxinglan.media.protocol.rtp.TimerRtpPacket;

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
public interface IProducerMediaSubscriber {


    /**
     * 处理包含时间信息的 RTP 数据包。
     * 该方法用于处理绑定了源时间戳的 RTP 数据包，通常用于时间同步、
     * 数据流分析或其他与时间相关的处理逻辑。
     *
     * @param producer       表示 RTP 数据包的生产者，提供有关媒体流的信息，
     *                       如同步源标识符 (SSRC)、流标识等。
     * @param timerRtpPacket 表示包含时间戳信息的 RTP 数据包，包含源时间 (sourceTimeNs)
     *                       和 RTP 数据包 (RtpPacket) 的结合体。
     */
    void onRtpPacket(IProducer producer, TimerRtpPacket timerRtpPacket);

    /**
     * 当源时间信息已准备就绪时回调该方法。
     * 此方法通常由媒体订阅者或处理器调用，用于通知相关逻辑，
     * 源时间已完成初始化并可用于时间同步或数据处理。
     *
     * @param producer 表示媒体流的生产者，提供与媒体流相关的上下文信息，
     *                 如同步源标识符 (SSRC)、流标识等。
     */
    void onSourceTimeReady(IProducer producer);
}
