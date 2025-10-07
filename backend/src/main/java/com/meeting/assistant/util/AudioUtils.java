package com.meeting.assistant.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 音频处理工具类
 */
public class AudioUtils {

    /**
     * 将 PCM 原始音频数据转换为 WAV 格式
     *
     * @param pcmData    PCM 音频数据
     * @param sampleRate 采样率（如 16000）
     * @param channels   声道数（1=单声道，2=立体声）
     * @param bitsPerSample 每个样本的位数（如 16）
     * @return WAV 格式的音频数据
     */
    public static byte[] pcmToWav(byte[] pcmData, int sampleRate, int channels, int bitsPerSample) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // WAV 文件头
            int dataSize = pcmData.length;
            int byteRate = sampleRate * channels * bitsPerSample / 8;
            int blockAlign = channels * bitsPerSample / 8;

            // RIFF chunk descriptor
            out.write("RIFF".getBytes());
            out.write(intToByteArray(dataSize + 36)); // ChunkSize
            out.write("WAVE".getBytes());

            // fmt sub-chunk
            out.write("fmt ".getBytes());
            out.write(intToByteArray(16)); // Subchunk1Size (16 for PCM)
            out.write(shortToByteArray((short) 1)); // AudioFormat (1 for PCM)
            out.write(shortToByteArray((short) channels)); // NumChannels
            out.write(intToByteArray(sampleRate)); // SampleRate
            out.write(intToByteArray(byteRate)); // ByteRate
            out.write(shortToByteArray((short) blockAlign)); // BlockAlign
            out.write(shortToByteArray((short) bitsPerSample)); // BitsPerSample

            // data sub-chunk
            out.write("data".getBytes());
            out.write(intToByteArray(dataSize)); // Subchunk2Size
            out.write(pcmData);

            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert PCM to WAV", e);
        }
    }

    /**
     * 将 int 转换为小端字节数组（4 字节）
     */
    private static byte[] intToByteArray(int value) {
        return new byte[]{
            (byte) (value & 0xFF),
            (byte) ((value >> 8) & 0xFF),
            (byte) ((value >> 16) & 0xFF),
            (byte) ((value >> 24) & 0xFF)
        };
    }

    /**
     * 将 short 转换为小端字节数组（2 字节）
     */
    private static byte[] shortToByteArray(short value) {
        return new byte[]{
            (byte) (value & 0xFF),
            (byte) ((value >> 8) & 0xFF)
        };
    }
}
