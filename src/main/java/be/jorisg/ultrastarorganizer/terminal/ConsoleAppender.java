//package be.jorisg.ultrastarorganizer.terminal;
//
//import org.apache.logging.log4j.core.*;
//import org.apache.logging.log4j.core.appender.AbstractAppender;
//import org.apache.logging.log4j.core.config.Property;
//import org.apache.logging.log4j.core.config.plugins.Plugin;
//import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
//
//import java.io.Serializable;
//
//@Plugin(name = ConsoleAppender.PLUGIN_NAME, category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
//public class ConsoleAppender extends AbstractAppender {
//
//    public static final String PLUGIN_NAME = "ConsoleAppender";
//
//    protected ConsoleAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
//        super(name, filter, layout, ignoreExceptions, properties);
//    }
//
//    @Override
//    public void append(LogEvent event) {
//        String msg = getLayout().toSerializable(event).toString();
//        Console.get().print(msg);
//    }
//
//    //
//
//    @PluginBuilderFactory
//    public static <B extends Builder<B>> B newBuilder() {
//        return new Builder<B>().asBuilder();
//    }
//
//    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
//            implements org.apache.logging.log4j.core.util.Builder<ConsoleAppender> {
//
//        @Override
//        public ConsoleAppender build() {
//            return new ConsoleAppender(getName(), getFilter(), getOrCreateLayout(),
//                    isIgnoreExceptions(), getPropertyArray());
//        }
//    }
//
//    //
//
//}
