package cn.duxinglan.media.core;

import cn.duxinglan.media.config.WebrtcConfig;
import cn.duxinglan.media.signaling.webrtc.WebsocketSignalingBootstrap;
import cn.duxinglan.media.transport.nio.webrtc.server.WebrtcUdpServer;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

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
public class MediaServer {

    private static volatile MediaServer instance;

    private final WebrtcConfig webrtcConfig;

    private WebsocketSignalingBootstrap websocketSignalingBootstrap;

    private Thread thread;

    private WebrtcUdpServer webrtcUdpServer;

    public static MediaServer getInstance(WebrtcConfig webrtcConfig) {
        if (instance == null) {
            synchronized (MediaServer.class) {
                if (instance == null) {
                    instance = new MediaServer(webrtcConfig);
                }
            }
        }
        return instance;
    }


    private MediaServer(WebrtcConfig webrtcConfig) {
        this.webrtcConfig = webrtcConfig;
    }


    public void startWebrtc() {
        websocketSignalingBootstrap = new WebsocketSignalingBootstrap(webrtcConfig.getSignalingPort(), webrtcConfig.getSignalingPath());
        webrtcUdpServer = new WebrtcUdpServer();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    webrtcUdpServer.start(webrtcConfig.getWebrtcRtpPort());
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
        thread.start();

//        webRtcNettyBootstrap = new WebRtcNettyBootstrap(webrtcConfig.getWebrtcRtpPort());
    }


    public List<Channel> getAllChannels() {
        Channel webrtcChannel = websocketSignalingBootstrap.getChannel();
//        Channel webrtcMediaChannel = webRtcNettyBootstrap.getChannel();
        return List.of(webrtcChannel/*, webrtcMediaChannel*/);
    }
}
