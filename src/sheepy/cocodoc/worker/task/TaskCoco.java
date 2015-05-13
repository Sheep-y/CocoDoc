package sheepy.cocodoc.worker.task;

import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.worker.parser.coco.ParserCoco;

public class TaskCoco extends Task {
   private String startTag = null; // Start/end tag support is included in code but *never* tested.
   private String endTag = null;

   public Action getAction () { return Action.COCO; }

   @Override protected Predicate<List<String>> validParam() { return isEmpty; }
   @Override protected String invalidParamMessage() { return "coco() task should have no parameters."; }

   @Override public void init() {
      super.init();
      if ( ! hasParams() ) return;
      //if ( params.size() > 2 ) throwOrWarn( new CocoParseError( "coco() task accepts up to three parameters: \"noerr\", start tag, and optional end tag. Given: " + getParamText() ) );
      setStartTag( params.get( 0 ) );
      setEndTag( params.get( params.size()-1 ) );
   }

   @Override protected void run () {
      log( Level.FINER, "Parsing Coco direcitve" );
      try ( ParserCoco parser = new ParserCoco( startTag, endTag ) ) {
         parser.start( getBlock() );
         log( Level.FINEST, "Executing Coco direcitves" );
         getBlock().setText( parser.get() );
      } catch ( CocoRunError | CocoParseError ex ) {
         throwOrWarn( ex );
      }
   }

   public String getStartTag() { return startTag; }
   public void setStartTag(String start_tag) { this.startTag = start_tag; }

   public String getEndTag() { return endTag; }
   public void setEndTag(String end_tag) { this.endTag = end_tag; }

   public void setTags( String start, String end ) {
      setStartTag( start );
      setEndTag( end );
   }
}