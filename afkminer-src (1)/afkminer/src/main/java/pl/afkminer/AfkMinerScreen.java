package pl.afkminer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class AfkMinerScreen extends Screen {

    public AfkMinerScreen() {
        super(Text.literal("AFK Miner"));
    }

    @Override
    protected void init() {
        int w = 220, h = 20;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 30;

        addDrawableChild(new ToggleSlider(x, y, w, h));
        addDrawableChild(new DelaySlider(x, y + 30, w, h));
        addDrawableChild(ButtonWidget.builder(Text.literal("Zamknij"), b -> close())
                .dimensions(x, y + 65, w, h).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§lAFK Miner"), this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7Przesuń suwak w prawo, aby włączyć"), this.width / 2, this.height / 2 - 47, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Suwak ON/OFF: w prawo = włączone, w lewo = wyłączone. Po puszczeniu "zatrzaskuje się". */
    private static class ToggleSlider extends SliderWidget {
        ToggleSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Text.empty(), AfkMinerClient.isEnabled() ? 1.0 : 0.0);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            boolean on = this.value >= 0.5;
            setMessage(Text.literal("AFK kopanie: " + (on ? "§aWŁĄCZONE" : "§cWYŁĄCZONE")));
        }

        @Override
        protected void applyValue() {
            AfkMinerClient.setEnabled(this.value >= 0.5);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            this.value = this.value >= 0.5 ? 1.0 : 0.0;
            updateMessage();
            super.onRelease(mouseX, mouseY);
        }
    }

    /** Suwak opóźnienia wpisania kodu: 0.5 – 5.0 s. */
    private static class DelaySlider extends SliderWidget {
        DelaySlider(int x, int y, int w, int h) {
            super(x, y, w, h, Text.empty(), (AfkMinerClient.delaySeconds - 0.5) / 4.5);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Opóźnienie wpisania kodu: " + AfkMinerClient.delaySeconds + " s"));
        }

        @Override
        protected void applyValue() {
            AfkMinerClient.delaySeconds = Math.round((0.5 + this.value * 4.5) * 2.0) / 2.0;
        }
    }
}
