package us.potatoboy.resourcereload;

import com.google.common.base.Strings;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.network.packet.s2c.play.ResourcePackSendS2CPacket;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.dedicated.ServerPropertiesHandler;
import net.minecraft.server.dedicated.ServerPropertiesLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourceReload implements ModInitializer {
	public static Logger LOGGER = LogManager.getLogger();

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
				@Override
				public Identifier getFabricId() {
					return new Identifier("resource-reload", "resource-pack");
				}

				@Override
				public void reload(ResourceManager manager) {
					Path path = Paths.get("server.properties");
					ServerPropertiesLoader loader = new ServerPropertiesLoader(path);
					ServerPropertiesHandler properties = loader.getPropertiesHandler();

					if (!properties.resourcePack.equals(server.getResourcePackUrl()) || !properties.resourcePackSha1.equals(server.getResourcePackHash())) {
						server.setResourcePack(properties.resourcePack, properties.resourcePackSha1);

						server.getPlayerManager().sendToAll(new ResourcePackSendS2CPacket(
								properties.resourcePack,
								properties.resourcePackSha1,
								properties.requireResourcePack,
								parseResourcePackPrompt(loader)
						));
					}
				}
			});
		});
	}

	@Nullable
	private static Text parseResourcePackPrompt(ServerPropertiesLoader propertiesLoader) {
		String string = propertiesLoader.getPropertiesHandler().resourcePackPrompt;
		if (!Strings.isNullOrEmpty(string)) {
			try {
				return Text.Serializer.fromJson(string);
			} catch (Exception var3) {
				LOGGER.warn("Failed to parse resource pack prompt '{}'", string, var3);
			}
		}

		return null;
	}
}
