package sheepy.cocodoc.worker.task;

import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.cocodoc.worker.directive.Directive;
import sheepy.cocodoc.worker.parser.ParserHtml;
import sheepy.util.Escape;
import sheepy.util.collection.CollectionPredicate;

public class TaskHtml extends Task {
   @Override public Action getAction () { return Action.HTML; }

   private static final Predicate<List<String>> validate = CollectionPredicate.<List<String>>size( 0, 1 );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "html() task should have no parameters."; }

   private String VAR_HEADER = "__html.headers__";

   @Override protected void run () {
      if ( ! hasParams() ) {
         parse();
      } else {
         switch ( getParam( 0 ).toLowerCase() ) {
            case "toc":
               if ( getDirective().getAction() != Directive.Action.POSTPROCESS ) {
                  getBlock().setText( "<?coco-postprocess " + toString() + " ?>" );
               } else {
                  outputTOC();
               }
               break;
         }
      }
   }

   private void parse () {
      getBlock().setName( "Parse HTML" );
      log( Level.FINER, "Parsing HTML tags" );
      ParserHtml p = new ParserHtml();
      p.start( getBlock() );
      getBlock().stats().setVar( VAR_HEADER, p.getHeaders() );
      log( Level.FINEST, "Parsed {0} HTML headers", p.getHeaderCount() );
   }

   private int count;

   private void outputTOC() {
      getBlock().setName( "Table of Content" );
      log( Level.FINER, "Generating Table of Content" );
      count = 0;
      ParserHtml.Header title = (ParserHtml.Header) getBlock().stats().getVar( VAR_HEADER );
      StringBuilder buf = new StringBuilder();
      buf.append( "<ol class='h0'>" );
      if ( title != null )
         recurTOC( title.children, buf );
      buf.append( "\n</ol>" );
      getBlock().setText( buf );
      log( Level.FINEST, "Generated Table of Content with {0} items.", count );
   }

   private void recurTOC( List<ParserHtml.Header> list, StringBuilder buf ) {
      if ( list == null ) return;
      for ( ParserHtml.Header h : list ) {
         ++count;
         String content = h.html.toString();
         content = content.replaceAll( "<a[^>]+>|</a>", "" ); // Removes <a>
         content = content.substring( 3, content.length()-5 ); // Removes <h1 ... </h1>
         buf.append( "\n<li class='h" ).append( h.level ).append( "'>" );
         buf.append( "<a href=\"#" ).append( Escape.xml( h.id ) ).append( '"' ).append( content ).append( "</a>" );
         if ( h.children != null && ! h.children.isEmpty() ) {
            buf.append( "\n<ol class='h" ).append( h.level ).append( "'>" );
            recurTOC( h.children, buf );
            buf.append( "\n</ol>" );
         }
         buf.append( "\n</li>" );
      }
   }
}