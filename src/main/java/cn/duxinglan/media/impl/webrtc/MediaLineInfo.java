package cn.duxinglan.media.impl.webrtc;

import cn.duxinglan.sdp.entity.rtp.RtpPayload;
import cn.duxinglan.sdp.entity.ssrc.SSRC;
import cn.duxinglan.sdp.entity.ssrc.SsrcGroup;
import cn.duxinglan.sdp.entity.type.MediaInfoType;
import lombok.Data;

import java.util.*;

/**
 * 用来保存一组ssrc，每一个媒体行都有自己独立的ssrc组
 * <p>
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
public class MediaLineInfo {

    private final MediaInfoType mediaInfoType;

    private final String mid;

    /**
     * 表示当前媒体配置是否为仅发送模式。
     * 当该值为 true 时，媒体流仅限于发送而不支持接收。
     * 适用于无法或者不需要接收媒体流的场景，例如单向推流。
     */
    private boolean sendOnly;

    /**
     * 表示当前媒体配置是否为只读模式。
     * 当该值为 true 时，禁止对该媒体配置进行修改，确保其配置处于不可变状态。
     * 该属性常用于需要保护配置不被外部更改的场景，以确保系统的一致性。
     */
    private boolean readOnly;

    private Info sendInfo;

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


    @Data
    public static class Info {
        private Map<Long, SSRC> ssrcMap = new HashMap<>();

        private List<SsrcGroup> ssrcGroups = new ArrayList<>();

        private Map<Integer, RtpPayload> rtpPayloads;
    }
}
