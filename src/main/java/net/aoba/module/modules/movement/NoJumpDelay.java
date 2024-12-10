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

package net.aoba.module.modules.movement;

import net.aoba.Aoba;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.TickListener;
import net.aoba.mixin.interfaces.ILivingEntity;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.FloatSetting;

public class NoJumpDelay extends Module implements TickListener {

	private FloatSetting delay = FloatSetting.builder().id("nojumpdelay_delay").displayName("Delay")
			.description("NoJumpDelay Delay.").defaultValue(1f).minValue(0f).maxValue(20f).step(1f).build();

	public NoJumpDelay() {
		super("NoJumpDelay");
		this.setCategory(Category.of("Movement"));
		this.setDescription("Makes it so the user can jump very quickly.");

		this.addSetting(delay);
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
		ILivingEntity ent = (ILivingEntity) MC.player;
		if (ent.getJumpCooldown() > delay.getValue()) {
			ent.setJumpCooldown(delay.getValue().intValue());
		}
	}

	@Override
	public void onTick(Post event) {

	}
}