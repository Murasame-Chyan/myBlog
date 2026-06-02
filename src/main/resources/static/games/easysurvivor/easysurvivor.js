// ==================== EasySurvivor - Web Edition ====================
const W = 1280, H = 720;
const FPS = 1000 / 60;

const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');

// ==================== 资源加载 ====================
const ASSET_BASE = '/games/easysurvivor/';
const imgCache = {};

function loadImage(src) {
    if (imgCache[src]) return imgCache[src];
    const img = new Image();
    img.src = ASSET_BASE + src;
    imgCache[src] = img;
    return img;
}

// 预加载关键资源
const assets = {
    bg: loadImage('img/background.png'),
    menuBg: loadImage('img/menu.png'),
    shadow_player: loadImage('img/shadow_player.png'),
    shadow_enemy: loadImage('img/shadow_enemy.png'),
};

// 音频
const audio = {
    bgm: new Audio(ASSET_BASE + 'mus/bgm.mp3'),
    hit: new Audio(ASSET_BASE + 'mus/hit.wav'),
};
audio.bgm.loop = true;
audio.hit.volume = 0.5;

// ==================== 输入 ====================
const keys = new Set();
document.addEventListener('keydown', e => {
    keys.add(e.key);
    e.preventDefault();
});
document.addEventListener('keyup', e => {
    keys.delete(e.key);
    e.preventDefault();
});

// ==================== Animation ====================
class Animation {
    constructor(pathPattern, num, interval) {
        this.frames = [];
        this.interval = interval;
        this.timer = 0;
        this.idx = 0;
        for (let i = 0; i < num; i++) {
            this.frames.push(loadImage(pathPattern.replace('%d', i)));
        }
    }

    play(x, y, delta) {
        this.timer += delta;
        if (this.timer >= this.interval) {
            this.idx = (this.idx + 1) % this.frames.length;
            this.timer = 0;
        }
        const img = this.frames[this.idx];
        if (img.complete) ctx.drawImage(img, x, y);
    }
}

// ==================== Player ====================
class Player {
    static SPEED = 8;
    static W = 80;
    static H = 80;
    static SHADOW_W = 32;

    constructor() {
        this.x = 500;
        this.y = 500;
        this.lives = 3;
        this.invincibleTimer = 0;
        this.speedMult = 1.0;
        this.animLeft = new Animation('img/paimon_left_%d.png', 6, 45);
        this.animRight = new Animation('img/paimon_right_%d.png', 6, 45);
        this.facingLeft = false;
    }

    get left()  { return this.x; }
    get right() { return this.x + Player.W; }
    get top()   { return this.y; }
    get bottom(){ return this.y + Player.H; }
    get cx()    { return this.x + Player.W / 2; }
    get cy()    { return this.y + Player.H / 2; }

    move(dt) {
        let dx = (keys.has('ArrowRight') || keys.has('d')) - (keys.has('ArrowLeft') || keys.has('a'));
        let dy = (keys.has('ArrowDown') || keys.has('s')) - (keys.has('ArrowUp') || keys.has('w'));
        const len = Math.sqrt(dx * dx + dy * dy);
        if (len > 0) {
            this.x += Player.SPEED * this.speedMult * dt * dx / len;
            this.y += Player.SPEED * this.speedMult * dt * dy / len;
        }
        this.x = Math.max(0, Math.min(W - Player.W, this.x));
        this.y = Math.max(0, Math.min(H - Player.H, this.y));

        if (dx < 0) this.facingLeft = true;
        else if (dx > 0) this.facingLeft = false;
    }

    draw(delta) {
        // 无敌闪烁
        if (this.invincibleTimer > 0 && Math.floor(this.invincibleTimer / 60) % 2 === 0) return;

        // 阴影
        const sx = this.x + (Player.W - Player.SHADOW_W) / 2;
        const sy = this.y + Player.H - 8;
        const sh = assets.shadow_player;
        if (sh.complete) ctx.drawImage(sh, sx, sy);

        (this.facingLeft ? this.animLeft : this.animRight).play(this.x, this.y, delta);
    }

    updateInvincibility(delta) {
        if (this.invincibleTimer > 0) this.invincibleTimer -= delta;
    }

    takeDamage() {
        if (this.invincibleTimer > 0) return false;
        this.lives--;
        this.invincibleTimer = 1200;
        return this.lives <= 0;
    }

