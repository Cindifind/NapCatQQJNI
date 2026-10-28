#!/usr/bin/env bash
# NapCat + Java 插件桥接器 一键打包脚本（bash 版）
set -e

echo "========================================================"
echo "  NapCat + Java 插件桥接器 一键打包脚本"
echo "========================================================"
echo

# 切换到脚本所在目录（仓库根）
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

# ---------- 环境检查 ----------
if ! command -v node >/dev/null 2>&1; then
    echo "[错误] 未检测到 Node.js，请先安装 Node.js 18+"
    exit 1
fi

if ! command -v pnpm >/dev/null 2>&1; then
    echo "[提示] 未检测到 pnpm，使用 npm i -g pnpm 安装"
    npm i -g pnpm
fi

NO_MAVEN=0
if ! command -v mvn >/dev/null 2>&1; then
    echo "[警告] 未检测到 Maven (mvn)，Java SDK 将不打包（只打包 Node 插件）"
    echo "       如需要 Java 插件，请安装 JDK 11+ 和 Maven 后重新运行"
    NO_MAVEN=1
fi

# ---------- 安装依赖 ----------
echo
echo "[1/6] 安装 Node 依赖..."
if [ ! -d node_modules ]; then
    pnpm install
else
    echo "  跳过（已存在 node_modules）"
fi

# ---------- 构建 Java SDK ----------
echo
if [ "$NO_MAVEN" = "0" ]; then
    echo "[2/6] 构建 Java SDK (NapCatSDK)..."
    echo "        Profile: bridge（构建运行时 fat-jar）+ dev（跳过 GPG 签名）"
    pushd NapCatSDK >/dev/null
    mvn -q -Pdev -Pbridge clean package -DskipTests
    popd >/dev/null
    if [ -f "NapCatSDK/target/onebot-bridge.jar" ]; then
        cp -f NapCatSDK/target/onebot-bridge.jar packages/napcat-JNI/onebot-bridge.jar
        echo "  JAR 已生成并拷贝到 packages/napcat-JNI/"
    else
        echo "[警告] JAR 未生成，请检查 Maven 构建日志"
    fi
else
    echo "[2/6] 跳过 Java SDK 构建（未检测到 Maven）"
fi

# ---------- 构建插件 ----------
echo
echo "[3/6] 构建 WebUI 前端..."
pnpm run build:webui || echo "[警告] build:webui 失败，继续打包..."

echo
echo "[4/6] 构建内置插件 & JNI 插件..."
pnpm run build:plugin-builtin
pnpm run build:plugin-jni

# ---------- 构建 Framework（主程序） ----------
# 前置检查：桥接 JAR 缺失时 vite-plugin-cp 只告警不报错，会静默产出不可用的插件包
# 优先用 Maven 输出；没有时复用 packages/napcat-JNI 内已有的 JAR
echo
if [ ! -f "NapCatSDK/target/onebot-bridge.jar" ] && [ -f "packages/napcat-JNI/onebot-bridge.jar" ]; then
    echo "[提示] 未找到 NapCatSDK/target/onebot-bridge.jar，复用 packages/napcat-JNI/onebot-bridge.jar"
    mkdir -p NapCatSDK/target
    cp -f packages/napcat-JNI/onebot-bridge.jar NapCatSDK/target/onebot-bridge.jar
fi
if [ ! -f "NapCatSDK/target/onebot-bridge.jar" ]; then
    echo "[错误] 未找到桥接 JAR，请先构建：cd NapCatSDK && mvn -Pdev -Pbridge clean package -DskipTests"
    exit 1
fi
echo "[5/6] 构建 NapCat Framework (主程序)..."
pnpm run build:framework

# ---------- 构建 Shell（Shell 启动器版本） ----------
# Shell 版本不依赖 Java SDK / JNI 插件，只需 WebUI 前端作为前置
echo
echo "[6/6] 构建 NapCat Shell (Shell 启动器版本)..."
pnpm run build:shell || echo "[警告] build:shell 失败，继续..."

echo
echo "========================================================"
echo "  打包完成！"
echo "    Framework 产物: packages/napcat-framework/dist/"
echo "    Shell     产物: packages/napcat-shell/dist/"
echo "========================================================"
echo
echo "  Framework 目录结构："
echo "    dist/"
echo "     +-- napcat.mjs            主程序入口"
echo "     +-- napiloader.dll / napimain.exe   QQNT 注入器"
echo "     +-- native/               原生依赖"
echo "     +-- static/               WebUI 前端"
echo "     +-- config/               默认配置文件"
echo "     +-- plugins/"
echo "          +-- napcat-plugin-builtin/    内置插件"
echo "          +-- napcat-plugin-jni/        Java 桥接插件（含 JAR）"
echo
echo "  Shell 目录结构："
echo "    dist/"
echo "     +-- napcat.mjs            Shell 启动器入口"
echo "     +-- native/               原生依赖"
echo "     +-- static/               WebUI 前端"
echo "     +-- config/               默认配置文件"
echo "     +-- napcat-shell-loader/  Shell 加载器"
echo "     +-- plugins/"
echo "          +-- napcat-plugin-jni/        Java 桥接插件（含 JAR）"
