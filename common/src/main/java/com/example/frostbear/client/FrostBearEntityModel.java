package com.example.frostbear.client;

import com.example.frostbear.entity.FrostBearEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A broad, low-slung glacial bear built around an articulated quadruped
 * skeleton. Fur, armour plates, claws and emissive ice all have deliberate UV
 * regions in the 128x128 atlas.
 */
public class FrostBearEntityModel extends EntityModel<FrostBearRenderState> {
	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart leftEar;
	private final ModelPart rightEar;
	private final ModelPart tail;

	private final ModelPart frontLeftUpper;
	private final ModelPart frontRightUpper;
	private final ModelPart hindLeftUpper;
	private final ModelPart hindRightUpper;
	private final ModelPart frontLeftLower;
	private final ModelPart frontRightLower;
	private final ModelPart hindLeftLower;
	private final ModelPart hindRightLower;
	private final ModelPart frontLeftPaw;
	private final ModelPart frontRightPaw;
	private final ModelPart hindLeftPaw;
	private final ModelPart hindRightPaw;

	public FrostBearEntityModel(ModelPart root) {
		super(root);
		this.root = root;
		this.body = root.getChild("body");
		this.neck = this.body.getChild("neck");
		this.head = this.neck.getChild("head");
		this.jaw = this.head.getChild("jaw");
		this.leftEar = this.head.getChild("left_ear");
		this.rightEar = this.head.getChild("right_ear");
		this.tail = this.body.getChild("tail");

		this.frontLeftUpper = this.body.getChild("front_left_upper");
		this.frontRightUpper = this.body.getChild("front_right_upper");
		this.hindLeftUpper = this.body.getChild("hind_left_upper");
		this.hindRightUpper = this.body.getChild("hind_right_upper");
		this.frontLeftLower = this.frontLeftUpper.getChild("lower");
		this.frontRightLower = this.frontRightUpper.getChild("lower");
		this.hindLeftLower = this.hindLeftUpper.getChild("lower");
		this.hindRightLower = this.hindRightUpper.getChild("lower");
		this.frontLeftPaw = this.frontLeftLower.getChild("paw");
		this.frontRightPaw = this.frontRightLower.getChild("paw");
		this.hindLeftPaw = this.hindLeftLower.getChild("paw");
		this.hindRightPaw = this.hindRightLower.getChild("paw");
	}


	private static void cub(CubeListBuilder builder, int u, int v,
			float centerX, float centerY, float centerZ,
			float width, float height, float depth) {
		builder.texOffs(u, v).addBox(
				centerX - width / 2.0F,
				-centerY - height / 2.0F,
				centerZ - depth / 2.0F,
				width, height, depth);
	}

	private static CubeListBuilder upperLeg(float width, float depth) {
		CubeListBuilder builder = CubeListBuilder.create();
		cub(builder, 0, 0, 0, -4.0F, 0, width + 1.0F, 10, depth);
		cub(builder, 0, 0, 0, -9.0F, 1.0F, width - 2.0F, 8, depth - 2.0F);
		return builder;
	}

	private static CubeListBuilder lowerLeg(float width, float depth) {
		CubeListBuilder builder = CubeListBuilder.create();
		cub(builder, 20, 8, 0, -4.0F, 0, width, 8, depth);
		return builder;
	}

	private static CubeListBuilder paw(float width, float depth) {
		CubeListBuilder builder = CubeListBuilder.create();
		cub(builder, 0, 28, 0, -1.8F, -2.0F, width + 1.0F, 4.0F, depth + 1.0F);
		for (int i = -1; i <= 1; i++) {
			cub(builder, 108, 112, i * 4.0F, -1.8F, -depth / 2.0F - 2.2F, 2.0F, 2.8F, 4.5F);
		}
		return builder;
	}

	private static void addCrystal(PartDefinition parent, String name,
			float x, float y, float z, float scale, float pitch, float roll) {
		CubeListBuilder builder = CubeListBuilder.create();
		cub(builder, 96, 84, 0, 3.1F * scale, 0, 5.2F * scale, 6.2F * scale, 5.2F * scale);
		cub(builder, 96, 84, 0, 8.0F * scale, 0, 3.7F * scale, 4.6F * scale, 3.7F * scale);
		cub(builder, 96, 84, 0, 11.1F * scale, 0, 1.8F * scale, 2.4F * scale, 1.8F * scale);
		parent.addOrReplaceChild(name, builder, PartPose.offsetAndRotation(x, -y, z, pitch, 0, roll));
	}

