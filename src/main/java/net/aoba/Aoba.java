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


package net.aoba;

import net.fabricmc.api.ModInitializer;

/**
 * A class to initialize and hold the Singleton of Aoba Client.
 */
public class Aoba implements ModInitializer {
    public static AobaClient instance;

    @Override
    public void onInitialize() {
        instance = new AobaClient();
        instance.Initialize();
    }

    /**
     * Returns the singleton instance of Aoba client.
     *
     * @return Aoba Client
     */
    public static AobaClient getInstance() {
        return instance;
    }
}