    addLife() { this.lives++; }
    setInvincible(ms) { this.invincibleTimer = ms; }
}

// ==================== Bullet ====================
class Bullet {
    static RADIUS = 10;
    constructor() {
        this.x = 0;
        this.y = 0;
    }

    draw() {
        ctx.beginPath();
        ctx.arc(this.x, this.y, Bullet.RADIUS, 0, Math.PI * 2);
        ctx.fillStyle = '#C84B0A';
        ctx.fill();
        ctx.strokeStyle = '#FF9B32';
        ctx.lineWidth = 2;
        ctx.stroke();
    }
}

// ==================== Enemy ====================
class Enemy {
    static SPEED = 7;
    static W = 80;
    static H = 80;
    static SHADOW_W = 48;

    constructor() {
        const edge = Math.floor(Math.random() * 4);
        switch (edge) {
            case 0: this.x = Math.random() * W; this.y = -Enemy.H; break;
            case 1: this.x = Math.random() * W; this.y = H; break;
            case 2: this.x = -Enemy.W; this.y = Math.random() * H; break;
            case 3: this.x = W; this.y = Math.random() * H; break;
        }
        this.alive = true;
        this.frozen = false;
        this.facingLeft = false;
        this.animLeft = new Animation('img/boar_left_%d.png', 6, 45);
        this.animRight = new Animation('img/boar_right_%d.png', 6, 45);
    }

    get cx() { return this.x + Enemy.W / 2; }
    get cy() { return this.y + Enemy.H / 2; }

    move(player, dt) {
        if (this.frozen) return;

        const dx = player.cx - this.cx;
        const dy = player.cy - this.cy;
        const len = Math.sqrt(dx * dx + dy * dy);
        if (len > 0) {
            this.x += Enemy.SPEED * dt * dx / len;
            this.y += Enemy.SPEED * dt * dy / len;
        }
        if (dx < 0) this.facingLeft = true;
        else if (dx > 0) this.facingLeft = false;
    }

    draw(delta) {
        const sx = this.x + (Enemy.W - Enemy.SHADOW_W) / 2;
        const sy = this.y + Enemy.H - 35;
        const sh = assets.shadow_enemy;
        if (sh.complete) ctx.drawImage(sh, sx, sy);

        (this.facingLeft ? this.animLeft : this.animRight).play(this.x, this.y, delta);

        if (this.frozen) {
            ctx.beginPath();
            ctx.arc(this.x + Enemy.W / 2, this.y - 6, 7, 0, Math.PI * 2);
            ctx.fillStyle = '#00B4FF';
            ctx.fill();
            ctx.strokeStyle = '#FFF';
            ctx.lineWidth = 1.5;
            ctx.stroke();
        }
    }

    checkBulletCollision(bullet) {
        return bullet.x >= this.x && bullet.x <= this.x + Enemy.W &&
               bullet.y >= this.y && bullet.y <= this.y + Enemy.H;
    }

    checkPlayerCollision(player) {
        return this.cx >= player.left && this.cx <= player.right &&
               this.cy >= player.top && this.cy <= player.bottom;
    }

    hurt() { this.alive = false; }
}

// ==================== Item ====================
const ItemType = {
    Heal: 0, Invincible: 1, BulletBoost: 2, SpeedBoost: 3, KillAll: 4, FreezeAll: 5,
    props: [
        { color: '#FF3C50', label: '命' },
        { color: '#FFD700', label: '盾' },
        { color: '#3296FF', label: '弹' },
        { color: '#32DC50', label: '速' },
        { color: '#FF8C1E', label: '灭' },
        { color: '#00C8DC', label: '冻' },
    ]
};

class Item {
    static MAX_LIFE = 8000;
    static RADIUS = 14;

    constructor(type, x, y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.lifetime = Item.MAX_LIFE;
    }

    update(delta) { this.lifetime -= delta; }
    isExpired() { return this.lifetime <= 0; }

    draw() {
        const p = ItemType.props[this.type];
        // 外圈
        ctx.beginPath();
        ctx.arc(this.x, this.y, Item.RADIUS, 0, Math.PI * 2);
        ctx.fillStyle = p.color;
        ctx.fill();
        ctx.strokeStyle = '#FFF';
        ctx.lineWidth = 2;
        ctx.stroke();
        // 内圈
        ctx.beginPath();
        ctx.arc(this.x, this.y, Item.RADIUS - 3, 0, Math.PI * 2);
        ctx.fillStyle = '#000';
        ctx.fill();
        // 文字
        ctx.fillStyle = p.color;
        ctx.font = '14px "Microsoft YaHei", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(p.label, this.x, this.y);
        ctx.textAlign = 'start';
    }

