package edu.vu.isis.logger.ui;

import org.xml.sax.Attributes;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.InterpretationContext;

public class AppenderStoreAction extends Action {
	
	@SuppressWarnings("unchecked")
	@Override
	public void begin(InterpretationContext ec, String name,
			Attributes attributes) {
		final Object top = ec.peekObject();
		if(top instanceof Appender) {
			AppenderStore.getInstance().addAppender(
					(Appender<ILoggingEvent>) top);
		}
	}
	
	@Override
	public void end(InterpretationContext ic, String name) {
	}
	
}
