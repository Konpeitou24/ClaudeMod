package com.claudemod.block;

import com.claudemod.blockentity.PrismiumCompressorBlockEntity;
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
 * Block for Prismium Compressor (session 70, scheduled). See {@link
 * PrismiumCompressorBlockEntity} for the item-processing/energy logic;
 * this class is the block shell, copied structurally from {@link
 * PrismiumSmelterBlock} (same {@code BaseEntityBlock} + {@code
 * BlockStateProperties.LIT} skeleton, same right-click contract - empty
 * hand opens the GUI; a Prismium Shard in hand no longer manually
 * charges FE as of 2026-08-31 direct-chat feedback - PROGRESS.md TODO6 -
 * see {@link #use}).
 * Third machine in the mod's item-processing chain: Prismium Ore
 * -[Pulverizer]-> Shard -[Smelter]-> Ingot -[this block]-> Prismium
 * Alloy Ingot (see {@link PrismiumCompressorBlockEntity}'s class doc
 * for the recipe/energy numbers).
 */
public class PrismiumCompressorBlock extends BaseEntityBlock {

    public PrismiumCompressorBlock(Properties properties) {
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
        return new PrismiumCompressorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.PRISMIUM_COMPRESSOR.get(), PrismiumCompressorBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PrismiumCompressorBlockEntity compressor)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        Item prismiumShard = ModItems.PRISMIUM_SHARD.get();

        if (held.is(prismiumShard)) {
            // 2026-08-31 direct-chat feedback (PROGRESS.md TODO6): direct
            // hand-charging let a player skip Prismium Generator entirely
            // ("Generatorが死にアイテム化している"), so this consumer no
            // longer accepts a shard by hand at all - FE now only arrives
            // through the Generator -> Cable network (see
            // EnergyPushHelper#pushThroughNetwork), restoring the intended
            // Generator/Cable/consumer role split. The shard itself is
            // intentionally left unconsumed (unlike the old branch this
            // replaces) since no energy actually changes hands here.
            player.displayClientMessage(
                    Component.translatable("message.claudemod.prismium_compressor.no_direct_charge"), true);
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, compressor, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}
