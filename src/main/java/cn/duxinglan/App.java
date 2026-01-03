package cn.duxinglan;

import cn.duxinglan.media.config.WebrtcConfig;
import cn.duxinglan.media.core.MediaServer;
import cn.duxinglan.media.module.CacheModel;
import cn.duxinglan.media.signaling.sdp.Candidate;
import cn.duxinglan.media.signaling.sdp.type.CandidateAddressType;
import cn.duxinglan.media.signaling.sdp.type.CandidateTransportType;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

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
public class App {

    private static final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static void main(String[] args) throws InterruptedException {

        WebrtcConfig webrtcConfig = new WebrtcConfig();

        InetAddress ipv4 = null;
        try {
            ipv4 = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            log.error(e.getMessage(), e);
        }
        CacheModel.setLocalAddress(ipv4);


        Candidate candidate = new Candidate();
        candidate.setFoundation(1);
        candidate.setComponentId(1);
        candidate.setCandidateTransportType(CandidateTransportType.UDP);
        candidate.setConnectionAddress("10.240.1.51");
        candidate.setPort(webrtcConfig.getWebrtcRtpPort());
        candidate.setCandidateAddressType(CandidateAddressType.HOST);
        CacheModel.setLocalCandidate(candidate);

        MediaServer mediaServer = MediaServer.getInstance(webrtcConfig);
        mediaServer.startWebrtc();

        allChannels.addAll(mediaServer.getAllChannels());
        allChannels.newCloseFuture().sync();
    }
}
