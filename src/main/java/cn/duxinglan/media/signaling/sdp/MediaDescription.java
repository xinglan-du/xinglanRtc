package cn.duxinglan.media.signaling.sdp;

import cn.duxinglan.media.impl.sdp.IceInfo;
import cn.duxinglan.media.signaling.sdp.codec.VideoCodec;
import cn.duxinglan.media.signaling.sdp.media.*;
import cn.duxinglan.media.signaling.sdp.rtp.RtpPayload;
import cn.duxinglan.media.signaling.sdp.ssrc.SSRC;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
@Data
public class MediaDescription {


    public static final String MID_KEY = "";

    private Info info;

    private Connection connection;

    private RtcpConnection rtcpConnection;

    private IceInfo iceInfo;

    private Fingerprint fingerprint;

    private Setup setup;

    /**
     * 媒体描述行ID
     */
    protected MId mId;

    private LinkedHashMap<String, ExtMap> extMap = new LinkedHashMap<>();

    /**
     * 媒体方向
     */
    private MediaDirection mediaDirection;

    private MSId msid;

    /**
     * 表示是否启用RTCP多路复用（RTCP MUX）。
     * 如果值为true，则表明使用单个端口来同时传输RTP和RTCP数据，从而有效节省端口资源。
     * 如果值为false，则RTP和RTCP分别通过不同的端口进行传输。
     */
    private RtcpMux rtcpMux;

    /**
     * 表示是否启用 RTCP 缩减大小（Reduced-Sized RTCP）。
     * <p>
     * RTCP 缩减大小是一种优化模式，其特点是减少 RTCP 报文的发送频率和大小，
     * 从而节约带宽资源。在实时通信中，启用 RTCP 缩减大小可以提高传输效率，特别是在网络条件较差的情况下。
     * <p>
     * 此变量用于标志是否启用了这一功能：
     * - 如果值为 true，则表明启用了 RTCP 缩减大小；
     * - 如果值为 false，则不启用。
     */
    private RtcpRsize rtcpRsize;

    /**
     * 编码器列表
     */
    protected List<VideoCodec> codecs;

    private Map<Integer, RtpPayload> rtpPayloads = new LinkedHashMap<>();


    private List<SSRC> ssrcList = new ArrayList<>();


    /**
     * 返回所有的编解码器payload信息，要严格定义信息
     */
    public String getPayloadsToString() {
        StringBuilder sb = new StringBuilder();
        for (VideoCodec codec : codecs) {
            sb.append(codec.getPayloadType()).append(" ").append(codec.getRetransmitPayloadType());
        }
        return sb.toString();
    }

    public void addCodec(VideoCodec videoCodec) {
        if (this.codecs == null) {
            this.codecs = new ArrayList<>();
        }
        this.codecs.add(videoCodec);
    }

    public void addCodecs(List<VideoCodec> videoCodecs) {
        if (this.codecs == null) {
            this.codecs = new ArrayList<>();
        }
        this.codecs.addAll(videoCodecs);
    }

    public void addSSRC(SSRC ssrc) {
        if (ssrcList == null) {
            return;
        }
        this.ssrcList.add(ssrc);
    }


    public void addExtMap(ExtMap extMap) {
        this.extMap.put(extMap.getKey(), extMap);
    }

    public void addRtpPayload(RtpPayload rtpPayload) {
        rtpPayloads.put(rtpPayload.getPayloadType(), rtpPayload);
    }
}
