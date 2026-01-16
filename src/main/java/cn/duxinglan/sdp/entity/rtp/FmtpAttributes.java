package cn.duxinglan.sdp.entity.rtp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Locale;
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
public class FmtpAttributes {


    /**
     * 用来存放apt相关数据
     */
    private Integer associatedPayloadType;

    private final Map<String, String> params = new LinkedHashMap<>();

    public FmtpAttributes() {
    }

    public FmtpAttributes(Integer associatedPayloadType) {
        this.associatedPayloadType = associatedPayloadType;
    }

    public void putParam(String key, String value) {
        params.put(key.toLowerCase(Locale.ROOT), value);
    }


}
