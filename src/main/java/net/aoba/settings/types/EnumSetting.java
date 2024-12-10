package net.aoba.settings.types;

import java.util.function.Consumer;

import net.aoba.settings.Setting;

public class EnumSetting<T extends Enum<?>> extends Setting<T> {
	private T[] enumConstants;

	@SuppressWarnings("unchecked")
	protected EnumSetting(String ID, String displayName, String description, T defaultValue, Consumer<T> onUpdate) {
		super(ID, displayName, description, defaultValue, onUpdate);
		this.enumConstants = (T[]) defaultValue.getDeclaringClass().getEnumConstants();
		type = TYPE.ENUM;
	}

	/**
	 * Setter for the value. Validates if the value is an instance of the enum
	 * constants.
	 */
	@Override
	public void setValue(T value) {
		for (T constant : enumConstants) {
			if (constant.equals(value)) {
				super.setValue(value);
				return;
			}
		}
		throw new IllegalArgumentException("Invalid enum value: " + value);
	}

	/**
	 * Checks whether or not a value is within this setting's valid range. In this
	 * context, it checks if the value is a valid enum constant.
	 */
	@Override
	protected boolean isValueValid(T value) {
		for (T constant : enumConstants) {
			if (constant.equals(value)) {
				return true;
			}
		}
		return false;
	}

	public static <E extends Enum<E>> BUILDER<E> builder() {
		return new BUILDER<E>();
	}

	public static class BUILDER<E extends Enum<?>> extends Setting.BUILDER<BUILDER<E>, EnumSetting<E>, E> {
		protected BUILDER() {
			super();
		}

		@Override
		public EnumSetting<E> build() {
			return new EnumSetting<E>(id, displayName, description, defaultValue, onUpdate);
		}
	}
}