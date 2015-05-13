package sheepy.cocodoc.worker.task;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import javax.activation.MimetypesFileTypeMap;
import sheepy.cocodoc.worker.directive.Directive;
import sheepy.util.collection.NullData;

public class TaskPrefix extends Task {

   @Override public Action getAction () { return Action.PREFIX; }

   @Override protected Predicate<List<String>> validParam() { return nonEmpty; }
   @Override protected String invalidParamMessage() { return "prefix() task should have parameter."; }

   @Override protected void run () {
      if ( ! hasParams() ) {
         log( Level.INFO, "Skipping, no parameter" );
         return;
      }
      log( Level.FINER, "Adding prefix" );

      StringBuilder affix = new StringBuilder();
      for ( String s : getParams() ) {
         if ( s.equals( "${auto-datauri}" ) ) {
            s = detect_prefix();
         }
         affix.append( s );
      }
      if ( affix.length() <= 0 ) return;

      getBlock().setText( getBlock().getText().insert( 0, affix ) );
      log( Level.FINEST, "Added {0} characters as prefix", affix.length() );
   }

   private static MimetypesFileTypeMap mimeMap;

   private String detect_prefix() {
      Directive parent = getDirective();
      String lastFile = null;
      Charset charset = null;
      boolean hasBase64 = false;
      for ( Task task : parent.getTasks() ) {
         if ( task == this ) break;
         switch ( task.getAction() ) {
            case FILE:
               lastFile = NullData.last( task.getParams() );
               charset = null;
               hasBase64 = false;
               break;

            case BINARY:
            case TEXT:
               charset = getBlock().getCurrentCharset();
               break;

            case ENCODE:
               if ( task.getParams().contains( "base64" ) )
                  hasBase64 = true;
               break;
         }
      }
      if ( lastFile == null ) {
         log( Level.WARNING, "Auto-detect datauri failed because no file is found: {0}", getDirective() );
         return "";
      }

      File f = new File( getBlock().getBasePath(), lastFile.toLowerCase() );
      StringBuilder result = new StringBuilder( 48 ).append( "data:" ).append( getContentType( f ) );
      if ( charset != null ) result.append( ';' ).append( charset.toString() );
      if ( hasBase64 ) result.append( ";base64" );
      result.append( ',' );

      log( Level.FINEST, "Auto-detected datauri prefix of {0} as {1}", f, result );
      return result.toString();
   }

   private String getContentType(File f) {
      synchronized ( TaskPrefix.class ) {
         if ( mimeMap == null ) mimeMap = new MimetypesFileTypeMap();
         return mimeMap.getContentType( f );
      }
   }
}