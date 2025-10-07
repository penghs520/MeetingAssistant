#!/bin/bash
# 启动 Paraformer 流式服务

cd "$(dirname "$0")"

echo "Starting Paraformer streaming service..."
source whisper_env/bin/activate
python3 paraformer_service.py
