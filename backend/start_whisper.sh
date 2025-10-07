#!/bin/bash
# 启动本地 Whisper 服务

cd "$(dirname "$0")"

echo "Starting local Whisper service..."
source whisper_env/bin/activate
python3 whisper_service.py
