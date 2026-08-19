package com.claudemod.block;

import com.claudemod.blockentity.PrismiumPulverizerBlockEntity;
import com.claudemod.energy.PrismiumEnergyStorage;
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
 * Block for Prismium Pulverizer (session 67). See {@link
 * PrismiumPulverizerBlockEntity} for the item-processing/energy logic;
 * this class is the block shell, copied structurally from {@link
 * PrismiumWardstoneBlock}/{@link PrismiumGeneratorBlock} (same {@code
 * BaseEntityBlock} + reused {@code BlockStateProperties.LIT} pattern).
 *
 * <p>Right-click with an empty hand: opens Prismium Pulverizer's GUI (see
 * {@link com.claudemod.menu.PrismiumPulverizerMenu}), where items are
 * dropped into the input slot and shards collected from the output slot -
 * same {@code NetworkHooks.openScreen} pattern every other energy block
 * in this mod uses. Right-click holding a Prismium Shard: manually add
 * {@link PrismiumPulverizerBlockEntity#SHARD_CHARGE_AMOUNT} FE, identical
 * shape to Wardstone/Restorer/Pylon's manual charge interaction - lets a
 * player bootstrap the machine by hand before building cable/generator
 * infrastructure.
 */
public class PrismiumPulverizerBlock extends BaseEntityBlock {

    public PrismiumPulverizerBlock(Properties properties) {
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
        return new PrismiumPulverizerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.PRISMIUM_PULVERIZER.get(), PrismiumPulverizerBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PrismiumPulverizerBlockEntity pulverizer)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        Item prismiumShard = ModItems.PRISMIUM_SHARD.get();

        if (held.is(prismiumShard)) {
            PrismiumEnergyStorage storage = pulverizer.getEnergyStorage();
            int accepted = storage.receiveEnergy(PrismiumPulverizerBlockEntity.SHARD_CHARGE_AMOUNT, true);
            if (accepted <= 0) {
                player.displayClientMessage(
                        Component.translatable("message.claudemod.prismium_pulverizer.full"), true);
                return InteractionResult.CONSUME;
            }
            storage.receiveEnergy(accepted, false);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            pulverizer.setChanged();
            player.displayClientMessage(
                    Component.translatable("message.claudemod.prismium_pulverizer.charged",
                            storage.getEnergyStored(), storage.getMaxEnergyStored()), true);
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, pulverizer, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}
