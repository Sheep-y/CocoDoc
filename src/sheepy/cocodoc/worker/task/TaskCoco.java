package sheepy.cocodoc.worker.task;

import java.util.List;
import java.util.function.Predicate;
import sheepy.cocodoc.worker.error.CocoParseError;
import sheepy.cocodoc.worker.parser.coco.ParserCoco;

public class TaskCoco extends Task {
   private String startTag = null;
   private String endTag = null;

   public Action getAction () { return Action.COCO; }

   @Override protected Predicate<List<String>> validParam() { return isEmpty; }
   @Override protected String invalidParamMessage() { return "coco() task should have no parameters."; }

   @Override public void init() {
      super.init();
      if ( ! hasParams() ) return;
      if ( params.size() > 2 ) throw new CocoParseError( "coco() task accepts up to three parameters: \"noerr\", start tag, and optional end tag. Given: " + getParamText() );
      setStartTag( params.get( 0 ) );
      setEndTag( params.get( params.size()-1 ) );
   }

   @Override public void run () {
      try ( ParserCoco parser = new ParserCoco( startTag, endTag ) ) {
         CharSequence result = parser.parse( getBlock() );
         if ( result != null ) getBlock().setText( result );
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