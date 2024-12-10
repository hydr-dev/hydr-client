package net.aoba.macros.actions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import net.aoba.mixin.interfaces.IMouse;
import net.minecraft.client.Mouse;

public class MouseScrollMacroEvent extends MacroEvent {

	private double deltaX = 0;
	private double deltaY = 0;
	
	public MouseScrollMacroEvent() {
		
	}
	
	public MouseScrollMacroEvent(long timestamp, double deltaX, double deltaY) {
		super(timestamp);
		this.deltaX = deltaX;
		this.deltaY = deltaY;
	}

	@Override
	public void write(DataOutputStream fs) throws IOException {
		super.write(fs);
		fs.writeDouble(deltaX);
		fs.writeDouble(deltaY);
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		super.read(in);
		deltaX = in.readDouble();
		deltaY = in.readDouble();
	}

	@Override
	public void execute() {
		Mouse mouse = MC.mouse;
		IMouse iMouse = (IMouse)mouse;
		if(iMouse != null) {
			iMouse.executeOnMouseScroll(timestamp, deltaX, deltaY);
		}
	}
}
