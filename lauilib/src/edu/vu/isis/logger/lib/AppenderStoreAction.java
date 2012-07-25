package edu.vu.isis.logger.lib;

import java.util.HashMap;

import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.ActionConst;
import ch.qos.logback.core.joran.spi.InterpretationContext;

/**
 * This action allows the hack to store the reference to the Appender bag to
 * work. This action needs to be invoked on each appender tag in the logback
 * configuration XML file. In order to make this work, a new rule needs to be
 * added to Joran. The easiest way to do this is to add the following to the
 * logback configuration file:
 * <p/>
 * 
 * newRule pattern="configuration/appender"
 * actionClass="edu.vu.isis.logger.lib.AppenderStoreAction"
 * <p/>
 * (This needs to be placed inside of &lt; and /> as usual with XML)
 * 
 * 
 * @author Nick King
 * 
 */
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
