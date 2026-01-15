package cn.duxinglan.sdp.entity;

import cn.duxinglan.media.impl.sdp.IceInfo;
import cn.duxinglan.sdp.entity.codec.VideoCodec;
import cn.duxinglan.sdp.entity.media.*;
import cn.duxinglan.sdp.entity.rtp.RtpPayload;
import cn.duxinglan.sdp.entity.ssrc.SSRC;
import cn.duxinglan.sdp.entity.ssrc.SsrcGroup;
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

    private List<Candidate> candidates = new ArrayList<>();

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

    /**
     * 表示媒体会话中 RTP 负载类型的映射关系。
     * <p>
     * 此变量用于存储所有的 RTP 负载类型及其相关属性信息，键为整型的负载类型标识符，
     * 值为对应的 {@link RtpPayload} 对象。其中：
     * 1. 键（Integer）表示负载类型编号（payload type）。
     * 2. 值（RtpPayload）包含了负载类型的详细信息，例如编码名称、时钟速率、媒体类型等。
     * <p>
     * 应用场景：
     * - 在会话协商阶段，用于匹配双方支持的负载类型。
     * - 在媒体传输阶段，用于解析或构造 RTP 数据包中的负载类型字段。
     * <p>
     * 注意：
     * - 此映射实现了 {@link LinkedHashMap}，以确保按添加顺序存储条目。
     * - 动态负载类型（通常范围为 96-127）需要在会话协商中显式声明。
     */
    private Map<Integer, RtpPayload> rtpPayloads = new LinkedHashMap<>();

    /**
     * 表示媒体流的 SSRC (同步源) 列表。
     * 每个 SSRC 实例包含主媒体流的标识、重传媒体流的标识以及 cname 信息。
     * <p>
     * 该变量通常与 RTP/RTCP 流的标识相关联，用于区分不同的媒体流。
     * <p>
     * 在 SDP 协议解析和媒体流描述中，该列表可能被用于记录和管理相关媒体流的 SSRC 信息。
     */
    private Map<Long, SSRC> ssrcMap = new LinkedHashMap<>();

    /**
     * 表示媒体描述中的SSRC组集合。
     * <p>
     * SSRC组的集合用于存储多个SSRC组，它们可以协作描述多媒体会话中相关联的SSRC流。
     * 每个SSRC组定义了一组具有特定关系（例如协同处理、分层编码等）的SSRC标识符。
     * 这种分组机制通常用于描述SRTP(Secure Real-time Transport Protocol)中通过RTP扩展实现的组合关系。
     * <p>
     * 该字段通过 `addSsrcGroup` 方法向列表中添加新的SSRC组。
     * 适用于需要处理多路复用媒体流或复杂的RTP关系的场景，例如视频会议、音视频流切片等。
     */
    private List<SsrcGroup> ssrcGroups = new ArrayList<>();


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
        ssrcMap.put(ssrc.getSsrc(), ssrc);

    }


    public void addExtMap(ExtMap extMap) {
        this.extMap.put(extMap.getKey(), extMap);
    }

    public void addRtpPayload(RtpPayload rtpPayload) {
        rtpPayloads.put(rtpPayload.getPayloadType(), rtpPayload);
    }

    public void addSsrcGroup(SsrcGroup ssrcGroup) {
        this.ssrcGroups.add(ssrcGroup);
    }

    public List<SSRC> getSsrcList() {
        return ssrcMap.values().stream().toList();
    }

    public void addCandidate(Candidate candidate) {
        candidates.add(candidate);
    }
}
