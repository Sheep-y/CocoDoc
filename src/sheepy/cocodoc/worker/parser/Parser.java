package sheepy.cocodoc.worker.parser;

import java.util.logging.Level;
import sheepy.cocodoc.worker.Block;

public abstract class Parser implements AutoCloseable {
   private Parser parent;
   protected Block context;

   public Parser() {}
   public Parser(Parser parent) {
      this.parent = parent;
   }

   public Parser getParent() { return parent; }

   public void start ( Block context ) {
      this.context = context;
      String text = context.getText().toString();
      if ( text.isEmpty() ) {
         log( Level.FINEST, "Nothing to parse" );
         return;
      }
      Parser oldParser = context.getParser();
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

   @Override public abstract Parser clone();

   @Override public void close() {}

   protected  void log ( Level level, String message, Object ... parameter ) {
      context.log( level, message, parameter );
   }
}