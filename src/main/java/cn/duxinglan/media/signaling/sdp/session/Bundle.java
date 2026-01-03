package cn.duxinglan.media.signaling.sdp.session;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
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
@Data
@Slf4j
public class Bundle {



    public static final String KEY = "group";

    private static final String BUNDLE_KEY = "BUNDLE";
    /**
     * 每个媒体流定义的唯一标识符
     */
    private List<String> mid = new ArrayList<>();

    public static Bundle parseLine(String line) {
        String line1 = line.substring(line.indexOf(":") + 1);
        String[] split = line1.split(" ");

        if (!split[0].equals(BUNDLE_KEY)) {
            log.error("无法解析:{}", line);
            return null;
        }

        Bundle bundle = new Bundle();
        bundle.setMid(Arrays.stream(split).skip(1).toList());
        return bundle;
    }

    /**
     * 添加流唯一标识符
     *
     * @param mid 流的唯一标识符
     */
    public void addMid(String mid) {
        this.mid.add(mid);
    }


    public static Bundle defaultBundle() {
        return new Bundle();
    }
}
