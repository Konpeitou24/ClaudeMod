package com.claudemod.block;

import com.claudemod.blockentity.PrismiumGeneratorBlockEntity;
import com.claudemod.registry.ModBlockEntities;
import com.claudemod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * Block for Prismium Generator (session 9). See
 * {@link PrismiumGeneratorBlockEntity} for the burn/energy-push logic;
 * this class adds the block shell (cube model backed by a block entity,
 * same confirmed pattern as {@link PrismiumCellBlock}), the mod's first
 * use of a blockstate property ({@code BlockStateProperties.LIT}, reused
 * from vanilla rather than declaring a duplicate - the same property
 * furnace/campfire/redstone_lamp use) to swap between a dark idle
 * texture and a glowing active one, and the player-facing fuel/status
 * interaction.
 *
 * <p>Right-click with an empty hand: report remaining fuel (seconds) and
 * current/max buffered FE via an action-bar message - the only way to
 * observe this block entity's state in-game, no GUI exists yet (same
 * situation as Prismium Cell).
 * Right-click holding a Prismium Shard: consume one shard and add
 * {@link PrismiumGeneratorBlockEntity#BURN_TIME_PER_SHARD} ticks of burn
 * time (stacks with any burn time already remaining, like furnace fuel).
 */
public class PrismiumGeneratorBlock extends BaseEntityBlock {

    public PrismiumGeneratorBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.LIT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrismiumGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.PRISMIUM_GENERATOR.get(), PrismiumGeneratorBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PrismiumGeneratorBlockEntity generator)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        Item prismiumShard = ModItems.PRISMIUM_SHARD.get();

        if (held.is(prismiumShard)) {
            generator.addFuel();
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            player.displayClientMessage(
                    Component.translatable("message.claudemod.prismium_generator.fueled",
                            generator.getBurnSeconds()), true);
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty()) {
            // Session 24: open Generator's GUI instead of printing a status
            // message - same NetworkHooks.openScreen pattern established by
            // PrismiumCellBlock#use in session 23 (see that class's doc for
            // why this specific API, not ServerPlayer#openMenu, is correct
            // for this mod's pinned Forge version). The old action-bar
            // status message is removed rather than kept alongside the
            // GUI, same call as Cell: the GUI is a strictly more capable
            // replacement for "check current fuel/energy".
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, generator, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}
