package org.belltaffmod.bellstaff_tplayers.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.belltaffmod.bellstaff_tplayers.Bellstaff_tplayers;
import org.belltaffmod.bellstaff_tplayers.BellstaffConstants;
import org.belltaffmod.bellstaff_tplayers.network.MinigameCompletePacket;
import org.belltaffmod.bellstaff_tplayers.network.ModNetwork;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SleepMinigameScreen extends Screen {

    // barra e fundo
    private static final int BAR_WIDTH = 200;               // largura da barra, px
    private static final int BAR_HEIGHT = 14;               // altura da barra, px
    private static final int BAR_BOTTOM_MARGIN = 40;        // distância do rodapé
    private static final int BAR_FILL_ARGB = 0xFF9B59FF;    // cor da barra
    private static final int BACKGROUND_ARGB = 0xB0000000;  // fundo: alfa + cor
    private static final String BAR_TEXT = "Pressione espaço";

    // acessibilidade
    private static final boolean ENABLE_BLINKS = true;      // piscadas e micro-sonos: telas pretas rápidas
    private static final boolean ENABLE_GLITCH = true;      // a palavra central tremula em runas
    private static final boolean ENABLE_HEARTBEAT = true;   // batimento cardíaco

    // fontes
    private static final boolean USE_CUSTOM_FONT = true;
    private static final float DREAD_FONT_CHANCE = 0.55F;   // chance de um texto usar a fonte própria
    private static final float RUNES_FONT_CHANCE = 0.12F;   // chance de um texto sair em runas
    private static final Style DREAD_STYLE = Style.EMPTY.withFont(
            new ResourceLocation(Bellstaff_tplayers.MODID, "dread_fonts"));
    private static final Style RUNES_STYLE = Style.EMPTY.withFont(
            new ResourceLocation("minecraft", "alt"));      // runas da mesa de encantamento

    // textos flutuantes
    private static final int SPAWN_INTERVAL_TICKS = 12;     // novo texto a cada 0,6 s
    private static final int TEXT_LIFETIME_TICKS = 55;      // cada texto dura ~2,7 s
    private static final int MAX_FLOATING_TEXTS = 10;       // máx. de textos juntos
    private static final float FLOATING_SCALE = 2.0F;       // tamanho dos textos
    private static final float INTRUSION_CHANCE = 0.20F;    // chance de uma frase "de outra fase" aparecer
    private static final float MIN_TILT_DEGREES = 8.0F;     // inclinação mínima
    private static final float MAX_TILT_DEGREES = 40.0F;    // inclinação máxima, mantenha abaixo de 60
    private static final int CENTER_SAFE_HALF_WIDTH = 110;  // área central sem textos, px
    private static final int CENTER_SAFE_HALF_HEIGHT = 45;  // em px

    // fases
    // A tela vai de "ainda lúcido" a "desistiu" conforme a vinheta fecha
    // Apertar Espaço abre a vinheta e traz as frases lúcidas de volta
    private static final float PHASE_DRIFTING_AT = 0.33F;   // fecha acima disso = à deriva
    private static final float PHASE_SURRENDER_AT = 0.66F;  // fecha acima disso = desistiu

    private static final String[] LUCID_TEXTS = {
            "Acorde", "Não durma", "Não desista", "Fique acordado", "Abra os olhos", "Ainda dá tempo"
    };
    private static final String[] DRIFTING_TEXTS = {
            "Seus olhos pesam", "Foi só um piscar", "Quanto tempo se passou?",
            "Você não lembra do último passo", "Está tão silencioso", "Isso já aconteceu antes",
            "Você ouviu isso?", "O chão parece macio"
    };
    private static final String[] SURRENDER_TEXTS = {
            "Já está tudo quieto", "Você já está dormindo", "Não precisa mais lutar",
            "Feche os olhos", "Descanse", "Durma", "Está quase", "Boa noite"
    };
    private static final String[][] PHASE_TEXTS = { LUCID_TEXTS, DRIFTING_TEXTS, SURRENDER_TEXTS };

    private static final String[] CENTER_LUCID = { "Resista", "Acorde", "Fique" };
    private static final String[] CENTER_DRIFTING = { "Cansado", "Pesado", "Piscou?", "Você está Cansado" };
    private static final String[] CENTER_SURRENDER = { "Durma", "Descanse", "Entregue-se", "Boa noite", "Shhh..." };
    private static final String[][] PHASE_CENTER = { CENTER_LUCID, CENTER_DRIFTING, CENTER_SURRENDER };

    private static final int[][] PHASE_COLORS = {           // ALTERE ESTAS CORES AQUI (RGB)
            { 0xFFFFFF, 0xFFD27A },                         // lúcido: branco e âmbar
            { 0xB8A0FF, 0x8A7BC8 },                         // à deriva: lilás
            { 0x8E2A3C, 0x5B3A6B, 0x9A8AA8 }                // desistiu: vinho, roxo apagado, cinza-lilás
    };

    // palavra central
    private static final int CENTER_CHANGE_INTERVAL_TICKS = 55; // troca sozinha a cada ~2,7 s
    private static final float CENTER_SCALE = 4.0F;         // tamanho da palavra
    private static final float PULSE_SPEED = 0.3F;          // velocidade da pulsação
    private static final float PULSE_AMOUNT = 0.08F;        // força da pulsação
    private static final float CENTER_SHAKE_BASE = 1.5F;    // tremor mínimo, px
    private static final float CENTER_SHAKE_EXTRA = 3.0F;   // tremor extra com a tela fechada, px
    private static final float CENTER_SHAKE_ANGLE = 1.5F;   // tremor de rotação, graus
    private static final float GHOST_BASE_PX = 1.5F;        // distância dos "fantasmas" da palavra, px
    private static final float GHOST_EXTRA_PX = 6.0F;       // distância extra com a tela fechada, px
    private static final int GHOST_LEFT_ARGB = 0x66FF2A3A;  // cópia vermelha, à esquerda
    private static final int GHOST_RIGHT_ARGB = 0x6630B8FF; // cópia azul-fria, à direita
    private static final float GLITCH_CHANCE_PER_TICK = 0.02F; // chance de tremular em runas
    private static final int GLITCH_TICKS = 3;              // duração da tremulação

    // palpebras e piscadas
    private static final int EYELID_ARGB = 0xF2050008;      // cor das pálpebras
    private static final float EYELID_MAX_COVER = 0.36F;    // quanto cada pálpebra cobre quando fechada, 0 a 0.5
    private static final int EYELID_SOFT_PX = 28;           // borda esfumada das pálpebras, px
    private static final int BLINK_FIRST_TICKS = 90;        // primeira piscada, ticks
    private static final int BLINK_INTERVAL_MAX_TICKS = 160;// intervalo entre piscadas com a tela aberta
    private static final int BLINK_INTERVAL_MIN_TICKS = 45; // intervalo com a tela fechada
    private static final int BLINK_INTERVAL_JITTER = 30;    // variação aleatória, ticks
    private static final int BLINK_CLOSE_TICKS = 3;         // olhos fechando
    private static final int BLINK_HOLD_TICKS = 2;          // piscada normal fechada
    private static final int BLINK_OPEN_TICKS = 6;          // olhos abrindo
    private static final int MICROSLEEP_HOLD_TICKS = 12;    // micro-sono: fechado por mais tempo
    private static final float MICROSLEEP_CHANCE_BASE = 0.05F;    // chance de a piscada virar micro-sono
    private static final float MICROSLEEP_CHANCE_CLOSURE = 0.50F; // chance extra com a tela fechada

    // batimento cardiaco
    private static final int HEARTBEAT_INTERVAL_OPEN_TICKS = 44;   // intervalo com a tela aberta
    private static final int HEARTBEAT_INTERVAL_CLOSED_TICKS = 16; // intervalo com a tela fechada
    private static final float HEARTBEAT_VOLUME_MIN = 0.15F;       // volume com a tela aberta
    private static final float HEARTBEAT_VOLUME_MAX = 0.85F;       // volume com a tela fechada
    private static final float HEARTBEAT_PITCH_MIN = 0.85F;        // tom com a tela aberta
    private static final float HEARTBEAT_PITCH_MAX = 1.20F;        // tom com a tela fechada

    // vinheta
    private static final int VIGNETTE_RGB = 0x0A0010;               // cor da fumaça
    private static final float VIGNETTE_MAX_ALPHA = 0.88F;          // 0 a 1: o quão escura fica
    private static final float VIGNETTE_CLOSE_SECONDS = 30.0F;      // tempo até fechar por completo
    private static final float VIGNETTE_PROGRESS_OPEN_STRENGTH = 1.0F; // quanto a barra cheia abre
    private static final float VIGNETTE_SMOOTHING = 0.12F;          // 0 a 1: suavidade do movimento
    private static final float VIGNETTE_OPEN_RADIUS = 1.20F;        // abertura quando "aberta"
    private static final float VIGNETTE_CLOSED_RADIUS = 0.22F;      // abertura quando "fechada"
    private static final float VIGNETTE_BAND = 0.60F;               // largura do degradê
    private static final float VIGNETTE_PULSE_RADIUS = 0.06F;       // quanto a abertura pulsa
    private static final float VIGNETTE_PULSE_ALPHA = 0.10F;        // quanto a escuridão pulsa
    private static final float VIGNETTE_PULSE_SPEED_SLOW = 0.12F;   // pulso com a tela aberta
    private static final float VIGNETTE_PULSE_SPEED_FAST = 0.45F;   // pulso com a tela fechada
    private static final float VIGNETTE_SMOKE_AMOUNT = 0.05F;       // irregularidade da borda
    private static final float VIGNETTE_SMOKE_SPEED = 0.05F;        // velocidade da fumaça
    private static final int VIGNETTE_SEGMENTS = 72;                // qualidade da borda
    private static final float VIGNETTE_FAR_RADIUS = 3.0F;          // cobre os cantos da tela

    // estado
    private enum FontKind { DEFAULT, DREAD, RUNES }

    private float progress = 0.0F;        // 0.0 = vazia, 1.0 = cheia
    private boolean spaceHeld = false;    // evita "segurar" Espaço para encher
    private boolean completed = false;    // já avisamos o servidor
    private int ticks = 0;
    private int waitingTicks = 0;
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final RandomSource random = RandomSource.create();

    // vinheta: 0.0 = aberta, 1.0 = fechada
    private float closure = 0.0F;
    private float prevClosure = 0.0F;
    private float pulsePhase = 0.0F;
    private float pulseSpeed = VIGNETTE_PULSE_SPEED_SLOW;

    // palavra central
    private String centerText = CENTER_LUCID[0];
    private int centerTextTimer = 0;
    private int centerGlitchTicks = 0;
    private float shakeX = 0.0F;
    private float shakeY = 0.0F;
    private float shakeAngle = 0.0F;

    // piscadas: blinkAge = -1 quando os olhos estão abertos
    private int blinkAge = -1;
    private int blinkHold = BLINK_HOLD_TICKS;
    private int nextBlinkTick = BLINK_FIRST_TICKS;

    private int heartbeatTimer = 20;

    public SleepMinigameScreen() {
        super(Component.literal("Sleep Minigame"));
    }

    // Impede fechar a tela com ESC e mantém o jogo rodando (multiplayer)
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    // entrada do teclado
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            if (!spaceHeld && !completed) {
                spaceHeld = true;
                progress += 1.0F / BellstaffConstants.MINIGAME_REQUIRED_PRESSES;
                if (progress >= 1.0F) {
                    progress = 1.0F;
                    completed = true;
                    ModNetwork.sendToServer(new MinigameCompletePacket()); // "enchi a barra"
                }
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            spaceHeld = false;
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    // lógica p/tick
    @Override
    public void tick() {
        ticks++;

        if (completed) {
            // Se o servidor não respondeu (ex.: validou "rápido demais"), tenta de novo
            if (++waitingTicks > 100) {
                completed = false;
                progress = 0.0F;
                waitingTicks = 0;
            }
        } else {
            // A barra perde progresso sozinha: é preciso continuar apertando
            float decayPerTick = BellstaffConstants.MINIGAME_DECAY_PRESSES_PER_SECOND
                    / BellstaffConstants.MINIGAME_REQUIRED_PRESSES / 20.0F;
            progress = Math.max(0.0F, progress - decayPerTick);
        }

        // vinheta: fecha com o tempo, abre conforme a barra enche
        float timeFactor = Mth.clamp(ticks / (VIGNETTE_CLOSE_SECONDS * 20.0F), 0.0F, 1.0F);
        float targetClosure = Mth.clamp(
                timeFactor - progress * VIGNETTE_PROGRESS_OPEN_STRENGTH, 0.0F, 1.0F);
        prevClosure = closure;
        closure += (targetClosure - closure) * VIGNETTE_SMOOTHING;
        pulseSpeed = Mth.lerp(closure, VIGNETTE_PULSE_SPEED_SLOW, VIGNETTE_PULSE_SPEED_FAST);
        pulsePhase += pulseSpeed;

        tickBlinks();
        tickHeartbeat();

        // palavra central: troca de tempos em tempos, treme e, às vezes, tremula em runas
        if (++centerTextTimer >= CENTER_CHANGE_INTERVAL_TICKS) {
            centerTextTimer = 0;
            centerText = nextCenterText();
        }
        float shakeAmount = CENTER_SHAKE_BASE + CENTER_SHAKE_EXTRA * closure;
        shakeX = (random.nextFloat() * 2.0F - 1.0F) * shakeAmount;
        shakeY = (random.nextFloat() * 2.0F - 1.0F) * shakeAmount;
        shakeAngle = (random.nextFloat() * 2.0F - 1.0F) * CENTER_SHAKE_ANGLE;

        if (centerGlitchTicks > 0) {
            centerGlitchTicks--;
        } else if (ENABLE_GLITCH && random.nextFloat() < GLITCH_CHANCE_PER_TICK * (0.3F + closure)) {
            centerGlitchTicks = GLITCH_TICKS;
        }

        //  textos flutuantes
        for (FloatingText text : floatingTexts) text.age++;
        floatingTexts.removeIf(text -> text.age >= text.lifetime);
        if (ticks % SPAWN_INTERVAL_TICKS == 0 && floatingTexts.size() < MAX_FLOATING_TEXTS) {
            spawnFloatingText();
        }
    }

    // Piscadas: os olhos fecham, e é nesse escuro que o mundo muda sem você ver
    private void tickBlinks() {
        if (!ENABLE_BLINKS) return;

        if (blinkAge >= 0) {
            blinkAge++;
            if (blinkAge == BLINK_CLOSE_TICKS) {
                onEyesClosed();
            }
            if (blinkAge >= BLINK_CLOSE_TICKS + blinkHold + BLINK_OPEN_TICKS) {
                blinkAge = -1;
                nextBlinkTick = ticks + (int) Mth.lerp(closure, BLINK_INTERVAL_MAX_TICKS, BLINK_INTERVAL_MIN_TICKS)
                        + random.nextInt(Math.max(1, BLINK_INTERVAL_JITTER));
            }
        } else if (ticks >= nextBlinkTick) {
            blinkAge = 0;
            boolean microsleep = random.nextFloat() < MICROSLEEP_CHANCE_BASE + MICROSLEEP_CHANCE_CLOSURE * closure;
            blinkHold = microsleep ? MICROSLEEP_HOLD_TICKS : BLINK_HOLD_TICKS;
        }
    }

    // Chamado no instante em que as pálpebras fecham por completo
    private void onEyesClosed() {
        centerText = nextCenterText();
        centerTextTimer = 0;
        if (blinkHold == MICROSLEEP_HOLD_TICKS) {
            floatingTexts.clear();   // no micro-sono, tudo some e volta diferente
        }
    }

    // Batimento: mais rápido, mais alto e mais agudo conforme a tela fecha
    private void tickHeartbeat() {
        if (!ENABLE_HEARTBEAT) return;
        if (--heartbeatTimer > 0) return;

        float volume = Mth.lerp(closure, HEARTBEAT_VOLUME_MIN, HEARTBEAT_VOLUME_MAX);
        float pitch = Mth.lerp(closure, HEARTBEAT_PITCH_MIN, HEARTBEAT_PITCH_MAX);
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, pitch, volume));
        heartbeatTimer = (int) Mth.lerp(closure, HEARTBEAT_INTERVAL_OPEN_TICKS, HEARTBEAT_INTERVAL_CLOSED_TICKS);
    }

    // 0 = lúcido, 1 = à deriva, 2 = rendido
    private int currentPhase() {
        if (closure >= PHASE_SURRENDER_AT) return 2;
        if (closure >= PHASE_DRIFTING_AT) return 1;
        return 0;
    }

    // Sorteia a próxima palavra central da fase atual, sem repetir a anterior
    private String nextCenterText() {
        String[] pool = PHASE_CENTER[currentPhase()];
        String next;
        int guard = 0;
        do {
            next = pool[random.nextInt(pool.length)];
        } while (next.equals(centerText) && pool.length > 1 && ++guard < 8);
        return next;
    }

    // Inclinação aleatória entre MIN e MAX, para a esquerda ou para a direita
    private float randomTilt() {
        float magnitude = MIN_TILT_DEGREES + random.nextFloat() * (MAX_TILT_DEGREES - MIN_TILT_DEGREES);
        return random.nextBoolean() ? magnitude : -magnitude;
    }

    private FontKind rollFontKind() {
        float roll = random.nextFloat();
        if (roll < RUNES_FONT_CHANCE) return FontKind.RUNES;
        if (roll < RUNES_FONT_CHANCE + DREAD_FONT_CHANCE) return FontKind.DREAD;
        return FontKind.DEFAULT;
    }

    private void spawnFloatingText() {
        int phase = currentPhase();
        if (random.nextFloat() < INTRUSION_CHANCE) {
            phase = random.nextInt(PHASE_TEXTS.length);   // uma frase que "não devia estar aqui"
        }
        String[] pool = PHASE_TEXTS[phase];
        String text = pool[random.nextInt(pool.length)];
        int[] colors = PHASE_COLORS[phase];
        int color = colors[random.nextInt(colors.length)];

        FontKind kind = rollFontKind();
        if (kind == FontKind.RUNES) {
            String runes = runic(text);
            if (runes.isEmpty()) kind = FontKind.DEFAULT; else text = runes;
        }

        // Sorteia a posição, tentando evitar a área da palavra central
        int x = this.width / 2;
        int y = this.height / 3;
        for (int attempt = 0; attempt < 8; attempt++) {
            x = 60 + random.nextInt(Math.max(1, this.width - 120));
            y = 30 + random.nextInt(Math.max(1, this.height - 140));
            boolean insideCenter = Math.abs(x - this.width / 2) < CENTER_SAFE_HALF_WIDTH
                    && Math.abs(y - this.height / 2) < CENTER_SAFE_HALF_HEIGHT;
            if (!insideCenter) break;
        }
        floatingTexts.add(new FloatingText(text, kind, x, y, color, TEXT_LIFETIME_TICKS, randomTilt()));
    }

    // A fonte de runas só tem letras: tira acentos e símbolos antes de usá-la
    private static String runic(String text) {
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z ]", "").trim();
    }

    private static Style styleFor(FontKind kind) {
        switch (kind) {
            case DREAD: return USE_CUSTOM_FONT ? DREAD_STYLE : Style.EMPTY;
            case RUNES: return RUNES_STYLE;
            default:    return Style.EMPTY;
        }
    }

    // desenho
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float closureNow = Mth.lerp(partialTick, prevClosure, closure);

        // 1) Fundo escuro
        g.fill(0, 0, this.width, this.height, BACKGROUND_ARGB);

        // 2) Textos flutuantes: surgem, sobem um pouco e somem (fade), inclinados
        for (FloatingText t : floatingTexts) {
            float life = (t.age + partialTick) / t.lifetime;           // 0.0 -> 1.0
            float fade = (float) Math.sin(life * Math.PI);             // 0 -> 1 -> 0
            int alpha = Mth.clamp((int) (fade * 255.0F), 8, 255);      // alfa mínimo 8
            float y = t.y - life * 20.0F;                              // sobe 20 px
            drawText(g, t.text, t.kind, t.x, y, FLOATING_SCALE, t.angle, (alpha << 24) | t.rgb);
        }

        // 3) Vinheta esfumaçada (efeito "túnel")
        drawVignette(g, partialTick);

        // 4) Palavra central: dois "fantasmas" coloridos, a palavra em si, pulsando e tremendo
        drawCenterWord(g, closureNow, partialTick);

        // 5) Pálpebras: fecham com o cansaço e piscam
        drawEyelids(g, closureNow, partialTick);

        // 6) Barra de Espaço, na parte inferior (sempre visível, por cima de tudo)
        int barX = (this.width - BAR_WIDTH) / 2;
        int barY = this.height - BAR_BOTTOM_MARGIN;
        g.drawCenteredString(this.font, BAR_TEXT, this.width / 2, barY - 14, 0xFFFFFF);
        g.fill(barX - 2, barY - 2, barX + BAR_WIDTH + 2, barY + BAR_HEIGHT + 2, 0xFFFFFFFF); // borda
        g.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF101010);                 // fundo
        int filled = (int) (BAR_WIDTH * Mth.clamp(progress, 0.0F, 1.0F));
        g.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, BAR_FILL_ARGB);                 // preenchimento

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawCenterWord(GuiGraphics g, float closureNow, float partialTick) {
        boolean glitching = centerGlitchTicks > 0;
        String shown = centerText;
        FontKind kind = FontKind.DREAD;
        if (glitching) {
            String runes = runic(centerText);
            if (!runes.isEmpty()) {
                shown = runes;
                kind = FontKind.RUNES;
            }
        }

        float pulse = 1.0F + PULSE_AMOUNT * (float) Math.sin((ticks + partialTick) * PULSE_SPEED);
        Component measure = Component.literal(shown).withStyle(styleFor(kind));
        float maxScale = (this.width * 0.85F) / Math.max(1, this.font.width(measure));   // cabe na tela
        float scale = Math.min(CENTER_SCALE, maxScale) * pulse;

        float cx = this.width / 2.0F + shakeX;
        float cy = this.height / 2.0F + shakeY;
        float ghost = GHOST_BASE_PX + GHOST_EXTRA_PX * closureNow;

        drawText(g, shown, kind, cx - ghost, cy, scale, shakeAngle, GHOST_LEFT_ARGB);
        drawText(g, shown, kind, cx + ghost, cy, scale, shakeAngle, GHOST_RIGHT_ARGB);
        drawText(g, shown, kind, cx, cy, scale, shakeAngle, 0xFFFFFFFF);
    }

    // Duas pálpebras (de cima e de baixo). O "blink" fecha as duas até se encontrarem.
    private void drawEyelids(GuiGraphics g, float closureNow, float partialTick) {
        float blink = smooth(blinkAmount(partialTick));
        float base = closureNow * EYELID_MAX_COVER;
        float cover = base + blink * (0.5F - base);
        int lid = (int) Math.ceil(cover * this.height);
        if (lid <= 0) return;

        int solid = EYELID_ARGB;
        int clear = EYELID_ARGB & 0x00FFFFFF;

        // de cima
        g.fill(0, 0, this.width, lid, solid);
        g.fillGradient(0, lid, this.width, lid + EYELID_SOFT_PX, solid, clear);
        // de baixo
        g.fill(0, this.height - lid, this.width, this.height, solid);
        g.fillGradient(0, this.height - lid - EYELID_SOFT_PX, this.width, this.height - lid, clear, solid);
    }

    // 0 = olhos abertos, 1 = totalmente fechados
    private float blinkAmount(float partialTick) {
        if (blinkAge < 0) return 0.0F;
        float age = blinkAge + partialTick;
        float closeEnd = BLINK_CLOSE_TICKS;
        float holdEnd = closeEnd + blinkHold;
        if (age < closeEnd) return age / closeEnd;
        if (age < holdEnd) return 1.0F;
        return Mth.clamp(1.0F - (age - holdEnd) / BLINK_OPEN_TICKS, 0.0F, 1.0F);
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    // Desenha texto centralizado em (x, y), com fonte, escala e rotação (em graus)
    private void drawText(GuiGraphics g, String text, FontKind kind, float x, float y,
                          float scale, float angleDegrees, int argb) {
        Component component = Component.literal(text).withStyle(styleFor(kind));
        PoseStack pose = g.pose();
        pose.pushPose();                                       // guarda o estado
        pose.translate(x, y, 0.0F);                            // move o ponto de desenho
        pose.mulPose(Axis.ZP.rotationDegrees(angleDegrees));   // gira em torno do centro do texto
        pose.scale(scale, scale, 1.0F);                        // amplia
        g.drawString(this.font, component, -this.font.width(component) / 2,
                -this.font.lineHeight / 2, argb, false);       // false = sem sombra
        pose.popPose();                                        // restaura o estado
    }

    // Vinheta esfumaçada
    private void drawVignette(GuiGraphics g, float partialTick) {
        float closureNow = Mth.lerp(partialTick, prevClosure, closure);
        float pulse = Mth.sin(pulsePhase + partialTick * pulseSpeed);

        // Raio da parte transparente (menor = tela mais fechada), pulsando
        float inner = Mth.lerp(closureNow, VIGNETTE_OPEN_RADIUS, VIGNETTE_CLOSED_RADIUS)
                * (1.0F + VIGNETTE_PULSE_RADIUS * pulse);
        float alpha = Mth.clamp(VIGNETTE_MAX_ALPHA * (1.0F + VIGNETTE_PULSE_ALPHA * pulse), 0.0F, 1.0F);
        float smokeTime = (ticks + partialTick) * VIGNETTE_SMOKE_SPEED * 20.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Duas camadas com fases diferentes dão a sensação de fumaça em movimento
        drawVignetteLayer(g, inner, alpha, smokeTime, 0.0F);
        drawVignetteLayer(g, inner * 1.25F, alpha * 0.5F, smokeTime, 2.7F);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // Uma camada: um anel elíptico que vai de transparente (dentro) a escuro (fora),
    // com a borda ondulando, e depois uma área escura até além dos cantos da tela
    private void drawVignetteLayer(GuiGraphics g, float innerRadius, float maxAlpha,
                                   float smokeTime, float phaseOffset) {
        Matrix4f matrix = g.pose().last().pose();
        float halfW = this.width / 2.0F;
        float halfH = this.height / 2.0F;
        int red = (VIGNETTE_RGB >> 16) & 0xFF;
        int green = (VIGNETTE_RGB >> 8) & 0xFF;
        int blue = VIGNETTE_RGB & 0xFF;
        int edgeAlpha = (int) (Mth.clamp(maxAlpha, 0.0F, 1.0F) * 255.0F);
        float outerRadius = innerRadius + VIGNETTE_BAND;

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();

        // Anel do degradê: alfa 0 na borda de dentro, alfa máximo na borda de fora
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= VIGNETTE_SEGMENTS; i++) {
            float angle = (float) (Math.PI * 2.0 * i / VIGNETTE_SEGMENTS);
            float wobble = smokeWobble(angle, smokeTime + phaseOffset);
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            buffer.vertex(matrix,
                            halfW + cos * innerRadius * wobble * halfW,
                            halfH + sin * innerRadius * wobble * halfH, 0.0F)
                    .color(red, green, blue, 0).endVertex();
            buffer.vertex(matrix,
                            halfW + cos * outerRadius * wobble * halfW,
                            halfH + sin * outerRadius * wobble * halfH, 0.0F)
                    .color(red, green, blue, edgeAlpha).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());

        // Área escura constante: da borda de fora do degradê até além dos cantos
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= VIGNETTE_SEGMENTS; i++) {
            float angle = (float) (Math.PI * 2.0 * i / VIGNETTE_SEGMENTS);
            float wobble = smokeWobble(angle, smokeTime + phaseOffset);
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            buffer.vertex(matrix,
                            halfW + cos * outerRadius * wobble * halfW,
                            halfH + sin * outerRadius * wobble * halfH, 0.0F)
                    .color(red, green, blue, edgeAlpha).endVertex();
            buffer.vertex(matrix,
                            halfW + cos * VIGNETTE_FAR_RADIUS * wobble * halfW,
                            halfH + sin * VIGNETTE_FAR_RADIUS * wobble * halfH, 0.0F)
                    .color(red, green, blue, edgeAlpha).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());
    }

    // Irregularidade da borda: soma de ondas que se movem com o tempo (parecer fumaça)
    private static float smokeWobble(float angle, float t) {
        return 1.0F + VIGNETTE_SMOKE_AMOUNT * (
                Mth.sin(angle * 3.0F + t * 0.9F)
                        + 0.6F * Mth.sin(angle * 5.0F - t * 1.3F)
                        + 0.4F * Mth.sin(angle * 8.0F + t * 0.5F));
    }

    // Dados de um texto flutuante
    private static class FloatingText {
        final String text;
        final FontKind kind;
        final int x, y, rgb, lifetime;
        final float angle;
        int age = 0;

        FloatingText(String text, FontKind kind, int x, int y, int rgb, int lifetime, float angle) {
            this.text = text;
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.rgb = rgb;
            this.lifetime = lifetime;
            this.angle = angle;
        }
    }
}