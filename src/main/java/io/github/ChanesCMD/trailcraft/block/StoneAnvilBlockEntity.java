package io.github.ChanesCMD.trailcraft.block;

import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Лежащая на наковальне крица: без вращения и без выпадения, просто предмет на верхней грани. */
public class StoneAnvilBlockEntity extends BlockEntity {
    private ItemStack bloom = ItemStack.EMPTY;

    public StoneAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModSmelting.STONE_ANVIL_ENTITY.get(), pos, state);
    }

    public boolean hasBloom() {
        return !bloom.isEmpty();
    }

    public ItemStack getBloom() {
        return bloom;
    }

    public void setBloom(ItemStack stack) {
        bloom = stack.copy();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ItemStack takeBloom() {
        ItemStack result = bloom;
        setBloom(ItemStack.EMPTY);
        return result;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!bloom.isEmpty()) {
            tag.put("bloom", bloom.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        bloom = tag.contains("bloom") ? ItemStack.parseOptional(registries, tag.getCompound("bloom")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
