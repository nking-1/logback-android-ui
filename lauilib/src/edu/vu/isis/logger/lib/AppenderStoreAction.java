/*Copyright (C) 2010-2012 Institute for Software Integrated Systems (ISIS)
This software was developed by the Institute for Software Integrated
Systems (ISIS) at Vanderbilt University, Tennessee, USA for the 
Transformative Apps program under DARPA, Contract # HR011-10-C-0175.
The United States Government has unlimited rights to this software. 
The US government has the right to use, modify, reproduce, release, 
perform, display, or disclose computer software or computer software 
documentation in whole or in part, in any manner and for any 
purpose whatsoever, and to have or authorize others to do so.
 */
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
