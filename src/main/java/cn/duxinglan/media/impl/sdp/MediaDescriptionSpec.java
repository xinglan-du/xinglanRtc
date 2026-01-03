package cn.duxinglan.media.impl.sdp;

import cn.duxinglan.media.signaling.sdp.ssrc.SSRC;
import cn.duxinglan.media.signaling.sdp.type.MediaInfoType;
import lombok.Data;

import java.util.List;
import java.util.Optional;

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
public class MediaDescriptionSpec implements IMediaDescriptionSpec {

    private final String mid;

    private boolean sendOnly;

    private boolean readOnly;

    /**
     * 表示发送端的同步源标识描述，包含主要媒体流和重传媒体流的标识信息。
     * 表示从本服务发送到对方的数据信息
     */
    private SSRCDescribe sender;

    /**
     * 表示接收端的同步源标识描述，包含主要媒体流和重传媒体流的标识信息。
     * 表示从对方发送到本服务的数据信息。
     */
    private SSRCDescribe receive;

    public MediaDescriptionSpec(String mid, boolean sendOnly, boolean readOnly) {
        this.mid = mid;
        this.sendOnly = sendOnly;
        this.readOnly = readOnly;
    }

    @Override
    public MediaInfoType getMediaInfoType() {
        return MediaInfoType.VIDEO;
    }

    @Override
    public String getMid() {
        return mid;
    }


    public Optional<SSRCDescribe> getSender() {
        return Optional.ofNullable(sender);
    }

    public Optional<SSRCDescribe> getReceive() {
        return Optional.ofNullable(receive);
    }


    @Override
    public boolean isSendOnly() {
        return sendOnly;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    public void addSender(SSRCDescribe ssrcDescribe) {
        this.sender = ssrcDescribe;
    }

    public void addReceiver(SSRCDescribe ssrcDescribe) {
        this.receive = ssrcDescribe;
    }

    public void addSender(List<SSRC> ssrcList) {
        if (ssrcList == null || ssrcList.isEmpty()) return;
        if (sender == null) {
            sender = new SSRCDescribe();
        }
        sender.setSsrc(ssrcList.getFirst());
    }

    public void addReceive(List<SSRC> ssrcList, String streamId) {
        if (ssrcList == null || ssrcList.isEmpty()) return;
        if (receive == null) {
            receive = new SSRCDescribe();
        }
        //TODO 这里先假设只有一个ssrc
        receive.setSsrc(ssrcList.getFirst());
        receive.setStreamId(streamId);
    }

    public void closeSender() {
        sendOnly = false;
    }

    @Data
    public static class SSRCDescribe {

        private long primaryMediaStream;

        private long rtxMediaStream;

        private String cname;

        private String streamId;

        public void setSsrc(SSRC ssrc) {
            this.primaryMediaStream = ssrc.getPrimaryMediaStream();
            this.rtxMediaStream = ssrc.getRtxMediaStream();
            this.cname = ssrc.getCname();
        }


    }
}
