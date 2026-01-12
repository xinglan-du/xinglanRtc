package cn.duxinglan.media.signaling.sdp.ssrc;

import lombok.Data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class SSRC {

    // ✅ 静态复用 Pattern
    private static final Pattern FID_PATTERN = Pattern.compile("a=ssrc-group:FID\\s+(\\d+)\\s+(\\d+)");
    private static final Pattern CNAME_PATTERN_TEMPLATE = Pattern.compile("a=ssrc:(\\d+)\\s+cname:([\\w\\-]+)");

    public static final String KEY = "ssrc";
    public static final String GROUP_KEY = "ssrc-group";
    private static final String FID_START_WITH = "FID";

    private long ssrc;

    private long primaryMediaStream;

    private long rtxMediaStream;

    private String cname;

    private String streamId;

    private String trackId;


    /**
     * 从 SDP 文本中提取并转换为 SSRC 实例
     */
    public static SSRC parseFromSdp(String sdp) {
        if (sdp == null || sdp.isEmpty()) {
            return null;
        }

        Matcher fidMatcher = FID_PATTERN.matcher(sdp);
        if (!fidMatcher.find()) {
            return null;
        }

        long primary = Long.parseLong(fidMatcher.group(1));
        long rtx = Long.parseLong(fidMatcher.group(2));

        // 提取 cname
        String cname = extractCname(sdp, primary);
        if (cname == null) {
            cname = extractCname(sdp, rtx);
        }
        SSRC ssrc = new SSRC();
        ssrc.setPrimaryMediaStream(primary);
        ssrc.setRtxMediaStream(rtx);
        ssrc.setCname(cname);

        return ssrc;
    }

    private static String extractCname(String sdp, long ssrc) {
        Pattern cnamePattern = Pattern.compile("a=ssrc:" + ssrc + "\\s+cname:([\\w\\-]+)");
        Matcher matcher = cnamePattern.matcher(sdp);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }


}
