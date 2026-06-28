package com.gzasc.aishopping.chat.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文件类型安全校验工具类
 * 【核心原则】绝不信任前端传来的 Content-Type 或文件名后缀，必须通过读取文件内容的"魔数"进行二次验证
 */
public class FileTypeUtil {

    // 允许上传的文件后缀白名单（仅这7种合法）
    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt");

    // 用于前端提示的友好文案
    public static final String SUPPORTED_EXTENSIONS_DISPLAY = "*.doc; *.docx; *.xls; *.xlsx; *.ppt; *.pptx; *.txt";

    /**
     * 魔数映射表：文件头部的固定字节签名
     * 【为什么需要这个】攻击者可以把 virus.exe 改名为 safe.docx，仅校验后缀会被骗过。
     * 但文件的二进制头部签名是无法伪造的，这是判断文件真实类型的唯一可靠依据。
     *
     * 注：Office 97-2003(doc/xls/ppt) 共用 OLE2 格式签名；
     *     Office 2007+(docx/xlsx/pptx) 本质是 ZIP 压缩包，共用 ZIP 签名。
     */
    private static final Map<Set<String>, byte[]> MAGIC_MAP = Map.of(
            Set.of("doc", "xls", "ppt"), new byte[]{(byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0},
            Set.of("docx", "xlsx", "pptx"), new byte[]{0x50, 0x4B, 0x03, 0x04}
    );

    /**
     * 批量校验入口
     * 【注意】使用 allMatch 而非 anyMatch：必须所有文件都合法才放行，任何一个非法都应拒绝整个批次
     */
    public static boolean isSupported(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return false;
        return files.stream().allMatch(FileTypeUtil::validateSingleFile);
    }

    /**
     * 单文件三重校验流水线：后缀 → 魔数/TXT内容
     */
    private static boolean validateSingleFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        // 第一重：基础合法性检查（无文件名、无扩展名的直接拒绝）
        if (originalFilename == null || !originalFilename.contains(".")) return false;

        // 第二重：后缀白名单校验
        // 【关键】必须转小写！否则用户上传 .DOCX 或 .Docx 会被误拒
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!SUPPORTED_EXTENSIONS.contains(ext)) return false;

        // TXT 和 Office 文件的深层校验逻辑完全不同，分开处理
        if ("txt".equals(ext)) {
            return isValidTextFile(file);
        }
        return matchesMagic(file, ext);
    }

    /**
     * Office 文件魔数校验
     * 【原理】只读取文件前4个字节与预期签名比对，不需要读完整个文件，性能开销极小
     */
    private static boolean matchesMagic(MultipartFile file, String ext) {
        // 根据后缀找到对应的预期魔数
        byte[] expectedMagic = MAGIC_MAP.entrySet().stream()
                .filter(entry -> entry.getKey().contains(ext))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (expectedMagic == null) return false;

        try (var is = file.getInputStream()) {
            byte[] header = new byte[4];
            // 文件不足4字节，肯定是损坏/伪造文件
            if (is.read(header) < 4) return false;
            // 逐字节精确比对
            return Arrays.equals(header, expectedMagic);
        } catch (IOException e) {
            // 流读取异常视为校验失败，不向外抛异常避免中断批量流程
            return false;
        }
    }

    /**
     * TXT 纯文本内容校验
     * 【为什么TXT要单独处理】TXT 没有二进制魔数签名，无法用上面的方法验证。
     * 【防御目标】防止攻击者把恶意二进制文件改名为 .txt 上传。
     * 【校验逻辑】读取前1KB内容，如果包含非打印控制字符（排除正常的换行/回车/制表符），则判定为伪装的二进制文件
     */
    private static boolean isValidTextFile(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            char[] buf = new char[1024];
            int len = reader.read(buf);
            for (int i = 0; i < len; i++) {
                char c = buf[i];
                // ASCII 0x00-0x1F 是控制字符区间，\t(0x09) \n(0x0A) \r(0x0D) 是合法空白符，其余一律非法
                if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') return false;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}