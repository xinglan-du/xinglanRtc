package cn.duxinglan.media.core;

import cn.duxinglan.media.impl.sdp.MediaDescriptionSpec;
import cn.duxinglan.media.protocol.rtcp.RtcpPacket;
import cn.duxinglan.media.protocol.rtp.RtpPacket;
import cn.duxinglan.media.transport.nio.webrtc.SRtcpContext;
import cn.duxinglan.media.transport.nio.webrtc.SRtpContext;

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
public interface IMediaProducer {

    /**
     * 获取清晰度优先级。
     *
     * @return 返回清晰度优先级的整数值，数值越高，优先级越高。
     */
//    int getClarityPriority();

    MediaDescriptionSpec.SSRCDescribe getSSRCDescribe();


    /**
     * 关闭当前实例并释放相关资源。
     * 调用此方法后，该实例可能无法继续使用，需确保在不再需要时调用。
     */
    void close();


    void addRtpPacket(RtpPacket rtpPacket);
    void addRtcpPacket(RtcpPacket rtcpPacket);

    SRtpContext getSRtpContext(long ssrc);

    SRtcpContext getSRtcpContext(long ssrc);

}
