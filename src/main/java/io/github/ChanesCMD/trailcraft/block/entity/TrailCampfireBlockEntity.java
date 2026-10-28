package io.github.ChanesCMD.trailcraft.block.entity;

import io.github.ChanesCMD.trailcraft.block.TrailCampfireBlock;
import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import io.github.ChanesCMD.trailcraft.init.ModOres;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Костёр TrailCraft (TrailCampfireBlock): 4 слота еды, как у ванильного костра (рецепты campfire_cooking), и брёвна.
 * Брёвна хранятся снизу вверх, по одному тонкому бревну своей породы; сколько их — свойство блока LOGS.
 * Пока горит, верхнее бревно выгорает за LOG_BURN_TICKS и пропадает, на его место встаёт следующее; без брёвен огонь
 * гаснет. Каждое выгоревшее бревно прибавляет золы (не больше MAX_ASH): столько её и собирает лопата.
 * Брёвна, побывавшие в огне, обуглены: из сломанного костра выпадают древесным углём, необгоревшие — сами собой.
 */
public class TrailCampfireBlockEntity extends BlockEntity {
    public static final int SLOTS = 4;
    /** Сколько тиков горит одно тонкое бревно (1 минута). */
    public static final int LOG_BURN_TICKS = 1200;
    /** Больше этого зола не копится. */
    public static final int MAX_ASH = 4;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final int[] cookingProgress = new int[SLOTS];
    private final int[] cookingTime = new int[SLOTS];
    private final NonNullList<ItemStack> logs = NonNullList.withSize(TrailCampfireBlock.MAX_LOGS, ItemStack.EMPTY);
    /** Сколько нижних брёвен обуглено, сколько ещё горит верхнее, сколько накопилось золы. */
    private int charred, burn, ash;
    private final RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> quickCheck =
            RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);

    public TrailCampfireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAMPFIRE.get(), pos, state);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    /** Сколько золы соберёт лопата: хотя бы одна, слой золы есть всегда. */
    public int ash() {
        return Math.max(1, ash);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TrailCampfireBlockEntity campfire) {
        if (state.getValue(TrailCampfireBlock.LIT)) {
            campfire.cook(level, pos, state);
            if (--campfire.burn <= 0) {
                campfire.burnOut(level, pos, state);
            }
            campfire.setChanged();
        } else {
            campfire.cool();
        }
    }

    /** Как у ванильного костра: дым над огнём и над тем, что жарится. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TrailCampfireBlockEntity campfire) {
        if (!state.getValue(TrailCampfireBlock.LIT)) {
            return;
        }
        RandomSource random = level.random;
        if (random.nextFloat() < 0.11f) {
            for (int i = 0; i < random.nextInt(2) + 2; i++) {
                CampfireBlock.makeParticles(level, pos, false, false);
            }
        }
        for (int i = 0; i < SLOTS; i++) {
            if (!campfire.items.get(i).isEmpty() && random.nextFloat() < 0.2f) {
                Vec3 at = TrailCampfireBlock.slotPosition(state, i);
                for (int k = 0; k < 4; k++) {
                    level.addParticle(ParticleTypes.SMOKE, pos.getX() + at.x, pos.getY() + at.y + 0.1, pos.getZ() + at.z,
                            0.0, 5.0E-4, 0.0);
                }
            }
        }
    }

    private void cook(Level level, BlockPos pos, BlockState state) {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty() || ++cookingProgress[i] < cookingTime[i]) {
                continue;
            }
            SingleRecipeInput input = new SingleRecipeInput(stack);
            ItemStack result = quickCheck.getRecipeFor(input, level)
                    .map(recipe -> recipe.value().assemble(input, level.registryAccess()))
                    .orElse(stack);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), result);
            items.set(i, ItemStack.EMPTY);
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    /** Без огня недожаренное понемногу остывает, как у ванильного костра. */
    private void cool() {
        boolean changed = false;
        for (int i = 0; i < SLOTS; i++) {
            if (cookingProgress[i] > 0) {
                cookingProgress[i] = Mth.clamp(cookingProgress[i] - 2, 0, cookingTime[i]);
                changed = true;
            }
        }
        if (changed) {
            setChanged();
        }
    }

    /** Верхнее бревно выгорело: оно пропадает, золы прибавляется; последнее — и огонь гаснет. */
    private void burnOut(Level level, BlockPos pos, BlockState state) {
        int left = Math.max(0, state.getValue(TrailCampfireBlock.LOGS) - 1);
        logs.set(left, ItemStack.EMPTY);
        charred = Math.min(charred, left);
        ash = Math.min(MAX_ASH, ash + 1);
        BlockState next = state.setValue(TrailCampfireBlock.LOGS, left);
        if (left == 0) {
            next = next.setValue(TrailCampfireBlock.LIT, false);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.0f);
        } else {
            burn = LOG_BURN_TICKS;
        }
        level.setBlockAndUpdate(pos, next);
    }

    /** Кладёт бревно поверх остальных (сколько их — по состоянию блока); в огне оно сразу обугливается. */
    public void addLog(Level level, BlockPos pos, BlockState state, ItemStack log) {
        int n = state.getValue(TrailCampfireBlock.LOGS);
        if (n >= TrailCampfireBlock.MAX_LOGS) {
            return;
        }
        logs.set(n, log.copyWithCount(1));
        if (state.getValue(TrailCampfireBlock.LIT)) {
            charred = n + 1;
        }
        level.setBlockAndUpdate(pos, state.setValue(TrailCampfireBlock.LOGS, n + 1));
        setChanged();
    }

    /** Огонь разгорелся: все брёвна обуглены, верхнее начинает гореть (если ещё не начало). */
    public void ignite(int logCount) {
        charred = logCount;
        if (burn <= 0) {
            burn = LOG_BURN_TICKS;
        }
        setChanged();
    }

    public Optional<RecipeHolder<CampfireCookingRecipe>> getCookableRecipe(ItemStack stack) {
        return items.stream().noneMatch(ItemStack::isEmpty) || level == null
                ? Optional.empty()
                : quickCheck.getRecipeFor(new SingleRecipeInput(stack), level);
    }

    /** Кладёт одну штуку в первый свободный слот. */
    public boolean placeFood(Player player, ItemStack stack, int time) {
        for (int i = 0; i < SLOTS; i++) {
            if (items.get(i).isEmpty()) {
                cookingTime[i] = time;
                cookingProgress[i] = 0;
                items.set(i, stack.copyWithCount(1));
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                setChanged();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
                return true;
            }
        }
        return false;
    }

    /** Что выпадает из сломанного костра: еда из слотов и брёвна (обугленные — древесным углём). */
    public void dropContents(Level level, BlockPos pos, int logCount) {
        for (ItemStack stack : items) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        for (int i = 0; i < logCount && i < logs.size(); i++) {
            ItemStack log = i < charred ? new ItemStack(ModOres.CHARCOAL.get()) : logs.get(i);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), log);
        }
    }

    /** Брёвна нового костра снизу вверх (TrailCampfireBlock.build). */
    public void setLogs(List<ItemStack> list) {
        for (int i = 0; i < logs.size(); i++) {
            logs.set(i, i < list.size() ? list.get(i).copyWithCount(1) : ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, true, registries);
        CompoundTag logTag = new CompoundTag();
        ContainerHelper.saveAllItems(logTag, logs, true, registries);
        tag.put("Logs", logTag);
        tag.putIntArray("CookingTimes", cookingProgress);
        tag.putIntArray("CookingTotalTimes", cookingTime);
        tag.putInt("Charred", charred);
        tag.putInt("Burn", burn);
        tag.putInt("Ash", ash);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        // клиенту приходят только слоты еды (getUpdateTag), остальное — с диска
        if (tag.contains("Logs")) {
            logs.clear();
            ContainerHelper.loadAllItems(tag.getCompound("Logs"), logs, registries);
        }
        copy(tag.getIntArray("CookingTimes"), cookingProgress);
        copy(tag.getIntArray("CookingTotalTimes"), cookingTime);
        charred = tag.getInt("Charred");
        burn = tag.getInt("Burn");
        ash = tag.getInt("Ash");
    }

    private static void copy(int[] from, int[] to) {
        System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, items, true, registries);
        return tag;
    }
}