    checkPlayerCollision(player) {
        const cx = this.x, cy = this.y, r = Item.RADIUS + 10;
        const nearX = Math.max(player.left, Math.min(cx, player.right));
        const nearY = Math.max(player.top, Math.min(cy, player.bottom));
        return (cx - nearX) ** 2 + (cy - nearY) ** 2 < r * r;
    }
}

// ==================== 等级系统 ====================
function getLevel(score) {
    if (score >= 60) return 5;
    if (score >= 40) return 4;
    if (score >= 25) return 3;
    if (score >= 10) return 2;
    return 1;
}

function getBulletCount(level) {
    return [0, 1, 2, 3, 3, 4][level] || 1;
}

function getBulletSpeed(level) {
    return level >= 4 ? 0.0090 : 0.0045;
}

function getXPInLevel(score) {
    const t = [0, 10, 25, 40, 60];
    return score - t[getLevel(score) - 1];
}

function getXPForNextLevel(level) {
    const t = [0, 10, 25, 40, 60, 999];
    return t[level] - t[level - 1];
}

// ==================== 道具掉落 ====================
function rollItemType() {
    const w = [0.3, 0.1, 0.5, 0.5, 0.1, 0.2];
    const r = Math.random() * 1.7;
    let acc = 0;
    for (let i = 0; i < 6; i++) {
        acc += w[i];
        if (r < acc) return i;
    }
    return 5;
}

// ==================== 游戏状态 ====================
let state = 'menu'; // 'menu' | 'playing' | 'gameover'
let player, enemies, bullets, items, score, level, prevLevel;
let running, imgBg;
let buffBulletTimer, buffSpeedTimer, freezeTimer;
let levelUpTimer, levelUpDisplay;
let enemySpawnTimer;

// ==================== DOM 元素 ====================
const menuOverlay = document.getElementById('menuOverlay');
const gameoverOverlay = document.getElementById('gameoverOverlay');
const finalScoreEl = document.getElementById('finalScore');
const finalLevelEl = document.getElementById('finalLevel');

// ==================== 菜单 ====================
document.getElementById('btnStart').addEventListener('click', () => {
    menuOverlay.classList.add('hidden');
    startGame();
});

document.getElementById('btnQuit').addEventListener('click', () => {
    window.location.href = '/';
});

// ==================== 游戏结束 ====================
document.getElementById('btnRestart').addEventListener('click', () => {
    gameoverOverlay.classList.add('hidden');
    startGame();
});

document.getElementById('btnBackMenu').addEventListener('click', () => {
    gameoverOverlay.classList.add('hidden');
    menuOverlay.classList.remove('hidden');
    state = 'menu';
});

// ==================== 开始游戏 ====================
function startGame() {
    player = new Player();
    enemies = [];
    bullets = [];
    items = [];
    score = 0;
    level = 1;
    prevLevel = 1;
    running = true;
    buffBulletTimer = 0;
    buffSpeedTimer = 0;
    freezeTimer = 0;
    levelUpTimer = 0;
    levelUpDisplay = 0;
    enemySpawnTimer = 800;

    bullets.push(new Bullet()); // 初始 1 个子弹

    try { audio.bgm.play(); } catch(e) {}

    state = 'playing';
}

// ==================== 游戏循环 ====================
let lastTime = performance.now();

