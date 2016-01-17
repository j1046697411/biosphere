package com.semperhilaris.biosphere.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.semperhilaris.biosphere.components.AbductHumans;
import com.semperhilaris.biosphere.components.Destroy;
import com.semperhilaris.biosphere.components.GiveEnergy;
import com.semperhilaris.biosphere.components.Health;
import com.semperhilaris.engine.Assets;
import com.semperhilaris.engine.components.Position;
import com.semperhilaris.engine.components.Rotation;
import com.semperhilaris.engine.util.RenderOrderComparator;

/**
 *
 */
public class DestroyRenderingSystem extends SortedIteratingSystem {

	private Texture texture;
	private TextureRegion textureRegion;
	private EarClippingTriangulator earClippingTriangulator = new EarClippingTriangulator();

	private Camera camera;
	private PolygonSpriteBatch spriteBatch;

	private boolean soundIsPlaying;
	private Sound sound;

	ComponentMapper<Destroy> destroyComponentMapper;
	ComponentMapper<Position> positionComponentMapper;
	ComponentMapper<Rotation> rotationComponentMapper;
	ComponentMapper<Health> healthComponentMapper;

	public DestroyRenderingSystem(Camera camera, PolygonSpriteBatch spriteBatch) {
		super(Family.all(Destroy.class, Position.class).get(), new RenderOrderComparator());

		destroyComponentMapper = ComponentMapper.getFor(Destroy.class);
		positionComponentMapper = ComponentMapper.getFor(Position.class);
		rotationComponentMapper = ComponentMapper.getFor(Rotation.class);
		healthComponentMapper = ComponentMapper.getFor(Health.class);

		this.camera = camera;
		this.spriteBatch = spriteBatch;

		texture = new Texture(Gdx.files.internal("textures/death_beam.png"));
		texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
		textureRegion = new TextureRegion(texture);
	}

	@Override
	public void update(float deltaTime) {
		//TODO AssetManager + Audio is broken on OSX El Capitan: https://github.com/libgdx/libgdx/issues/3572
		/*
		if (Assets.assetManager.getProgress() == 1 && sound == null) {
			sound = Assets.getSound("beam.mp3");
		}
		*/
		if (sound == null) {
			sound = Gdx.audio.newSound(Gdx.files.internal("audio/sounds/beam.mp3"));
		}
		spriteBatch.setProjectionMatrix(camera.combined);
		spriteBatch.begin();
		super.update(deltaTime);
		spriteBatch.end();
	}

	@Override
	protected void processEntity(Entity entity, float deltaTime) {
		Position position = positionComponentMapper.get(entity);
		Destroy destroy = destroyComponentMapper.get(entity);

		if (destroy.target != null) {
			Health targetHealth = healthComponentMapper.get(destroy.target);
			if (targetHealth == null || targetHealth.value <= 0) {
				stopSound();
				return;
			}

			Position targetPosition = positionComponentMapper.get(destroy.target);
			if (targetPosition != null) {
				float beamTarget1X = targetPosition.x - 10;
				float beamTarget1Y = targetPosition.y;
				float beamTarget2X = targetPosition.x + 10;
				float beamTarget2Y = targetPosition.y;

				Rotation rotation = rotationComponentMapper.get(destroy.target);
				if (rotation != null) {
					float degrees = rotation.x - 90;
					float radians = degrees * MathUtils.degreesToRadians;

					float tmp1X = beamTarget1X;
					float tmp1Y = beamTarget1Y;
					float tmp2X = beamTarget2X;
					float tmp2Y = beamTarget2Y;

					beamTarget1X = (float) (MathUtils.cos(radians) * (tmp1X - targetPosition.x) - Math.sin(radians) * (tmp1Y - targetPosition.y) + targetPosition.x);
					beamTarget1Y = (float) (Math.sin(radians) * (tmp1X - targetPosition.x) + Math.cos(radians) * (tmp1Y - targetPosition.y) + targetPosition.y);
					beamTarget2X = (float) (MathUtils.cos(radians) * (tmp2X - targetPosition.x) - Math.sin(radians) * (tmp2Y - targetPosition.y) + targetPosition.x);
					beamTarget2Y = (float) (Math.sin(radians) * (tmp2X - targetPosition.x) + Math.cos(radians) * (tmp2Y - targetPosition.y) + targetPosition.y);
				}

				float[] vertices = new float[]{
						beamTarget1X, beamTarget1Y,
						position.x, position.y - 50,
						position.x, position.y - 50,
						beamTarget2X, beamTarget2Y
				};

				short[] triangles = earClippingTriangulator.computeTriangles(vertices).toArray();

				PolygonRegion polygonRegion = new PolygonRegion(textureRegion, vertices, triangles);
				spriteBatch.draw(polygonRegion, 0, 0);

				playSound();
			} else {
				stopSound();
			}
		} else {
			stopSound();
		}
	}

	private void playSound() {
		if (sound != null && !soundIsPlaying) {
			sound.loop(1f, 0.5f, 0.5f);
			soundIsPlaying = true;
		}
	}

	private void stopSound() {
		if (sound != null && soundIsPlaying) {
			sound.stop();
			soundIsPlaying = false;
		}
	}

}
