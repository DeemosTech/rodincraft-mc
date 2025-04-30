package top.gregtao.deemos;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class RodinServer implements ModInitializer {

	public static final Item MODELER_TNT = new ModelerTNT(new FabricItemSettings());

	public static final EntityType<Entity> MODELER_TNT_ENTITY = Registry.register(
			Registry.ENTITY_TYPE,
			new Identifier("dmodel", "modeler_tnt"),
			EntityType.Builder.create(ModelerTNTEntity::new, SpawnGroup.MISC)
					.makeFireImmune().setDimensions(0.98F, 0.98F).maxTrackingRange(10)
					.trackingTickInterval(10).build("modeler_tnt")
	);

	public static final Block MODELER_TNTBLOCK = new Block(Block.Settings.copy(Blocks.TNT));

	@Override
	public void onInitialize() {
		Registry.register(Registry.ITEM, new Identifier("dmodel", "modeler_tnt"), MODELER_TNT);
		Networking.registerServer();
		Registry.register(Registry.BLOCK, new Identifier("dmodel", "modeler_tnt"), MODELER_TNTBLOCK);
	}
}