function gameLoop(now) {
    requestAnimationFrame(gameLoop);

    const delta = Math.min(now - lastTime, 50);
    lastTime = now;

    if (state === 'menu') {
        ctx.clearRect(0, 0, W, H);
        const mb = assets.menuBg;
        if (mb.complete) ctx.drawImage(mb, 0, 0);
        return;
    }

    if (state === 'gameover' || !running) return;

    const normDt = delta / (1000 / 60);

    // ======== 更新 ========
    player.move(normDt);
    player.updateInvincibility(delta);

    if (buffBulletTimer > 0) buffBulletTimer -= delta;
    if (buffSpeedTimer > 0) {
        buffSpeedTimer -= delta;
        if (buffSpeedTimer <= 0) player.speedMult = 1.0;
    }
    if (freezeTimer > 0) {
        freezeTimer -= delta;
        if (freezeTimer <= 0) {
            for (const e of enemies) e.frozen = false;
        }
    }

    const tangentSpd = buffBulletTimer > 0 ? 0.0135 : getBulletSpeed(level);
    updateBullets(tangentSpd);

    enemySpawnTimer -= delta;
    if (enemySpawnTimer <= 0) {
        enemies.push(new Enemy());
        enemySpawnTimer = 500 + Math.random() * 1300;
    }

    for (const enemy of enemies) enemy.move(player, normDt);

    // 碰撞检测
    for (const enemy of enemies) {
        if (enemy.checkPlayerCollision(player)) {
            try { audio.hit.play(); } catch(e) {}
            enemy.hurt();
            if (player.takeDamage()) {
                endGame();
                return;
            }
        }
    }

    for (const enemy of enemies) {
        for (const bullet of bullets) {
            if (enemy.checkBulletCollision(bullet)) {
                try { audio.hit.play(); } catch(e) {}
                enemy.hurt();
                score++;
            }
        }
    }

    // 升级检测
    level = getLevel(score);
    if (level > prevLevel) {
        const target = getBulletCount(level);
        while (bullets.length > target) bullets.pop();
        while (bullets.length < target) bullets.push(new Bullet());
        if (level === 2 || level === 4) player.addLife();
        levelUpTimer = 2000;
        levelUpDisplay = level;
        prevLevel = level;
    }

    // 移除敌人 + 掉落
    for (let i = enemies.length - 1; i >= 0; i--) {
        if (!enemies[i].alive) {
            if (Math.random() < 0.3) {
                items.push(new Item(rollItemType(), enemies[i].x + 40, enemies[i].y + 40));
            }
            enemies.splice(i, 1);
        }
    }

    // 道具更新 + 拾取
    for (let i = items.length - 1; i >= 0; i--) {
        items[i].update(delta);
        if (items[i].isExpired()) {
            items.splice(i, 1);
            continue;
        }
        if (items[i].checkPlayerCollision(player)) {
            applyItem(items[i]);
            items.splice(i, 1);
        }
    }

    // 升级提示计时
    if (levelUpTimer > 0) levelUpTimer -= delta;

    // ======== 渲染 ========
    ctx.clearRect(0, 0, W, H);

    // 背景
    const bg = assets.bg;
    if (bg.complete) ctx.drawImage(bg, 0, 0);

    // 玩家
    player.draw(delta);

    // 敌人
    for (const enemy of enemies) enemy.draw(delta);

    // 子弹
    for (const bullet of bullets) bullet.draw();

    // 道具
    for (const item of items) item.draw();

    // HUD
    drawHUD();

    // 升级提示
    if (levelUpTimer > 0) drawLevelUpNotice();
}

requestAnimationFrame(gameLoop);

// ==================== 结束游戏 ====================
function endGame() {
    running = false;
    state = 'gameover';
    try { audio.bgm.pause(); } catch(e) {}

    finalScoreEl.textContent = score;
    finalLevelEl.textContent = level;
    gameoverOverlay.classList.remove('hidden');

    // 上报补签（无门槛，游玩即可）
    if (typeof authFetch === "function") {
        authFetch("/sign-in/makeup", { method: "POST" })
            .then(function (r) { return r.json(); })
            .then(function (d) {
                if (d.code === 200 && d.data && d.data.isMakeup) {
                    var infoEl = document.getElementById("makeupInfo");
                    if (!infoEl) {
                        infoEl = document.createElement("p");
                        infoEl.id = "makeupInfo";
                        infoEl.className = "gameover-info";
                        infoEl.style.color = "#87CEEB";
                        infoEl.textContent = "补签成功! +" + d.data.totalExp + " EXP";
                        var panel = document.querySelector(".gameover-panel");
                        if (panel) panel.appendChild(infoEl);
                    }
                }
            })
            .catch(function () {});
    }
}

