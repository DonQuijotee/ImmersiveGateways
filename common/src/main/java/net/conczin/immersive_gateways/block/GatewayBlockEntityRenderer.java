package net.conczin.immersive_gateways.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.conczin.immersive_gateways.ImmersiveGateways;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.joml.*;
import org.joml.Math;

public class GatewayBlockEntityRenderer<T extends GatewayBlockEntity> implements BlockEntityRenderer<T> {
    public static final ResourceLocation RUNES_LOCATION = ImmersiveGateways.locate("textures/entity/runes.png");

    public static final Vector3f[] NORMALS = new Vector3f[]{
            new Vector3f(0.0f, 0.0f, -1.0f),
            new Vector3f(0.0f, 0.0f, 1.0f),
            new Vector3f(0.0f, -1.0f, 0.0f),
            new Vector3f(0.0f, 1.0f, 0.0f),
            new Vector3f(-1.0f, 0.0f, 0.0f),
            new Vector3f(1.0f, 0.0f, 0.0f)
    };

    public GatewayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // NO-OP
    }

    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Direction.Axis value = blockEntity.getBlockState().getValue(GatewayBlock.AXIS);

        poseStack.pushPose();

        Level level = blockEntity.getLevel();
        double time = (level == null ? 0.0 : (double) level.getGameTime() + partialTick) * 0.05;

        renderRuneCube(blockEntity, time, partialTick, poseStack, buffer, packedLight, packedOverlay, 0);
        renderRuneCube(blockEntity, time, partialTick, poseStack, buffer, packedLight, packedOverlay, 1);
        renderRuneCube(blockEntity, time, partialTick, poseStack, buffer, packedLight, packedOverlay, 2);
        renderRuneCube(blockEntity, time, partialTick, poseStack, buffer, packedLight, packedOverlay, 3);

        poseStack.popPose();
    }

    private double noise(double time) {
        return Math.sin(time) + Math.sin(time * 1.7) * 0.5 + Math.sin(time * 2.3) * 0.25 + Math.sin(time * 3.1) * 0.125;
    }

    private void renderRuneCube(T blockEntity, double time, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, int face) {
        BlockPos blockPos = blockEntity.getBlockPos();
        Vector3d position = blockEntity.getPosition(blockPos, blockEntity.getBlockState(), face);
        double seed = time + position.x() + position.y() * 1.7 + position.z() * 2.7;

        double distance = blockEntity.lastDistance[face] + (blockEntity.distance[face] - blockEntity.lastDistance[face]) * partialTick;

        // Jitter
        double noise = Math.max(0.0, noise(seed * 5.0) - 0.5) * 0.5f / 16.0f * distance;
        double vx = noise(seed * 7.0 + 1.0) * noise;
        double vy = noise(seed * 7.0 + 2.0) * noise;
        double vz = noise(seed * 7.0 + 3.0) * noise;

        // Wave offset
        double wave = distance * 0.5 / 16.0;
        double jx = noise(seed + 1.0) * wave;
        double jy = noise(seed + 2.0) * wave;
        double jz = noise(seed + 3.0) * wave;

        // Rotation
        double rotate = 0.1 * distance;
        double rx = noise(seed * 1.7 + 1.0) * rotate;
        double ry = noise(seed * 1.7 + 2.0) * rotate;
        double rz = noise(seed * 1.7 + 3.0) * rotate;

        poseStack.pushPose();
        poseStack.translate(
                position.x - blockPos.getX() + jx + vx,
                position.y - blockPos.getY() + jy + vy,
                position.z - blockPos.getZ() + jz + vz
        );
        Quaternionf q = new Quaternionf(blockEntity.lastRotations[face]);
        q.nlerp(blockEntity.rotations[face], partialTick);
        poseStack.mulPose(q);
        poseStack.mulPose(Axis.XP.rotation((float) rx));
        poseStack.mulPose(Axis.YP.rotation((float) ry));
        poseStack.mulPose(Axis.ZP.rotation((float) rz));
        poseStack.scale(0.1875f, 0.1875f, 0.1875f);

        this.renderCube(poseStack.last(), buffer.getBuffer(RenderType.endGateway()));
        this.renderCube(poseStack.last(), buffer.getBuffer(RenderType.entityTranslucentEmissive(RUNES_LOCATION)), packedLight, packedOverlay);

        poseStack.popPose();
    }

    private void renderCube(PoseStack.Pose pose, VertexConsumer consumer) {
        this.renderFace(pose, consumer, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        this.renderFace(pose, consumer, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f);
        this.renderFace(pose, consumer, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f);
        this.renderFace(pose, consumer, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f);
        this.renderFace(pose, consumer, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f);
        this.renderFace(pose, consumer, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f);
    }

    private void renderCube(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay) {
        this.renderFace(pose, consumer, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0, light, overlay);
        this.renderFace(pose, consumer, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1, light, overlay);
        this.renderFace(pose, consumer, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 2, light, overlay);
        this.renderFace(pose, consumer, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 3, light, overlay);
        this.renderFace(pose, consumer, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 4, light, overlay);
        this.renderFace(pose, consumer, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 5, light, overlay);
    }

    private void renderFace(PoseStack.Pose pose, VertexConsumer consumer, float x0, float x1, float y0, float y1, float z0, float z1, float z2, float z3) {
        consumer.vertex(pose.pose(), x0, y0, z0).endVertex();
        consumer.vertex(pose.pose(), x1, y0, z1).endVertex();
        consumer.vertex(pose.pose(), x1, y1, z2).endVertex();
        consumer.vertex(pose.pose(), x0, y1, z3).endVertex();
    }

    private void renderFace(PoseStack.Pose pose, VertexConsumer consumer, float x0, float x1, float y0, float y1, float z0, float z1, float z2, float z3, int face, int light, int overlay) {
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 1.0f;

        float u = Math.floor(face / 2.0f) * 6.0f;
        float v = (face % 2.0f) * 6.0f;

        Vector4f p = new Vector4f();
        Vector3f n = pose.normal().transform(new Vector3f(NORMALS[face]));

        pose.pose().transform(x0, y0, z0, 1.0f, p);
        consumer.vertex(p.x(), p.y(), p.z(), r, g, b, a, u / 32.0f, v / 32.0f, overlay, light, n.x(), n.y(), n.z());

        pose.pose().transform(x1, y0, z1, 1.0f, p);
        consumer.vertex(p.x(), p.y(), p.z(), r, g, b, a, (u + 6.0f) / 32.0f, v / 32.0f, overlay, light, n.x(), n.y(), n.z());

        pose.pose().transform(x1, y1, z2, 1.0f, p);
        consumer.vertex(p.x(), p.y(), p.z(), r, g, b, a, (u + 6.0f) / 32.0f, (v + 6.0f) / 32.0f, overlay, light, n.x(), n.y(), n.z());

        pose.pose().transform(x0, y1, z3, 1.0f, p);
        consumer.vertex(p.x(), p.y(), p.z(), r, g, b, a, u / 32.0f, (v + 6.0f) / 32.0f, overlay, light, n.x(), n.y(), n.z());
    }
}
