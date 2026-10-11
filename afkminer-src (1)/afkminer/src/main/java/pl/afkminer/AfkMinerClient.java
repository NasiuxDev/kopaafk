package pl.afkminer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AfkMinerClient implements ClientModInitializer {

    // Lapie np. "/kopafk 1uc583" albo "/kopanieafk 1uc583"
    private static final Pattern CODE_PATTERN =
            Pattern.compile("/(kopafk|kopanieafk)\\s+([A-Za-z0-9]{3,16})", Pattern.CASE_INSENSITIVE);

    private static KeyBinding toggleKey;
    private static KeyBinding guiKey;
    private static boolean enabled = false;

    /** Opóźnienie wpisania kodu w sekundach (ustawiane w GUI). */
    public static double delaySeconds = 1.0;

    private static String pendingCommand = null;
    private static int pendingDelayTicks = 0;

    private static String lastCode = "";
    private static long lastCodeTime = 0;

    private static final Random random = new Random();

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!value) {
            mc.options.attackKey.setPressed(false);
            pendingCommand = null;
        }
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(
                    "§a[AFK Miner] " + (value ? "WŁĄCZONY" : "WYŁĄCZONY")), true);
        }
    }

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.afkminer.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.afkminer"));
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.afkminer.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.afkminer"));

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) handleMessage(message.getString());
        });
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, ts) ->
                handleMessage(message.getString()));

        ClientTickEvents.END_CLIENT_TICK.register(AfkMinerClient::onTick);
    }

    private static void handleMessage(String raw) {
        if (!enabled) return;

        Matcher m = CODE_PATTERN.matcher(raw);
        if (!m.find()) return;

        String cmd = m.group(1).toLowerCase();
        String code = m.group(2);

        long now = System.currentTimeMillis();
        if (code.equals(lastCode) && now - lastCodeTime < 5000) return;
        lastCode = code;
        lastCodeTime = now;

        pendingCommand = cmd + " " + code;
        pendingDelayTicks = (int) (delaySeconds * 20) + random.nextInt(10);
    }

    private static void onTick(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) return;

        while (guiKey.wasPressed()) {
            client.setScreen(new AfkMinerScreen());
        }
        while (toggleKey.wasPressed()) {
            setEnabled(!enabled);
        }

        if (!enabled) return;

        if (pendingCommand != null && --pendingDelayTicks <= 0) {
            client.getNetworkHandler().sendChatCommand(pendingCommand);
            client.player.sendMessage(Text.literal("§a[AFK Miner] Wysłano: /" + pendingCommand), false);
            pendingCommand = null;
        }

        // kopanie tylko gdy nie ma otwartego GUI
        client.options.attackKey.setPressed(client.currentScreen == null);
        client.options.pauseOnLostFocus = false;
    }
}