// ==================== 道具应用 ====================
function applyItem(item) {
    switch (item.type) {
        case ItemType.Heal:
            player.addLife();
            break;
        case ItemType.Invincible:
            player.setInvincible(5000);
            break;
        case ItemType.BulletBoost:
            buffBulletTimer = 5000;
            break;
        case ItemType.SpeedBoost:
            buffSpeedTimer = 5000;
            player.speedMult = 1.5;
            break;
        case ItemType.KillAll:
            for (const e of enemies) e.hurt();
            break;
        case ItemType.FreezeAll:
            freezeTimer = 3000;
            for (const e of enemies) e.frozen = true;
            break;
    }
}

// ==================== 子弹更新 ====================
function updateBullets(tangentSpd) {
    const RADIAL_SPD = 0.0095;
    const n = bullets.length;
    const interval = n > 0 ? 2 * Math.PI / n : 0;
    const t = performance.now();
    const radius = 100 + 25 * Math.sin(t * RADIAL_SPD);

    for (let i = 0; i < n; i++) {
        const radian = t * tangentSpd + interval * i;
        bullets[i].x = player.cx + radius * Math.cos(radian);
        bullets[i].y = player.cy + radius * Math.sin(radian);
    }
}

// ==================== HUD ====================
function drawHUD() {
    ctx.font = 'bold 16px "Microsoft YaHei", sans-serif';
    ctx.textBaseline = 'top';

    // 得分
    ctx.fillStyle = '#FF55B9';
    ctx.fillText(`得分 [ ${score} ]`, 10, 10);

    // 等级
    ctx.fillStyle = '#FFD700';
    ctx.fillText(`等级 [ ${level} ]`, 10, 35);

    // 生命
    for (let i = 0; i < player.lives; i++) {
        const cx = 22 + i * 30, cy = 68;
        ctx.beginPath();
        ctx.arc(cx, cy, 10, 0, Math.PI * 2);
        ctx.fillStyle = '#DC1E1E';
        ctx.fill();
        ctx.strokeStyle = '#B41414';
        ctx.lineWidth = 1;
        ctx.stroke();
        // 高光
        ctx.beginPath();
        ctx.ellipse(cx - 4, cy - 5, 5, 3, 0, 0, Math.PI * 2);
        ctx.fillStyle = '#FF6464';
        ctx.fill();
    }

    // XP 条
    const xpCurrent = getXPInLevel(score);
    const xpMax = getXPForNextLevel(level);
    const bx = 10, by = 80, bw = 150, bh = 10;
    ctx.fillStyle = '#282828';
    ctx.fillRect(bx, by, bw, bh);
    ctx.strokeStyle = '#646464';
    ctx.lineWidth = 1;
    ctx.strokeRect(bx, by, bw, bh);

    if (xpMax > 0) {
        ctx.fillStyle = '#00C850';
        ctx.fillRect(bx, by, bw * xpCurrent / xpMax, bh);
    }
    ctx.font = '12px "Microsoft YaHei", sans-serif';
    ctx.fillStyle = '#C8C8C8';
    ctx.fillText(`EXP ${xpCurrent}/${xpMax}`, bx + bw + 5, by - 1);

    // buff 状态
    let sy = 95;
    ctx.font = '13px "Microsoft YaHei", sans-serif';
    if (buffBulletTimer > 0) {
        ctx.fillStyle = '#3296FF';
        ctx.fillText(`弹速↑ ${(buffBulletTimer / 1000).toFixed(1)}s`, 10, sy);
        sy += 18;
    }
    if (buffSpeedTimer > 0) {
        ctx.fillStyle = '#32DC50';
        ctx.fillText(`移速↑ ${(buffSpeedTimer / 1000).toFixed(1)}s`, 10, sy);
        sy += 18;
    }
    if (freezeTimer > 0) {
        ctx.fillStyle = '#00C8DC';
        ctx.fillText(`冻结 ${(freezeTimer / 1000).toFixed(1)}s`, 10, sy);
    }
}

// ==================== 升级提示 ====================
function drawLevelUpNotice() {
    const alpha = levelUpTimer > 1500 ? 1 : levelUpTimer / 1500;
    const yOff = (2000 - levelUpTimer) / 8;
    ctx.save();
    ctx.globalAlpha = alpha;
    ctx.font = 'bold 28px "Microsoft YaHei", sans-serif';
    ctx.fillStyle = '#FFD700';
    ctx.textAlign = 'center';
    ctx.fillText(`升 级 !  Lv.${levelUpDisplay}`, W / 2, H / 2 - 40 - yOff);
    ctx.textAlign = 'start';
    ctx.restore();
}
