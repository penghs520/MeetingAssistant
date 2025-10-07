#!/usr/bin/env python3
"""
Paraformer 流式语音转文字服务
使用 FunASR 提供 HTTP API 接口，支持流式实时转录
"""

import numpy as np
import logging
from flask import Flask, request, jsonify, Response
from flask_cors import CORS
from funasr import AutoModel
import json
import time

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

# 全局变量存储模型
model = None
MODEL_NAME = "paraformer-zh"  # 阿里达摩院中文语音识别模型

# 流式识别缓冲区
class StreamBuffer:
    def __init__(self):
        self.audio_chunks = []
        self.last_result = ""
        self.chunk_duration_ms = 300  # 每个音频块的时长（毫秒）

    def add_chunk(self, audio_data):
        """添加音频块"""
        self.audio_chunks.append(audio_data)

    def get_audio(self):
        """获取累积的音频数据"""
        if not self.audio_chunks:
            return None
        # 合并所有音频块
        return np.concatenate(self.audio_chunks)

    def clear(self):
        """清空缓冲区"""
        self.audio_chunks = []
        self.last_result = ""

def load_model():
    """加载 Paraformer 模型"""
    global model
    if model is None:
        logger.info(f"Loading Paraformer model: {MODEL_NAME}")
        try:
            # 加载带 VAD 的 Paraformer 模型
            model = AutoModel(
                model=MODEL_NAME,
                vad_model="fsmn-vad",  # 语音活动检测
                punc_model="ct-punc",  # 标点预测
                device="cpu"           # M4 Pro 可以使用 CPU
            )
            logger.info(f"Model {MODEL_NAME} loaded successfully")
        except Exception as e:
            logger.error(f"Failed to load model: {e}")
            # 降级：只加载基础 Paraformer
            model = AutoModel(model=MODEL_NAME, device="cpu")
            logger.info("Loaded basic Paraformer model without VAD")
    return model

@app.route('/health', methods=['GET'])
def health():
    """健康检查接口"""
    return jsonify({
        'status': 'ok',
        'model': MODEL_NAME,
        'model_loaded': model is not None,
        'streaming': True
    })

@app.route('/transcribe', methods=['POST'])
def transcribe():
    """
    音频转文字接口（批处理模式，兼容 Whisper）

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
        audio_array = np.frombuffer(audio_bytes, dtype=np.int16).astype(np.float32) / 32768.0

        logger.info(f"Converted to numpy array, shape: {audio_array.shape}")

        # 确保模型已加载
        model_instance = load_model()

        # 转录音频
        logger.info("Starting transcription...")
        start_time = time.time()

        result = model_instance.generate(
            input=audio_array,
            batch_size_s=300,  # 批处理大小（秒）
            hotword=""
        )

        elapsed = time.time() - start_time

        # 提取文本
        if isinstance(result, list) and len(result) > 0:
            text = result[0].get('text', '')
        elif isinstance(result, dict):
            text = result.get('text', '')
        else:
            text = str(result)

        logger.info(f"Transcription completed in {elapsed:.2f}s: {text[:100]}...")

        return jsonify({
            'text': text.strip(),
            'language': 'zh',
            'duration': elapsed
        })

    except Exception as e:
        logger.error(f"Transcription error: {str(e)}", exc_info=True)
        return jsonify({
            'error': f'Transcription failed: {str(e)}'
        }), 500

@app.route('/transcribe-stream', methods=['POST'])
def transcribe_stream():
    """
    流式音频转文字接口

    接收：PCM 16-bit 音频数据（16kHz, Mono）
    返回：Server-Sent Events 流式响应
    """
    def generate():
        try:
            # 获取音频数据
            audio_bytes = request.data

            if not audio_bytes:
                yield f"data: {json.dumps({'error': 'No audio data received'})}\n\n"
                return

            logger.info(f"Received streaming audio data: {len(audio_bytes)} bytes")

            # 将 PCM 16-bit 数据转换为 float32 numpy array
            audio_array = np.frombuffer(audio_bytes, dtype=np.int16).astype(np.float32) / 32768.0

            # 确保模型已加载
            model_instance = load_model()

            # 流式转录（模拟，Paraformer 本身是批处理）
            # 将音频分成小块处理
            chunk_size = 16000 * 1  # 1秒的音频
            num_chunks = max(1, len(audio_array) // chunk_size)

            accumulated_audio = np.array([], dtype=np.float32)

            for i in range(num_chunks):
                start_idx = i * chunk_size
                end_idx = min((i + 1) * chunk_size, len(audio_array))
                chunk = audio_array[start_idx:end_idx]

                # 累积音频
                accumulated_audio = np.concatenate([accumulated_audio, chunk])

                # 每次都用累积的音频进行识别
                if len(accumulated_audio) >= chunk_size:
                    result = model_instance.generate(
                        input=accumulated_audio,
                        batch_size_s=300
                    )

                    # 提取文本
                    if isinstance(result, list) and len(result) > 0:
                        text = result[0].get('text', '')
                    elif isinstance(result, dict):
                        text = result.get('text', '')
                    else:
                        text = str(result)

                    # 判断是否为最后一块
                    is_final = (i == num_chunks - 1)

                    # 发送部分结果
                    response = {
                        'type': 'transcript_partial' if not is_final else 'transcript',
                        'text': text.strip(),
                        'isPartial': not is_final,
                        'chunk': i + 1,
                        'total_chunks': num_chunks
                    }

                    yield f"data: {json.dumps(response)}\n\n"

                    logger.info(f"Sent chunk {i+1}/{num_chunks}: {text[:50]}...")

        except Exception as e:
            logger.error(f"Streaming transcription error: {str(e)}", exc_info=True)
            error_response = {
                'type': 'error',
                'error': f'Transcription failed: {str(e)}'
            }
            yield f"data: {json.dumps(error_response)}\n\n"

    return Response(generate(), mimetype='text/event-stream')

@app.route('/model', methods=['GET'])
def get_model_info():
    """获取当前模型信息"""
    return jsonify({
        'model_name': MODEL_NAME,
        'loaded': model is not None,
        'streaming': True
    })

if __name__ == '__main__':
    # 启动时预加载模型
    logger.info("Starting Paraformer streaming service...")
    load_model()

    # 启动 Flask 服务
    # host='0.0.0.0' 允许外部访问
    # port=5002 避免与 Whisper 服务冲突
    app.run(host='0.0.0.0', port=5002, debug=False, threaded=True)
