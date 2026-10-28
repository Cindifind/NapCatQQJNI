import React, { useState, useEffect, useRef, useCallback } from 'react';
import { createPortal } from 'react-dom';
import './Live2DPanel.css';

// ===== 类型声明 =====
interface Live2DModelAPI {
  setScale?: (scale: number) => void;
  addParameterDelta?: (param: string, value: number) => void;
  startTalkFromBytes?: (bytes: ArrayBuffer, text: string) => void;
  startTalk?: (audio: string, text: string) => void;
  onReady?: (cb: () => void) => void;
  onTalkEnd?: (cb: () => void) => void;
  resetContainer?: () => void;
}

declare global {
  interface Window {
    Live2DModel?: Live2DModelAPI;
    AudioContext: typeof AudioContext;
    webkitAudioContext: typeof AudioContext;
  }
}

interface Position {
  x: number;
  y: number;
}

// ===== 工具函数 =====
function getPos (e: MouseEvent | TouchEvent): Position {
  const t = (e as TouchEvent).touches ? (e as TouchEvent).touches[0] : (e as MouseEvent);
  return { x: t.clientX, y: t.clientY };
}

// 静态容器 ID：容器在 index.html 中静态声明，插件 window.load 扫描时必然存在
const WRAPPER_ID = 'live2d-wrapper';
// 外层定位容器 ID：回复气泡显示在它内部（wrapper 上方），避免被 wrapper 的 overflow:hidden 裁剪
const ROOT_ID = 'live2d-root';

