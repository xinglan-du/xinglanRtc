/*
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
 */
package cn.duxinglan.media.impl.webrtc;

import cn.duxinglan.sdp.entity.rtp.RtpPayload;
import cn.duxinglan.sdp.entity.ssrc.SSRC;
import cn.duxinglan.sdp.entity.ssrc.SsrcGroup;
import cn.duxinglan.sdp.entity.type.MediaInfoType;
import lombok.Data;

import java.util.*;

/**
 * 表示媒体线路信息的类，封装了媒体类型、标识符及媒体配置信息。
 * 该类用于描述和管理媒体流的相关属性，包括媒体的发送和接收状态。
 */
@Data
public class MediaLineInfo {

    /**
     * 表示媒体信息的类型。
     * 用于区分媒体的具体种类，例如音频或视频。
     * 该变量为 final 类型，一旦初始化后不可更改，确保其在整个生命周期中保持一致性。
     */
    private final MediaInfoType mediaInfoType;

    /**
     * 表示媒体标识符的变量，用于唯一标识媒体线路。
     * 该标识符通常为字符串类型，确保在媒体线路的上下文中具有唯一性。
     * 在多媒体通信系统中，mid 变量用于区分不同的媒体流，支持媒体协商和管理。
     * 该变量为 final 类型，一旦初始化后不可更改。
     */
    private final String mid;

    /**
     * 表示当前媒体配置是否为仅发送模式。
     * 当该值为 true 时，媒体流仅限于发送而不支持接收。
     * 适用于无法或者不需要接收媒体流的场景，例如单向推流。
     */
    private boolean sendOnly;

    /**
     * 表示当前媒体配置是否为仅接收模式的变量。
     * 当该值为 true 时，媒体流仅限于接收，不支持发送媒体数据。
     * 标识当前媒体行是否支持接受媒体
     */
    private boolean readOnly;

    /**
     * 表示发送信息的变量。
     * <p>
     * 该变量用于存储与媒体发送相关的信息，包括 SSRC 和 RTP 负载等。
     * sendInfo 是一个复杂数据结构，提供了对媒体发送所需配置信息的封装。
     * 在多媒体通信的上下文中，该变量负责组织和管理发送端的关键元数据。
     */
    private Info sendInfo;

    /**
     * 表示与媒体接收相关的信息的变量。
     * <p>
     * 该变量用于存储媒体接收端的配置信息和元数据。
     * readInfo 是一个复杂的数据结构，封装了接收端所需的 SSRC、RTP 负载类型以及 SSRC 组等。
     * 在多媒体通信系统中，readInfo 负责管理和组织接收端的关键参数，
     * 确保媒体接收部分的正常运行和有效协作。
     */
    private Info readInfo;

    public MediaLineInfo(MediaInfoType mediaInfoType, String mid, boolean sendOnly, boolean readOnly) {
        this.mediaInfoType = mediaInfoType;
        this.mid = mid;
        this.sendOnly = sendOnly;
        this.readOnly = readOnly;
    }


    public void closeSender() {
        this.sendOnly = false;
    }


    /**
     * 表示与媒体信息相关的内部封装类。
     * 该类用于管理媒体流的 SSRC、SSRC 组和 RTP 负载等信息。
     */
    @Data
    public static class Info {
        /**
         * ssrcMap 是一个用于存储 SSRC 映射关系的容器。
         * <p>
         * 该映射的键为 SSRC 值，类型为 Long，代表唯一的流标识符。
         * 值为 SSRC 对象，包含与媒体流相关的关键信息，如 cname、streamId 和 trackId。
         * <p>
         * 此映射用于管理和快速查找媒体会话中的 SSRC 信息，
         * 方便对具体的媒体流进行操作和追踪。
         */
        private Map<Long, SSRC> ssrcMap = new HashMap<>();

        /**
         * ssrcGroups 是一个用于存储 SSRC 组的列表。
         * <p>
         * 每个 SSRC 组由一个 {@link SsrcGroup} 对象表示，
         * 其中包含了 SSRC 的分组信息和相关的组类型。
         * <p>
         * 此变量用于描述一些共享或协同关系的 SSRC 集合，
         * 例如协调的媒体流或基于 RTP 的操作组。
         * <p>
         * 在媒体会话中，ssrcGroups 可用于管理特定的
         * SSRC 逻辑分组需求，如硬件编码器需要对多个
         * SSRC 进行联动操作时的场景。
         */
        private List<SsrcGroup> ssrcGroups = new ArrayList<>();

        /**
         * 表示 RTP 负载类型的映射集合。
         * <p>
         * rtpPayloads 的键是整型（Integer），表示 RTP 负载类型编号（payload type）。
         * 值是 {@link RtpPayload} 类型的对象，包含该 RTP 负载类型的详细信息。
         * <p>
         * 通过此映射，可以快速查找和管理会话中的 RTP 负载类型，用于支持媒体处理、传输及协商。
         * <p>
         * 典型应用场景包括：
         * 1. 根据负载类型（payload type）查找对应的媒体编码格式及其参数。
         * 2. 在 RTP 会话中实现负载类型的动态适配和匹配。
         */
        private Map<Integer, RtpPayload> rtpPayloads;
    }
}
