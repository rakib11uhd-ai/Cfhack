package cfpredict;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;

public class CfPredict implements ClientModInitializer {

    static final List<String> seq = new ArrayList<>();
    static final int GREEN = 0xFF22C55E, RED = 0xFFEF4444, GRAY = 0xFF6B7280;
    static final int BG = 0xCC18181B;

    @Override
    public void onInitializeClient() {
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            String s = msg.getString().replaceAll("§.", "").toLowerCase();
            if (s.isEmpty()) return;
            boolean w = s.matches(".*(you|your).{0,25}(won|win).*") || s.contains("won the coinflip");
            boolean l = s.matches(".*(you|your).{0,25}(lost|lose).*") || s.contains("lost the coinflip");
            if (w && !l) push("W");
            else if (l && !w) push("L");
        });

        HudRenderCallback.EVENT.register((ctx, delta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) return;
            render(ctx, mc);
        });
    }

    static void push(String v) {
        seq.add(v);
        if (seq.size() > 40) seq.remove(0);
    }

    static int[] detect() {
        int n = seq.size();
        for (int p = 1; p <= 5; p++) {
            if (n < p * 2) continue;
            String a = String.join("", seq.subList(n - p * 2, n - p));
            String b = String.join("", seq.subList(n - p, n));
            if (!a.equals(b)) continue;
            int reps = 0;
            for (int i = n - p; i >= 0; i -= p) {
                if (String.join("", seq.subList(i, i + p)).equals(b)) reps++;
                else break;
            }
            return new int[]{p, Math.min(100, reps * 25)};
        }
        return null;
    }

    static void render(DrawContext ctx, MinecraftClient mc) {
        int sw = mc.getWindow().getScaledWidth();
        int x = sw / 2 - 55, y = 34;

        int[] hit = detect();
        String label = "...";
        int col = GRAY;
        int conf = 0;

        if (hit != null) {
            conf = hit[1];
            boolean bet = "W".equals(seq.get(seq.size() - hit[0]));
            label = bet ? "BET" : "SKIP";
            col = bet ? GREEN : RED;
        }

        ctx.fill(x, y, x + 110, y + 34, BG);
        ctx.fill(x, y, x + 110, y + 2, col);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, label, sw / 2, y + 8, col);
        ctx.fill(x + 4, y + 27, x + 106, y + 30, 0xFF27272A);
        ctx.fill(x + 4, y + 27, x + 4 + (102 * conf / 100), y + 30, col);
        ctx.drawTextWithShadow(mc.textRenderer, seq.size() + "f", x + 4, y + 3, GRAY);
    }
}
