package cn.duxinglan.media.transport.nio.webrtc.handler.ice;

import cn.duxinglan.media.core.IMediaNode;
import cn.duxinglan.media.impl.sdp.IceInfo;
import lombok.Getter;
import lombok.Setter;

/**
 * 本类用于管理和存储本地与远程的 ICE 信息以及相关的 WebRTC 节点信息。
 * ICE（Interactive Connectivity Establishment）是通过 NAT 和防火墙建立点对点连接的技术。
 * <p>
 * 提供以下功能：
 * 1. 默认初始化本地 ICE 信息。
 * 2. 提供对远程 ICE 信息的设置与获取接口。
 * 3. 提供对 WebRTC 节点的设置与获取接口。
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

public class LocalIceInfo {

    @Getter
    private final IceInfo localIceInfo;

    @Setter
    @Getter
    private IceInfo remoteIceInfo;

    @Setter
    @Getter
    private IMediaNode mediaNode;

    public LocalIceInfo() {
        this.localIceInfo = IceInfo.defaultIceInfo();
    }


}
