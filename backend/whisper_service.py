#!/usr/bin/env python3
"""
本地 Whisper 语音转文字服务
使用 Flask 提供 HTTP API 接口
"""

import whisper
import numpy as np
import io
import logging
from flask import Flask, request, jsonify
from flask_cors import CORS

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

# 全局变量存储模型（启动时加载一次，避免重复加载）
model = None
MODEL_NAME = "medium"  # 可选: tiny, base, small, medium, large, large-v3

def load_model():
    """加载 Whisper 模型"""
    global model
    if model is None:
        logger.info(f"Loading Whisper model: {MODEL_NAME}")
        model = whisper.load_model(MODEL_NAME)
        logger.info(f"Model {MODEL_NAME} loaded successfully")
    return model

@app.route('/health', methods=['GET'])
def health():
    """健康检查接口"""
    return jsonify({
        'status': 'ok',
        'model': MODEL_NAME,
        'model_loaded': model is not None
    })

@app.route('/transcribe', methods=['POST'])
def transcribe():
    """
    音频转文字接口

    接收：PCM 16-bit 音频数据（16kHz, Mono）
    返回：JSON { "text": "转录文本" }
    """
    try:
        # 获取音频数据
        audio_bytes = request.data

        if not audio_bytes:
            return jsonify({'error': 'No audio data received'}), 400

        logger.info(f"Received audio data: {len(audio_bytes)} bytes")

        # 将 PCM 16-bit 数据转换为 float32 numpy array
        # PCM 16-bit 范围是 -32768 到 32767，需要归一化到 -1.0 到 1.0
        audio_array = np.frombuffer(audio_bytes, dtype=np.int16).astype(np.float32) / 32768.0

        logger.info(f"Converted to numpy array, shape: {audio_array.shape}")

        # 确保模型已加载
        model_instance = load_model()

        # 转录音频
        logger.info("Starting transcription...")
        result = model_instance.transcribe(
            audio_array,
            language='zh',  # 中文
            fp16=False,     # M系列芯片使用fp32更稳定
            # 使用自然的上下文提示，避免被识别为转录内容
            initial_prompt="以下是普通话的句子。",
            # 其他可选参数来提升准确度
            beam_size=5,           # 增加束搜索大小（默认5），可提高准确度但会变慢
            best_of=5,             # 从多个候选中选最佳（默认5）
            temperature=0.0,       # 使用贪婪解码（更确定性）
            compression_ratio_threshold=2.4,  # 压缩率阈值
            logprob_threshold=-1.0,           # 对数概率阈值
            no_speech_threshold=0.7,          # 提高到0.7，更严格地过滤背景噪音（默认0.6）
            condition_on_previous_text=True   # 基于上下文改进识别
        )

        transcribed_text = result['text'].strip()
        logger.info(f"Transcription completed: {transcribed_text[:100]}...")

        return jsonify({
            'text': transcribed_text,
            'language': result.get('language', 'zh')
        })

    except Exception as e:
        logger.error(f"Transcription error: {str(e)}", exc_info=True)
        return jsonify({
            'error': f'Transcription failed: {str(e)}'
        }), 500

@app.route('/model', methods=['GET'])
def get_model_info():
    """获取当前模型信息"""
    return jsonify({
        'model_name': MODEL_NAME,
        'loaded': model is not None
    })

if __name__ == '__main__':
    # 启动时预加载模型
    logger.info("Starting Whisper service...")
    load_model()

    # 启动 Flask 服务
    # host='0.0.0.0' 允许外部访问
    # port=5001 避免与其他服务冲突
    app.run(host='0.0.0.0', port=5001, debug=False)
