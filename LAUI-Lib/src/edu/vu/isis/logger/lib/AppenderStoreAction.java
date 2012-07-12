package edu.vu.isis.logger.lib;

import java.util.HashMap;

import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.ActionConst;
import ch.qos.logback.core.joran.spi.InterpretationContext;

public class AppenderStoreAction extends Action {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void begin(InterpretationContext ec, String name,
			Attributes attributes) {
		try {
			AppenderStore.storeReference(((HashMap) ec.getObjectMap().get(
					ActionConst.APPENDER_BAG)));
		} catch (IllegalStateException e) {
			// Do nothing
		}
	}

	@Override
	public void end(InterpretationContext ic, String name) {
	}

}
