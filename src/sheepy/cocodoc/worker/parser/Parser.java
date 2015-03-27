/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sheepy.cocodoc.worker.parser;

import java.util.logging.Level;
import java.util.logging.Logger;
import sheepy.cocodoc.worker.Block;
import sheepy.util.Text;

public abstract class Parser implements AutoCloseable {
   protected static final Logger log = Logger.getLogger( Parser.class.getSimpleName() );
   private Parser parent;

   public Parser() {}
   public Parser(Parser parent) {
      this.parent = parent;
   }

   public Parser getParent() { return parent; }

   public StringBuilder parse ( Block context ) {
      String text = context.getText().toString();
      if ( text.isEmpty() ) return null;
      log.log( Level.INFO, "Parsing {1} characters with {0}. {2}", new Object[]{ this.getClass().getSimpleName(), text.length(), findFirstTag( text ) } ) ;
      context.setParser( this );
      return implParse( context, text );
   }
   protected abstract StringBuilder implParse ( Block context, String text );

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