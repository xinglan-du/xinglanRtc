package cn.duxinglan.media.module;

import cn.duxinglan.media.signaling.sdp.Candidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;

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
public class CacheModel {

    /**
     * 一个静态的、不可变的 ObjectMapper 实例，用于在整个应用中处理对象与 JSON 字符串之间的转换。
     *
     * ObjectMapper 是 Jackson 库的核心类，提供了对象序列化为 JSON 和解析 JSON 为对象的功能。
     *
     * 此实例通过静态块完成初始化，并且使用了 @Getter 注解，允许外部通过访问器获取此对象。
     *
     * 该对象在类加载时一次性创建，避免了多次实例化带来的性能开销。
     */
    @Getter
    private static final ObjectMapper objectMapper;

    /**
     * 静态变量，表示本地网络地址。
     *
     * localAddress 是一个 InetAddress 类型的静态变量，用于存储本地网络地址信息。
     * 此变量可以通过 @Getter 和 @Setter 提供的访问器方法进行获取和设置。
     *
     * 该地址通常用于标识程序运行所在的网络接口，可能是在 RTC 通信等场景中使用。
     * 在网络通信过程中，可以用于绑定或指定本地 IP 地址。
     */
    @Getter
    @Setter
    private static InetAddress localAddress;

    /**
     * 一个静态变量，用于存储当前节点的本地 Candidate 对象。
     *
     * localCandidate 是 ICE (Interactive Connectivity Establishment) 中的一个重要概念，
     * 表示当前节点的候选连接点信息，包括网络地址、端口号、传输协议以及优先级等。
     * 该信息通常在 WebRTC 或其他实时通信场景中与对端共享，用于协商最佳的通信路径。
     *
     * 通过 @Getter 和 @Setter 注解可以实现读写操作。
     *
     * 特性:
     * 1. foundation：用于标识候选者来源的字符串。
     * 2. componentId：组件编号，通常为 1（RTP）或 2（RTCP）。
     * 3. candidateTransportType：传输协议，通常为 UDP 或 TCP。
     * 4. priority：候选者的优先级值，优先级越高越早进行连接尝试。
     * 5. connectionAddress：候选者的 IP 地址。
     * 6. port：候选者的端口号。
     * 7. candidateAddressType：候选者地址类型，可能为 host、srflx、relay 等。
     */
    @Getter
    @Setter
    private static Candidate localCandidate;

    static {
        objectMapper = new ObjectMapper();
    }


}