	private static void addPlate(PartDefinition parent, String name,
			float x, float y, float z, float roll) {
		CubeListBuilder builder = CubeListBuilder.create();
		cub(builder, 96, 84, 0, 0, 0, 4.0F, 13.0F, 12.0F);
		cub(builder, 96, 84, 0, 6.2F, -1.0F, 2.5F, 5.0F, 8.0F);
		parent.addOrReplaceChild(name, builder, PartPose.offsetAndRotation(x, -y, z, 0, 0, roll));
	}

	public static LayerDefinition getTexturedModelData() {
		MeshDefinition modelData = new MeshDefinition();
		PartDefinition root = modelData.getRoot();

		CubeListBuilder bodyBuilder = CubeListBuilder.create();
		PartDefinition body = root.addOrReplaceChild("body", bodyBuilder, PartPose.offset(0, -5, 4));

		CubeListBuilder shoulderBuilder = CubeListBuilder.create();
		cub(shoulderBuilder, 0, 0, 0, -2, 0, 24, 12, 16);
		cub(shoulderBuilder, 0, 0, 0, 4, 0, 30, 9, 15);
		cub(shoulderBuilder, 0, 0, 0, 9, 1, 22, 7, 13);
		body.addOrReplaceChild("shoulder_mass", shoulderBuilder, PartPose.offsetAndRotation(0, -6, -13, -0.22F, 0, 0));

		CubeListBuilder ribcageBuilder = CubeListBuilder.create();
		cub(ribcageBuilder, 0, 0, 0, 0, 0, 24, 15, 17);
		cub(ribcageBuilder, 0, 0, 0, 6, 0, 21, 7, 14);
		body.addOrReplaceChild("ribcage_mass", ribcageBuilder, PartPose.offsetAndRotation(0, -5, -3, -0.12F, 0, 0));

		CubeListBuilder waistBuilder = CubeListBuilder.create();
		cub(waistBuilder, 0, 0, 0, 0, 0, 18, 10, 13);
		body.addOrReplaceChild("waist_mass", waistBuilder, PartPose.offsetAndRotation(0, -5, 7, -0.05F, 0, 0));

		CubeListBuilder haunchBuilder = CubeListBuilder.create();
		cub(haunchBuilder, 0, 0, 0, 0, 0, 24, 16, 17);
		cub(haunchBuilder, 0, 0, 0, 6, 0, 20, 8, 14);
		cub(haunchBuilder, 0, 0, 0, 2, 7, 20, 12, 8);
		body.addOrReplaceChild("haunch_mass", haunchBuilder, PartPose.offsetAndRotation(0, -5, 15, 0.16F, 0, 0));

		CubeListBuilder neckBuilder = CubeListBuilder.create();
		cub(neckBuilder, 0, 52, 0, 1, 0, 20, 13, 15);
		cub(neckBuilder, 0, 52, 0, 0, -6, 17, 11, 11);
		PartDefinition neck = body.addOrReplaceChild("neck", neckBuilder, PartPose.offset(0, -5, -20));

		CubeListBuilder headBuilder = CubeListBuilder.create();
		cub(headBuilder, 0, 52, 0, 0.8F, -1, 15, 12, 10);           // taller broad skull
		cub(headBuilder, 0, 52, 0, -1.4F, -5.5F, 13, 8, 7);         // deeper cheek taper
		cub(headBuilder, 64, 52, 0, -3.1F, -8.8F, 10, 5, 5);        // short bear muzzle
		cub(headBuilder, 88, 66, 0, -3.5F, -11.9F, 7, 3, 2);        // broad flat nose
		cub(headBuilder, 64, 52, 3.5F, 2, -6.8F, 6, 2, 2);          // right brow
		cub(headBuilder, 64, 52, -3.5F, 2, -6.8F, 6, 2, 2);         // left brow
		// Hair-thin plates sit flush against the cheek face while retaining a
		// non-degenerate render surface for the emissive eye pass.
		cub(headBuilder, 112, 10, 3.6F, 0.55F, -9.06F, 3, 1, 0.1F);
		cub(headBuilder, 120, 10, -3.6F, 0.55F, -9.06F, 3, 1, 0.1F);
		PartDefinition head = neck.addOrReplaceChild("head", headBuilder, PartPose.offset(0, 2, -9));

		CubeListBuilder jawBuilder = CubeListBuilder.create();
		// A single UV-isolated mass avoids overlapping textured jaw shells.
		cub(jawBuilder, 64, 78, 0, -1.2F, -3.2F, 9, 3, 5);
		cub(jawBuilder, 84, 112, 2.8F, 0.1F, -6.2F, 1, 2.5F, 1);
		cub(jawBuilder, 84, 112, -2.8F, 0.1F, -6.2F, 1, 2.5F, 1);
		head.addOrReplaceChild("jaw", jawBuilder, PartPose.offset(0, 5, -5));

		CubeListBuilder earBuilder = CubeListBuilder.create();
		cub(earBuilder, 0, 52, 0, 1, 0, 3, 3, 2);
		head.addOrReplaceChild("left_ear", earBuilder, PartPose.offset(5.2F, -4.2F, -0.5F));
		head.addOrReplaceChild("right_ear", earBuilder, PartPose.offset(-5.2F, -4.2F, -0.5F));
		CubeListBuilder tailBuilder = CubeListBuilder.create();
		cub(tailBuilder, 0, 0, 0, -1, 3, 6, 6, 7);
		cub(tailBuilder, 0, 0, 0, -2, 5.5F, 4, 4, 4);
		body.addOrReplaceChild("tail", tailBuilder, PartPose.offsetAndRotation(0, -4, 22, 0.35F, 0, 0));

		PartDefinition frontLeftUpper = body.addOrReplaceChild(
				"front_left_upper", upperLeg(13, 12), PartPose.offsetAndRotation(10, 0, -15, 0, 0, -0.04F));
		PartDefinition frontRightUpper = body.addOrReplaceChild(
				"front_right_upper", upperLeg(13, 12), PartPose.offsetAndRotation(-10, 0, -15, 0, 0, 0.04F));
		PartDefinition hindLeftUpper = body.addOrReplaceChild(
				"hind_left_upper", upperLeg(14, 14), PartPose.offsetAndRotation(9, 0, 15.5F, 0, 0, -0.04F));
		PartDefinition hindRightUpper = body.addOrReplaceChild(
				"hind_right_upper", upperLeg(14, 14), PartPose.offsetAndRotation(-9, 0, 15.5F, 0, 0, 0.04F));

		PartDefinition frontLeftLower = frontLeftUpper.addOrReplaceChild(
				"lower", lowerLeg(8.5F, 7.5F), PartPose.offset(0, 13, 1));
		PartDefinition frontRightLower = frontRightUpper.addOrReplaceChild(
				"lower", lowerLeg(8.5F, 7.5F), PartPose.offset(0, 13, 1));
		PartDefinition hindLeftLower = hindLeftUpper.addOrReplaceChild(
				"lower", lowerLeg(9.5F, 9.5F), PartPose.offset(0, 13, 1));
		PartDefinition hindRightLower = hindRightUpper.addOrReplaceChild(
				"lower", lowerLeg(9.5F, 9.5F), PartPose.offset(0, 13, 1));

		frontLeftLower.addOrReplaceChild("paw", paw(13, 12), PartPose.offset(0, 8, -1));
		frontRightLower.addOrReplaceChild("paw", paw(13, 12), PartPose.offset(0, 8, -1));
		hindLeftLower.addOrReplaceChild("paw", paw(14, 14), PartPose.offset(0, 8, -1));
		hindRightLower.addOrReplaceChild("paw", paw(14, 14), PartPose.offset(0, 8, -1));

		addPlate(body, "left_shoulder_plate", 14, 8, -12, -0.18F);
		addPlate(body, "right_shoulder_plate", -14, 8, -12, 0.18F);
		addCrystal(body, "left_crown_crystal", 7.5F, 18, -12, 1.05F, -0.08F, -0.12F);
		addCrystal(body, "right_crown_crystal", -7.5F, 18, -12, 1.05F, 0.08F, 0.12F);
		addCrystal(body, "center_crown_crystal", 0, 18, -8, 1.15F, -0.12F, 0);
		addCrystal(body, "mid_back_crystal", 0, 12.2F, 2, 0.9F, 0.12F, 0);
		addCrystal(body, "rear_back_crystal", 0, 15, 13, 0.68F, 0.18F, 0);
		addCrystal(body, "left_flank_shard", 11.5F, 4, 3, 0.48F, 0.25F, -0.55F);
		addCrystal(body, "right_flank_shard", -11.5F, 4, 3, 0.48F, -0.25F, 0.55F);
		addCrystal(head, "left_brow_shard", 7, 4.5F, -2.5F, 0.34F, -0.2F, -0.3F);
		addCrystal(head, "right_brow_shard", -7, 4.5F, -2.5F, 0.34F, 0.2F, 0.3F);

		return LayerDefinition.create(modelData, 128, 128);
	}

