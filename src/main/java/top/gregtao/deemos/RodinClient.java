package top.gregtao.deemos;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

public class RodinClient implements ClientModInitializer {
	public static Logger LOGGER = LogManager.getLogger("dmodel");

	@Override
	public void onInitializeClient() {
		KeyBinding binding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"dmodel.hotkey.screen",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_U,
				"dmodel.hotkeys"
		));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (binding.wasPressed()) {
				client.openScreen(new ModelerScreen());
			}
		});
		EntityRendererRegistry.INSTANCE.register(RodinServer.MODELER_TNT_ENTITY,
				(manager, context) -> new ModelerTNTRenderer(manager));
		RodinCraftConfig.readApiKey();
	}
}