// ===== 组件 =====
const Live2DPanel: React.FC = () => {
  // ===== 状态 =====
  const [panelVisible, setPanelVisible] = useState(false);
  const [modelScale, setModelScaleState] = useState(1.0);
  const [wrapperW, setWrapperW] = useState(300);
  const [wrapperH, setWrapperH] = useState(400);
  const [wrapperX, setWrapperX] = useState<number | null>(null);
  const [wrapperY, setWrapperY] = useState<number | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const dragRef = useRef({ startX: 0, startY: 0, initialX: 0, initialY: 0 });

  const [chatInput, setChatInput] = useState('');
  const [isChatVisible, setIsChatVisible] = useState(false);
  const [bubbleText, setBubbleText] = useState('');
  const [isTalking, setIsTalking] = useState(false);
  const [isThinking, setIsThinking] = useState(false);
  const [chatLoading, setChatLoading] = useState(false);
  const animRef = useRef({ thinkRafId: 0, talkRafId: 0, talkSafetyTimer: 0 });
  const audioRef = useRef<{ ctx: AudioContext | null; source: AudioBufferSourceNode | null }>({
    ctx: null,
    source: null,
  });

  const [isResizing, setIsResizing] = useState(false);
  const resizeRef = useRef({ startX: 0, startY: 0, startW: 0, startH: 0 });

  // ===== 缩放 =====
  const onGripDown = useCallback((e: React.MouseEvent | React.TouchEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsResizing(true);
    const p = getPos(e.nativeEvent);
    resizeRef.current.startX = p.x;
    resizeRef.current.startY = p.y;
    resizeRef.current.startW = wrapperW;
    resizeRef.current.startH = wrapperH;
  }, [wrapperW, wrapperH]);

  const onGripMove = useCallback((e: MouseEvent | TouchEvent) => {
    if (!isResizing) return;
    if ((e as MouseEvent).cancelable !== false) e.preventDefault();
    const p = getPos(e);
    setWrapperW(Math.max(150, Math.min(800, resizeRef.current.startW + p.x - resizeRef.current.startX)));
    setWrapperH(Math.max(200, Math.min(1000, resizeRef.current.startH + p.y - resizeRef.current.startY)));
  }, [isResizing]);

  const onGripUp = useCallback(() => {
    setIsResizing(false);
  }, []);

  // ===== 拖动 =====
  const onOuterDown = useCallback((e: MouseEvent | TouchEvent) => {
    const target = e.target as HTMLElement;
    if (target.closest('.resize-grip') || target.closest('.control-panel') ||
      target.closest('.chat-box') || target.closest('.chat-toggle-btn')) {
      return;
    }
    e.preventDefault();
    setIsDragging(true);
    const p = getPos(e);
    dragRef.current.startX = p.x;
    dragRef.current.startY = p.y;
    if (wrapperX === null || wrapperY === null) {
      const rect = document.getElementById(ROOT_ID)?.getBoundingClientRect();
      if (rect) {
        setWrapperX(rect.right - window.innerWidth);
        setWrapperY(rect.bottom - window.innerHeight);
      }
    }
    dragRef.current.initialX = wrapperX ?? 0;
    dragRef.current.initialY = wrapperY ?? 0;
  }, [wrapperX, wrapperY]);

  const onOuterMove = useCallback((e: MouseEvent | TouchEvent) => {
    if (!isDragging) return;
    if ((e as MouseEvent).cancelable !== false) e.preventDefault();
    const p = getPos(e);
    setWrapperX(dragRef.current.initialX + (p.x - dragRef.current.startX));
    setWrapperY(dragRef.current.initialY + (p.y - dragRef.current.startY));
  }, [isDragging]);

  const onOuterUp = useCallback(() => {
    setIsDragging(false);
  }, []);

  // ===== 面板 & 缩放 =====
  const togglePanel = useCallback(() => setPanelVisible(!panelVisible), [panelVisible]);
  const setScale = useCallback((v: number) => {
    const newScale = Math.round(Math.max(0.3, Math.min(1, v)) * 100) / 100;
    setModelScaleState(newScale);
    window.Live2DModel?.setScale?.(newScale);
  }, []);

  // ===== 动画参数叠加 =====
  const applyAnimParams = useCallback((t: number, api: Live2DModelAPI, intensity: number) => {
    api.addParameterDelta?.('ParamAngleZ', 2 * intensity * Math.sin(t * 0.8));
    api.addParameterDelta?.('ParamEyeBallY', (0.4 + 0.2 * Math.sin(t * 1.2)) * intensity);
    api.addParameterDelta?.('ParamEyeBallX', 0.2 * intensity * Math.sin(t * 0.5));
    api.addParameterDelta?.('ParamBodyAngleX', -1.2 * intensity * Math.sin(t * 0.6));
  }, []);

  // ===== 思考动画 =====
  const stopThinking = useCallback(() => {
    setIsThinking(false);
    if (animRef.current.thinkRafId) {
      cancelAnimationFrame(animRef.current.thinkRafId);
      animRef.current.thinkRafId = 0;
    }
  }, []);

  const startThinking = useCallback(() => {
    setIsThinking(true);
    if (animRef.current.thinkRafId) return;
    const loop = (now: number) => {
      if (!isThinking) {
        animRef.current.thinkRafId = 0;
        return;
      }
      const api = window.Live2DModel;
      if (api?.addParameterDelta) {
        applyAnimParams(now / 1000, api, 1);
      }
      animRef.current.thinkRafId = requestAnimationFrame(loop);
    };
    animRef.current.thinkRafId = requestAnimationFrame(loop);
  }, [isThinking, applyAnimParams]);

  // ===== 说话效果 =====
  const stopTalkEffects = useCallback(() => {
    setIsTalking(false);
    clearTimeout(animRef.current.talkSafetyTimer);
    if (animRef.current.talkRafId) {
      cancelAnimationFrame(animRef.current.talkRafId);
      animRef.current.talkRafId = 0;
    }
  }, []);

  const startTalkEffects = useCallback(() => {
    if (animRef.current.talkRafId) return;
    const loop = (now: number) => {
      if (!isTalking) {
        animRef.current.talkRafId = 0;
        return;
      }
      const t = now / 1000;
      const api = window.Live2DModel;
      if (api?.addParameterDelta) {
        applyAnimParams(t, api, 0.7);
        api.addParameterDelta?.('ParamCheek', 0.4 + 0.2 * Math.sin(t * 2));
        api.addParameterDelta?.('ParamEyeLSmile', 0.2);
        api.addParameterDelta?.('ParamEyeRSmile', 0.2);
        api.addParameterDelta?.('ParamBrowLAngle', 0.2 * Math.abs(Math.sin(t * 3)));
        api.addParameterDelta?.('ParamBrowRAngle', 0.15 * Math.abs(Math.cos(t * 3.2)));
        api.addParameterDelta?.('ParamMouthOpenY', 0.3 * Math.abs(Math.sin(t * 4)));
        api.addParameterDelta?.('ParamBodyAngleX', 1.5 * Math.sin(t * 2));
      }
      animRef.current.talkRafId = requestAnimationFrame(loop);
    };
    animRef.current.talkRafId = requestAnimationFrame(loop);
    clearTimeout(animRef.current.talkSafetyTimer);
    animRef.current.talkSafetyTimer = window.setTimeout(stopTalkEffects, 30000);
  }, [isTalking, applyAnimParams, stopTalkEffects]);

  // ===== 音频播放 =====
  const stopAudio = useCallback(() => {
    if (audioRef.current.source) {
      try { audioRef.current.source.stop(); } catch (_) {}
      audioRef.current.source = null;
    }
  }, []);

  const playAudioWithSync = useCallback((arrayBuf: ArrayBuffer, replyText: string) => {
    stopAudio();
    if (!audioRef.current.ctx) {
      audioRef.current.ctx = new (window.AudioContext || window.webkitAudioContext)();
    }
    audioRef.current.ctx.decodeAudioData(
      arrayBuf.slice(0),
      (buf) => {
        audioRef.current.source = audioRef.current.ctx!.createBufferSource();
        audioRef.current.source.buffer = buf;
        audioRef.current.source.connect(audioRef.current.ctx!.destination);
        audioRef.current.source.start(0);
        setTimeout(stopTalkEffects, buf.duration * 1000 + 200);
      },
      (err) => console.error('[Audio] decode error:', err)
    );
    window.Live2DModel?.startTalkFromBytes?.(arrayBuf, replyText);
  }, [stopAudio, stopTalkEffects]);

  // ===== 对话 =====
  const toggleChat = useCallback(() => setIsChatVisible(!isChatVisible), [isChatVisible]);

  const sendMessage = useCallback(async () => {
    const prompt = chatInput.trim();
    if (!prompt || chatLoading) return;
    setChatLoading(true);
    setChatInput('');
    setBubbleText('');
    startThinking();

    try {
      const llmRes = await fetch('https://muqingxi.com:2345/proxy/live2Dllm', {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain' },
        body: prompt,
      });
      const llmData = await llmRes.json();
      const replyText = llmData.message || '抱歉，我无法回答这个问题。';
      stopThinking();

      try {
        const voiceRes = await fetch('https://muqingxi.com:2345/proxy/live2DVoice', {
          method: 'POST',
          headers: { 'Content-Type': 'text/plain;charset=UTF-8' },
          body: replyText,
        });
        if (voiceRes.ok) {
          const arrayBuf = await (await voiceRes.blob()).arrayBuffer();
          setBubbleText(replyText);
          setIsTalking(true);
          startTalkEffects();
          playAudioWithSync(arrayBuf, replyText);
        } else {
          setBubbleText(replyText);
          setIsTalking(true);
          startTalkEffects();
          window.Live2DModel?.startTalk?.('', replyText);
          setTimeout(stopTalkEffects, 3000);
        }
      } catch {
        setBubbleText(replyText);
        setIsTalking(true);
        startTalkEffects();
        window.Live2DModel?.startTalk?.('', replyText);
        setTimeout(stopTalkEffects, 3000);
      }
    } catch (error) {
      console.error('对话接口调用失败:', error);
      stopThinking();
      setBubbleText('网络错误，请稍后再试。');
      setTimeout(() => setBubbleText(''), 2000);
    } finally {
      setChatLoading(false);
    }
  }, [chatInput, chatLoading, startThinking, stopThinking, startTalkEffects, stopTalkEffects, playAudioWithSync]);

  // ===== 静态容器样式同步 + 拖动/双击事件（容器在 index.html 中，不在 React 树内） =====
  useEffect(() => {
    const root = document.getElementById(ROOT_ID);
    if (!root) return;
    root.style.width = wrapperW + 'px';
    root.style.height = wrapperH + 'px';
    root.style.right = wrapperX !== null ? (-wrapperX) + 'px' : '20px';
    root.style.bottom = wrapperY !== null ? (-wrapperY) + 'px' : '20px';
    root.style.cursor = isDragging ? 'grabbing' : 'grab';
  }, [wrapperW, wrapperH, wrapperX, wrapperY, isDragging]);

  useEffect(() => {
    const el = document.getElementById(ROOT_ID);
    if (!el) return;
    const onDown = (e: MouseEvent | TouchEvent) => onOuterDown(e);
    const onDblClick = () => togglePanel();
    el.addEventListener('pointerdown', onDown);
    el.addEventListener('touchstart', onDown, { passive: false } as AddEventListenerOptions);
    el.addEventListener('dblclick', onDblClick);
    return () => {
      el.removeEventListener('pointerdown', onDown);
      el.removeEventListener('touchstart', onDown);
      el.removeEventListener('dblclick', onDblClick);
    };
  }, [onOuterDown, togglePanel]);

  // ===== 执行 Live2D 外挂脚本（兜底：正常情况下插件已在 index.html 静态加载） =====
  useEffect(() => {
    const CORE_URL = 'https://live2d.muqingxi.com/Core/live2dcubismcore.js';
    const PLUGIN_URL = 'https://live2d.muqingxi.com/live2dplugin.js';

    // 插件已执行（window.Live2DModel 已就绪）则跳过，避免重复加载
    if (window.Live2DModel) return;

    let cancelled = false;

    function loadScript (src: string, type?: string, retry = 0): Promise<void> {
      return new Promise((resolve, reject) => {
        // 已存在相同脚本标签则跳过（避免 StrictMode 重复插入）
        if (retry === 0 && document.querySelector(`script[src="${src}"]`)) { resolve(); return; }
        const s = document.createElement('script');
        s.src = src;
        if (type) s.type = type;
        s.onload = () => resolve();
        s.onerror = () => {
          // 移除失败标签，允许重试
          s.remove();
          if (retry < 2) {
            loadScript(src, type, retry + 1).then(resolve, reject);
          } else {
            reject(new Error(`Failed to load ${src}`));
          }
        };
        document.body.appendChild(s);
      });
    }

    loadScript(CORE_URL)
      .then(() => loadScript(PLUGIN_URL, 'module'))
      .then(() => {
        if (!cancelled) console.log('[Live2DPanel] 外挂脚本加载完成');
      })
      .catch(err => {
        if (!cancelled) console.error('[Live2DPanel] 脚本加载失败:', err);
      });

    return () => { cancelled = true; };
  }, []);

  // ===== 生命周期 =====
  useEffect(() => {
    // 挂载
    const t = setInterval(() => {
      if (!window.Live2DModel) return;
      clearInterval(t);
      window.Live2DModel.onReady?.(() => {
        console.log('[Live2DPanel] 模型加载完成');
        window.Live2DModel?.onTalkEnd?.(() => {
          stopTalkEffects();
          setTimeout(() => setBubbleText(''), 1500);
        });
      });
    }, 100);

    // 清理
    return () => {
      clearInterval(t);
      stopThinking();
      stopTalkEffects();
      stopAudio();
      document.removeEventListener('pointermove', onGripMove);
      document.removeEventListener('pointerup', onGripUp);
      document.removeEventListener('touchmove', onGripMove);
      document.removeEventListener('touchend', onGripUp);
      document.removeEventListener('pointermove', onOuterMove);
      document.removeEventListener('pointerup', onOuterUp);
      document.removeEventListener('touchmove', onOuterMove);
      document.removeEventListener('touchend', onOuterUp);
    };
  }, []);

  // 注册全局事件监听
  useEffect(() => {
    document.addEventListener('pointermove', onGripMove);
    document.addEventListener('pointerup', onGripUp);
    document.addEventListener('touchmove', onGripMove, { passive: false } as AddEventListenerOptions);
    document.addEventListener('touchend', onGripUp);
    document.addEventListener('pointermove', onOuterMove);
    document.addEventListener('pointerup', onOuterUp);
    document.addEventListener('touchmove', onOuterMove, { passive: false } as AddEventListenerOptions);
    document.addEventListener('touchend', onOuterUp);

    return () => {
      document.removeEventListener('pointermove', onGripMove);
      document.removeEventListener('pointerup', onGripUp);
      document.removeEventListener('touchmove', onGripMove);
      document.removeEventListener('touchend', onGripUp);
      document.removeEventListener('pointermove', onOuterMove);
      document.removeEventListener('pointerup', onOuterUp);
      document.removeEventListener('touchmove', onOuterMove);
      document.removeEventListener('touchend', onOuterUp);
    };
  }, [onGripMove, onGripUp, onOuterMove, onOuterUp]);

  // ===== 渲染（UI 通过 Portal 注入 index.html 中的静态容器：气泡→外层 root，其余→内层 wrapper） =====
  return (
    <>
      {createPortal(
        <>
          {/* 气泡（位于 wrapper 上方的 root 内，不受 overflow:hidden 裁剪） */}
          {bubbleText && (
            <div className='bubble'>
              <span className='bubble-arrow' />
              {bubbleText}
            </div>
          )}
        </>,
        document.getElementById(ROOT_ID) as HTMLElement
      )}
      {createPortal(
        <>
          {/* 思考指示器 */}
          {isThinking && !bubbleText && (
            <div className='think-indicator'>
              <span className='think-dot think-dot-1' />
              <span className='think-dot think-dot-2' />
              <span className='think-dot think-dot-3' />
            </div>
          )}

          {/* 底部交互栏 */}
          <div
            className='absolute bottom-0 left-0 right-0 z-30 flex items-center justify-between px-3 pb-2.5 pt-8'
            style={{ background: 'linear-gradient(to top, rgba(0,0,0,0.15) 0%, transparent 100%)' }}
          >
            {/* 对话按钮 */}
            <button
              className={`chat-toggle-btn ${isChatVisible ? 'active' : ''}`}
              onClick={(e) => { e.stopPropagation(); toggleChat(); }}
              title='对话'
            >
              <svg viewBox='0 0 24 24' width='16' height='16' fill='currentColor'>
                <path d='M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z' />
              </svg>
            </button>

            {/* 缩放手柄 */}
            <div
              className='resize-grip opacity-0 hover:opacity-60 transition-opacity duration-300 cursor-nwse-resize text-white/50'
              onPointerDown={(e) => { e.stopPropagation(); onGripDown(e); }}
              onTouchStart={(e) => { e.stopPropagation(); onGripDown(e); }}
            >
              <svg viewBox='0 0 14 14' width='12' height='12'>
                <path d='M14 0v2L2 14H0l14-14zM14 4v2L6 14H4l10-10zM14 8v2l-4 4h-2l6-6zM14 12v2h-2l2-2z' fill='currentColor' />
              </svg>
            </div>
          </div>

          {/* 对话输入栏（从底部滑出） */}
          {isChatVisible && (
            <div className='chat-box' onPointerDown={(e) => e.stopPropagation()} onTouchStart={(e) => e.stopPropagation()}>
              <div className='chat-bar'>
                <input
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  className='chat-input'
                  placeholder='说点什么...'
                  onKeyUp={(e) => { if (e.key === 'Enter') sendMessage(); }}
                  disabled={chatLoading}
                />
                <button
                  className={`chat-send ${chatInput.trim() && !chatLoading ? 'can-send' : ''}`}
                  onClick={sendMessage}
                  disabled={chatLoading || !chatInput.trim()}
                >
                  {!chatLoading
                    ? (
                      <svg viewBox='0 0 24 24' width='14' height='14' fill='currentColor'>
                        <path d='M2.01 21L23 12 2.01 3 2 10l15 2-15 2z' />
                      </svg>
                    )
                    : (
                      <span className='send-loading'>
                        <span /><span /><span />
                      </span>
                    )}
                </button>
              </div>
            </div>
          )}

          {/* 控制面板 */}
          {panelVisible && (
            <div className='control-panel' onPointerDown={(e) => e.stopPropagation()}>
              <div className='panel-header'>
                <span>设置</span>
                <button className='panel-close' onClick={() => setPanelVisible(false)}>&times;</button>
              </div>
              <div className='panel-slider-row'>
                <span className='text-white/50 text-[10px]'>30%</span>
                <input
                  type='range'
                  min='30'
                  max='100'
                  className='panel-slider'
                  value={Math.round(modelScale * 100)}
                  onChange={(e) => setScale(parseFloat(e.target.value) / 100)}
                />
                <span className='text-white/50 text-[10px]'>100%</span>
              </div>
              <div className='text-center text-white/70 text-xs tabular-nums'>
                {Math.round(modelScale * 100)}%
              </div>
              <button
                className='panel-reset'
                onClick={() => {
                  window.Live2DModel?.resetContainer?.();
                  setScale(1);
                }}
              >
                重置缩放
              </button>
            </div>
          )}
        </>,
        document.getElementById(WRAPPER_ID) as HTMLElement
      )}
    </>
  );
};

export default Live2DPanel;