	@Override
	public void setupAnim(FrostBearRenderState state) {
		super.setupAnim(state);
		float limbAngle = state.walkAnimationPos;
		float limbDistance = state.walkAnimationSpeed;
		float animationProgress = state.ageInTicks;
		float headYaw = state.yRot;
		float headPitch = state.xRot;

		float idleBreath = Mth.sin(animationProgress * 0.08F);
		float strideAmount = Math.min(limbDistance * 1.35F, 1.0F);
		float stride = limbAngle * 0.62F;
		float rightStep = Mth.cos(stride) * strideAmount;
		float leftStep = Mth.cos(stride + (float) Math.PI) * strideAmount;

		this.body.y += idleBreath * 0.16F;
		this.body.y += Math.abs(Mth.sin(stride)) * 0.65F * strideAmount;
		this.body.zRot = Mth.cos(stride * 0.5F) * 0.045F * strideAmount;
		this.neck.xRot = -0.05F + idleBreath * 0.018F;
		this.head.yRot = headYaw * 0.017453292F + Mth.sin(animationProgress * 0.035F) * 0.055F;
		this.head.xRot = headPitch * 0.017453292F + 0.08F;
		this.jaw.xRot = 0.04F + (idleBreath + 1.0F) * 0.012F;
		this.leftEar.zRot = -0.08F + Mth.sin(animationProgress * 0.11F) * 0.035F;
		this.rightEar.zRot = 0.08F - Mth.sin(animationProgress * 0.11F) * 0.035F;
		this.tail.yRot = Mth.sin(animationProgress * 0.07F) * 0.12F;

		this.frontRightUpper.xRot = rightStep * 0.85F;
		this.hindLeftUpper.xRot = rightStep * 0.72F;
		this.frontLeftUpper.xRot = leftStep * 0.85F;
		this.hindRightUpper.xRot = leftStep * 0.72F;
		this.frontRightLower.xRot = Math.max(0, -rightStep) * 0.5F;
		this.hindLeftLower.xRot = Math.max(0, -rightStep) * 0.45F;
		this.frontLeftLower.xRot = Math.max(0, -leftStep) * 0.5F;
		this.hindRightLower.xRot = Math.max(0, -leftStep) * 0.45F;
		this.frontRightPaw.xRot = -this.frontRightLower.xRot * 0.55F;
		this.frontLeftPaw.xRot = -this.frontLeftLower.xRot * 0.55F;
		this.hindRightPaw.xRot = -this.hindRightLower.xRot * 0.5F;
		this.hindLeftPaw.xRot = -this.hindLeftLower.xRot * 0.5F;

		float melee = Mth.sin((float) Math.sqrt(state.attackTime) * (float) Math.PI);
		if (melee > 0.001F && !state.isBusy()) {
			this.body.yRot = -0.16F * melee;
			this.neck.xRot += 0.28F * melee;
			this.head.xRot += 0.5F * melee;
			this.jaw.xRot += 0.72F * melee;
			this.frontRightUpper.xRot -= 1.15F * melee;
			this.frontRightLower.xRot += 0.38F * melee;
		}

		float roarTicks = state.roarTicks;
		if (roarTicks > 0) {
			float progress = 1.0F - roarTicks / (float) FrostBearEntity.ROAR_DURATION;
			float pose = progress < 0.3F
					? progress / 0.3F
					: progress < 0.82F ? 1.0F : 1.0F - (progress - 0.82F) / 0.18F;
			this.body.xRot = -0.44F * pose;
			this.body.y -= 2.2F * pose;
			this.neck.xRot = -0.34F * pose;
			this.head.xRot = -0.62F * pose;
			this.jaw.xRot = 0.08F + 1.0F * pose;
			this.frontLeftUpper.xRot -= 0.82F * pose;
			this.frontRightUpper.xRot -= 0.82F * pose;
			this.frontLeftLower.xRot += 0.28F * pose;
			this.frontRightLower.xRot += 0.28F * pose;
			this.leftEar.zRot -= 0.18F * pose;
			this.rightEar.zRot += 0.18F * pose;
			this.tail.xRot += 0.22F * pose;
		}

		float breathTicks = state.breathTicks;
		if (breathTicks > 0) {
			float progress = 1.0F - breathTicks / (float) FrostBearEntity.BREATH_DURATION;
			float pose = progress < 0.18F
					? progress / 0.18F
					: progress < 0.86F ? 1.0F : 1.0F - (progress - 0.86F) / 0.14F;
			this.body.xRot = 0.08F * pose;
			this.neck.xRot = 0.2F * pose;
			this.head.xRot = 0.35F * pose;
			this.head.yRot += Mth.sin(animationProgress * 0.55F) * 0.2F * pose;
			this.jaw.xRot = 0.07F + 0.76F * pose;
			this.frontLeftUpper.xRot += 0.12F * pose;
			this.frontRightUpper.xRot += 0.12F * pose;
			this.leftEar.zRot += 0.18F * pose;
			this.rightEar.zRot -= 0.18F * pose;
		}

		float slamTicks = state.slamTicks;
		if (slamTicks > 0) {
			float progress = 1.0F - slamTicks / (float) FrostBearEntity.SLAM_DURATION;
			float rear = progress < 0.45F
					? progress / 0.45F
					: progress < 0.62F ? 1.0F - (progress - 0.45F) / 0.17F : 0.0F;
			float impact = progress >= 0.5F && progress < 0.8F
					? Mth.sin((progress - 0.5F) / 0.3F * (float) Math.PI) : 0.0F;
			this.body.xRot = -0.48F * rear + 0.1F * impact;
			this.body.y -= 1.8F * rear;
			this.body.y += 1.0F * impact;
			this.neck.xRot = -0.12F * rear + 0.2F * impact;
			this.head.xRot = 0.14F * rear + 0.28F * impact;
			this.frontLeftUpper.xRot -= 1.25F * rear;
			this.frontRightUpper.xRot -= 1.25F * rear;
			this.frontLeftUpper.xRot += 0.42F * impact;
			this.frontRightUpper.xRot += 0.42F * impact;
			this.frontLeftLower.xRot += 0.35F * rear;
			this.frontRightLower.xRot += 0.35F * rear;
		}

		float chargeTicks = state.chargeTicks;
		if (chargeTicks > 0) {
			float progress = 1.0F - chargeTicks / (float) FrostBearEntity.CHARGE_DURATION;
			float pose = Math.min(progress / 0.18F, 1.0F);
			this.body.xRot = 0.11F * pose;
			this.neck.xRot = 0.28F * pose;
			this.head.xRot = 0.42F * pose;
			this.jaw.xRot = 0.22F * pose;
			this.leftEar.zRot += 0.25F * pose;
			this.rightEar.zRot -= 0.25F * pose;
			this.tail.xRot += 0.3F * pose;
			this.body.y += Mth.sin(animationProgress * 1.2F) * 0.35F * pose;
		}

		if (state.hurtTime > 0 && !state.isBusy()) {
			float recoil = state.hurtTime / 10.0F;
			this.body.zRot += Mth.sin(recoil * (float) Math.PI) * 0.1F;
			this.head.yRot += Mth.sin(recoil * (float) Math.PI) * 0.16F;
			this.leftEar.zRot -= 0.16F * recoil;
			this.rightEar.zRot += 0.16F * recoil;
		}
	}
}
