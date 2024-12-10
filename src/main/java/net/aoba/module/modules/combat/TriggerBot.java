/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.combat;

import net.aoba.Aoba;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.TickListener;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.FloatSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class TriggerBot extends Module implements TickListener {
    private FloatSetting radius = FloatSetting.builder()
    		.id("triggerbot_radius")
    		.displayName("Radius")
    		.description("Radius that TriggerBot will trigger.")
    		.defaultValue(5f)
    		.minValue(0.1f)
    		.maxValue(10f)
    		.step(0.1f)
    		.build();
    
	private BooleanSetting targetAnimals = BooleanSetting.builder()
		    .id("triggerbot_target_animals")
		    .displayName("Target Animals")
		    .description("Target animals.")
		    .defaultValue(false)
		    .build();
	
	private BooleanSetting targetMonsters = BooleanSetting.builder()
		    .id("triggerbot_target_monsters")
		    .displayName("Target Monsters")
		    .description("Target Monsters.")
		    .defaultValue(true)
		    .build();
	
	private BooleanSetting targetPlayers = BooleanSetting.builder()
		    .id("triggerbot_target_players")
		    .displayName("Target Players")
		    .description("Target Players.")
		    .defaultValue(true)
		    .build();
	
	private BooleanSetting targetFriends = BooleanSetting.builder()
		    .id("triggerbot_target_friends")
		    .displayName("Target Friends")
		    .description("Target Friends.")
		    .defaultValue(false)
		    .build();
	
    private FloatSetting attackDelay = FloatSetting.builder()
    		.id("triggerbot_attack_delay")
    		.displayName("Attack Delay")
    		.description("Delay in milliseconds between attacks.")
    		.defaultValue(0f)
    		.minValue(0f)
    		.maxValue(500f)
    		.step(10f)
    		.build();
    
    private FloatSetting randomness = FloatSetting.builder()
    		.id("triggerbot_randomness")
    		.displayName("Randomness")
    		.description("The randomness of the delay between when TriggerBot will hit a target.")
    		.defaultValue(0.0f)
    		.minValue(0.0f)
    		.maxValue(60.0f)
    		.step(1f)
    		.build();
    
    private long lastAttackTime;

    public TriggerBot() {
    	super("Triggerbot");

        this.setCategory(Category.of("Combat"));
        this.setDescription("Attacks anything you are looking at.");

        this.addSetting(attackDelay);
        this.addSetting(radius);
        this.addSetting(targetAnimals);
        this.addSetting(targetMonsters);
        this.addSetting(targetPlayers);
        this.addSetting(targetFriends);
        this.addSetting(randomness);
        
        this.lastAttackTime = 0L;
    }

    @Override
    public void onDisable() {
        Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
    }

    @Override
    public void onEnable() {
        Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
    }

    @Override
    public void onToggle() {

    }


	@Override
	public void onTick(Pre event) {
		int randomnessValue = randomness.getValue().intValue();
		boolean state = randomnessValue == 0 || (Math.round(Math.random() * Math.round(randomness.max_value))) % randomnessValue == 0;
		
		if (MC.player.getAttackCooldownProgress(0) == 1 && state) {
            HitResult ray = MC.crosshairTarget;

            if (ray != null && ray.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityResult = (EntityHitResult) ray;
                Entity ent = entityResult.getEntity();

                if (!(ent instanceof LivingEntity)) {
                    return;
                }

                if (ent instanceof AnimalEntity && !this.targetAnimals.getValue())
                    return;
                if (ent instanceof PlayerEntity && !this.targetPlayers.getValue())
                    return;
                if (ent instanceof Monster && !this.targetMonsters.getValue())
                    return;

                if (System.currentTimeMillis() - this.lastAttackTime >= attackDelay.getValue()) {
                    MC.interactionManager.attackEntity(MC.player, entityResult.getEntity());
                    MC.player.swingHand(Hand.MAIN_HAND);
                    this.lastAttackTime = System.currentTimeMillis();
                }
            }
        }
	}

	@Override
	public void onTick(Post event) {

	}
}
