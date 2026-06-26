/**
 * 博客音乐播放器 - IIFE 组件
 */
(function () {
  "use strict";

  // ==================== 状态管理 ====================
  const STORAGE_KEY = "musicPlayerState";

  const state = {
    isOpen: false,
    isPanelExpanded: false,
    isPlaying: false,
    currentTrackIndex: 0,
    volume: 0.7,
    currentTime: 0,
    capsulePos: { x: null, y: null },
    panelPos: { x: null, y: null },
  };

  // ==================== 歌单配置 ====================
  const playlist = [
    {
      title: "TOMATO",
      artist: "7co",
      src: "/api/music/stream/TOMATO-7co",
      lrc: "/music/TOMATO-7co.lrc",
    },
  ];

  // ==================== DOM 引用 ====================
  const $ = (id) => document.getElementById(id);

  let miniCapsule, fullPanel, capsuleCover, capsuleTitle, capsuleArtist;
  let panelTitle, panelArtist;
  let progressBar, progressFill, currentTimeEl, totalTimeEl;
  let lyricsContainer, playlistContainer;
  let visualizerCanvas, visualizerCtx;
  let capsulePlayBtn, playBtn, prevBtn, nextBtn, panelCloseBtn;

  // ==================== Audio ====================
  const audio = new Audio();
  audio.volume = state.volume;
  audio.preload = "auto";

  // ==================== Web Audio API 可视化 ====================
  let audioContext = null;
  let analyser = null;
  let sourceNode = null;
  let visualizerId = null;
  let freqData = null;

  function initAudioContext() {
    if (audioContext) return;
    try {
      audioContext = new (window.AudioContext || window.webkitAudioContext)();
      analyser = audioContext.createAnalyser();
      analyser.fftSize = 128;
      analyser.smoothingTimeConstant = 0.6;
      sourceNode = audioContext.createMediaElementSource(audio);
      sourceNode.connect(analyser);
      analyser.connect(audioContext.destination);
      freqData = new Uint8Array(analyser.frequencyBinCount);
    } catch (_) {
      audioContext = null;
      analyser = null;
    }
  }

  function drawVisualizer() {
    if (!visualizerCtx || !visualizerCanvas || !analyser) {
      visualizerId = requestAnimationFrame(drawVisualizer);
      return;
    }

    const prefersReduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (prefersReduced) {
      visualizerCtx.clearRect(0, 0, visualizerCanvas.width, visualizerCanvas.height);
      visualizerId = requestAnimationFrame(drawVisualizer);
      return;
    }

    analyser.getByteFrequencyData(freqData);

    const W = visualizerCanvas.width;
    const H = visualizerCanvas.height;
    visualizerCtx.clearRect(0, 0, W, H);

    // 使用全频段，更多更细的音柱
    const startBin = 0;
    const endBin = freqData.length;
    const usableBins = endBin - startBin;
    const barCount = 64;
    const barWidth = (W / barCount) * 0.45;
    const gap = (W / barCount) * 0.55;
    const maxHeight = H * 0.85;

    for (let i = 0; i < barCount; i++) {
      const binIndex = startBin + Math.floor((i / barCount) * usableBins);
      const value = freqData[binIndex] || 0;
      const ratio = value / 255;
      // ease-out 让节奏感更明显
      const eased = 1 - Math.pow(1 - ratio, 2.2);
      const barH = Math.max(1.5, eased * maxHeight);

      const x = i * (barWidth + gap) + gap / 2;
      const y = (H - barH) / 2;

      // 渐变色：底部天空蓝 → 顶部浅蓝白
      const gradient = visualizerCtx.createLinearGradient(x, H / 2 + barH / 2, x, H / 2 - barH / 2);
      gradient.addColorStop(0, "rgba(135, 206, 235, 0.95)");
      gradient.addColorStop(0.4, "rgba(135, 206, 235, 0.55)");
      gradient.addColorStop(1, "rgba(200, 235, 255, 0.25)");

      visualizerCtx.fillStyle = gradient;
      visualizerCtx.beginPath();
      // 手动画圆角矩形，兼容不支持 roundRect 的浏览器
      const r = Math.min(barWidth / 2, 1.5);
      visualizerCtx.moveTo(x + r, y);
      visualizerCtx.lineTo(x + barWidth - r, y);
      visualizerCtx.arcTo(x + barWidth, y, x + barWidth, y + r, r);
      visualizerCtx.lineTo(x + barWidth, y + barH - r);
      visualizerCtx.arcTo(x + barWidth, y + barH, x + barWidth - r, y + barH, r);
      visualizerCtx.lineTo(x + r, y + barH);
      visualizerCtx.arcTo(x, y + barH, x, y + barH - r, r);
      visualizerCtx.lineTo(x, y + r);
      visualizerCtx.arcTo(x, y, x + r, y, r);
      visualizerCtx.closePath();
      visualizerCtx.fill();
    }

    visualizerId = requestAnimationFrame(drawVisualizer);
  }

  function startVisualizer() {
    if (visualizerId) return;
    if (!visualizerCanvas) return;
    const rect = visualizerCanvas.parentElement.getBoundingClientRect();
    visualizerCanvas.width = rect.width;
    visualizerCanvas.height = rect.height;
    visualizerCtx = visualizerCanvas.getContext("2d");
    initAudioContext();
    drawVisualizer();
  }

  function stopVisualizer() {
    if (visualizerId) {
      cancelAnimationFrame(visualizerId);
      visualizerId = null;
    }
    if (visualizerCtx && visualizerCanvas) {
      visualizerCtx.clearRect(0, 0, visualizerCanvas.width, visualizerCanvas.height);
    }
  }

  // ==================== 封面旋转（仅迷你胶囊） ====================
  function startCoverSpin() {
    if (capsuleCover) capsuleCover.classList.add("spinning");
  }

  function stopCoverSpin() {
    if (capsuleCover) capsuleCover.classList.remove("spinning");
  }

  // ==================== LRC 解析器 ====================
  function parseLRC(lrcText) {
    const lines = lrcText.split("\n");
    const result = [];
    const timeRegex = /\[(\d{2}):(\d{2})\.(\d{2,3})\]/g;

    for (const line of lines) {
      const matches = [...line.matchAll(timeRegex)];
      if (matches.length === 0) continue;

      let text = line.replace(timeRegex, "").trim();
      if (!text) text = "";

      for (const match of matches) {
        const min = parseInt(match[1], 10);
        const sec = parseInt(match[2], 10);
        let ms = match[3];
        if (ms.length === 2) ms = parseInt(ms, 10) / 100;
        else ms = parseInt(ms, 10) / 1000;
        result.push({ time: min * 60 + sec + ms, text: text });
      }
    }

    result.sort((a, b) => a.time - b.time);
    return result;
  }

  function fetchLyrics(index) {
    const track = playlist[index];
    if (!track || !track.lrc) {
      lyricsData = [];
      renderLyrics();
      return;
    }

    fetch(track.lrc)
      .then((res) => {
        if (!res.ok) throw new Error("Lyrics not found");
        return res.text();
      })
      .then((text) => {
        lyricsData = parseLRC(text);
        lyricsData = lyricsData.filter(
          (item) => !(item.time === 0 && (item.text.includes("TME") || item.text.includes("词：") || item.text.includes("曲：")))
        );
        const titleArtist = `${track.title} - ${track.artist}`;
        lyricsData = lyricsData.filter(
          (item) => !(item.time === 0 && item.text === titleArtist)
        );
        currentLyricIndex = -1;
        renderLyrics();
      })
      .catch(() => {
        lyricsData = [];
        renderLyrics();
      });
  }

  let lyricsData = [];
  let currentLyricIndex = -1;

  function renderLyrics() {
    if (!lyricsContainer) return;
    lyricsContainer.innerHTML = "";

    if (lyricsData.length === 0) {
      lyricsContainer.innerHTML =
        '<div class="lyric-line">暂无歌词</div>';
      return;
    }

    const uniqueLines = [];
    const seen = new Set();
    for (const item of lyricsData) {
      const key = `${item.time.toFixed(2)}_${item.text}`;
      if (!seen.has(key)) {
        seen.add(key);
        uniqueLines.push(item);
      }
    }

    for (let i = 0; i < uniqueLines.length; i++) {
      const div = document.createElement("div");
      div.className = "lyric-line";
      div.textContent = uniqueLines[i].text;
      div.dataset.index = i;
      div.addEventListener("click", () => {
        if (uniqueLines[i].time > 0) {
          audio.currentTime = uniqueLines[i].time;
        }
      });
      lyricsContainer.appendChild(div);
    }
    currentLyricIndex = -1;
  }

  function syncLyrics() {
    if (!lyricsContainer || lyricsData.length === 0) return;

    const ct = audio.currentTime;
    let newIndex = -1;

    for (let i = lyricsData.length - 1; i >= 0; i--) {
      if (ct >= lyricsData[i].time) {
        newIndex = i;
        break;
      }
    }

    if (newIndex === currentLyricIndex) return;
    currentLyricIndex = newIndex;

    const lines = lyricsContainer.querySelectorAll(".lyric-line");
    lines.forEach((l) => l.classList.remove("active"));

    if (newIndex >= 0) {
      const target = lyricsContainer.querySelector(
        `.lyric-line[data-index="${newIndex}"]`
      );
      if (target) {
        target.classList.add("active");
        target.scrollIntoView({ behavior: "smooth", block: "center" });
      }
    }
  }

  // ==================== 播放控制 ====================
  function loadTrack(index) {
    if (index < 0 || index >= playlist.length) return;
    state.currentTrackIndex = index;
    const track = playlist[index];
    audio.src = track.src;
    audio.load();
    updateTrackInfo();
    updatePlaylistUI();
    fetchLyrics(index);
    saveState();
  }

  function play() {
    // 确保音频源已设置
    const track = playlist[state.currentTrackIndex];
    if (!audio.src || !audio.src.includes(track.src)) {
      audio.src = track.src;
      audio.load();
    }

    // 等待音频可播放后再执行 play()
    const doPlay = () => {
      audio.play()
        .then(() => {
          state.isPlaying = true;
          updatePlayButtons();
          startCoverSpin();
          if (state.isPanelExpanded) startVisualizer();
        })
        .catch((err) => {
          if (err.name === "NotAllowedError" && typeof showToast === "function") {
            showToast("请点击播放按钮开始播放", "info");
          }
        });
    };

    if (audio.readyState >= HTMLMediaElement.HAVE_CURRENT_DATA) {
      doPlay();
    } else {
      audio.addEventListener("canplay", doPlay, { once: true });
    }
  }

  function pause() {
    audio.pause();
    state.isPlaying = false;
    updatePlayButtons();
    stopCoverSpin();
    stopVisualizer();
  }

  function togglePlay() {
    if (state.isPlaying) {
      pause();
    } else {
      play();
    }
  }

  function playPrev() {
    if (playlist.length <= 1) {
      audio.currentTime = 0;
      if (!state.isPlaying) play();
      return;
    }
    let idx = state.currentTrackIndex - 1;
    if (idx < 0) idx = playlist.length - 1;
    loadTrack(idx);
    play();
  }

  function playNext() {
    if (playlist.length <= 1) {
      audio.currentTime = 0;
      if (!state.isPlaying) play();
      return;
    }
    let idx = state.currentTrackIndex + 1;
    if (idx >= playlist.length) idx = 0;
    loadTrack(idx);
    play();
  }

  // 进度条拖拽（使用 pointer 事件，阻止所有浏览器默认行为）
  let progressDragging = false;

  function seekToClient(clientX) {
    const rect = progressBar.getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
    audio.currentTime = ratio * (audio.duration || 0);
  }

  function onProgressStart(e) {
    e.preventDefault();
    e.stopPropagation();
    progressDragging = true;
    progressBar.setPointerCapture(e.pointerId);
    seekToClient(e.clientX);
  }

  function onProgressMove(e) {
    if (!progressDragging) return;
    e.preventDefault();
    seekToClient(e.clientX);
  }

  function onProgressEnd(e) {
    progressDragging = false;
    progressBar.releasePointerCapture(e.pointerId);
  }

  function updatePlayButtons() {
    const icon = state.isPlaying ? "bi-pause-fill" : "bi-play-fill";
    const title = state.isPlaying ? "暂停" : "播放";
    if (playBtn) {
      playBtn.innerHTML = `<i class="bi ${icon}"></i>`;
      playBtn.title = title;
    }
    if (capsulePlayBtn) {
      capsulePlayBtn.innerHTML = `<i class="bi ${icon}"></i>`;
      capsulePlayBtn.title = title;
    }
  }

  function updateTrackInfo() {
    const track = playlist[state.currentTrackIndex];
    if (capsuleTitle) capsuleTitle.textContent = track.title;
    if (capsuleArtist) capsuleArtist.textContent = track.artist;
    if (panelTitle) panelTitle.textContent = track.title;
    if (panelArtist) panelArtist.textContent = track.artist;
  }

  function updatePlaylistUI() {
    if (!playlistContainer) return;
    const items = playlistContainer.querySelectorAll(".playlist-item");
    items.forEach((item) => {
      const idx = parseInt(item.dataset.index, 10);
      item.classList.toggle("active", idx === state.currentTrackIndex);
    });
  }

  function formatTime(seconds) {
    if (isNaN(seconds) || seconds < 0) return "00:00";
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
  }

  // ==================== 统一拖拽系统（胶囊+面板联体移动） ====================
  function getCapsulePos() {
    const rect = miniCapsule.getBoundingClientRect();
    return { x: rect.left, y: rect.top };
  }

  function applyCapsulePos(x, y) {
    miniCapsule.style.left = x + "px";
    miniCapsule.style.top = y + "px";
    miniCapsule.style.right = "auto";
    miniCapsule.style.bottom = "auto";
  }

  // 面板相对于胶囊的偏移（展开时计算）
  let panelOffsetX = -316;
  let panelOffsetY = -556;

  function snapPanelToCapsule() {
    const cp = getCapsulePos();
    let px = cp.x + panelOffsetX;
    let py = cp.y + panelOffsetY;
    // 边界限制
    if (px < 8) px = 8;
    if (px + 340 > window.innerWidth - 8) px = window.innerWidth - 340 - 8;
    if (py < 8) { py = cp.y + miniCapsule.offsetHeight + 10; }
    fullPanel.style.left = px + "px";
    fullPanel.style.top = py + "px";
    fullPanel.style.right = "auto";
    fullPanel.style.bottom = "auto";
  }

  function makeUnifiedDraggable(element, isCapsule) {
    let startX, startY, capStartX, capStartY, panelStartX, panelStartY;
    let isDragging = false;
    let moved = false;

    function onStart(e) {
      if (e.target.closest("button") || e.target.closest("#progressBar")) return;
      e.preventDefault();
      const cp = getCapsulePos();
      capStartX = cp.x; capStartY = cp.y;
      const pRect = fullPanel.getBoundingClientRect();
      panelStartX = pRect.left; panelStartY = pRect.top;

      const touch = e.touches ? e.touches[0] : e;
      startX = touch.clientX;
      startY = touch.clientY;
      isDragging = true;
      moved = false;
      element.classList.add("dragging");
      fullPanel.classList.add("dragging");
      document.addEventListener("mousemove", onMove);
      document.addEventListener("mouseup", onEnd);
      document.addEventListener("touchmove", onMove, { passive: false });
      document.addEventListener("touchend", onEnd);
    }

    function onMove(e) {
      if (!isDragging) return;
      const touch = e.touches ? e.touches[0] : e;
      const dx = touch.clientX - startX;
      const dy = touch.clientY - startY;
      if (Math.abs(dx) < 3 && Math.abs(dy) < 3) return;
      moved = true;

      // 计算两个元素的目标位置（相同位移，保持相对位置）
      const capW = miniCapsule.offsetWidth;
      const capH = miniCapsule.offsetHeight;
      const targetCapX = capStartX + dx;
      const targetCapY = capStartY + dy;

      // 组合边界框从胶囊开始，面板展开时才纳入面板
      let comboMinX = targetCapX;
      let comboMinY = targetCapY;
      let comboMaxX = targetCapX + capW;
      let comboMaxY = targetCapY + capH;

      if (state.isPanelExpanded) {
        const panelW = fullPanel.offsetWidth;
        const panelH = fullPanel.offsetHeight;
        const targetPanelX = panelStartX + dx;
        const targetPanelY = panelStartY + dy;
        comboMinX = Math.min(comboMinX, targetPanelX);
        comboMinY = Math.min(comboMinY, targetPanelY);
        comboMaxX = Math.max(comboMaxX, targetPanelX + panelW);
        comboMaxY = Math.max(comboMaxY, targetPanelY + panelH);
      }

      // 将组合框 clamp 到视口内，计算需要的偏移修正
      const viewW = window.innerWidth;
      const viewH = window.innerHeight;
      let shiftX = 0, shiftY = 0;
      if (comboMinX < 8) shiftX = 8 - comboMinX;
      else if (comboMaxX > viewW - 8) shiftX = (viewW - 8) - comboMaxX;
      if (comboMinY < 8) shiftY = 8 - comboMinY;
      else if (comboMaxY > viewH - 8) shiftY = (viewH - 8) - comboMaxY;

      const actualDx = dx + shiftX;
      const actualDy = dy + shiftY;

      // 两元素使用完全相同的位移
      applyCapsulePos(capStartX + actualDx, capStartY + actualDy);

      if (state.isPanelExpanded) {
        fullPanel.style.left = (panelStartX + actualDx) + "px";
        fullPanel.style.top = (panelStartY + actualDy) + "px";
        fullPanel.style.right = "auto";
        fullPanel.style.bottom = "auto";
      }

      if (e.cancelable) e.preventDefault();
    }

    function onEnd() {
      if (!isDragging) return;
      isDragging = false;
      element.classList.remove("dragging");
      fullPanel.classList.remove("dragging");
      document.removeEventListener("mousemove", onMove);
      document.removeEventListener("mouseup", onEnd);
      document.removeEventListener("touchmove", onMove);
      document.removeEventListener("touchend", onEnd);
      if (moved) {
        const cp = getCapsulePos();
        state.capsulePos = { x: cp.x, y: cp.y };
        saveState();
      }
    }

    element.addEventListener("mousedown", onStart);
    element.addEventListener("touchstart", onStart, { passive: false });
  }

  // ==================== 面板展开/收起 ====================
  function expandPanel() {
    if (state.isPanelExpanded) return;
    state.isPanelExpanded = true;
    snapPanelToCapsule();
    fullPanel.classList.add("visible", "panel-entering");
    fullPanel.classList.remove("panel-entered");
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        fullPanel.classList.remove("panel-entering");
        fullPanel.classList.add("panel-entered");
      });
    });
    if (state.isPlaying) startVisualizer();
  }

  function collapsePanel() {
    if (!state.isPanelExpanded) return;
    state.isPanelExpanded = false;
    fullPanel.classList.add("panel-entering");
    fullPanel.classList.remove("panel-entered");
    setTimeout(() => {
      fullPanel.classList.remove("visible", "panel-entering");
    }, 250);
    stopVisualizer();
  }

  // ==================== 播放器开关 ====================
  function openPlayer() {
    state.isOpen = true;
    miniCapsule.classList.add("visible");
    if (state.capsulePos.x != null && state.capsulePos.y != null) {
      applyCapsulePos(state.capsulePos.x, state.capsulePos.y);
    }

    // 始终设置音频源以确保跨页面正常工作
    const track = playlist[state.currentTrackIndex];
    audio.src = track.src;
    audio.load();
    audio.volume = state.volume;

    // 恢复播放位置（等 loadedmetadata 触发后设置）
    if (state.currentTime > 0) {
      const onMeta = () => {
        audio.currentTime = state.currentTime;
        state.currentTime = 0;
        audio.removeEventListener("loadedmetadata", onMeta);
      };
      audio.addEventListener("loadedmetadata", onMeta);
    }

    fetchLyrics(state.currentTrackIndex);
    saveState();
  }

  function closePlayer() {
    state.isOpen = false;
    state.isPanelExpanded = false;
    pause();
    miniCapsule.classList.remove("visible");
    fullPanel.classList.remove("visible", "panel-entering", "panel-entered");
    stopVisualizer();
    saveState();
  }

  // ==================== localStorage ====================
  function saveState() {
    const data = {
      isOpen: state.isOpen,
      currentTrackIndex: state.currentTrackIndex,
      volume: state.volume,
      currentTime: audio.currentTime || state.currentTime,
      capsulePos: state.capsulePos,
      panelPos: state.panelPos,
    };
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    } catch (_) {}
  }

  function loadState() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return;
      const data = JSON.parse(raw);
      state.isOpen = data.isOpen || false;
      state.currentTrackIndex = data.currentTrackIndex || 0;
      state.volume = data.volume != null ? data.volume : 0.7;
      state.currentTime = data.currentTime || 0;
      state.capsulePos = data.capsulePos || { x: null, y: null };
      state.panelPos = data.panelPos || { x: null, y: null };
    } catch (_) {}
  }

  // ==================== 事件绑定 ====================
  function bindEvents() {
    miniCapsule.addEventListener("click", (e) => {
      if (e.target.closest("button")) return;
      if (state.isPanelExpanded) collapsePanel();
      else expandPanel();
    });

    capsulePlayBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      togglePlay();
    });

    playBtn.addEventListener("click", togglePlay);
    prevBtn.addEventListener("click", playPrev);
    nextBtn.addEventListener("click", playNext);

    panelCloseBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      collapsePanel();
    });

    progressBar.addEventListener("pointerdown", onProgressStart);
    progressBar.addEventListener("pointermove", onProgressMove);
    progressBar.addEventListener("pointerup", onProgressEnd);
    progressBar.addEventListener("pointercancel", onProgressEnd);

    audio.addEventListener("loadedmetadata", () => {
      if (totalTimeEl) totalTimeEl.textContent = formatTime(audio.duration);
      updatePlayButtons();
      updateTrackInfo();
    });

    audio.addEventListener("timeupdate", () => {
      if (currentTimeEl) currentTimeEl.textContent = formatTime(audio.currentTime);
      if (progressFill && audio.duration) {
        progressFill.style.width = (audio.currentTime / audio.duration) * 100 + "%";
      }
      syncLyrics();
    });

    audio.addEventListener("ended", () => playNext());

    audio.addEventListener("play", () => {
      state.isPlaying = true;
      updatePlayButtons();
      startCoverSpin();
      if (state.isPanelExpanded) startVisualizer();
    });

    audio.addEventListener("pause", () => {
      state.isPlaying = false;
      updatePlayButtons();
      stopCoverSpin();
      stopVisualizer();
    });

    audio.addEventListener("error", () => {
      state.isPlaying = false;
      updatePlayButtons();
      stopCoverSpin();
      stopVisualizer();
      if (typeof showToast === "function") {
        showToast("音频加载失败，请检查网络", "error");
      }
    });

    window.addEventListener("beforeunload", () => {
      state.currentTime = audio.currentTime || state.currentTime;
      saveState();
    });

    setInterval(() => {
      if (state.isOpen && state.isPlaying) {
        state.currentTime = audio.currentTime;
        saveState();
      }
    }, 5000);

    window.addEventListener("musicPlayerToggle", (e) => {
      if (e.detail.open) openPlayer();
      else closePlayer();
    });

    if (state.isOpen) openPlayer();

    syncNavToggleState();
  }

  function syncNavToggleState() {
    if (state.isOpen) {
      const toggleEl = document.getElementById("musicPlayerToggle");
      const statusEl = document.getElementById("musicPlayerStatus");
      if (toggleEl) toggleEl.classList.add("active");
      if (statusEl) statusEl.textContent = "已开启";
    }
  }

  // ==================== 初始化 ====================
  function init() {
    miniCapsule = $("musicMiniCapsule");
    fullPanel = $("musicFullPanel");
    capsuleCover = $("capsuleCover");
    capsuleTitle = $("capsuleTitle");
    capsuleArtist = $("capsuleArtist");
    panelTitle = $("panelTitle");
    panelArtist = $("panelArtist");
    progressBar = $("progressBar");
    progressFill = $("progressFill");
    currentTimeEl = $("currentTime");
    totalTimeEl = $("totalTime");
    lyricsContainer = $("lyricsContainer");
    playlistContainer = $("playlistContainer");
    visualizerCanvas = $("visualizerCanvas");
    capsulePlayBtn = $("capsulePlayBtn");
    playBtn = $("playBtn");
    prevBtn = $("prevBtn");
    nextBtn = $("nextBtn");
    panelCloseBtn = $("panelCloseBtn");

    if (!miniCapsule) return;

    loadState();
    makeUnifiedDraggable(miniCapsule, true);
    makeUnifiedDraggable(fullPanel, false);
    bindEvents();

    if (!state.panelPos || state.panelPos.x === null) {
      fullPanel.style.right = "24px";
      fullPanel.style.bottom = "90px";
    }
  }

  // ==================== 启动 ====================
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
