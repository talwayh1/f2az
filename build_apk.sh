#!/bin/bash

# TikHub视频解析器 - APK打包脚本
# 使用方法: ./build_apk.sh [debug|release]

set -e

# 默认构建debug版本
BUILD_TYPE=${1:-debug}
APP_NAME="TikHubVideoParser"

echo "🚀 开始打包 $APP_NAME ($BUILD_TYPE 版本)..."

# 检查环境
echo "📋 检查构建环境..."

if ! command -v ./gradlew &> /dev/null; then
    echo "❌ 未找到 gradlew，请确保在项目根目录执行此脚本"
    exit 1
fi

# 清理之前的构建
echo "🧹 清理之前的构建..."
./gradlew clean

# 构建APK
echo "🔨 构建 $BUILD_TYPE APK..."
if [ "$BUILD_TYPE" = "release" ]; then
    ./gradlew assembleRelease
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
else
    ./gradlew assembleDebug
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

# 检查构建结果
if [ -f "$APK_PATH" ]; then
    echo "✅ APK构建成功!"
    echo "📱 APK路径: $APK_PATH"

    # 获取APK信息
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "📦 APK大小: $APK_SIZE"

    # 生成MD5校验码
    MD5_CHECKSUM=$(md5sum "$APK_PATH" | cut -d' ' -f1)
    echo "🔐 MD5: $MD5_CHECKSUM"

    # 复制到dist目录
    DIST_DIR="dist"
    mkdir -p "$DIST_DIR"

    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    APK_FILENAME="${APP_NAME}_${BUILD_TYPE}_${TIMESTAMP}.apk"
    cp "$APK_PATH" "$DIST_DIR/$APK_FILENAME"

    echo "📋 APK已复制到: $DIST_DIR/$APK_FILENAME"
    echo ""
    echo "🎉 打包完成! 现在可以安装到手机测试了:"
    echo "   adb install -r $APK_PATH"
    echo ""
    echo "📱 或直接将APK文件传输到手机安装"

else
    echo "❌ APK构建失败!"
    exit 1
fi