package net.coderbot.iris.compat.sodium.mixin.block_id;

import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.pipeline.FluidRenderer;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import net.coderbot.iris.vertices.ExtendedDataHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.coderbot.iris.compat.sodium.impl.block_context.ChunkBuildBuffersExt;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;

/**
 * Passes additional information indirectly to the vertex writer to support the mc_Entity and at_midBlock parts of the vertex format.
 */
@Mixin(ChunkRenderRebuildTask.class)
public class MixinChunkRenderRebuildTask {
	@Inject(method = "performBuild", at = @At(value = "INVOKE",
			target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;setRenderOffset(III)V"),
			locals = LocalCapture.CAPTURE_FAILHARD)
	private void iris$setLocalPos(ChunkRenderCacheLocal cache, ChunkBuildBuffers buffers, CancellationSource cancellationSource, CallbackInfoReturnable<ChunkBuildResult<T>> cir, int relX, int relY, int relZ) {
		if (buffers instanceof ChunkBuildBuffersExt) {
			((ChunkBuildBuffersExt) buffers).iris$setLocalPos(relX, relY, relZ);
		}
	}

	@Inject(method = "performBuild", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/util/MathUtil;hashPos(Lnet/minecraft/util/math/BlockPos;)J"), locals = LocalCapture.CAPTURE_FAILHARD)
	private void iris$wrapGetBlockLayer(){

	}
	/*

	@Redirect(method = "performBuild", at = @At(value = "INVOKE",
			target = "Lme/jellysquid/mods/sodium/client/util/MathUtil;hashPos(Lnet/minecraft/util/math/BlockPos;)J"))
	private long iris$wrapGetBlockLayer(BlockPos pos, ChunkRenderCacheLocal cache, ChunkBuildBuffers buffers) {
		if (buffers instanceof ChunkBuildBuffersExt) {
			((ChunkBuildBuffersExt) buffers).iris$setMaterialId(blockState, ExtendedDataHelper.BLOCK_RENDER_TYPE);
		}

		return MathUtil.hashPos(pos);
	}

	 */
	/*
	@Redirect(method = "performBuild", at = @At(value = "INVOKE",
			target = "Lme/jellysquid/mods/sodium/client/render/pipeline/FluidRenderer;render(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/core/BlockPos;Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuffers;)Z"))
	private boolean iris$wrapGetFluidLayer(FluidRenderer renderer, BlockAndTintGetter world, FluidState fluidState, BlockPos pos, ChunkModelBuffers modelBuffers, ChunkRenderCacheLocal cache, ChunkBuildBuffers buffers) {
		if (buffers instanceof ChunkBuildBuffersExt) {
			((ChunkBuildBuffersExt) buffers).iris$setMaterialId(fluidState.createLegacyBlock(), ExtendedDataHelper.FLUID_RENDER_TYPE);
		}

		return renderer.render(world, fluidState, pos, modelBuffers);
	}
	*/
	@Inject(method = "performBuild",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;hasTileEntity(Lnet/minecraft/block/state/IBlockState;)Z"))
	private void iris$resetContext(ChunkRenderCacheLocal cache, ChunkBuildBuffers buffers,
							  CancellationSource cancellationSource, CallbackInfoReturnable<ChunkBuildResult<?>> cir) {
		if (buffers instanceof ChunkBuildBuffersExt) {
			((ChunkBuildBuffersExt) buffers).iris$resetBlockContext();
		}
	}
}
