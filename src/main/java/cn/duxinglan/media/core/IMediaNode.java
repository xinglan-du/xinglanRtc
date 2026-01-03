package cn.duxinglan.media.core;

import cn.duxinglan.media.impl.webrtc.GlobalIProducerMediaRouter;
import cn.duxinglan.media.impl.webrtc.NodeFlowManager;
import cn.duxinglan.media.impl.webrtc.WebRTCCertificateGenerator;
import cn.duxinglan.media.signaling.data.NodeSignalingData;
import lombok.NonNull;

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
public interface IMediaNode {


    /**
     * 获取媒体节点的唯一标识。
     *
     * @return 媒体节点的唯一标识符，通常为字符串类型。
     */
    @NonNull
    String getNodeId();

    int getNodeVersion();

    /**
     * 初始化媒体节点的状态或资源。
     * 此方法通常在媒体节点被添加到管理器后调用，确保其处于可用的初始状态。
     */
    void init();

    /**
     * 处理节点信令数据。
     *
     * @param nodeSignalingData 表示节点信令数据的对象，包含节点的唯一标识和相关数据的内容。
     */
    void handleNodeData(NodeSignalingData nodeSignalingData);

    void updateOfferInfo();

    NodeFlowManager getNodeFlowManager();

    GlobalIProducerMediaRouter getGlobalMediaRouter();

    WebRTCCertificateGenerator.DTLSKeyMaterial getDTLSKeyMaterial();

    IConsumer createConsumer(IProducer producer);

    boolean removeConsumer(IConsumer consumer);

    void close();


}
