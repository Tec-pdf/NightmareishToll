package org.belltaffmod.bellstaff_tplayers.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import org.belltaffmod.bellstaff_tplayers.BellstaffConstants;
import org.belltaffmod.bellstaff_tplayers.network.MinigameCompletePacket;
import org.belltaffmod.bellstaff_tplayers.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class SleepMinigameScreen extends Screen {

    // Barra e fundo
    private static final int BAR_WIDTH = 200;               // largura da barra em px
    private static final int BAR_HEIGHT = 14;               // altura da barra em px
    private static final int BAR_BOTTOM_MARGIN = 40;        // distância do rodapé
    private static final int BAR_FILL_ARGB = 0xFF9B59FF;    // cor da barra
    private static final int BACKGROUND_ARGB = 0x90000000;  // fundo: alfa + cor
    private static final String BAR_TEXT = "Pressione espaço";   // texto acima da barra

    // Textos flutuantes
    private static final int SPAWN_INTERVAL_TICKS = 10;     // intervalo, novo texto a cada 0,5 s
    private static final int TEXT_LIFETIME_TICKS = 50;      // duração de cada texto, 2,5 s
    private static final int MAX_FLOATING_TEXTS = 12;       // máx. de textos juntos
    private static final float FLOATING_SCALE = 2.0F;       // tamanho dos textos
    private static final float DISCOURAGING_CHANCE = 0.65F; // 65% das falas são desencorajadoras

    private static final float MIN_TILT_DEGREES = 8.0F;     // inclinação mínima dos textos
    private static final float MAX_TILT_DEGREES = 40.0F;    // inclinação máxima, manter abaixo de 60

    // Área central onde os textos flutuantes não aparecem para não cobrir a palavra do meio
    private static final int CENTER_SAFE_HALF_WIDTH = 110;  // largura da área segura em px
    private static final int CENTER_SAFE_HALF_HEIGHT = 45;  // altura da área segura em px

    private static final String[] ENCOURAGING_TEXTS = {     // textos encorajadores
            "Acorde", "Não Durma", "Não desista"
    };
    private static final int[] ENCOURAGING_COLORS = {       // cor dos textos em rgb
            0xFFFFFF, 0xFFD27A
    };
    private static final String[] DISCOURAGING_TEXTS = {    // textos desencorajadores
            "Desista", "Durma", "Descanse", "É inútil", "Não adianta",
            "Ninguém virá", "Você está cansado", "Feche os olhos", "Entregue-se",
            "Pare de lutar", "Está tão escuro...", "Só mais um pouco", "Não há saída",
            "Você já perdeu"
    };
    private static final int[] DISCOURAGING_COLORS = {      // cores dos textos em rgb
            0xB8A0FF, 0xFF3030, 0x8A7BC8
    };

    // Palavra central
    private static final String[] CENTER_TEXTS = {          // possiveis palavras
            "Resista", "Durma", "Desista", "Você está Cansado",
            "Descanse", "Entregue-se", "Não lute", "Acorde"
    };
    private static final int CENTER_CHANGE_INTERVAL_TICKS = 50; // palavra central troca a cada 2,5 s
    private static final float CENTER_SCALE = 4.0F;         // tamanho da palavra
    private static final float PULSE_SPEED = 0.3F;          // velocidade da pulsação
    private static final float PULSE_AMOUNT = 0.08F;        // força da pulsação
    private static final float CENTER_SHAKE_BASE = 1.5F;    // tremor mínimo em px
    private static final float CENTER_SHAKE_EXTRA = 3.0F;   // tremor extra com a tela fechada em px
    private static final float CENTER_SHAKE_ANGLE = 1.5F;   // tremor de rotação em graus

    // Vinheta
    private static final int VIGNETTE_RGB = 0x0A0010;               // cor da fumaça
    private static final float VIGNETTE_MAX_ALPHA = 0.99F;          // 0 a 1: o quão escura fica
    private static final float VIGNETTE_CLOSE_SECONDS = 15.0F;      // tempo até fechar por completo
    private static final float VIGNETTE_PROGRESS_OPEN_STRENGTH = 0.7F; // quanto a barra cheia abre
    private static final float VIGNETTE_SMOOTHING = 0.12F;          // 0 a 1: suavidade do movimento
    private static final float VIGNETTE_OPEN_RADIUS = 1.20F;        // abertura quando "aberta"
    private static final float VIGNETTE_CLOSED_RADIUS = 1.20F;      // abertura quando "fechada"
    private static final float VIGNETTE_BAND = 0.40F;               // largura do degradê
    private static final float VIGNETTE_PULSE_RADIUS = 0.06F;       // quanto a abertura pulsa
    private static final float VIGNETTE_PULSE_ALPHA = 0.10F;        // quanto a escuridão pulsa
    private static final float VIGNETTE_PULSE_SPEED_SLOW = 0.12F;   // pulso com a tela aberta
    private static final float VIGNETTE_PULSE_SPEED_FAST = 0.45F;   // pulso com a tela fechada
    private static final float VIGNETTE_SMOKE_AMOUNT = 0.05F;       // irregularidade da borda
    private static final float VIGNETTE_SMOKE_SPEED = 0.05F;        // velocidade da fumaça
    private static final int VIGNETTE_SEGMENTS = 72;                // qualidade da borda
    private static final float VIGNETTE_FAR_RADIUS = 3.0F;          // cobre os cantos da tela (não precisa mexer)

    // Estado da barra
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
    private String centerText = CENTER_TEXTS[0];
    private int centerTextTimer = 0;
    private float shakeX = 0.0F;
    private float shakeY = 0.0F;
    private float shakeAngle = 0.0F;

    public SleepMinigameScreen() {
        super(Component.literal("Sleep Minigame"));
    }

    // Impede fechar a tela com ESC e mantém o jogo rodando (multiplayer).
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    // Teclado
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

    // lógica por tick
    @Override
    public void tick() {
        ticks++;

        if (completed) {
            // Se o servidor não respondeu (ex.: validou "rápido demais"), tenta de novo.
            if (++waitingTicks > 100) {
                completed = false;
                progress = 0.0F;
                waitingTicks = 0;
            }
        } else {
            // A barra perde progresso sozinha: é preciso continuar apertando.
            float decayPerTick = BellstaffConstants.MINIGAME_DECAY_PRESSES_PER_SECOND
                    / BellstaffConstants.MINIGAME_REQUIRED_PRESSES / 20.0F;
            progress = Math.max(0.0F, progress - decayPerTick);
        }

        // vinheta: fecha com o tempo, abre conforme a barra enche
        float timeFactor = Mth.clamp(ticks / (VIGNETTE_CLOSE_SECONDS * 20.0F), 0.0F, 1.0F);
        float targetClosure = Mth.clamp(
                timeFactor - progress * VIGNETTE_PROGRESS_OPEN_STRENGTH, 0.0F, 1.0F);
        prevClosure = closure;
        closure += (targetClosure - closure) * VIGNETTE_SMOOTHING;   // movimento suave
        pulseSpeed = Mth.lerp(closure, VIGNETTE_PULSE_SPEED_SLOW, VIGNETTE_PULSE_SPEED_FAST);
        pulsePhase += pulseSpeed;                                     // quanto mais fechada, mais acelerado

        // palavra central: troca de tempos em tempos e treme
        if (++centerTextTimer >= CENTER_CHANGE_INTERVAL_TICKS) {
            centerTextTimer = 0;
            centerText = nextCenterText();
        }
        float shakeAmount = CENTER_SHAKE_BASE + CENTER_SHAKE_EXTRA * closure;
        shakeX = (random.nextFloat() * 2.0F - 1.0F) * shakeAmount;
        shakeY = (random.nextFloat() * 2.0F - 1.0F) * shakeAmount;
        shakeAngle = (random.nextFloat() * 2.0F - 1.0F) * CENTER_SHAKE_ANGLE;

        // textos flutuantes
        for (FloatingText text : floatingTexts) text.age++;
        floatingTexts.removeIf(text -> text.age >= text.lifetime);
        if (ticks % SPAWN_INTERVAL_TICKS == 0 && floatingTexts.size() < MAX_FLOATING_TEXTS) {
            spawnFloatingText();
        }
    }

    // sorteia a próxima palavra central, sem repetir a atual
    private String nextCenterText() {
        String next;
        do {
            next = CENTER_TEXTS[random.nextInt(CENTER_TEXTS.length)];
        } while (next.equals(centerText) && CENTER_TEXTS.length > 1);
        return next;
    }

    // inclinação aleatória entre MIN e MAX, para a esquerda ou para a direita
    private float randomTilt() {
        float magnitude = MIN_TILT_DEGREES + random.nextFloat() * (MAX_TILT_DEGREES - MIN_TILT_DEGREES);
        return random.nextBoolean() ? magnitude : -magnitude;
    }

    private void spawnFloatingText() {
        boolean discouraging = random.nextFloat() < DISCOURAGING_CHANCE;
        String[] pool = discouraging ? DISCOURAGING_TEXTS : ENCOURAGING_TEXTS;
        int[] colors = discouraging ? DISCOURAGING_COLORS : ENCOURAGING_COLORS;
        String text = pool[random.nextInt(pool.length)];
        int color = colors[random.nextInt(colors.length)];

        // sorteia a posição, tentando evitar a área da palavra central
        int x = this.width / 2;
        int y = this.height / 3;
        for (int attempt = 0; attempt < 8; attempt++) {
            x = 60 + random.nextInt(Math.max(1, this.width - 120));
            y = 30 + random.nextInt(Math.max(1, this.height - 140));
            boolean insideCenter = Math.abs(x - this.width / 2) < CENTER_SAFE_HALF_WIDTH
                    && Math.abs(y - this.height / 2) < CENTER_SAFE_HALF_HEIGHT;
            if (!insideCenter) break;
        }
        floatingTexts.add(new FloatingText(text, x, y, color, TEXT_LIFETIME_TICKS, randomTilt()));
    }

    // Desenho
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 1) Fundo escuro
        g.fill(0, 0, this.width, this.height, BACKGROUND_ARGB);

        // 2) Textos flutuantes: surgem, sobem um pouco e somem (fade), inclinados
        for (FloatingText t : floatingTexts) {
            float life = (t.age + partialTick) / t.lifetime;           // 0.0 -> 1.0
            float fade = (float) Math.sin(life * Math.PI);             // 0 -> 1 -> 0
            int alpha = Mth.clamp((int) (fade * 255.0F), 8, 255);      // alfa mínimo 8
            float y = t.y - life * 20.0F;                              // sobe 20 px
            drawText(g, t.text, t.x, y, FLOATING_SCALE, t.angle, (alpha << 24) | t.rgb);
        }

        // 3) Vinheta por cima dos textos flutuantes (efeito "túnel").
        //    Para os textos ficarem POR CIMA da vinheta, mova esta linha para antes do bloco 2.
        drawVignette(g, partialTick);

        // 4) Palavra central: pulsa, treme e troca
        float pulse = 1.0F + PULSE_AMOUNT * (float) Math.sin((ticks + partialTick) * PULSE_SPEED);
        float maxScale = (this.width * 0.85F) / Math.max(1, this.font.width(centerText)); // cabe na tela
        float scale = Math.min(CENTER_SCALE, maxScale) * pulse;
        drawText(g, centerText, this.width / 2.0F + shakeX, this.height / 2.0F + shakeY,
                scale, shakeAngle, 0xFFFFFFFF);

        // 5) Barra de Espaço, na parte inferior (sempre visível, por cima de tudo)
        int barX = (this.width - BAR_WIDTH) / 2;
        int barY = this.height - BAR_BOTTOM_MARGIN;
        g.drawCenteredString(this.font, BAR_TEXT, this.width / 2, barY - 14, 0xFFFFFF);
        g.fill(barX - 2, barY - 2, barX + BAR_WIDTH + 2, barY + BAR_HEIGHT + 2, 0xFFFFFFFF); // borda
        g.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF101010);                 // fundo
        int filled = (int) (BAR_WIDTH * Mth.clamp(progress, 0.0F, 1.0F));
        g.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, BAR_FILL_ARGB);                 // preenchimento

        super.render(g, mouseX, mouseY, partialTick);
    }

    // Desenha texto centralizado em (x, y), com escala e rotação (em graus).
    private void drawText(GuiGraphics g, String text, float x, float y,
                          float scale, float angleDegrees, int argb) {
        PoseStack pose = g.pose();
        pose.pushPose();                                       // guarda o estado
        pose.translate(x, y, 0.0F);                            // move o ponto de desenho
        pose.mulPose(Axis.ZP.rotationDegrees(angleDegrees));   // gira em torno do centro do texto
        pose.scale(scale, scale, 1.0F);                        // amplia
        g.drawCenteredString(this.font, text, 0, -this.font.lineHeight / 2, argb);
        pose.popPose();                                        // restaura o estado
    }

    // vinheta esfumaçada
    private void drawVignette(GuiGraphics g, float partialTick) {
        float closureNow = Mth.lerp(partialTick, prevClosure, closure);
        float pulse = Mth.sin(pulsePhase + partialTick * pulseSpeed);

        // raio da parte transparente (menor = tela mais fechada), pulsando
        float inner = Mth.lerp(closureNow, VIGNETTE_OPEN_RADIUS, VIGNETTE_CLOSED_RADIUS)
                * (1.0F + VIGNETTE_PULSE_RADIUS * pulse);
        float alpha = Mth.clamp(VIGNETTE_MAX_ALPHA * (1.0F + VIGNETTE_PULSE_ALPHA * pulse), 0.0F, 1.0F);
        float smokeTime = (ticks + partialTick) * VIGNETTE_SMOKE_SPEED * 20.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // duas camadas com fases diferentes dão a sensação de fumaça em movimento
        drawVignetteLayer(g, inner, alpha, smokeTime, 0.0F);
        drawVignetteLayer(g, inner * 1.25F, alpha * 0.5F, smokeTime, 2.7F);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // uma camada: um anel elíptico que vai de transparente (dentro) a escuro (fora),
    // com a borda ondulando, e depois uma área escura até além dos cantos da tela.
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

        // anel do degradê: alfa 0 na borda de dentro, alfa máximo na borda de fora
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

        // área escura constante: da borda de fora do degradê até além dos cantos
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

    // irregularidade da borda: soma de ondas que se movem com o tempo (parece fumaça).
    private static float smokeWobble(float angle, float t) {
        return 1.0F + VIGNETTE_SMOKE_AMOUNT * (
                Mth.sin(angle * 3.0F + t * 0.9F)
                        + 0.6F * Mth.sin(angle * 5.0F - t * 1.3F)
                        + 0.4F * Mth.sin(angle * 8.0F + t * 0.5F));
    }

    // dados de um texto flutuante.
    private static class FloatingText {
        final String text;
        final int x, y, rgb, lifetime;
        final float angle;
        int age = 0;

        FloatingText(String text, int x, int y, int rgb, int lifetime, float angle) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.rgb = rgb;
            this.lifetime = lifetime;
            this.angle = angle;
        }
    }
}