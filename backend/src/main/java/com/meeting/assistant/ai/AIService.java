package com.meeting.assistant.ai;

import com.meeting.assistant.entity.Speaker;
import java.util.List;

/**
 * AI服务接口 - 可插拔设计，便于后续切换不同的AI模型
 * 支持本地模型和云端API
 */
public interface AIService {

    /**
     * 将音频转录为文字
     * @param audioData 音频数据（WAV/PCM格式）
     * @return 转录文本
     */
    String transcribe(byte[] audioData);

    /**
     * 生成会议总结
     * @param transcript 完整转录文本
     * @param speakers 说话人列表
     * @return 结构化总结
     */
    String summarize(String transcript, List<Speaker> speakers);

    /**
     * 获取当前使用的模型名称
     * @return 模型名称
     */
    String getModelName();
}
