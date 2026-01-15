package cn.duxinglan.sdp.entity;

import cn.duxinglan.sdp.entity.type.CandidateAddressType;
import cn.duxinglan.sdp.entity.type.CandidateTransportType;
import lombok.Data;

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
public class Candidate {

    public static String WEBRTC_SDP_KEY = "candidate";

    /**
     * 来源标识
     */
    private String foundation;

    /**
     * 组件编号：1=RTP，2=RTCP。启用 a=rtcp-mux 时通常只会出现 1。
     */
    private int componentId;

    /**
     * 传输协议。WebRTC 绝大多数走 UDP。若为 tcp，会额外带 tcptype（见下）。
     */
    private CandidateTransportType candidateTransportType;

    /**
     * ICE 优先级，越大越先被检测。由公式计算
     */
    private Integer priority;

    /**
     * 连接地址
     */
    private String connectionAddress;

    /**
     * 端口
     */
    private int port;

    /**
     * 候选的地址：
     * host 候选：本机局域网 IP；
     * srflx：STUN 映射得到的公网 IP；
     * relay：TURN 中继服务器 IP；
     * 现代浏览器可能用 mDNS 名称（xxxx.local）来隐藏内网 IP，由对端通过 mDNS 解析。
     */
    private CandidateAddressType candidateAddressType;

    private String relatedAddress;
    private Integer relatedPort;
    private String tcpType;        // active / passive / so
    private Integer generation;
    private Integer networkId;
    private Integer networkCost;




    public String toSdpStr() {
        //TODO 这里需要写检查参数的情况
        if (this.priority == null) {
            calculateCandidatePriority();
        }
        return String.format("a=candidate:%d %d %s %d %s %d typ %s", foundation, componentId, candidateTransportType.value, priority, connectionAddress, port, candidateAddressType.value);
    }


    /**
     * 计算 ICE Candidate 的优先级
     * <p>
     * 优先级 (整数，越大越优先)
     */
    public void calculateCandidatePriority() {
        if (priority != null) {
            return;
        }
        //TODO 65535是人为控制的优先级，可以在传递默认参数的时候传入，值时0 - 65535 数字越大 优先级越高
        int typePref = switch (candidateAddressType) {
            case HOST -> 126;
            case PRFLX -> 110;
            case SRFLX -> 100;
            case RELAY -> 50;
        };
        this.priority = (typePref << 24) + (65535 << 8) + (256 - this.componentId);
    }
}
