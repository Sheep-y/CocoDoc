package sheepy.cocodoc.worker.task;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.cocodoc.CocoRunError;
import static sheepy.util.collection.CollectionPredicate.noDuplicate;
import static sheepy.util.collection.CollectionPredicate.onlyContains;

public class TaskCData extends Task {

   @Override public Action getAction () { return Action.CDATA; }

   private static final String[] validParams = new String[]{ "js","css" };
   private static final Predicate<List<String>> validate = onlyContains( Arrays.asList( validParams ) ).and( noDuplicate() );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "cdata() task can only take 'js' or 'css' as parameters."; }

   @Override protected void run () {
      log( Level.FINER, "Quoting text in CDATA" );

      StringBuilder text = getBlock().getText();
      if ( text.length() <= 0 ) return;
      if ( text.indexOf( "<![CDATA[" ) >= 0 || text.indexOf( "]]>" ) >= 0 )
         throwOrWarn( new CocoRunError( "cdata() task cannot wrap content containing cdata" ) );

      String param = getParam( 0 );
      if ( param != null ) param = param.toLowerCase();

      if ( "js".equals( param ) || "css".equals( param ) )
         text.insert( 0, "/*<![CDATA[*/" ).append( "/*]]>*/" );
      else
         text.insert( 0, "<![CDATA[" ).append( "]]>" );

      getBlock().setText( text );

      log( Level.FINEST, "Quoted text in CDATA" );
   }
}