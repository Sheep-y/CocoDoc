package sheepy.cocodoc.worker.parser;

import java.util.logging.Level;
import java.util.logging.Logger;
import sheepy.cocodoc.worker.Block;
import sheepy.util.Text;

public abstract class Parser implements AutoCloseable {
   protected static final Logger log = Logger.getLogger( Parser.class.getSimpleName() );
   private Parser parent;
   static {
      log.setLevel( Level.ALL );
   }

   public Parser() {}
   public Parser(Parser parent) {
      this.parent = parent;
   }

   public Parser getParent() { return parent; }

   public void start ( Block context ) {
      String text = context.getText().toString();
      if ( text.isEmpty() ) return;
      Parser oldParser = context.getParser();
      log.log( Level.INFO, "Parsing {1} characters with {0}. {2}", new Object[]{ this.getClass().getSimpleName(), text.length(), findFirstTag( text ) } );
      try {
         context.setParser( this );
         start( context, text );
      } finally {
         context.setParser( oldParser );
      }
   }
   protected abstract void start( Block context, String text );
   public abstract CharSequence get();


   protected static boolean shouldStop() { return Thread.currentThread().isInterrupted(); }


   private static String findFirstTag ( CharSequence text ) {
      return Text.ellipsis( text, 30 );
      /*
      String firstTagRegx = "<[^!>][^>]*>";
      Matcher firstTagMatcher = tagPool.get( firstTagRegx ).reset( text );
      String firstTag = firstTagMatcher.find() ? firstTagMatcher.group() : "";
      if ( firstTag.length() > 30 ) firstTag = Text.toString( firstTag.codePoints().limit(27) ) + "...";
      tagPool.recycle( firstTagRegx, firstTagMatcher );
      return firstTag;
      */
   }

   @Override public abstract Parser clone();

   @Override public void close() {}
}